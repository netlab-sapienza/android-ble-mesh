package it.drone.mesh.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import it.drone.mesh.ConnectionActivity;
import it.drone.mesh.models.User;
import it.drone.mesh.tasks.ConnectBLETask;

/**
 * Constants for use in the Bluetooth Advertisements sample
 */
public class Constants {

    /*
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     * <p>
     * Bluetooth requires a certain format for UUIDs associated with Services.
     * The official specification can be found here:
     * {@link https://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery}
     */
    /*public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Service_UUID_client = ParcelUuid
            .fromString("9999b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Characteristic_UUID = ParcelUuid
            .fromString("1111b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Descriptor_UUID = ParcelUuid
            .fromString("2222b81d-0000-1000-8000-00805f9b34fb");*/

    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("00001814-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Service_UUID_client = ParcelUuid
            .fromString("00002A14-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Characteristic_UUID = ParcelUuid
            .fromString("1111b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Descriptor_UUID = ParcelUuid
            .fromString("2222b81d-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    // Strings for data exchange activity
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_USER_USERNAME = "USER_USERNAME";
    public static final String EXTRAS_USER_DEVICE = "USER_DEVICE";
    public static final String EXTRAS_USER_SOCKET = "USER_SOCKET";
    public static final String EXTRAS_USER_SERVER_SOCKET = "USER_SERVER_SOCKET";
    public static final String EXTRAS_USER_GATT_SERVER = "USER_GATT_SERVER";
    public static final String EXTRAS_USER_GATT = "USER_GATT";

    private final static String TAG = "SendMessage";

    public static void sendMessage(String macAddress, String message, BluetoothManager mBluetoothManager, User user, Context context) {
        Log.d(TAG, "OUD: " + "sendMessage: Inizio invio messaggio");
        //final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        final BluetoothGatt gatt = /*UserList.getUser(mDeviceName).getBluetoothGatt();*/ user.getBluetoothGatt();
        ConnectBLETask connectBLETask = null;

        while(!(BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)) || !(BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT))) {
            connectBLETask = new ConnectBLETask(user, context);
            connectBLETask.startClient();
            Log.d(TAG, "OUD: " + "Restauro connessione");
            Log.d(TAG, "OUD: " + "StateServer connesso ? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)));
            Log.d(TAG, "OUD: " + "StateGatt connesso? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT)));
        }

        if (connectBLETask != null) {
            while(!connectBLETask.getServiceDiscovered()) {
                Log.d(TAG, "OUD: " + "Wait for services");
            }
        }
        String finalMessage = null;
        if (macAddress != null) {
            finalMessage = macAddress + ":" + message;
        }
        else{
            finalMessage = message;
        }

        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                            chars.setValue(finalMessage);
                            //gatt.beginReliableWrite();
                            boolean res = gatt.writeCharacteristic(chars);
                            //gatt.executeReliableWrite();
                            Log.d(TAG, "OUD: " + finalMessage);
                            Log.d(TAG, "OUD: " + "Inviato? -> " + res);

                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
    }
}
