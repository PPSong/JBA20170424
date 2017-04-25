package com.penn.jba.model;

/**
 * Created by penn on 23/04/2017.
 */

public class Geo {
    public float lon;
    public float lat;

    public Geo(float lon, float lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public static Geo getDefaultGeo() {
        return new Geo(121.52619934082031f, 31.216968536376953f);
    }
}
