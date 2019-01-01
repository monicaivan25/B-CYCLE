package com.example.monica.b_cycle.model;

import com.google.android.gms.maps.model.LatLng;

public class Elevation {

    private LatLng point;
    private Integer value;

    public Elevation(LatLng point, Integer value) {
        this.point = point;
        this.value = value;
    }

    public LatLng getPoint() {
        return point;
    }

    public void setPoint(LatLng point) {
        this.point = point;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
