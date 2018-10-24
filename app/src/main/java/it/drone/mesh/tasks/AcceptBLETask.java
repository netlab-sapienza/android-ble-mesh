package it.drone.mesh.tasks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import it.drone.mesh.models.User;
import it.drone.mesh.utility.Constants;

public class AcceptBLETask {
    private final static String TAG_BLE_TASK = AcceptBLETask.class.getName();
    //private User user;
    private BluetoothGattServer mGattServer;
    private BluetoothGattServerCallback mGattServerCallback;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private BluetoothGattDescriptor mGattDescriptor;
    private BluetoothManager mBluetoothManager;
    private Context context;

    public AcceptBLETask(User user, BluetoothManager mBluetoothManager, final Context context) {
        //this.user = user;
        this.mBluetoothManager = mBluetoothManager;
        this.context = context;
        mGattService = new BluetoothGattService(Constants.Service_UUID.getUuid(), 0);
        mGattCharacteristic = new BluetoothGattCharacteristic(Constants.Characteristic_UUID.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattDescriptor = new BluetoothGattDescriptor(Constants.Descriptor_UUID.getUuid(), BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattServerCallback = new BluetoothGattServerCallback() {
            // DO SOMETHING WHEN THE CONNECTION UPDATES
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG_BLE_TASK, "I'm the server, I've connected to " + device.getName());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG_BLE_TASK, "onConnectionStateChange: DISCONNECTED from" + device.getName());
                }
                super.onConnectionStateChange(device, status, newState);
            }

            // DO SOMETHING WHEN A SERVICE IS ADDED
            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.d(TAG_BLE_TASK, "I've added a service " + service.toString());
                super.onServiceAdded(status, service);
            }

            // WHAT HAPPENS WHEN I GET A CHARACTERISTIC READ REQ
            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                final BluetoothDevice tempdev = device;
                Log.d(TAG_BLE_TASK, "I've been asked to read from " + tempdev.getName());
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            // WHAT HAPPENS WHEN I GET A CHARACTERISTIC WRITE REQ
            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                final BluetoothDevice tempdev = device;
                final String tempval = new String(value);
                Log.d(TAG_BLE_TASK, "I've been asked to write from " + tempdev.getName() + " " + tempval);
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d(TAG_BLE_TASK, "I've been asked to read descriptor from " + device.getName());
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG_BLE_TASK, "I've been asked to write descriptor from " + device.getName() + "    " + device.getAddress());
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                final BluetoothDevice tempdev = device;
                Log.d(TAG_BLE_TASK, "I'm writing from " + tempdev.getName());
                super.onExecuteWrite(device, requestId, execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.d(TAG_BLE_TASK, "I've notified " + device.getName());
                super.onNotificationSent(device, status);
            }

            @Override
            public void onMtuChanged(BluetoothDevice device, int mtu) {
                super.onMtuChanged(device, mtu);
            }

            @Override
            public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(device, txPhy, rxPhy, status);
            }

            @Override
            public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                super.onPhyRead(device, txPhy, rxPhy, status);
            }
        };

    }

    public void startServer() {
        // I CREATE A SERVICE WITH 1 CHARACTERISTIC AND 1 DESCRIPTOR
        this.mGattCharacteristic.addDescriptor(mGattDescriptor);
        this.mGattService.addCharacteristic(mGattCharacteristic);
        // I START OPEN THE GATT SERVER
        this.mGattServer = mBluetoothManager.openGattServer(context, mGattServerCallback);
        this.mGattServer.addService(this.mGattService);
        /*try {
            wait(600);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        //user.setBluetoothGattServer(this.mGattServer);
    }
}
