package com.example.monica.b_cycle.exceptions;

import android.util.Log;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException() {
        Log.e("LOCATIONNOTFOUND", "Could not find address");
    }
}
