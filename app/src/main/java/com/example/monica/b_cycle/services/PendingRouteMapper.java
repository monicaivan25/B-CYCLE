package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.PendingRoute;
import com.example.monica.b_cycle.model.PendingRouteDao;

public class PendingRouteMapper {

    private RouteDaoMapper routeDaoMapper = new RouteDaoMapper();

    public PendingRoute map(PendingRouteDao routeDao){
        PendingRoute route = new PendingRoute();

        route.setEmails(routeDao.getEmails());
        route.setRoute(routeDaoMapper.map(routeDao.getRouteDao()));
        route.setKey(routeDao.getKey());
        route.setFlags(routeDao.getFlags());
        return route;
    }

    public PendingRouteDao map(PendingRoute route){
        PendingRouteDao routeDao = new PendingRouteDao();
        routeDao.setEmails(route.getEmails());
        routeDao.setRouteDao(routeDaoMapper.map(route.getRoute()));
        routeDao.setKey(route.getKey());
        routeDao.setFlags(route.getFlags());

        return routeDao;
    }
}
