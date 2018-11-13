package it.drone.mesh.roles.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import it.drone.mesh.R;
import it.drone.mesh.roles.common.exceptions.NotEnabledException;
import it.drone.mesh.roles.common.exceptions.NotSupportedException;

import static it.drone.mesh.roles.common.Utility.isBLESupported;

/**
 * The first step in interacting with a BLE device is connecting to it— more specifically,
 * connecting to the GATT server on the device. To connect to a GATT server on a BLE device,
 * you use the connectGatt() method.
 * This method takes three parameters:
 * a Context object, autoConnect (boolean indicating whether to automatically connect to the BLE device
 * as soon as it becomes available), and a reference to a BluetoothGattCallback:
 * <p>
 * mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
 * <p>
 * This connects to the GATT server hosted by the BLE device,
 * and returns a BluetoothGatt instance, which
 * you can then use to conduct GATT client operations.
 * The caller (the Android app) is the GATT client.
 * The BluetoothGattCallback is used to deliver results to the client,
 * such as connection status, as well as any further GATT client operations.
 * <p>
 * src :  https://developer.android.com/guide/topics/connectivity/bluetooth-le#connect
 */

public class BLEClient {

    private static BLEClient singleton;

    // potrebbero venir riutilizzati, quindi non convertire a local
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BLEClient(Context context) throws NotSupportedException, NotEnabledException {
        bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            mBluetoothAdapter = bluetoothManager.getAdapter();
        else
            throw new NotSupportedException(context.getResources().getString(R.string.bt_null));

        if (!isBLESupported(context)) {
            throw new NotSupportedException(context.getResources().getString(R.string.bt_not_supported));
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            throw new NotEnabledException(context.getResources().getString(R.string.bt_not_enabled_leaving));
        }
    }

    public static BLEClient getInstance(Context context) throws NotEnabledException, NotSupportedException {
        if (singleton == null)
            singleton = new BLEClient(context);
        return singleton;
    }

    // TODO: 13/11/18 implementare client things vedi doc a inizio file 


}
