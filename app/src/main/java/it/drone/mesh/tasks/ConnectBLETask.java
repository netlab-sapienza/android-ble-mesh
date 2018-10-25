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
    private BluetoothGattCharacteristic mChar;
    private Context context;


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
                Log.d(TAG, "OUD: " + "I discovered a service" + mGatt.getServices());

                for (BluetoothGattService service : mGatt.getServices()) {
                    if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                        if (service.getCharacteristics() != null) {
                            for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                                    /*Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                                    mGatt.setCharacteristicNotification(chars, true);
                                    chars.setValue("COMPILATO DA GIGI");
                                    gatt.beginReliableWrite();
                                    gatt.writeCharacteristic(chars);
                                    gatt.executeReliableWrite();
                                    Log.d(TAG, "OUD: " + "caratteristica ok");
                                    if (chars.getDescriptors() != null) {
                                        for (BluetoothGattDescriptor desc : chars.getDescriptors()) {
                                            if (desc.getUuid().toString().equals(Constants.Descriptor_UUID.toString())) {
                                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                                boolean res = mGatt.writeDescriptor(desc);
                                                Log.d(TAG, "OUD: " +  "Desc Discovered: " + res);
                                            }
                                        }

                                    }*/
                                }
                            }
                        }
                    }
                }
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
                Log.d(TAG, "OUD: " + mGatt.getDevice().getName());
                for (BluetoothGattService service : mGatt.getServices()) {
                    if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                        if (service.getCharacteristics() != null) {
                            for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                                    mGatt.setCharacteristicNotification(chars, true);
                                    chars.setValue("test string");
                                    gatt.beginReliableWrite();
                                    boolean res = mGatt.writeCharacteristic(chars);
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
        // TODO: 23/10/18 perchè auto connect è false?
        // PERCHÈ LUDOVICO HA DETTO CHE ALTRIMENTI SFACIOLA ¯\_(ツ)_/¯

        this.mGatt = user.getBluetoothDevice().connectGatt(context, false, mGattCallback);

        /*try {
            wait(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        user.setBluetoothGatt(this.mGatt);
        user.getBluetoothGatt().requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        user.getBluetoothGatt().connect();

        /*try {
            wait(600);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        this.mGatt.discoverServices();
    }
}

