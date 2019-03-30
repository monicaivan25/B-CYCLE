package com.example.monica.b_cycle.services;

import android.graphics.Color;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.R;
import com.example.monica.b_cycle.model.Duration;
import com.example.monica.b_cycle.model.Elevation;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteBuilder implements RouteFinderListener, ElevationFinderListener {

    private final double POINT_TO_LINE_TOLERANCE_IN_METERS = 20;
    private final int POINT_TO_POINT_TOLERANCE_IN_METERS = 100;
    private final int INTERSECTION_TOLERANCE_IN_POINTS = 2;

    private List<PatternItem> polylinePattern = Arrays.asList(new Dot(), new Gap(20));
    private GoogleMap mMap;
    private GraphView mGraph;
    private TextView mDistance;
    private TextView mDuration;
    private List<LatLng> allBikePoints;
    private RouteBuilderListener routeBuilderListener;
    private TravelMode travelMode;
    private Boolean customRoute;
    private Route routeFound;

    public RouteBuilder(LatLng origin, LatLng destination, GoogleMap mMap, GraphView mGraph, TextView mDistance, TextView mDuration, RouteBuilderListener routeBuilderListener, TravelMode travelMode, Boolean customRoute) {
        new RouteFinder(origin, destination, travelMode, this).findRoute();
        this.travelMode = travelMode;
        this.customRoute = customRoute;
        this.mMap = mMap;
        this.mGraph = mGraph;
        this.mDistance = mDistance;
        this.routeBuilderListener = routeBuilderListener;
        this.mDuration = mDuration;
        allBikePoints = new ArrayList<>();
        MapsActivity.bikeRoutes.forEach(route -> {
            allBikePoints.addAll(route.getPointList());
        });
    }

    /**
     * Draws the combined route formed of Road + Bicycle lanes or Sidewalk + bicycle lanes
     *
     * @param drivingRoute
     */
    private List<Polyline> drawCombinedRoute(Route drivingRoute) {
        List<Polyline> polylines = new ArrayList<>();

        double bikingDistanceInKm = 0;
        PolylineOptions bikePoly = getNewFullPoly();
        PolylineOptions roadPoly = getNewDottedPoly();
        List<LatLng> drivingRoutePointList = drivingRoute.getPointList();

        LatLng lastPoint = drivingRoutePointList.get(0);
        int bikeTrailPoints = 0;

        for (LatLng drivingPoint : drivingRoutePointList) {
            boolean pointOnBikeTrail = false;
            for (LatLng bikePoint : allBikePoints) {
                if (arePointsClose(drivingPoint, bikePoint) && PolyUtil.distanceToLine(bikePoint, lastPoint, drivingPoint) < POINT_TO_LINE_TOLERANCE_IN_METERS) {
                    pointOnBikeTrail = true;
                    bikeTrailPoints++;
                    bikingDistanceInKm += 0.1;
                    break;
                }
            }
            if (!pointOnBikeTrail) {
                if (bikeTrailPoints <= INTERSECTION_TOLERANCE_IN_POINTS) {
                    roadPoly.addAll(bikePoly.getPoints());
                } else {
                    polylines.add(mMap.addPolyline(bikePoly));
                }

                roadPoly.add(lastPoint);
                roadPoly.add(drivingPoint);

                bikePoly = getNewFullPoly();
            } else {
                bikePoly.add(lastPoint);
                bikePoly.add(drivingPoint);

                polylines.add(mMap.addPolyline(roadPoly));
                roadPoly = getNewDottedPoly();
            }

            lastPoint = drivingPoint;
        }
        setDistance(drivingRoute);
        setDuration(drivingRoute, bikingDistanceInKm, (drivingRoute.getDistance().getValue() / 1000.0) - bikingDistanceInKm);

        polylines.add(mMap.addPolyline(roadPoly));
        polylines.add(mMap.addPolyline(bikePoly));
        return polylines;
    }

    /**
     * @return New custom Polyline signifying Road or Sidewalk areas
     */
    private PolylineOptions getNewDottedPoly() {
        return new PolylineOptions()
                .geodesic(true)
                .color(Color.rgb(215, 101, 63))
                .pattern(polylinePattern).width(15);
    }

    /**
     * @return New custom Polyline signifying Bike Lanes
     */
    private PolylineOptions getNewFullPoly() {
        return new PolylineOptions()
                .geodesic(true)
                .color(Color.rgb(215, 101, 63))
                .width(15);
    }

    /**
     * Function to determine whether two points are within the minimum distance permitted
     *
     * @param pointA
     * @param pointB
     * @return true or false as appropriate
     */
    private boolean arePointsClose(LatLng pointA, LatLng pointB) {
        float distance[] = new float[1];
        Location.distanceBetween(pointA.latitude, pointA.longitude, pointB.latitude, pointB.longitude, distance);
        return distance[0] <= POINT_TO_POINT_TOLERANCE_IN_METERS;
    }

    /**
     * Sets the distance of the given route
     *
     * @param route
     */
    private void setDistance(Route route) {
        mDistance.setText(route.getDistance().getText());
    }

    /**
     * Sets the duration of the given route, based on how the distance travelled on bicycle lanes,
     * the distance travelled on Road/Sidewalk and TODO: Elevation
     *
     * @param route
     * @param bikingDistance
     * @param drivingOrWalkingDistance
     */
    private void setDuration(Route route, double bikingDistance, double drivingOrWalkingDistance) {
        double durationInHours = bikingDistance / TravelMode.BICYCLING.getDefaultSpeed() + drivingOrWalkingDistance / this.travelMode.getDefaultSpeed();
        String duration;
        if (durationInHours > (int) durationInHours) {
            int minutes = (int) ((durationInHours - (int) durationInHours) * 60);
            if ((int) durationInHours == 0) {
                duration = String.valueOf(minutes) + "min";
            } else {
                duration = String.valueOf((int) durationInHours) + "h " + String.valueOf(minutes) + "min";
            }
        } else duration = String.valueOf(durationInHours) + "h";
        route.setDuration(new Duration(duration, (int) durationInHours));
        mDuration.setText(route.getDuration().getText());
    }

    /**
     * Draws the elevation points on the graphView
     *
     * @param elevations
     */
    private void drawElevations(List<Elevation> elevations) {
        mGraph.removeAllSeries();
        DataPoint[] dataPoints = new DataPoint[elevations.size()];
        for (int i = 0; i < elevations.size(); i++) {
            dataPoints[i] = new DataPoint(i, elevations.get(i).getElevation().intValue());
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        series.setColor(Color.rgb(120, 204, 199));
        series.setDrawBackground(true);
        series.setBackgroundColor(Color.argb(66, 120, 204, 199));
        series.setDrawDataPoints(true);
        series.setThickness(8);

        mGraph.addSeries(series);
    }

    /**
     * Method overridden from RouteFinderListener.
     * Creates a new Elevation finder for the first route of the ones received as parameters
     * Calls drawing method.
     *
     * @param routes
     */
    @Override
    public void onRouteFinderSuccess(List<Route> routes) {
        try {
            routeFound = routes.get(0);
            List<Polyline> polylines = drawCombinedRoute(routeFound);
            if (customRoute) {
                routeBuilderListener.onPartialRouteFound(routeFound, polylines);
            } else {
                new ElevationFinder(routeFound, this).findElevations();
            }
        } catch (IndexOutOfBoundsException e) {
            routeBuilderListener.onRouteNotFound();
        }
    }

    /**
     * Method overridden from ElevationFinderListener.
     * Calls drawing method.
     * Calls the routeBuilderListener onFinish() method
     *
     * @param elevations
     */
    @Override
    public void onElevationFinderSuccess(List<Elevation> elevations) {
        drawElevations(elevations);
        routeBuilderListener.onFinish(routeFound);
    }
}