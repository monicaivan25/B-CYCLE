package com.example.monica.b_cycle.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Route {

    private Distance distance;
    private Duration duration;
    private List<Elevation> elevationList;
    private SimpleAddress origin;
    private SimpleAddress destination;
    private Speed recordedSpeed;
    private List<LatLng> pointList;

    private TravelMode travelMode;

    public Route() {
    }

    public Route(Distance distance, Duration duration, List<Elevation> elevationList, SimpleAddress origin, SimpleAddress destination, Speed recordedSpeed, List<LatLng> pointList, TravelMode travelMode) {
        this.distance = distance;
        this.duration = duration;
        this.elevationList = elevationList;
        this.origin = origin;
        this.destination = destination;
        this.recordedSpeed = recordedSpeed;
        this.pointList = pointList;
        this.travelMode = travelMode;
    }

    public Distance getDistance() {
        return distance;
    }

    public void setDistance(Distance distance) {
        this.distance = distance;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public List<Elevation> getElevationList() {
        return elevationList;
    }

    public void setElevationList(List<Elevation> elevationList) {
        this.elevationList = elevationList;
    }

    public SimpleAddress getOrigin() {
        return origin;
    }

    public void setOrigin(SimpleAddress origin) {
        this.origin = origin;
    }

    public SimpleAddress getDestination() {
        return destination;
    }

    public void setDestination(SimpleAddress destination) {
        this.destination = destination;
    }

    public Speed getRecordedSpeed() {
        return recordedSpeed;
    }

    public void setRecordedSpeed(Speed recordedSpeed) {
        this.recordedSpeed = recordedSpeed;
    }

    public List<LatLng> getPointList() {
        return pointList;
    }

    public void setPointList(List<LatLng> pointList) {
        this.pointList = pointList;
    }

    public TravelMode getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode;
    }
}
