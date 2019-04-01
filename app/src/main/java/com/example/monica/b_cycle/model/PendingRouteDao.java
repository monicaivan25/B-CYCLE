package com.example.monica.b_cycle.model;

import java.util.ArrayList;
import java.util.List;

public class PendingRouteDao {

    private List<String> emails;
    private List<String> flags;
    private RouteDao routeDao;
    private String key;

    public PendingRouteDao() {
    }

    public PendingRouteDao(String email, String flag, RouteDao routeDao) {
        this.emails = new ArrayList<>();
        emails.add(email);
        this.routeDao = routeDao;
        this.flags = new ArrayList<>();
        flags.add(flag);
    }

    public PendingRouteDao(List<String> emails, List<String> flags, RouteDao routeDao) {
        this.emails = emails;
        this.flags = flags;
        this.routeDao = routeDao;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public RouteDao getRouteDao() {
        return routeDao;
    }

    public void setRouteDao(RouteDao routeDao) {
        this.routeDao = routeDao;
    }

    public List<String> getFlags() {
        return flags;
    }

    public void setFlags(List<String> flags) {
        this.flags = flags;
    }
}
