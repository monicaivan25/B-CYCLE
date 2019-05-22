package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.Distance;
import com.example.monica.b_cycle.model.Duration;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.SimpleAddress;
import com.example.monica.b_cycle.model.TravelMode;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class RouteFinder implements Finder {

    private final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json?";
    private final String GOOGLE_API_KEY = "AIzaSyAzEnSbPCaBHN-Txq0JcsoorIdQxPKTHwU";

    private LatLng origin;
    private LatLng destination;
    private TravelMode travelMode;
    private List<Route> routes = new ArrayList<>();
    private RouteFinderListener routeFinderListener;

    RouteFinder(LatLng origin, LatLng destination, TravelMode travelMode, RouteFinderListener routeFinderListener) {
        this.travelMode = travelMode;
        this.origin = origin;
        this.destination = destination;
        this.routeFinderListener = routeFinderListener;
    }

   @Override
    public String createURL(){
        return DIRECTIONS_URL
                + "origin=" + String.valueOf(origin.latitude) + "," + String.valueOf(origin.longitude)
                + "&destination=" + String.valueOf(destination.latitude) + "," + String.valueOf(destination.longitude)
                + "&mode=" + travelMode.getName()
                + "&key=" + GOOGLE_API_KEY;
    }

    /**
     * Creates a new Async instance of JsonDownloader in order to retrieve the details
     * from the URL created.
     */
    void findRoute(){
        new JsonDownloader(this).execute(createURL());
    }

    @Override
    public void parseJson(String data){
        try {
            JSONObject jsonObject = new JSONObject(data);
            JSONArray jsonArray = jsonObject.getJSONArray("routes");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonRoute = jsonArray.getJSONObject(i);
                JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
                JSONObject jsonLeg = jsonRoute.getJSONArray("legs").getJSONObject(0);
                JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
                JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
                JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
                JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");

                Route route = new Route();
                route.setDistance(new Distance(jsonDistance.getString("text"), jsonDistance.getInt("value")));
                route.setDuration(new Duration(jsonDuration.getString("text"), jsonDuration.getInt("value")));
                route.setOrigin(new SimpleAddress(jsonLeg.getString("start_address"), new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng"))));
                route.setDestination(new SimpleAddress(jsonLeg.getString("end_address"), new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng"))));
                route.setPointList(PolyUtil.decode(overview_polylineJson.getString("points")));
                route.setTravelMode(travelMode);

                routes.add(route);
            }
            routeFinderListener.onRouteFinderSuccess(routes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
