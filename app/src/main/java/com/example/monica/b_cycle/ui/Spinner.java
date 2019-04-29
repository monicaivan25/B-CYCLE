package com.example.monica.b_cycle.ui;

import android.view.View;
import android.widget.ProgressBar;

public class Spinner {

    private ProgressBar circleProgressBar;
    private View spinnerBackground;

    public Spinner() {
    }

    public Spinner(ProgressBar circleProgressBar, View spinnerBackground) {
        this.circleProgressBar = circleProgressBar;
        this.spinnerBackground = spinnerBackground;
    }

    /**
     * Starts the progress circleProgressBar.
     */
    public void start(){
        circleProgressBar.setVisibility(View.VISIBLE);
        spinnerBackground.setVisibility(View.VISIBLE);
    }

    /**
     * Stops the progress circleProgressBar.
     */
    public void stop() {
        circleProgressBar.setVisibility(View.GONE);
        spinnerBackground.setVisibility(View.GONE);
    }

    public ProgressBar getCircleProgressBar() {
        return circleProgressBar;
    }

    public void setCircleProgressBar(ProgressBar circleProgressBar) {
        this.circleProgressBar = circleProgressBar;
    }

    public View getSpinnerBackground() {
        return spinnerBackground;
    }

    public void setSpinnerBackground(View spinnerBackground) {
        this.spinnerBackground = spinnerBackground;
    }
}
