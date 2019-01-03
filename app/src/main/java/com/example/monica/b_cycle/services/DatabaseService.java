package com.example.monica.b_cycle.services;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import static com.example.monica.b_cycle.MapsActivity.bla;

public class DatabaseService implements RouteFinderListener {

    private DatabaseReference mRootRef;
    private int numberOfRoutes;

    public DatabaseService() {
        this.mRootRef = FirebaseDatabase.getInstance().getReference();
        mRootRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
              //TODO: notify user that a new bike trail is available for review.
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
        numberOfRoutes = 0;
    }

    public void addToDatabase(LatLng origin, LatLng destination) {
        new RouteFinder(origin, destination, TravelMode.DRIVING, this).findRoute();
    }

    @Override
    public void onRouteFinderSuccess(List<Route> routes) {
        mRootRef.child("route" + numberOfRoutes).setValue(routes.get(0));
        numberOfRoutes ++;
    }

}
