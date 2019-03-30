package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.Route;
import com.google.android.gms.maps.model.Polyline;

import java.util.List;

public interface RouteBuilderListener {
    void onFinish(Route finalRoute);
    void onRouteNotFound();
    void onPartialRouteFound(Route partialRoute, List<Polyline> polylines);
}
