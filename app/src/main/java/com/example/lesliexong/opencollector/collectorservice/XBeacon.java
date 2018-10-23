package com.example.lesliexong.opencollector.collectorservice;

public class XBeacon extends XBasic {
    public String name;
    public int major;
    public int minor;
    public String proximityUuid;
    public int txPower;

    XBeacon(){
        super();
    }

    XBeacon(String mac,int major,int minor,int rssi)
    {
        super(mac,rssi);
        this.major=major;
        this.minor = minor;
    }

    public String toString() {
        return String.format("major:%d minor:%d mac:%s rssi:%d", major, minor, mac, rssi);
    }
}
