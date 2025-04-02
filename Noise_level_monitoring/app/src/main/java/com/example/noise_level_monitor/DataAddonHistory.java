package com.example.noise_level_monitor;

public class DataAddonHistory {

    private int id;
    private double noiseLevel;
    private long timestamp;
    private String studySuitability;

    public DataAddonHistory(int id, double noiseLevel, long timestamp, String studySuitability) {
        this.id = id;
        this.noiseLevel = noiseLevel;
        this.timestamp = timestamp;
        this.studySuitability = studySuitability;
    }

    // Getters and Setters (if needed)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStudySuitability() {
        return studySuitability;
    }

    public void setStudySuitability(String studySuitability) {
        this.studySuitability = studySuitability;
    }
}