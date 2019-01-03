package com.example.monica.b_cycle.services;

import android.graphics.Color;

import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteBuilder implements RouteFinderListener {

    private final double MAX_COMFORTABLE_DISTANCE = 0.00005;
    private List<Route> drivingRoutes;
    private List<Route> walkingRoutes;
    private List<Polyline> polylinePaths = new ArrayList<>();
    private List<PatternItem> polylinePattern = Arrays.asList(new Dot(), new Gap(20));
    private GoogleMap mMap;
    private ElevationFinder elevationFinder;
    private List<Route> allRoutes;
    private PolylineOptions bikeRoute;

    public RouteBuilder(LatLng origin, LatLng destination, GoogleMap mMap) {
        new RouteFinder(origin, destination, TravelMode.DRIVING, this).findRoute();
        new RouteFinder(origin, destination, TravelMode.WALKING, this).findRoute();
        this.mMap = mMap;
        allRoutes = new ArrayList<>();
    }

    private void drawWalkAndBikeRoute(List<Route> routes){
        bikeRoute = new PolylineOptions().
                geodesic(true).
                color(Color.RED).width(10);
        PolylineOptions bikeRoute2 = new PolylineOptions().
                geodesic(true).
                color(Color.RED).width(10);
        List<LatLng> bikePoints = new ArrayList<>();

        polylinePaths = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.getOrigin().getLocation(), 16));

            mMap.addMarker(new MarkerOptions()
                    .position(route.getOrigin().getLocation()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.rgb(0, 127, 255)).
                    pattern(polylinePattern).
                    width(10);
            for (int i = 0; i < route.getPointList().size(); i++) {
                polylineOptions.add(route.getPointList().get(i));
                if (i > 2 && i < 7)
                    bikeRoute.add(route.getPointList().get(i));
                if (i > 20 && i < 57)
                    bikeRoute2.add(route.getPointList().get(i));
            }
            bikePoints = bikeRoute.getPoints();
            bikePoints.addAll(bikeRoute2.getPoints());
            Polyline road = mMap.addPolyline(polylineOptions);
            polylinePaths.add(road);
            road.remove();
        }
        List<LatLng> finalBikePoints = bikePoints;
        for (Polyline polyline : polylinePaths) {
            PolylineOptions bikePoly = getNewBikePoly();
            PolylineOptions roadPoly = getNewWalkPoly();
            List<LatLng> points = polyline.getPoints();
            boolean lastPointOnBikeTrail = false;
            for (LatLng point : points) {
                boolean pointOnBikeTrail = false;

                for (LatLng bikePoint : finalBikePoints) {
                    if (pointsAreClose(bikePoint, point)) {
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
                    roadPoly = getNewWalkPoly();

                    lastPointOnBikeTrail = true;
                }
            }
            mMap.addPolyline(roadPoly);
            mMap.addPolyline(bikePoly);
        }
    }

    private void drawDriveAndBikeRoute(List<Route> routes) {
        bikeRoute = new PolylineOptions().
                geodesic(true)
                .width(10);
        PolylineOptions bikeRoute2 = new PolylineOptions().
                geodesic(true)
                .width(10);
        List<LatLng> bikePoints = new ArrayList<>();

        polylinePaths = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.getOrigin().getLocation(), 16));

            mMap.addMarker(new MarkerOptions()
                    .position(route.getOrigin().getLocation()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.rgb(0, 127, 255)).
                    pattern(polylinePattern).
                    width(10);
            for (int i = 0; i < route.getPointList().size(); i++) {
                polylineOptions.add(route.getPointList().get(i));
                if (i > 2 && i < 7)
                    bikeRoute.add(route.getPointList().get(i));
                if (i > 20 && i < 57)
                    bikeRoute2.add(route.getPointList().get(i));
            }
            bikePoints = bikeRoute.getPoints();
            bikePoints.addAll(bikeRoute2.getPoints());
            Polyline road = mMap.addPolyline(polylineOptions);
            polylinePaths.add(road);
            road.remove();
        }
        List<LatLng> finalBikePoints = bikePoints;


        for (Polyline polyline : polylinePaths) {
            PolylineOptions bikePoly = getNewBikePoly();
            PolylineOptions roadPoly = getNewRoadPoly();
            List<LatLng> points = polyline.getPoints();
            boolean lastPointOnBikeTrail = false;
            for (LatLng point : points) {
                boolean pointOnBikeTrail = false;

                for (LatLng bikePoint : finalBikePoints) {
                    if (pointsAreClose(bikePoint, point)) {
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
    }

    /**
     * Method overrun from RouteFinderListener.
     * Adds all routes to class variable allRoutes.
     * Calls drawing method appropriately.
     * @param routes
     */
    @Override
    public void onRouteFinderSuccess(List<Route> routes) {
        allRoutes.addAll(routes);
        drawDriveAndBikeRoute(routes);
        drawWalkAndBikeRoute(routes);
        //TODO: split functionalities of walking and driving routes & optimise alg for route building

    }

    /**
     * Function to determine whether two points are close enough to prioritise
     * the one on a bike lane. This is also determined by MAX_COMFORTABLE_DISTANCE
     * @param point
     * @param secondPoint
     * @return true or false as appropriate
     */
    private boolean pointsAreClose(LatLng point, LatLng secondPoint) {
        if ((Math.abs(point.latitude - secondPoint.latitude) <= MAX_COMFORTABLE_DISTANCE) && (Math.abs(point.longitude - secondPoint.longitude) <= MAX_COMFORTABLE_DISTANCE))
            return true;
        return false;
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
                .color(Color.rgb(0, 128, 128))
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


    public List<Route> getDrivingRoutes() {
        return drivingRoutes;
    }

    public void setDrivingRoutes(List<Route> drivingRoutes) {
        this.drivingRoutes = drivingRoutes;
    }

    public List<Route> getWalkingRoutes() {
        return walkingRoutes;
    }

    public void setWalkingRoutes(List<Route> walkingRoutes) {
        this.walkingRoutes = walkingRoutes;
    }

    public List<Polyline> getPolylinePaths() {
        return polylinePaths;
    }

    public void setPolylinePaths(List<Polyline> polylinePaths) {
        this.polylinePaths = polylinePaths;
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
