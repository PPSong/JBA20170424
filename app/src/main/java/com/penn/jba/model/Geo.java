package com.penn.jba.model;

/**
 * Created by penn on 23/04/2017.
 */

public class Geo {
    public double lon;
    public double lat;

    public Geo(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public static Geo getDefaultGeo() {
        return new Geo(121.52619934082031d, 31.216968536376953d);
    }
}
