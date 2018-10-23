package com.example.lesliexong.opencollector.collectorservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;


/**
 * iBeacon用于定位的扫描线程 传入Context，扫描间隔取均值，结果通过handler返回 Android </br>
 * 4.3（ miniApi 18）版本后才能使用<br>
 * 对于5.0以上的系统 BluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback)
 */
public class XBeaconScanner extends Thread {
    private BluetoothAdapter mBluetoothAdapter = null;
    private final static String TAG = "XBeaconScanner";

    private BeaconScanListener callback;

    XBeaconScanner(Context context) {

        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.
                FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context,
                    "This device is not support BLE", Toast.LENGTH_SHORT).show();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            final Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if (mBluetoothAdapter != null)
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
             callback.onBeaconsDiscovered(device, rssi,scanRecord);
        }
    };

    void registerScanListener(BeaconScanListener callback) {
        this.callback = callback;
    }

    public interface BeaconScanListener {
        void onBeaconsDiscovered(final BluetoothDevice bluetoothDevice, final int rssi, final byte[] scanRecord);
    }

    @Override
    public void run() {
        scanLeDevice(true);
        super.run();
    }

    void stopScan()
    {
        scanLeDevice(false);
    }
}
