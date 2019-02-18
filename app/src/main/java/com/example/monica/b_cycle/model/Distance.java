package com.example.monica.b_cycle.model;


public class Distance {

    private String text;
    private Integer value;

    public Distance() {
    }

    public Distance(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
