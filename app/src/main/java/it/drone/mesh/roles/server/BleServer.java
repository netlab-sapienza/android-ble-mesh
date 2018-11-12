package it.drone.mesh.roles.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;


public class BleServer {
    final private BluetoothManager bluetoothManager;
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;

    public BleServer(Context context) {
        this.context = context;

        bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


    }


    public static boolean isBleSupported(Context context) {
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

    }

    private void initializeServer() {

    }


    public void sendMessage(String message) {

    }

}
