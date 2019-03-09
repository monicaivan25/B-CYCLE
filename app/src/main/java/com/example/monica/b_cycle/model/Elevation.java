package com.example.monica.b_cycle.model;

import com.google.android.gms.maps.model.LatLng;

public class Elevation {

    private LatLng location;
    private Double elevation;

    public Elevation() {
    }

    public Elevation(LatLng location, Double elevation) {
        this.location = location;
        this.elevation = elevation;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    @Override
    public String toString() {
        return "Elevation{" +
                "location=" + location +
                ", elevation=" + elevation +
                '}';
    }
}
