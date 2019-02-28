package com.example.monica.b_cycle.services;

import android.graphics.Color;

import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.CameraUpdateFactory;
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

    private final double TOLERANCE = 0.01;
    private final double MIN_ERROR = 0.01;

    private final double MAX_COMFORTABLE_DISTANCE = 0.001;
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

        List<LatLng> allBikePoints = new ArrayList<>();
        MapsActivity.bikeRoutes.forEach(route -> {
            allBikePoints.addAll(route.getPointList());
        });
        Route route = routes.get(0);

        PolylineOptions bikePoly = getNewBikePoly();
        PolylineOptions roadPoly = getNewRoadPoly();
        List<LatLng> points = route.getPointList();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
        boolean lastPointOnBikeTrail = false;
        LatLng lastPoint = points.get(0);
        for (LatLng point : points) {
//            mMap.addMarker(new MarkerOptions().position(lastPoint).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

            boolean pointOnBikeTrail = false;
            for (LatLng bikePoint : allBikePoints) {
//                mMap.addMarker(new MarkerOptions().position(bikePoint));

                if (arePointsClose(bikePoint, lastPoint, point)) {
                    pointOnBikeTrail = true;
//                    mMap.addMarker(new MarkerOptions().position(bikePoint));
                    break;
                }
            }
            lastPoint = point;

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


    /**
     * Function to determine whether a point is close enough to the line created
     * by two other points.
     *
     * @param pointX
     * @param pointB
     * @param pointA
     * @return true or false as appropriate
     */
    public boolean arePointsClose(LatLng pointX, LatLng pointB, LatLng pointA) {
        if (pointA.latitude == pointB.latitude) {
            if ((pointX.latitude - pointA.latitude <= TOLERANCE) && longitudeBetween(pointX, pointA, pointB)) {
                return true;
            }
        } else if (pointA.longitude == pointB.longitude) {
            if ((pointX.longitude - pointA.longitude <= TOLERANCE) && latitudeBetween(pointX, pointA, pointB)) {
                return true;
            }
        } else if (doesPointBelongOnLine(pointX, pointA, pointB, 0.05) && !tooFar(pointX, pointA, pointB)) {
            return true;
        }
        return false;
    }

    boolean doesPointBelongOnLine(LatLng point, LatLng a, LatLng b, Double tolerance) {

        double x = Math.abs((point.longitude - b.longitude) / (a.longitude - b.longitude) -
                (point.latitude - b.latitude) / (a.latitude - b.latitude));
        return x <= tolerance;
    }

    boolean longitudeBetween(LatLng x, LatLng a, LatLng b) {
        return (((x.longitude < a.longitude) && (x.longitude > b.longitude))
                || ((x.longitude > a.longitude) && (x.longitude < b.longitude))
                || Math.abs(a.longitude - x.longitude) <= TOLERANCE
                || Math.abs(b.longitude - x.longitude) <= TOLERANCE);
    }

    boolean latitudeBetween(LatLng x, LatLng a, LatLng b) {
        return (((x.latitude < a.latitude) && (x.latitude > b.latitude))
                || ((x.latitude > a.latitude) && (x.latitude < b.latitude))
                || Math.abs(a.latitude - x.latitude) <= TOLERANCE
                || Math.abs(b.latitude - x.latitude) <= TOLERANCE);
    }

    double distanceFromPointToLine(LatLng x, LatLng a, LatLng b) {
        return Math.abs((b.longitude - a.longitude) * x.longitude
                - (b.latitude - a.latitude) * x.latitude
                + b.latitude * a.longitude - b.longitude * a.latitude)
                / Math.sqrt((b.longitude - a.longitude) * (b.longitude - a.longitude)
                + (b.latitude - a.latitude) * (b.latitude - a.latitude));
    }

    boolean tooFar(LatLng x, LatLng a, LatLng b) {
        if (Math.min(
                Math.abs(x.longitude - a.longitude),
                Math.abs(x.longitude - b.longitude)) > TOLERANCE)
            return true;
        if (Math.min(
                Math.abs(x.latitude - a.latitude),
                Math.abs(x.latitude - b.latitude)) > TOLERANCE)
            return true;
        return false;
    }

}