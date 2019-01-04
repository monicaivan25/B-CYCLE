package com.example.monica.b_cycle.model;

public class SimpleAddressDAO {

    private String name;
    private MyLatLng location;

    public SimpleAddressDAO() {
    }

    public SimpleAddressDAO(String name, MyLatLng location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MyLatLng getLocation() {
        return location;
    }

    public void setLocation(MyLatLng location) {
        this.location = location;
    }
}
