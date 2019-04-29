package com.example.monica.b_cycle.services;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.example.monica.b_cycle.LoginActivity;
import com.example.monica.b_cycle.MapsActivity;
import com.example.monica.b_cycle.model.PendingRoute;
import com.example.monica.b_cycle.model.PendingRouteDao;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.RouteDao;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.example.monica.b_cycle.MapsActivity.bikeRoutes;

public class DatabaseService {

    private DatabaseReference mRootRef;
    private RouteDaoMapper mapper = new RouteDaoMapper();
    private PendingRouteMapper pendingRouteMapper = new PendingRouteMapper();
    private MapsActivity mapsActivity;
    private final int MIN_DISTANCE_IN_METERS = 100;

    private List<PendingRouteDao> pendingRouteDaos;

    public DatabaseService(MapsActivity mapsActivity) {
        this.mRootRef = FirebaseDatabase.getInstance().getReference();
        this.mapsActivity = mapsActivity;
        this.pendingRouteDaos = new ArrayList<>();
        readData();
        addChildListeners();
    }

    private void readData() {
        mRootRef.child("approved").addListenerForSingleValueEvent(new ValueEventListener() {
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
        mRootRef.child("pending").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                PendingRouteDao pendingRouteDao = dataSnapshot.getValue(PendingRouteDao.class);
                if (pendingRouteDao != null) {
                    pendingRouteDao.setKey(dataSnapshot.getKey());
                    if (pendingRouteDao.getEmails().contains(LoginActivity.email)) {
                        bikeRoutes.add(mapper.map(pendingRouteDao.getRouteDao()));
                    } else {
                        pendingRouteDaos.add(pendingRouteDao);
                    }
                }

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

    public void startReview() {

        boolean thereAreRoutesByOtherUsers = pendingRouteDaos.stream()
                .anyMatch(pendingRouteDao -> !pendingRouteDao.getEmails().contains(LoginActivity.email)
                        && !pendingRouteDao.getFlags().contains(LoginActivity.email));

        if (thereAreRoutesByOtherUsers) {
            @SuppressLint("ValidFragment")
            class RouteReviewDialog extends DialogFragment {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("New bike lanes available in your area");
                    builder.setMessage("Would you like to review them?")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    List<PendingRoute> pendingRoutes = new ArrayList<>();
                                    pendingRouteDaos.forEach(pendingRouteDao ->
                                            pendingRoutes.add(pendingRouteMapper.map(pendingRouteDao)));
                                    mapsActivity.startReview(pendingRoutes);
                                }
                            })
                            .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    return builder.create();
                }
            }
            new RouteReviewDialog().show(mapsActivity.getSupportFragmentManager(), "Starting Route Review");
        }
    }

    /**
     * Saves the given Route object to the database
     *
     * @param route
     */
    public void addToDatabase(Route route) {
        DatabaseReference newRouteRef = mRootRef.child("pending").push();
        LocationUtils.expandPath(route, 100);
//        newRouteRef.setValue(route);
        newRouteRef.setValue(new PendingRouteDao(LoginActivity.email, LoginActivity.email, mapper.map(route)));
    }

    public void updateRoute(PendingRoute pendingRoute) {
        PendingRouteDao pendingRouteDao = pendingRouteMapper.map(pendingRoute);
        if (pendingRouteDao.getEmails().size() >= 5) {
            DatabaseReference databaseReference= mRootRef.child("approved").push();
            databaseReference.setValue(pendingRouteDao.getRouteDao());
            mRootRef.child("pending").child(pendingRoute.getKey()).removeValue();
        } else if (pendingRouteDao.getFlags().size() >= 3) {
            mRootRef.child("pending").child(pendingRoute.getKey()).removeValue();
        }else{
            mRootRef.child("pending")
                    .child(pendingRouteDao.getKey())
                    .child("emails")
                    .setValue(pendingRouteDao.getEmails());
            mRootRef.child("pending")
                    .child(pendingRouteDao.getKey())
                    .child("flags")
                    .setValue(pendingRouteDao.getFlags());
        }
    }
}

