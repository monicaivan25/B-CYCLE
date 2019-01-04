package com.example.monica.b_cycle.services;

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

public class DatabaseService implements RouteFinderListener {

    private DatabaseReference mRootRef;
    private RouteDaoMapper mapper = new RouteDaoMapper();

    public DatabaseService() {
        this.mRootRef = FirebaseDatabase.getInstance().getReference();
        readData();
        addChildListeners();
    }

    private void readData() {
        mRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    RouteDao route = child.getValue(RouteDao.class);
                    Route r = mapper.map(route);
                    MapsActivity.bikeRoutes.add(mapper.map(route));
                    Log.d("LATIPULA", r.toString());
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

    public void addToDatabase(LatLng origin, LatLng destination) {
        new RouteFinder(origin, destination, TravelMode.DRIVING, this).findRoute();
    }

    @Override
    public void onRouteFinderSuccess(List<Route> routes) {
        DatabaseReference newRouteRef = mRootRef.push();
        newRouteRef.setValue(mapper.map(routes.get(0)));
    }
}