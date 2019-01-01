package com.example.monica.b_cycle.model;

import com.google.android.gms.maps.model.LatLng;

public class SimpleAddress {

    private String name;
    private LatLng location;

    public SimpleAddress(String name, LatLng location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }
}
