package com.isa.backend.dto;

public class GeoLocationDto {
    private Double lat;
    private Double lon;
    private String error;

    public GeoLocationDto() {}

    public GeoLocationDto(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public GeoLocationDto(String error) {
        this.error = error;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}


