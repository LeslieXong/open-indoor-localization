package com.example.lesliexong.opencollector.collectorservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * WiFi is different with ble scan, we must set a timer to schedule to start scan.
 */
public class XWiFiScanner extends Thread {
    private static String TAG = "XWiFiScanner";

    private Context mContext;
    private WiFiScanListener wiFiScanListener;
    private WifiManager wifiManager;
    private IntentFilter intent = null;
    private Timer timer;

    //Interval between two scan
    private static int SCAN_INTERVAL = 1000;

    XWiFiScanner(Context context) {
        mContext = context;
    }

    private void switchScanWiFi(final boolean enable) {
        wifiManager = (WifiManager) mContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null)
            return;

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (enable) {
            if (intent == null) {
                intent = new IntentFilter();
                intent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            }

//          wifiManager.startScan(); //remove here if use receiver.
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    wifiManager.startScan();
                    mContext.registerReceiver(wifiReceiver, intent);
//                    ArrayList<ScanResult> listScanResults = new ArrayList<>(wifiManager.getScanResults());
//                    if (wiFiScanListener != null)
//                        wiFiScanListener.onWiFiDiscovered(listScanResults);
//                    wifiManager.startScan();
                }
            }, 0, SCAN_INTERVAL);
        } else if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
            mContext.unregisterReceiver(wifiReceiver);
        }
    }

    public interface WiFiScanListener {
        void onWiFiDiscovered(final ArrayList<ScanResult> listBeacon);
    }

    void registerScanListener(WiFiScanListener listener) {
        wiFiScanListener = listener;
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
            mContext.unregisterReceiver(this);
            ArrayList<ScanResult> listScanResults = new ArrayList<>(wifiManager.getScanResults());
            if (wiFiScanListener != null)
                wiFiScanListener.onWiFiDiscovered(listScanResults);
        }
    };

    @Override
    public void run() {
        switchScanWiFi(true);
        super.run();
        Log.d(TAG, "start WiFi Thread" + Thread.currentThread());
    }

    void stopScan() {
        switchScanWiFi(false);
        Log.d(TAG, "stop WiFi Scan");
    }
}
