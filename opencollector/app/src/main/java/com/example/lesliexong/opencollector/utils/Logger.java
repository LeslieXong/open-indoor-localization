package com.example.lesliexong.opencollector.utils;

import android.graphics.PointF;
import android.os.Environment;

import com.example.lesliexong.opencollector.collectorservice.Fingerprint;
import com.example.lesliexong.opencollector.collectorservice.XBeacon;
import com.example.lesliexong.opencollector.collectorservice.XWiFi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Logger {
    private static String TAG = "Logger";

    private static String getRootDir() {
        return Environment.getExternalStorageDirectory() + "/OpenCollector";
    }

    public static void saveFingerprintData(String type, Fingerprint fingerprint) {
        List<String> data = new LinkedList<>();

        for (XBeacon beacon : fingerprint.beaconData) {
            StringBuilder rssiList = new StringBuilder();
            for (Integer rssi : beacon.rssiList) {
                rssiList.append(" ");
                rssiList.append(rssi);
            }
            data.add(String.format(Locale.ENGLISH, "%s %d b |%s", beacon.mac, beacon.rssi, rssiList.toString()));
        }

        for (XWiFi wifi : fingerprint.wifiData) {
            StringBuilder rssiList = new StringBuilder();
            for (Integer rssi : wifi.rssiList) {
                rssiList.append(" ");
                rssiList.append(rssi);
            }
            data.add(String.format(Locale.ENGLISH, "%s %d w |%s", wifi.mac, wifi.rssi, rssiList.toString()));
        }

        String firstLine = String.format(Locale.ENGLISH, "%.2f %.2f %d %s", fingerprint.x, fingerprint.y, data.size()
                ,getTimeStamp());

        String rootPath = getRootDir();
        String filePath = rootPath + "/" + type + getDateUnderLine() + ".txt";

        try {
            File dir = new File(filePath);
            dir.getParentFile().mkdirs();
            //not use "+"
            //<<Effective Java>> 51
            StringBuilder sb = new StringBuilder();
            sb.append(firstLine).append("\r\n");

            for (String str : data) {
                sb.append(str).append("\r\n");
            }
            sb.append("\r\n");

            FileWriter fw = new FileWriter(filePath, true);
            try {
                fw.write(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<PointF> getCollectedGrid(String type) {
        String rootPath = getRootDir();
        String filePath = rootPath + "/" + type + getDateUnderLine() + ".txt";

        List<PointF> result = new ArrayList<>();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return result;
        }

        try {
            String line = in.readLine();
            while (line != null) {
                if (!line.trim().equals("")) { //no blank line
                    if (!line.contains("|")) {
                        String[] attr = line.split(" ");
                        PointF p = new PointF(Float.valueOf(attr[0]), Float.valueOf(attr[1]));
                        result.add(p);
                    }
                }
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    //For create file name
    private static String getDateUnderLine() {
        //SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat("MM_dd");
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }

    private static String getTimeStamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }

}
