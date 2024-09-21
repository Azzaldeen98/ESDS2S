package com.example.esds2s.Models;

public class SpeechModelInfo {
    private String model;
    private int index;

    public SpeechModelInfo(String model, int index) {
        this.model = model;
        this.index = index;
    }


    public String getModel() {
        return model;
    }

    public int getIndex() {
        return index;
    }
}
