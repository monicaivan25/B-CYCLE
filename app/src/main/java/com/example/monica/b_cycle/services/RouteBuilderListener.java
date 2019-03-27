package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.Route;

public interface RouteBuilderListener {
    void onFinish();
    void onRouteNotFound();
    void onPartialRouteFound(Route partialRoute);
}
