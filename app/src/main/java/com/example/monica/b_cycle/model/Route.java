package com.example.monica.b_cycle.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.List;

public class Route {

    private Distance distance;
    private Duration duration;
    private List<Elevation> elevationList;
    private SimpleAddress origin;
    private SimpleAddress destination;
    private List<LatLng> pointList;
    private Polyline polyline;
    private TravelMode travelMode;

    public Route() {
    }

    public Route(Route route){
        this.distance = route.getDistance();
        this.duration = route.getDuration();
        this.elevationList = route.getElevationList();
        this.origin = route.getOrigin();
        this.destination = route.getDestination();
        this.pointList = route.getPointList();
        this.travelMode = route.getTravelMode();
    }

    public Route(Distance distance, Duration duration, List<Elevation> elevationList, SimpleAddress origin, SimpleAddress destination, List<LatLng> pointList, TravelMode travelMode) {
        this.distance = distance;
        this.duration = duration;
        this.elevationList = elevationList;
        this.origin = origin;
        this.destination = destination;
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

    public Polyline getPolyline() {
        return polyline;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    @Override
    public String toString() {
        return "Route{" +
                "distance=" + distance +
                ", duration=" + duration +
                ", elevationList=" + elevationList +
                ", origin=" + origin +
                ", destination=" + destination +
                ", pointList=" + pointList +
                ", travelMode=" + travelMode +
                '}';
    }
}
