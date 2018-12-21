package it.drone.mesh.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

public class BLEClient {

    private static final String TAG = BLEClient.class.getSimpleName();
    private String id;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BLEClient(Context context) {
        bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

}
