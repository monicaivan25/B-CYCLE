package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.MyLatLng;
import com.example.monica.b_cycle.model.SimpleAddress;
import com.example.monica.b_cycle.model.SimpleAddressDAO;
import com.google.android.gms.maps.model.LatLng;

public class SimpleAddressDaoMapper {

    public SimpleAddress map(SimpleAddressDAO simpleAddressDAO){
        SimpleAddress simpleAddress = new SimpleAddress();
        simpleAddress.setName(simpleAddressDAO.getName());
        simpleAddress.setLocation(new LatLng(simpleAddressDAO.getLocation().getLatitude(), simpleAddressDAO.getLocation().getLongitude()));
        return simpleAddress;
    }

    public SimpleAddressDAO map(SimpleAddress simpleAddress){
        SimpleAddressDAO simpleAddressDAO = new SimpleAddressDAO();
        simpleAddressDAO.setName(simpleAddress.getName());
        simpleAddressDAO.setLocation(new MyLatLng(simpleAddress.getLocation().latitude, simpleAddress.getLocation().longitude));
        return simpleAddressDAO;
    }
}
