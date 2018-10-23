package com.example.lesliexong.opencollector.collectorservice;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by LeslieXong.<br>
 * Usage: First add to manifest the service.<br><br>
 * IndoorCollectManager indoorCollectManager = new IndoorCollectManager(this);
 * indoorCollectManager.startCollectService();
 * indoorCollectManager.registerCollectorListener(....); //to receive result when needed.
 * indoorCollectManager.unregisterCollectorListener();   //to unregister
 * indoorCollectManager.startScan();             //stop service
 */

public class IndoorCollectManager {
    public interface CollectorListener {
        void onCollectFinished(final ArrayList<XBeacon> beaconData, final ArrayList<XWiFi> wifiData);
    }

    private static final String TAG = "IndoorCollectManager";

    static final String TAG_WIFI_DATA = "TAG_RSSI_DATA";
    static final String TAG_BEACON_DEVICE = "TAG_BEACON_DEVICE";
    static final String TAG_BEACON_RSSI = "TAG_BEACON_RSSI";
    static final String TAG_BEACON_SCAN_RECORD = "TAG_BEACON_SCAN_RECORD";

    static final String TAG_B_WIFI = "TAG_B_WIFI";
    static final String TAG_B_BEACON = "TAG_B_BEACON";
    static final String TAG_START_STOP = "TAG_START_STOP";

    private IntentFilter rssiDataFilter;
    private RssiDataReceiver rssiDataReceiver;
    private CollectorListener collectorListener;

    private Context mContext;

    private Map<String, XBeacon> macBeaconMap;
    private Map<String, XWiFi> macWiFiMap;
    private List<Float> magneticList;

    private static int scanPeriodDefault = 8500;
    private int scanPeriodMills = scanPeriodDefault;

    public IndoorCollectManager(Context context) {
        this.mContext = context.getApplicationContext();

        rssiDataFilter = new IntentFilter(IndoorCollectService.ACTION_BROADCAST_DATA);
        rssiDataReceiver = new RssiDataReceiver();

        macBeaconMap = new HashMap<>();
        macWiFiMap = new HashMap<>();
        magneticList = new ArrayList<>();
    }

    /**
     * This will update the data periodically.
     * Must  startCollectService before start using,and startScan after register.
     *
     * @param collectorListener the sensorListener to receive collection data
     */
    //More than one activity may use same collect service ,so use broadcast to inform.
    public void registerCollectorListener(CollectorListener collectorListener) {
        this.collectorListener = collectorListener;
    }

    public void unregisterCollectorListener() {
        if (collectorListener != null) {
            collectorListener = null;
        }
    }


    public void setScanPeriodMills(int scanPeriodMills) {
        /*
         * if set as 0, it will return once get scan result
         */
        this.scanPeriodMills = scanPeriodMills;
    }

    /**
     * @param bWifi   if collected wifi data
     * @param bBeacon if collected beacon data
     */
    public void startScan(boolean bWifi, boolean bBeacon) {
//        startSensor();
        startWireless(bWifi, bBeacon);
        startSendDataPeriodically(true);
    }

    public void stopScan() {
        startSendDataPeriodically(false);
        stopWireless();
        stopSensor();
    }

    private void startWireless(boolean bWifi, boolean bBeacon) {
        //prepare to receive data from collector service
        mContext.registerReceiver(rssiDataReceiver, rssiDataFilter);
        Intent commandIntent = new Intent(IndoorCollectService.ACTION_BROADCAST_COMMAND);
        commandIntent.putExtra(TAG_START_STOP, true);
        commandIntent.putExtra(TAG_B_WIFI, bWifi);
        commandIntent.putExtra(TAG_B_BEACON, bBeacon);
        mContext.sendBroadcast(commandIntent);
    }

    private void stopWireless() {
        mContext.unregisterReceiver(rssiDataReceiver);
        Intent commandIntent = new Intent(IndoorCollectService.ACTION_BROADCAST_COMMAND);
        commandIntent.putExtra(TAG_START_STOP, false);
        mContext.sendBroadcast(commandIntent);
    }

    private Handler handler;
    private Runnable runnable;

