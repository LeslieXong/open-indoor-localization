package com.example.lesliexong.opencollector.collectorservice;

import java.util.ArrayList;


public class XBasic {
    public String mac;
    public int rssi;
    public ArrayList<Integer> rssiList;

    XBasic(){
        rssiList = new ArrayList<>();
    }

    XBasic(String mac, int rssi) {
        this.mac = mac;
        this.rssi = rssi;
        rssiList = new ArrayList<>();
        rssiList.add(rssi);
    }

    public String toString() {
        return String.format("mac:%s rssi:%d", mac, rssi);
    }
}
