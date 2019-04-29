package com.example.monica.b_cycle.services;

import android.location.Location;

import com.example.monica.b_cycle.model.Route;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class LocationUtils {
    /**
     * Adds more points, at a constant distance, to the route to be saved
     *
     * @param route
     */
    public static void expandPath(Route route, int min_distance_in_meters) {
        List<LatLng> pointList = route.getPointList();
        LocationUtils locationUtils = new LocationUtils();
        for (int i = 0; i < pointList.size() - 1; i++) {
            float distance[] = new float[1];
            LatLng pointA = pointList.get(i);
            LatLng pointB = pointList.get(i + 1);
            Location.distanceBetween(pointA.latitude, pointA.longitude,
                    pointB.latitude, pointB.longitude, distance);
            if (distance[0] > min_distance_in_meters) {
                pointList.add(i + 1, locationUtils.getNewPointBetweenAAndB(pointA, pointB, distance[0], min_distance_in_meters));
            }
        }
    }

    private LatLng getNewPointBetweenAAndB(LatLng pointA, LatLng pointB, float distance, int min_distance_in_meters) {
        LatLng vector = new LatLng((float) (pointB.latitude - pointA.latitude) / distance, (float) (pointB.longitude - pointA.longitude) / distance);
        LatLng pointC = new LatLng(pointA.latitude + vector.latitude * min_distance_in_meters, pointA.longitude + vector.longitude * min_distance_in_meters);
        return pointC;
    }
}