    private void startSendDataPeriodically(boolean start) {
        if (scanPeriodMills == 0)
            return;

        if (start) {
            //      start to feed back data periodically
            if (handler == null)
                handler = new Handler();

            if (runnable == null)
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (IndoorCollectManager.this.collectorListener != null) {
                            processDataAndSend();
                            handler.postDelayed(this, scanPeriodMills);
                        }
                    }
                };
            handler.postDelayed(runnable, scanPeriodMills);
        } else if (handler != null) {
            handler.removeCallbacks(runnable);
            handler = null;
            runnable = null;
        }
    }


    private SensorManager sensorManager;
    private SensorEventListener sensorListener;

    private void startSensor() {
        if (sensorListener == null) {
            sensorListener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor arg0, int arg1) {
                }

                @Override
                public void onSensorChanged(SensorEvent event) {
                    Sensor sensor = event.sensor;
                    if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        magneticList.add((float) Math.sqrt(event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]));
                    }
                }
            };
        }

        sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopSensor() {
        if (sensorManager != null && sensorListener != null)
            sensorManager.unregisterListener(sensorListener);
    }

    /**
     * <p>This will start the collecting service but not start scan and save the data unless use @link: registerCollectorListener to receive data.</p>
     */
    public void startCollectService() {
        if (!isServiceRunning(mContext)) {
            Intent serviceIntent = new Intent(mContext, IndoorCollectService.class);
            mContext.startService(serviceIntent);
        }
    }

    public void stopCollectService() {
        if (isServiceRunning(mContext)) {
            Intent serviceIntent = new Intent(mContext, IndoorCollectService.class);
            mContext.stopService(serviceIntent);
        }
    }

    private void processDataAndSend() {
        final ArrayList<XBeacon> beaconList = new ArrayList<>(macBeaconMap.values());
        final ArrayList<XWiFi> wifiList = new ArrayList<>(macWiFiMap.values());
//        final ArrayList<Float> magnetic = new ArrayList<>(magneticList);

        macBeaconMap.clear();
        macWiFiMap.clear();
        magneticList.clear();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (collectorListener != null) {
                    collectorListener.onCollectFinished(beaconList, wifiList);
                }
            }
        };

        Thread mProcessThread = new Thread(runnable);
        mProcessThread.start();
    }

    private class RssiDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extra = intent.getExtras();
            if (extra != null) {
                ArrayList<ScanResult> listScanResults = extra.getParcelableArrayList(TAG_WIFI_DATA);
                if (listScanResults != null) {
                    Log.d(TAG, "onReceive WiFi: " + listScanResults.size());
                    for (ScanResult scanResult : listScanResults) {
                        XWiFi xwifi = new XWiFi(scanResult.BSSID, scanResult.level);
                        String mac = xwifi.mac.toLowerCase();
                        if (macWiFiMap.containsKey(mac)) {
                            macWiFiMap.get(mac).rssiList.add(xwifi.rssi);
                        } else {
                            macWiFiMap.put(mac, new XWiFi(mac, xwifi.rssi));
                        }
                    }
                    if (scanPeriodMills == 0)
                        processDataAndSend();
                }

                BluetoothDevice bluetoothDevice = extra.getParcelable(TAG_BEACON_DEVICE);
                if (bluetoothDevice != null) {
                    final XBeacon beacon = XBeaconParser.fromScanData(bluetoothDevice, extra.getInt(TAG_BEACON_RSSI),
                            extra.getByteArray(TAG_BEACON_SCAN_RECORD));
                    if (beacon != null) {
                        String mac = beacon.mac.toLowerCase();
                        if (macBeaconMap.containsKey(mac)) {
                            macBeaconMap.get(mac).rssiList.add(beacon.rssi);
                        } else {
                            macBeaconMap.put(mac, new XBeacon(mac,
                                    beacon.major, beacon.minor, beacon.rssi));
                        }
                    }
                }
            }
        }
    }

    private boolean isServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                    .getRunningServices(Integer.MAX_VALUE);

            if (!(serviceList.size() > 0)) {
                return false;
            }

            for (int i = 0; i < serviceList.size(); i++) {
                ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
                ComponentName serviceName = serviceInfo.service;

                if (serviceName.getClassName().contains(IndoorCollectService.TAG)) {
                    return true;
                }
            }
        }
        return false;
    }

}
