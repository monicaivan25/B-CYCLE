package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.MyLatLng;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.RouteDao;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class RouteDaoMapper {

    private SimpleAddressDaoMapper mapper = new SimpleAddressDaoMapper();

    public Route map(RouteDao routeDao){
        Route route = new Route();

        route.setOrigin(mapper.map(routeDao.getOrigin()));
        route.setDestination(mapper.map(routeDao.getDestination()));
        route.setDistance(routeDao.getDistance());
        route.setDuration(routeDao.getDuration());
        route.setElevationList(routeDao.getElevationList());

        List<LatLng> routePoints = new ArrayList<>();
        List<MyLatLng> routeDaoPoints = routeDao.getPointList();
        routeDaoPoints.forEach(point -> {
            routePoints.add(new LatLng(point.getLatitude(), point.getLongitude()));
        });
        route.setPointList(routePoints);
        route.setTravelMode(routeDao.getTravelMode());

        return route;
    }

    public RouteDao map(Route route){
        RouteDao routeDao = new RouteDao();
        routeDao.setOrigin(mapper.map(route.getOrigin()));
        routeDao.setDestination(mapper.map(route.getDestination()));
        routeDao.setDistance(route.getDistance());
        routeDao.setDuration(route.getDuration());
        routeDao.setElevationList(route.getElevationList());

        List<MyLatLng> routeDaoPoints = new ArrayList<>();
        List<LatLng> routePoints = route.getPointList();
        routePoints.forEach(point -> {
            routeDaoPoints.add(new MyLatLng(point.latitude, point.longitude));
        });
        routeDao.setPointList(routeDaoPoints);
        routeDao.setTravelMode(route.getTravelMode());

        return routeDao;
    }
}
