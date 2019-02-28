package com.example.monica.b_cycle.services;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.RouteDao;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import static com.example.monica.b_cycle.MapsActivity.bikeRoutes;

public class DatabaseService implements RouteFinderListener {

    private DatabaseReference mRootRef;
    private RouteDaoMapper mapper = new RouteDaoMapper();
    private MapsActivity mMap;
    private final int MIN_DISTANCE_IN_METERS = 100;

    public DatabaseService(MapsActivity mMap) {
        this.mRootRef = FirebaseDatabase.getInstance().getReference();
        this.mMap = mMap;
        readData();
        addChildListeners();
    }

    private void readData() {
        mRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    RouteDao routeDao = child.getValue(RouteDao.class);
                    bikeRoutes.add(mapper.map(routeDao));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addChildListeners() {
        mRootRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //TODO: notify user that a new bike trail is available for review.
                // Route route = dataSnapshot.getValue(Route.class);
                //TODO: Momentan pot adauga rute cu addToDatabase() si AICI primesc notificari cand este adaugata o ruta noua.
                Log.d("YEEEEEEEEEE", dataSnapshot.getValue().toString());
                mMap.notifyUser("New Bike Lane available for review");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private LatLng getNewPointBetweenAandB(LatLng pointA, LatLng pointB, float distance) {
        LatLng vector = new LatLng((float)(pointB.latitude - pointA.latitude)/distance, (float)(pointB.longitude - pointA.longitude)/distance);
        LatLng pointC = new LatLng(pointA.latitude + vector.latitude * MIN_DISTANCE_IN_METERS, pointA.longitude + vector.longitude * MIN_DISTANCE_IN_METERS);
        return pointC;
    }

    private void expandPath(Route route) {
        List<LatLng> pointList = route.getPointList();
        for (int i = 0; i < pointList.size() - 1; i++) {
            float distance[] = new float[1];
            LatLng pointA = pointList.get(i);
            LatLng pointB = pointList.get(i + 1);
            Location.distanceBetween(pointA.latitude, pointA.longitude,
                    pointB.latitude, pointB.longitude, distance);
            if (distance[0] > MIN_DISTANCE_IN_METERS) {
                pointList.add(i + 1, getNewPointBetweenAandB(pointA, pointB, distance[0]));
            }
        }
    }

    public void addToDatabase(LatLng origin, LatLng destination, TravelMode travelMode) {
        new RouteFinder(origin, destination, travelMode, this).findRoute();
    }


    @Override
    public void onRouteFinderSuccess(List<Route> routes) {
        DatabaseReference newRouteRef = mRootRef.push();
        expandPath(routes.get(0));
        newRouteRef.setValue(mapper.map(routes.get(0)));
    }


}