package com.example.monica.b_cycle.services;

import com.example.monica.b_cycle.model.Elevation;

import java.util.List;

public interface ElevationFinderListener {
    /**
     * Method to be called by an ElevationFinder instance.
     * Once a call has been successfully made to the Google Elevation URL
     * and the received JSON has been successfully parsed, the ElevationFinder
     * instance calls this method.
     *
     * @param elevations
     */
    void onElevationFinderSuccess(List<Elevation> elevations);
}
