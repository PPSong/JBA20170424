package com.penn.jba.model;

/**
 * Created by penn on 03/05/2017.
 */

public class MessageEvent {
    public String type;
    public String data;

    public MessageEvent(String type, String data) {
        this.type = type;
        this.data = data;
    }
}
