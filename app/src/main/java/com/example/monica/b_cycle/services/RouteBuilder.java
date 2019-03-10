package com.example.monica.b_cycle.services;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.widget.TextView;

import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.model.Distance;
import com.example.monica.b_cycle.model.Duration;
import com.example.monica.b_cycle.model.Elevation;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RouteBuilder implements RouteFinderListener, ElevationFinderListener {

    private final double POINT_TO_LINE_TOLERANCE_IN_METERS = 20;
    private final int POINT_TO_POINT_TOLERANCE_IN_METERS = 100;

    private List<PatternItem> polylinePattern = Arrays.asList(new Dot(), new Gap(20));
    private GoogleMap mMap;
    private GraphView mGraph;
    private TextView mDistance;
    private TextView mDuration;
    private List<Route> allRoutes;
    private List<LatLng> allBikePoints;
    private PolylineOptions bikeRoute;

    public RouteBuilder(LatLng origin, LatLng destination, GoogleMap mMap, GraphView mGraph, TextView mDistance, TextView mDuration) {
        new RouteFinder(origin, destination, TravelMode.DRIVING, this).findRoute();
        this.mMap = mMap;
        this.mGraph = mGraph;
        this.mDistance = mDistance;
        this.mDuration = mDuration;
        allRoutes = new ArrayList<>();
        allBikePoints = new ArrayList<>();
        MapsActivity.bikeRoutes.forEach(route -> {
            allBikePoints.addAll(route.getPointList());
        });
    }

    /**
     * Draws the combined route formed of Road + Bicycle lanes
     *
     * @param routes
     */
    private void drawDriveAndBikeRoute(List<Route> routes) {
        double bikingDistanceInKm = 0;
        Route drivingRoute = routes.get(0);

        PolylineOptions bikePoly = getNewBikePoly();
        PolylineOptions roadPoly = getNewRoadPoly();
        List<LatLng> drivingRoutePointList = drivingRoute.getPointList();

        LatLng lastPoint = drivingRoutePointList.get(0);
        boolean lastPointOnBikeTrail = false;
        for (LatLng drivingPoint : drivingRoutePointList) {

            boolean pointOnBikeTrail = false;
            for (LatLng bikePoint : allBikePoints) {

                if (arePointsClose(drivingPoint, bikePoint) && PolyUtil.distanceToLine(bikePoint, lastPoint, drivingPoint) < POINT_TO_LINE_TOLERANCE_IN_METERS) {
                    pointOnBikeTrail = true;
                    bikingDistanceInKm += 0.1;
                    break;
                }
            }
            lastPoint = drivingPoint;

            if (!pointOnBikeTrail) {
                roadPoly.add((drivingPoint));

                if (lastPointOnBikeTrail) {
                    bikePoly.add(drivingPoint);
                }
                mMap.addPolyline(bikePoly);
                bikePoly = getNewBikePoly();
                lastPointOnBikeTrail = false;
            } else {
                bikePoly.add(drivingPoint);
                if (!lastPointOnBikeTrail) {
                    roadPoly.add(drivingPoint);
                }
                mMap.addPolyline(roadPoly);
                roadPoly = getNewRoadPoly();
                lastPointOnBikeTrail = true;
            }
        }
        setDistance(drivingRoute);
        setDuration(drivingRoute, bikingDistanceInKm, (drivingRoute.getDistance().getValue() / 1000.0)- bikingDistanceInKm);

        mMap.addPolyline(roadPoly);
        mMap.addPolyline(bikePoly);
    }

    /**
     * @return New custom Polyline signifying Road areas
     */
    private PolylineOptions getNewRoadPoly() {
        return new PolylineOptions()
                .geodesic(true)
                .color(Color.rgb(0, 128, 255))
                .pattern(polylinePattern).width(10);
    }

    /**
     * @return New custom Polyline signifying Bike Lanes
     */
    private PolylineOptions getNewBikePoly() {
        return new PolylineOptions()
                .geodesic(true)
                .color(Color.rgb(0, 128, 255))
                .width(12);
    }

    /**
     * Returns the routes with Travel Mode set on DRIVING
     *
     * @return
     */
    private List<Route> getDrivingRoutes() {
        return allRoutes.stream()
                .filter(route -> route.getTravelMode() == TravelMode.DRIVING)
                .collect(Collectors.toList());
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

    private void setDistance(Route route) {
        mDistance.setText(route.getDistance().getText());
    }

    private void setDuration(Route route, double bikingDistance, double drivingDistance) {
        double durationInHours = bikingDistance / TravelMode.BICYCLING.getDefaultSpeed() + drivingDistance / TravelMode.DRIVING.getDefaultSpeed();
        String duration = "";
        if (durationInHours > (int) durationInHours) {
            int minutes = (int) ((durationInHours - (int) durationInHours) * 60);
            if ((int) durationInHours == 0) {
                duration = String.valueOf(minutes) + "min";
            } else {
                duration = String.valueOf((int) durationInHours) + "h " + String.valueOf(minutes) + "min";
            }
        } else duration = String.valueOf(durationInHours)+ "h";
        route.setDuration(new Duration(duration, (int) durationInHours));
        mDuration.setText(route.getDuration().getText());
    }

    private void drawElevations(List<Elevation> elevations) {
        mGraph.removeAllSeries();
        DataPoint[] dataPoints = new DataPoint[elevations.size()];
        for (int i = 0; i < elevations.size(); i++) {
            dataPoints[i] = new DataPoint(i, elevations.get(i).getElevation().intValue());
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        series.setDrawBackground(true);
        series.setDrawDataPoints(true);
        series.setThickness(8);

        mGraph.addSeries(series);
    }

    /**
     * Method overrun from RouteFinderListener.
     * Adds all routes to class variable allRoutes.
     * Calls drawing method appropriately.
     *
     * @param routes
     */
    @Override
    public void onRouteFinderSuccess(List<Route> routes) {
        new ElevationFinder(routes.get(0), this).findRoute();
        allRoutes.add(routes.get(0));
        drawDriveAndBikeRoute(getDrivingRoutes());
    }

    @Override
    public void onElevationFinderSuccess(List<Elevation> elevations) {
        drawElevations(elevations);
    }
}