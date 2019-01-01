package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.Route;

import java.util.List;

public interface RouteFinderListener {
    /**
     * Method to be called by a RouteFinder instance.
     * Once a call has been successfully made to the Google Directions URL
     * and the received JSON has been successfully parsed, the RouteFinder
     * instance calls this method.
     * @param routes
     */
    void onRouteFinderSuccess(List<Route> routes);
}
