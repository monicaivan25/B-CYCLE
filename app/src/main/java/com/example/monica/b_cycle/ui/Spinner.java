package com.example.monica.b_cycle.ui;

import android.view.View;
import android.widget.ProgressBar;

public class Spinner {

    private ProgressBar mSpinner;
    private View mSpinnerBackground;

    public Spinner() {
    }

    public Spinner(ProgressBar mSpinner, View mSpinnerBackground) {
        this.mSpinner = mSpinner;
        this.mSpinnerBackground = mSpinnerBackground;
    }

    /**
     * Starts the progress spinner.
     */
    public void start(){
        mSpinner.setVisibility(View.VISIBLE);
        mSpinnerBackground.setVisibility(View.VISIBLE);
    }

    /**
     * Stops the progress spinner.
     */
    public void stop() {
        mSpinner.setVisibility(View.GONE);
        mSpinnerBackground.setVisibility(View.GONE);
    }

    public ProgressBar getmSpinner() {
        return mSpinner;
    }

    public void setmSpinner(ProgressBar mSpinner) {
        this.mSpinner = mSpinner;
    }

    public View getmSpinnerBackground() {
        return mSpinnerBackground;
    }

    public void setmSpinnerBackground(View mSpinnerBackground) {
        this.mSpinnerBackground = mSpinnerBackground;
    }
}
