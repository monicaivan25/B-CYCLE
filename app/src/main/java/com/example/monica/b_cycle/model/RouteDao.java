package com.example.monica.b_cycle.model;

import java.util.List;

public class RouteDao {
    private Distance distance;
    private Duration duration;
    private List<Elevation> elevationList;
    private SimpleAddressDAO origin;
    private SimpleAddressDAO destination;
    private List<MyLatLng> pointList;

    private TravelMode travelMode;

    public RouteDao() {
    }

    public RouteDao(Distance distance, Duration duration, List<Elevation> elevationList, SimpleAddressDAO origin, SimpleAddressDAO destination, List<MyLatLng> pointList, TravelMode travelMode) {
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

    public SimpleAddressDAO getOrigin() {
        return origin;
    }

    public void setOrigin(SimpleAddressDAO origin) {
        this.origin = origin;
    }

    public SimpleAddressDAO getDestination() {
        return destination;
    }

    public void setDestination(SimpleAddressDAO destination) {
        this.destination = destination;
    }

    public List<MyLatLng> getPointList() {
        return pointList;
    }

    public void setPointList(List<MyLatLng> pointList) {
        this.pointList = pointList;
    }

    public TravelMode getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode;
    }
}
