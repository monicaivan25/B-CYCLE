package com.example.monica.b_cycle.model;

import android.graphics.Color;

public enum TravelMode {

    BICYCLING(Color.GREEN, "bicycling", 10),
    DRIVING(Color.BLUE, "driving", 15),
    WALKING(Color.RED, "walking", 7);

    private final int color;
    private final String name;
    private final double defaultSpeed;

    TravelMode(int color, String name, double defaultSpeed) {
        this.color = color;
        this.name = name;
        this.defaultSpeed = defaultSpeed;
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public double getDefaultSpeed() {
        return defaultSpeed;
    }
}
