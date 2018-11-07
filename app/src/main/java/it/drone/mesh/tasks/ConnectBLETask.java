package it.drone.mesh.tasks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import it.drone.mesh.models.User;
import it.drone.mesh.utility.Constants;

public class ConnectBLETask {
    private final static String TAG = ConnectBLETask.class.getName();

    private User user;
    private BluetoothGattCallback mGattCallback;
    private BluetoothGatt mGatt;
    private Context context;

    public ConnectBLETask(User user, Context context, BluetoothGattCallback callback) {
        // GATT OBJECT TO CONNECT TO A GATT SERVER
        this.context = context;
        this.user = user;
        this.mGattCallback = callback;

    }

    public ConnectBLETask(User user, Context context) {
        // GATT OBJECT TO CONNECT TO A GATT SERVER
        this.context = context;
        this.user = user;
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyRead(gatt, txPhy, rxPhy, status);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT client " + gatt.getDevice().getName());
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "OUD: " + "GATT: " + gatt.toString());
                Log.d(TAG, "OUD: " + "I discovered a service" + gatt.getServices());
                // TODO: 07/11/18 marked to be deleted, please @Nero or @Locatelli check and delete 
                /*for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                        if (service.getCharacteristics() != null) {
                            for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                    /*if (chars.getUuid().equals(Constants.Characteristic_UUID.getUuid())) {
                                    Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                                    gatt.setCharacteristicNotification(chars, true);
                                    chars.setValue("COMPILATO DA GIGI");
                                    gatt.beginReliableWrite();
                                    gatt.writeCharacteristic(chars);
                                    gatt.executeReliableWrite();
                                    Log.d(TAG, "OUD: " + "caratteristica ok");
                                    }
                            }
                        }
                    }
                }*/
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: " + "I read a characteristic");
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: " + "I wrote a characteristic");
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "OUD: " + "Characteristic changed");
                Log.d(TAG, "OUD: " + new String(characteristic.getValue()));
                super.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.d(TAG, "OUD: " + "I read a descriptor");
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.d(TAG, "OUD: " + "I wrote a descriptor");
                Log.d(TAG, "OUD: " + gatt.getDevice().getName());
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().equals(Constants.Service_UUID.getUuid())) {
                        if (service.getCharacteristics() != null) {
                            for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                if (chars.getUuid().equals(Constants.Characteristic_UUID.getUuid())) {
                                    gatt.setCharacteristicNotification(chars, true);
                                    chars.setValue("test string");
                                    gatt.beginReliableWrite();
                                    boolean res = gatt.writeCharacteristic(chars);
                                    gatt.executeReliableWrite();
                                    Log.d(TAG, "OUD: " + res + "");
                                }
                            }
                        }
                    }
                }
                super.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                Log.d(TAG, "OUD: " + "I reliably wrote ");
                super.onReliableWriteCompleted(gatt, status);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                Log.d(TAG, "OUD: " + "I read the remote rssi");
                super.onReadRemoteRssi(gatt, rssi, status);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
            }
        };
    }

    public void startClient() {
        this.mGatt = user.getBluetoothDevice().connectGatt(context, false, mGattCallback);

        /*try {
            wait(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        user.setBluetoothGatt(this.mGatt);
        user.getBluetoothGatt().requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        user.getBluetoothGatt().connect();
        Log.d(TAG, "OUD:" + "startClient: " + mGatt.getDevice().getName());

        /*try {
            wait(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        this.mGatt.discoverServices();

    }

    public void stopClient() {
        this.mGatt.disconnect();
        this.mGatt = null;
    }
}

