package com.example.monica.b_cycle.services;

import android.util.Log;

import com.example.monica.b_cycle.model.Elevation;
import com.example.monica.b_cycle.model.Route;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ElevationFinder implements Finder {

    private final String ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/json?locations=";
    private final String GOOGLE_API_KEY = "AIzaSyAzEnSbPCaBHN-Txq0JcsoorIdQxPKTHwU";
    private final short NUMBER_OF_ELEVATION_POINTS = 15;

    private List<LatLng> essentialPoints;
    private ElevationFinderListener elevationFinderListener;
    private List<Elevation> elevations = new ArrayList<>();

    ElevationFinder(Route route, ElevationFinderListener elevationFinderListener) {
        this.essentialPoints = getEssentialPoints(route);
        this.elevationFinderListener = elevationFinderListener;
    }

    /**
     * Given a Route, returns a list of 10-11 points, representative of the route given
     *
     * @param route
     * @return List<LatLng>
     */
    private List<LatLng> getEssentialPoints(Route route) {
        List<LatLng> essentialPoints = new ArrayList<>();
        List<LatLng> allPoints = route.getPointList();
        int ratio = allPoints.size() / NUMBER_OF_ELEVATION_POINTS;
        for (int i = 0; i < allPoints.size(); i += ratio) {
            essentialPoints.add(allPoints.get(i));
        }
        return essentialPoints;
    }

    /**
     * Creates the URL needed to call in order to obtain directions
     *
     * @return the URL concatenated with origin, destination, mode and API key
     */
    private String createURL() {
        StringBuilder points = new StringBuilder();
        essentialPoints.forEach(point -> {
            points.append(point.latitude).append(",").append(point.longitude).append("|");
        });
        points.setLength(points.length() - 1);

        return ELEVATION_URL
                + points.toString()
                + "&key=" + GOOGLE_API_KEY;
    }

    /**
     * Creates a new Async instance of JsonDownloader in order to retrieve the details
     * from the URL created.
     */
    void findRoute() {
        Log.d("CACA", createURL());
        new JsonDownloader(this).execute(createURL());
    }

    /**
     * Parses the received data as instances of Route.
     *
     * @param data the JSON as a String
     */
    public void parseJson(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            JSONArray jsonArray = jsonObject.getJSONArray("results");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonResult = jsonArray.getJSONObject(i);
                JSONObject jsonLocation = jsonResult.getJSONObject("location");

                Elevation elevation = new Elevation();

                elevation.setElevation(jsonResult.getDouble("elevation"));
                elevation.setLocation(new LatLng(jsonLocation.getDouble("lat"), jsonLocation.getDouble("lng")));
                elevations.add(elevation);
            }
            elevationFinderListener.onElevationFinderSuccess(elevations);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}

