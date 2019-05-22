package com.example.monica.b_cycle.services;

public interface Finder {
    /**
     * Parses the received data as instances of the Object needed.
     * @param data the JSON as a String
     */
    void parseJson(String data);

    /**
     * Creates the URL needed to call in order to obtain the json data
     * @return the URL concatenated with API key
     */
    String createURL();
}
