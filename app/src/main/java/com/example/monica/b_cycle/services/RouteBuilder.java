package com.example.monica.b_cycle.services;

import android.graphics.Color;
import android.location.Location;

import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RouteBuilder implements RouteFinderListener {

    private final double POINT_TO_LINE_TOLERANCE_IN_METERS = 20;
    private final int POINT_TO_POINT_TOLERANCE_IN_METERS = 100;


    private List<PatternItem> polylinePattern = Arrays.asList(new Dot(), new Gap(20));
    private GoogleMap mMap;
    private ElevationFinder elevationFinder;
    private List<Route> allRoutes;
    private List<LatLng> allBikePoints;
    private PolylineOptions bikeRoute;

    public RouteBuilder(LatLng origin, LatLng destination, GoogleMap mMap) {
        new RouteFinder(origin, destination, TravelMode.DRIVING, this).findRoute();
        new RouteFinder(origin, destination, TravelMode.WALKING, this).findRoute();
        this.mMap = mMap;
        allRoutes = new ArrayList<>();
        allBikePoints = new ArrayList<>();
        MapsActivity.bikeRoutes.forEach(route -> {
            allBikePoints.addAll(route.getPointList());
        });
    }

    private void drawWalkAndBikeRoute(List<Route> routes) {
        List<LatLng> allBikePoints = new ArrayList<>();
        MapsActivity.bikeRoutes.forEach(route -> {
            allBikePoints.addAll(route.getPointList());
        });
        Route route = routes.get(0);

        PolylineOptions bikePoly = getNewBikePoly();
        PolylineOptions roadPoly = getNewRoadPoly();
        List<LatLng> points = route.getPointList();

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(route.getPointList());

        boolean lastPointOnBikeTrail = false;
        for (LatLng point : points) {
            boolean pointOnBikeTrail = false;

            for (LatLng bikePoint : allBikePoints) {
                if (PolyUtil.containsLocation(bikePoint, points, true)) {
                    pointOnBikeTrail = true;
                    break;
                }
            }
            if (!pointOnBikeTrail) {
                roadPoly.add((point));

                if (lastPointOnBikeTrail) {
                    bikePoly.add(point);
                }
                mMap.addPolyline(bikePoly);
                bikePoly = getNewBikePoly();
                lastPointOnBikeTrail = false;
            } else {
                bikePoly.add(point);
                if (!lastPointOnBikeTrail) {
                    roadPoly.add(point);
                }
                mMap.addPolyline(roadPoly);
                roadPoly = getNewRoadPoly();
                lastPointOnBikeTrail = true;
            }
        }
        mMap.addPolyline(roadPoly);
        mMap.addPolyline(bikePoly);
    }


    /**
     * Draws the combined route formed of Road + Bicycle lanes
     *
     * @param routes
     */
    private void drawDriveAndBikeRoute(List<Route> routes) {

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
        mMap.addPolyline(roadPoly);
        mMap.addPolyline(bikePoly);
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
        allRoutes.add(routes.get(0));
        drawDriveAndBikeRoute(getDrivingRoutes());
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
     * @return New custom Polyline signifying Sidewalks
     */
    private PolylineOptions getNewWalkPoly() {
        return new PolylineOptions()
                .geodesic(true)
                .color(Color.rgb(0, 128, 0))
                .pattern(polylinePattern).width(10);
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
     * Returns the routes with Travel Mode set on WALKING
     *
     * @return
     */
    public List<Route> getWalkingRoutes() {
        return allRoutes.stream()
                .filter(route -> route.getTravelMode() == TravelMode.WALKING)
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

    public List<PatternItem> getPolylinePattern() {
        return polylinePattern;
    }

    public void setPolylinePattern(List<PatternItem> polylinePattern) {
        this.polylinePattern = polylinePattern;
    }

    public GoogleMap getmMap() {
        return mMap;
    }

    public void setmMap(GoogleMap mMap) {
        this.mMap = mMap;
    }

    public ElevationFinder getElevationFinder() {
        return elevationFinder;
    }

    public void setElevationFinder(ElevationFinder elevationFinder) {
        this.elevationFinder = elevationFinder;
    }

    public List<Route> getAllRoutes() {
        return allRoutes;
    }

    public void setAllRoutes(List<Route> allRoutes) {
        this.allRoutes = allRoutes;
    }

    public PolylineOptions getBikeRoute() {
        return bikeRoute;
    }

    public void setBikeRoute(PolylineOptions bikeRoute) {
        this.bikeRoute = bikeRoute;
    }

}