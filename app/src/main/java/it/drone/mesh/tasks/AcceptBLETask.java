package it.drone.mesh.tasks;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.utility.Constants;

public class AcceptBLETask {
    private final static String TAG = AcceptBLETask.class.getName();
    private BluetoothGattServer mGattServer;
    private BluetoothGattServerCallback mGattServerCallback;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private BluetoothGattDescriptor mGattDescriptor;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Context context;

    public AcceptBLETask(final BluetoothAdapter mBluetoothAdapter, BluetoothManager mBluetoothManager, final Context context) {
        this.mBluetoothAdapter = mBluetoothAdapter;
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
                    Log.d(TAG, "OUD: " + "I'm the server, I've connected to " + device.getName());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "OUD: " + "onConnectionStateChange: DISCONNECTED from" + device.getName());
                }
                super.onConnectionStateChange(device, status, newState);
            }

            // DO SOMETHING WHEN A SERVICE IS ADDED
            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.d(TAG, "OUD: " + "I've added a service " + service.toString());
                Log.d(TAG, "OUD: " + "startServer -> ServiceUID : " + service.getUuid().toString());
                //Log.d(TAG, "OUD: " + "startServer -> CharUID : " + this.mGattCharacteristic.getUuid().toString());
                //Log.d(TAG, "OUD: " + "startServer -> DescriptorUID : " + this.mGattDescriptor.getUuid().toString());
                super.onServiceAdded(status, service);
            }

            // WHAT HAPPENS WHEN I GET A CHARACTERISTIC READ REQ
            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                final BluetoothDevice tempdev = device;
                Log.d(TAG, "OUD: " + "I've been asked to read from " + tempdev.getName());
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            // WHAT HAPPENS WHEN I GET A CHARACTERISTIC WRITE REQ
            @Override
            public void onCharacteristicWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                final String valueReceived;
                Log.d(TAG, "OUD: " + "I've been asked to write from " + device.getName() + "    " + device.getAddress());
                Log.d(TAG, "OUD: " + "Device address: " + device.getAddress());
                Log.d(TAG, "OUD: " + "ReqId: " + requestId);
                Log.d(TAG, "OUD: " + "PreparedWrite: " + preparedWrite);
                Log.d(TAG, "OUD: " + "offset: " + offset);
                Log.d(TAG, "OUD: " + "BytesN: " + value.length);
                if (value.length > 0) {
                    valueReceived = new String(value);
                    Log.d(TAG, "OUD: " + valueReceived);

                    final BluetoothLeScanner mBluetoothScan = mBluetoothAdapter.getBluetoothLeScanner();
                    final ScanCallback mScanCallback = new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            super.onScanResult(callbackType, result);

                            Log.d(TAG, "OUD: " + result.toString());

                            // IF THE NEWLY DISCOVERED USER IS IN MY LIST OF USER, RETURNS
                            final User user = new User(result.getDevice(), result.getDevice().getName());
                            boolean newUser = true;
                            for (User temp : UserList.getUserList()) {
                                if (temp.getBluetoothDevice().getName().equals(user.getUserName())) {
                                    newUser = false;
                                }
                            }

                            Log.d(TAG, "onScanResult: Nuovo SERVER");

                            // STARTS THE GATT+

                            if (newUser) {
                                UserList.addUser(user);
                                ConnectBLETask connectBLETask = new ConnectBLETask(user, context, new BluetoothGattCallback() {
                                    @Override
                                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                                            Log.i(TAG, "OUD:" + "Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                                            gatt.discoverServices();
                                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                            Log.i(TAG, "OUD:" + "Disconnected from GATT client " + gatt.getDevice().getName());
                                        }
                                    }

                                    @Override
                                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                        Log.d(TAG, "OUD: " + "GATT: " + gatt.toString());
                                        Log.d(TAG, "OUD: " + "I discovered services from " + gatt.getDevice().getName());
                                        List<User> temp = UserList.getUserList();
                                        for (User user : temp) {
                                            if (user.getUserName().equals(device.getName())) {
                                                Log.d(TAG, "OUD: " + device.getName() + "è il mittente");
                                                continue;
                                            }
                                            Log.d(TAG, "OUD: " + "Provo ad inviare a " + user.getUserName());
                                            Log.d(TAG, "OUD: " + "size service: " + gatt.getServices().size());
                                            for (BluetoothGattService service : gatt.getServices()) {
                                                //Log.d(TAG, "OUD: " + "onServicesDiscovered: " + service.toString());
                                                //Log.d(TAG, "OUD: " + "NOSTRO UUID: " + Constants.Service_UUID.toString());
                                                //Log.d(TAG, "OUD: " + "SUO UUID: " + service.getUuid().toString());
                                                if (new ParcelUuid(service.getUuid()).equals(Constants.Service_UUID)) {
                                                    // Log.d(TAG, "OUD: "+ "onServicesDiscovered: yeeee");
                                                    if (service.getCharacteristics() != null) {
                                                        for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                                            //Log.d(TAG, "OUD: " + "CharCHar: " + chars.getUuid().toString());
                                                            if (chars.getUuid().equals(Constants.Characteristic_UUID.getUuid())) {
                                                                Log.d(TAG, "OUD: " + "Char: " + chars.toString());
                                                                gatt.setCharacteristicNotification(chars, true);
                                                                chars.setValue(valueReceived);
                                                                gatt.beginReliableWrite();
                                                                boolean res = gatt.writeCharacteristic(chars);
                                                                gatt.executeReliableWrite();
                                                                Log.d(TAG, "OUD: " + "messaggio reinviato a " + gatt.getDevice().getName() + " ? -->" + res);
                                                            }
                                                        }
                                                    } else
                                                        Log.d(TAG, "OUD: " + "onServicesDiscovered: no characteristics");
                                                }
                                            }
                                            Log.d(TAG, "OUD: " + "onServicesDiscovered: fine for");
                                        }
                                    }

                                    @Override
                                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                        super.onCharacteristicWrite(gatt, characteristic, status);
                                    }
                                });
                                connectBLETask.startClient();
                            }
                        }
                    };

                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "OUD: " + "Stopping Scanning");

                            // Stop the scan, wipe the callback.
                            mBluetoothScan.stopScan(mScanCallback);

                        }
                    }, 5000);
                    UserList.cleanUserList();
                    mBluetoothScan.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
                }
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d(TAG, "OUD: " + "I've been asked to read descriptor from " + device.getName());
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "OUD: " + "I've been asked to write descriptor from " + device.getName() + "    " + device.getAddress());
                Log.d(TAG, "OUD: " + "Device address: " + device.getAddress());
                Log.d(TAG, "OUD: " + "ReqId: " + requestId);
                Log.d(TAG, "OUD: " + "PreparedWrite: " + preparedWrite);
                Log.d(TAG, "OUD: " + "offset: " + offset);
                Log.d(TAG, "OUD: " + "BytesN: " + value.length);
                Log.d(TAG, "OUD: " + "I've been asked to write descriptor from " + device.getName());
                Log.d(TAG, "OUD: " + new String(descriptor.getCharacteristic().getValue(), Charset.defaultCharset()));
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                final BluetoothDevice tempdev = device;
                Log.d(TAG, "OUD: " + "I'm writing from " + tempdev.getName());
                super.onExecuteWrite(device, requestId, execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.d(TAG, "OUD: " + "I've notified " + device.getName());
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
    }

    public void stopServer() {
        this.mGattServer.clearServices();
        this.mGattServer.close();
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        //builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        return builder.build();
    }
}
