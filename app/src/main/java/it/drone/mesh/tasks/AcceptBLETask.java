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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.roles.common.Constants;
import it.drone.mesh.roles.common.Utility;
import it.drone.mesh.roles.server.ServerNode;


public class AcceptBLETask {
    private final static String TAG = AcceptBLETask.class.getName();
    private BluetoothGattServer mGattServer;
    private BluetoothGattServerCallback mGattServerCallback;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private BluetoothGattDescriptor mGattDescriptor;
    private BluetoothGattDescriptor mGattClientConfigurationDescriptor;
    private BluetoothGattDescriptor mGattDescriptorNextId;
    private BluetoothGattCharacteristic mGattCharacteristicNextServerId;
    private BluetoothGattService mGattServiceRoutingTable;
    private BluetoothGattCharacteristic mGattCharacteristicRoutingTable;
    private BluetoothGattDescriptor mGattDescriptorRoutingTable;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, String> messageMap;
    private Context context;
    private ServerNode mNode;
    private LinkedList<ScanResult> serversToAsk;
    private String id;

    public AcceptBLETask(final BluetoothAdapter mBluetoothAdapter, BluetoothManager mBluetoothManager, final Context context) {
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.mBluetoothManager = mBluetoothManager;
        this.context = context;
        messageMap = new HashMap<>();
        serversToAsk = null;
        mGattService = new BluetoothGattService(Constants.ServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mGattCharacteristic = new BluetoothGattCharacteristic(Constants.CharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattDescriptor = new BluetoothGattDescriptor(Constants.DescriptorUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattDescriptorNextId = new BluetoothGattDescriptor(Constants.NEXT_ID_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientConfigurationDescriptor = new BluetoothGattDescriptor(Constants.Client_Configuration_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattCharacteristicNextServerId = new BluetoothGattCharacteristic(Constants.CharacteristicNextServerIdUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattServiceRoutingTable = new BluetoothGattService(Constants.RoutingTableServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mGattCharacteristicRoutingTable = new BluetoothGattCharacteristic(Constants.RoutingTableCharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattDescriptorRoutingTable = new BluetoothGattDescriptor(Constants.RoutingTableDescriptorUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
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
                Log.d(TAG, "OUD: " + "I've been asked to read from " + device.getName());
                mGattServer.sendResponse(device, requestId, 0, 0, null);
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            // WHAT HAPPENS WHEN I GET A CHARACTERISTIC WRITE REQ
            @Override
            public void onCharacteristicWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if (characteristic.getUuid().equals(Constants.CharacteristicNextServerIdUUID)) {
                    mGattServer.sendResponse(device,requestId,0,0,value);
                }
                else if(characteristic.getUuid().equals(Constants.RoutingTableCharacteristicUUID)){
                    if (value.length >= 4) {
                        byte flagByte = value[1];
                        if (Utility.getBit(flagByte,0) == 1) {  //NuovoServer
                            mNode.updateRoutingTable(value);
                            mGattServer.sendResponse(device, requestId, 0, 0, null);
                            for (ScanResult temp : serversToAsk) {
                                ConnectBLETask client = Utility.sendBroadcastNextServerid(device,new String(value),context,value);
                                client.startClient();
                                try {
                                    Thread.sleep(1000);
                                    client.stopClient();
                                } catch (Exception e) {
                                    client.stopClient();
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }

                            }
                        }
                        else{
                            final String senderId = Utility.getStringId(value[0]);
                            byte[] correctMap = new byte[value.length - 2];
                            for (int i = 0; i < value.length - 2; i++) {
                                correctMap[i] = value[i + 2];
                            }
                            String previousMsg = messageMap.get(senderId);
                            if (previousMsg == null) previousMsg = "";
                            messageMap.put(senderId, previousMsg + new String(value));
                            if (Utility.getBit(value[0],0) != 0) {
                                mGattServer.sendResponse(device, requestId, 0, 0, null);
                                return;
                            }
                            mNode = ServerNode.buildRoutingTable(Utility.buildMapFromString(messageMap.get(senderId)), getId());
                            mGattServer.sendResponse(device, requestId, 0, 0, null);
                            for (ScanResult temp : serversToAsk) {
                                ConnectBLETask client = Utility.sendBroadcastRoutingTable(temp.getDevice(), new String(mGattDescriptorRoutingTable.getValue()), context, messageMap.get(senderId).getBytes(), getId());
                                client.startClient();
                                try {
                                    Thread.sleep(1000);
                                    client.stopClient();
                                } catch (Exception e) {
                                    client.stopClient();
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }

                            }
                            messageMap.put(senderId, "");
                        }

                    }
                    else {
                        mGattServer.sendResponse(device, requestId, 6, 0, null);
                    }
                }
                else { //è la caratteristica dei messaggi
                    final String valueReceived;
                    //mGattServer.notifyCharacteristicChanged(mNode.getClient("1"),characteristic,false);
                    Log.d(TAG, "OUD: " + "I've been asked to write from " + device.getName() + "  address:  " + device.getAddress());
                    Log.d(TAG, "OUD: " + "Device address: " + device.getAddress());
                    Log.d(TAG, "OUD: " + "ReqId: " + requestId);
                    Log.d(TAG, "OUD: " + "PreparedWrite: " + preparedWrite);
                    Log.d(TAG, "OUD: " + "offset: " + offset);
                    Log.d(TAG, "OUD: " + "BytesN: " + value.length);
                    if (value.length > 0) {
                        byte[] correct_message = new byte[value.length - 2];
                        byte sorgByte = value[0];
                        byte destByte = value[1];
                        for (int i = 0; i < value.length - 2; i++) {
                            correct_message[i] = value[i + 2];
                        }
                        valueReceived = new String(correct_message);
                        Log.d(TAG, "OUD: " + valueReceived);
                        final int[] infoSorg = Utility.getByteInfo(sorgByte);
                        final int[] infoDest = Utility.getByteInfo(destByte);
                        Log.d(TAG, "OUD: " + "id sorg: " + infoSorg[0] + "" + infoSorg[1]);
                        Log.d(TAG, "OUD: " + "id dest: " + infoDest[0] + "" + infoDest[1]);
                        final String senderId = Utility.getStringId(sorgByte);
                        String previousMsg = messageMap.get(senderId);
                        if (previousMsg == null) previousMsg = "";
                        messageMap.put(senderId, previousMsg + valueReceived);

                        if (Utility.getBit(sorgByte, 0) != 0) {
                            Log.d(TAG, "OUD: " + "NOT last message");
                            mGattServer.sendResponse(device, requestId, 0, 0, null);
                            try {
                                characteristic.setValue(value);
                                BluetoothDevice dest = mNode.getClient("" + infoDest[1]);
                                boolean res = mGattServer.notifyCharacteristicChanged(dest, characteristic, false);
                                Log.d(TAG, "OUD: " + "Notification sent? --> " + res);
                            } catch (IllegalArgumentException e) {
                                Log.d(TAG, "OUD: " + e.getMessage());
                            }
                            return;
                        }
                        mGattServer.sendResponse(device, requestId, 0, 0, null);
                        final String message = previousMsg + valueReceived;
                        Log.d(TAG, "OUD: " + message);
                        messageMap.remove(senderId);
                        Handler mHandler = new Handler(Looper.getMainLooper());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Messaggio ricevuto dall'utente " + senderId + ", messaggio: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
                        BluetoothDevice dest = mNode.getClient("" + infoDest[1]);   // TODO: 05/12/18 Controllare a quale server mandarlo
                        if (dest == null) Log.d(TAG, "OUD: " + "null");
                        try {
                            characteristic.setValue(value);
                            boolean res = mGattServer.notifyCharacteristicChanged(dest, characteristic, false);
                            Log.d(TAG, "OUD: " + "Notification sent? --> " + res);
                        } catch (IllegalArgumentException e) {
                            Log.d(TAG, "OUD: " + e.getMessage());
                        }
                        final BluetoothLeScanner mBluetoothScan = mBluetoothAdapter.getBluetoothLeScanner();
                        final ScanCallback mScanCallback = new ScanCallback() {
                            @Override
                            public void onScanResult(int callbackType, final ScanResult result) {
                                super.onScanResult(callbackType, result);
                                // IF THE NEWLY DISCOVERED USER IS IN MY LIST OF USER, RETURNS
                                final User user = new User(result.getDevice(), result.getDevice().getName());
                                boolean newUser = true;
                                for (User temp : UserList.getUserList()) {
                                    Log.d(TAG, "OUD: " + "UserlistDevice: " + temp.getUserName());
                                    if (temp.getBluetoothDevice().getName().equals(user.getUserName())) {
                                        newUser = false;
                                    }
                                }

                                // STARTS THE GATT+
                                if (newUser) {
                                    Log.d(TAG, "onScanResult: Nuovo SERVER");
                                    UserList.addUser(user);
                                    ConnectBLETask connectBLETask = new ConnectBLETask(user, context, new BluetoothGattCallback() {
                                        // TODO: 23/11/18 MIGLIORARE il broadcast ora abbiamo gli ID
                                        @Override
                                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                                Log.i(TAG, "OUD: " + "Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                                                gatt.discoverServices();
                                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                                Log.i(TAG, "OUD: " + "Disconnected from GATT client " + gatt.getDevice().getName());
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
                                                //boolean res = Utility.sendMessage(message, gatt, infoSorg); non ha senso
                                                //Log.d(TAG, "OUD: " + "Inviato ? " + res);
                                            }
                                        }

                                        @Override
                                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                            super.onCharacteristicWrite(gatt, characteristic, status);
                                            Log.d(TAG, "OUD: " + "i broadcasted char " + gatt.getDevice());
                                        }
                                    });
                                    connectBLETask.startClient();
                                    try {
                                        Thread.sleep(500);
                                    } catch (Exception e) {
                                        Log.d(TAG, "OUD: " + "Andata male la wait");
                                    }
                                }
                            }
                        };

                        //mHandler = new Handler(Looper.getMainLooper());
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "OUD: " + "Stopping Scanning");

                                // Stop the scan, wipe the callback.
                                mBluetoothScan.stopScan(mScanCallback);

                            }
                        }, 5000);
                        // TODO: 07/11/2018 perchè ogni volta si pulisce la lista?
                        UserList.cleanUserList();
                        mBluetoothScan.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
                    }
                }
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d(TAG, "OUD: " + "I've been asked to read descriptor from " + device.getName());
                if (descriptor.getUuid().toString().equals((mGattDescriptorNextId.getUuid().toString()))) {
                    int current_id = mNode.nextId(device);
                    if (current_id == -1) {
                        mGattServer.sendResponse(device, requestId, 6, 0, descriptor.getValue());
                    }
                    else {
                        // TODO: 24/11/18 notify all client that another one is online
                        mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                        mNode.setClientOnline("" + current_id, device);
                        String value = "" + (current_id + 1);
                        mGattDescriptorNextId.setValue(value.getBytes());
                        Log.d(TAG, "OUD: " + "NextId: " + value);
                    }
                }
                else {
                    if (mNode.isFull()) {
                        mGattServer.sendResponse(device, requestId, 6, 0, descriptor.getValue());
                    }
                    else {
                        mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                    }
                }
                Log.d(TAG, "OUD: " + new String(mGattDescriptor.getValue()));

                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "OUD: " + "I've been asked to write descriptor from " + device.getName() + "    " + device.getAddress());
                Log.d(TAG, "OUD: " + "I've been asked to write descriptor " + descriptor.getUuid());
                if (responseNeeded) {
                    boolean res = mGattServer.sendResponse(device, requestId, 0, 0, value);
                    Log.d(TAG, "OUD: " + res);
                } else Log.d(TAG, "OUD: " + "response not needed");
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.d(TAG, "OUD: " + "I'm writing from " + device.getName());
                mGattServer.sendResponse(device, requestId, 0, 0, null);
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

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return this.id;
    }

    public void startServer() {
        // I CREATE A SERVICE WITH 1 CHARACTERISTIC AND 1 DESCRIPTOR
        String next_id = "1";
        // TODO: 23/11/18 CREAZIONE ID SERVER

        if(serversToAsk != null) {
            for (ScanResult temp : serversToAsk) {
                if (temp.getDevice() == null) continue;
                final User u = new User(temp.getDevice(),temp.getDevice().getName());
                ConnectBLETask client = new ConnectBLETask(u, context, new BluetoothGattCallback() {
                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        BluetoothGattService service =  gatt.getService(Constants.ServiceUUID);
                        if (service == null) return;
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicNextServerIdUUID);
                        if (characteristic == null) return;
                        boolean res = gatt.readCharacteristic(characteristic);
                        Log.d(TAG, "OUD: " + "Read Characteristic nextServerID: " + res);
                        super.onServicesDiscovered(gatt, status);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            setId(new String(characteristic.getValue()));
                            mNode = new ServerNode(id);
                            characteristic.setValue(""+(Integer.parseInt(getId())+1));
                            gatt.beginReliableWrite();
                            boolean res = gatt.writeCharacteristic(characteristic);
                            Log.d(TAG, "OUD: " + "Write Characteristic :--> " + res);
                            gatt.executeReliableWrite();
                        }
                        super.onCharacteristicRead(gatt, characteristic, status);
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(Constants.CharacteristicNextServerIdUUID)) {
                            Log.d(TAG, "OUD: " + "I wrote a characteristic !");
                            BluetoothGattService serv =  gatt.getService(Constants.RoutingTableServiceUUID);
                            if (serv == null) return;
                            BluetoothGattCharacteristic characteristic1 = serv.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                            if (characteristic1 == null) return;
                            characteristic1.setValue(mNode.parseNewServer());
                            gatt.beginReliableWrite();
                            boolean res = gatt.writeCharacteristic(characteristic1);
                            Log.d(TAG, "OUD: " + "Write Characteristic :--> " + res);
                            gatt.executeReliableWrite();
                        } else if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(Constants.RoutingTableCharacteristicUUID)) {
                            Log.d(TAG, "OUD: " + "i wrote a characterisic!");
                        }
                        super.onCharacteristicWrite(gatt, characteristic, status);
                    }
                });
                client.startClient();
            }
        }
        else {
            setId("1");
            mNode = new ServerNode(id);
            mGattCharacteristicNextServerId.setValue("2");
        }
        String s = "0";
        mGattDescriptorRoutingTable.setValue(s.getBytes());
        mGattDescriptor.setValue(id.getBytes());
        this.mGattCharacteristic.addDescriptor(mGattDescriptor);
        mGattDescriptorNextId.setValue(next_id.getBytes());
        this.mGattCharacteristic.addDescriptor(mGattDescriptorNextId);
        this.mGattCharacteristic.addDescriptor(mGattClientConfigurationDescriptor);
        this.mGattCharacteristicRoutingTable.addDescriptor(mGattDescriptorRoutingTable);
        this.mGattService.addCharacteristic(mGattCharacteristic);
        this.mGattService.addCharacteristic(mGattCharacteristicNextServerId);
        this.mGattServiceRoutingTable.addCharacteristic(mGattCharacteristicRoutingTable);
        // I START OPEN THE GATT SERVER
        this.mGattServer = mBluetoothManager.openGattServer(context, mGattServerCallback);
        this.mGattServer.addService(this.mGattService);
        this.mGattServer.addService(this.mGattServiceRoutingTable);


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

    public void setStartServerList(LinkedList<ScanResult> l) {
        this.serversToAsk = l;
    }

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
