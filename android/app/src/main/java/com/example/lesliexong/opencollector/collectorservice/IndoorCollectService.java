package com.example.lesliexong.opencollector.collectorservice;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.IBinder;
import android.util.Log;
import java.util.ArrayList;

/**
 * Must register in manifest file before using.
 */

public class IndoorCollectService extends Service {
    public static final String TAG = "IndoorCollectService";
    public static final String ACTION_BROADCAST_DATA = "IndoorCollectService.BroadcastRSSI";
    public static final String ACTION_BROADCAST_COMMAND = "IndoorCollectService.BroadcastCommand";

    private XWiFiScanner wifiScanner;
    private XBeaconScanner beaconScanner;

    private IntentFilter cmdFilter;
    private CommandReceiver cmdReceiver;

    public IndoorCollectService() {
    }

    //Only run once
    @Override
    public void onCreate() {
        super.onCreate();

        cmdFilter = new IntentFilter(ACTION_BROADCAST_COMMAND);
        cmdReceiver = new CommandReceiver();

        Log.d(TAG, "service onCreate() executed");
    }

    //Run when every time startService ready to receive start command
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.registerReceiver(cmdReceiver, cmdFilter);

        Log.d(TAG, "service onStartCommand executed");
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopScanThread() {
        if (beaconScanner != null) {
            beaconScanner.stopScan();
            beaconScanner = null;
        }
        if (wifiScanner != null) {
            wifiScanner.stopScan();
            wifiScanner = null;
        }
    }

    private void startScanThread(boolean bBeacon, boolean bWiFi) {
        stopScanThread();

        if (bBeacon && beaconScanner == null) {
            beaconScanner = new XBeaconScanner(this);
            beaconScanner.registerScanListener(new XBeaconScanner.BeaconScanListener() {
                @Override
                public void onBeaconsDiscovered(final BluetoothDevice bluetoothDevice, final int rssi, final byte[] scanRecord) {
                    Intent broadcastDataIntent = new Intent(ACTION_BROADCAST_DATA);
                    broadcastDataIntent.putExtra(IndoorCollectManager.TAG_BEACON_DEVICE, bluetoothDevice);
                    broadcastDataIntent.putExtra(IndoorCollectManager.TAG_BEACON_RSSI, rssi);
                    broadcastDataIntent.putExtra(IndoorCollectManager.TAG_BEACON_SCAN_RECORD, scanRecord);
                    sendBroadcast(broadcastDataIntent);
                }
            });
            beaconScanner.start();
        }

        if (bWiFi && wifiScanner == null) {
            wifiScanner = new XWiFiScanner(this);
            wifiScanner.registerScanListener(new XWiFiScanner.WiFiScanListener() {
                @Override
                public void onWiFiDiscovered(ArrayList<ScanResult> listWifi) {
                    Intent broadcastDataIntent = new Intent(ACTION_BROADCAST_DATA);
                    broadcastDataIntent.putParcelableArrayListExtra(IndoorCollectManager.TAG_WIFI_DATA, listWifi);
                    sendBroadcast(broadcastDataIntent);
                }
            });
            wifiScanner.start();
        }
    }

    /**
     * It's not always running scan
     * just start scan thread when receive command and stop scan once result return to manager.
     */
    public class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean ifStart = intent.getBooleanExtra(IndoorCollectManager.TAG_START_STOP, false);
            if (ifStart) {

                boolean bWiFi = intent.getBooleanExtra(IndoorCollectManager.TAG_B_WIFI, false);
                boolean bBeacon = intent.getBooleanExtra(IndoorCollectManager.TAG_B_BEACON, false);

                Log.d(TAG, "startScanThread: " + bWiFi + bBeacon);
                if (bWiFi || bBeacon) {
                    startScanThread(bBeacon, bWiFi);
                }
            } else {
                stopScanThread();
            }
        }
    }


    @Override
    public void onDestroy() {
        this.unregisterReceiver(cmdReceiver);
        super.onDestroy();
        Log.d(TAG, "Service onDestroy() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
