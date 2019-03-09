package com.example.monica.b_cycle.services;

public interface Finder {
    /**
     * Parses the received data as instances of the Object needed.
     * @param data the JSON as a String
     */
    void parseJson(String data);
}
