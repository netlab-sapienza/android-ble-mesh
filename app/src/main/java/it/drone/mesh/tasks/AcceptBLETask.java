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
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Set;

import it.drone.mesh.common.Constants;
import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.models.Server;
import it.drone.mesh.server.ServerNode;


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
    private BluetoothGattCharacteristic mGattCharacteristicClientOnline;
    private BluetoothGattDescriptor mGattClientOnlineConfigurationDescriptor;
    private BluetoothGattDescriptor mGattClientOnlineDescriptor;

    private BluetoothGattCharacteristic mGattCharacteristicRoutingTable;
    private BluetoothGattDescriptor mGattDescriptorRoutingTable;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, String> messageMap;
    private Context context;
    private ServerNode mNode;
    private String id;
    private HashMap<String, BluetoothDevice> nearDeviceMap;


    public AcceptBLETask(final BluetoothAdapter mBluetoothAdapter, BluetoothManager mBluetoothManager, final Context context) {
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.mBluetoothManager = mBluetoothManager;
        this.context = context;
        messageMap = new HashMap<>();
        nearDeviceMap = null;
        mGattService = new BluetoothGattService(Constants.ServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mGattCharacteristic = new BluetoothGattCharacteristic(Constants.CharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattDescriptor = new BluetoothGattDescriptor(Constants.DescriptorUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattDescriptorNextId = new BluetoothGattDescriptor(Constants.NEXT_ID_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientConfigurationDescriptor = new BluetoothGattDescriptor(Constants.Client_Configuration_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientOnlineConfigurationDescriptor = new BluetoothGattDescriptor(Constants.ClientOnline_Configuration_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientOnlineDescriptor = new BluetoothGattDescriptor(Constants.DescriptorClientOnlineUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        mGattCharacteristicNextServerId = new BluetoothGattCharacteristic(Constants.CharacteristicNextServerIdUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattCharacteristicClientOnline = new BluetoothGattCharacteristic(Constants.ClientOnlineCharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
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

                Log.d(TAG, "OUD: " + "I've been asked to read from " + device.getName() + ", my value: " + new String(characteristic.getValue()));
                mGattServer.sendResponse(device, requestId, 0, 0, characteristic.getValue());
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            // WHAT HAPPENS WHEN I GET A CHARACTERISTIC WRITE REQ
            @Override
            public void onCharacteristicWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if (characteristic.getUuid().equals(Constants.CharacteristicNextServerIdUUID)) {
                    Log.d(TAG, "OUD: " + "nextId: " + new String(value));
                    characteristic.setValue(value);
                    mGattServer.sendResponse(device, requestId, 0, 0, value);
                } else if (characteristic.getUuid().equals(Constants.RoutingTableCharacteristicUUID)) {
                    if (value.length >= 2) {
                        byte flagByte = value[1];
                        if (Utility.getBit(flagByte, 0) == 1) {  //il primo bit del secondo byte indica che è la richiesta di unione alla rete da parte di un nuovo server
                            Log.d(TAG, "OUD: " + "Nuovo server nella rete ");
                            Utility.printByte(value[0]);
                            Utility.printByte(value[1]);
                            Utility.printByte(value[2]);
                            Utility.printByte(value[3]);
                            Utility.printByte(value[4]);
                            boolean isNearToMe = mNode.updateRoutingTable(value);
                            Log.d(TAG, "OUD : isNear ? : " + isNearToMe);
                            mNode.printMapStatus();
                            mGattServer.sendResponse(device, requestId, 0, 0, value);
                            byte[][] tempMap = new byte[16][ServerNode.SERVER_PACKET_SIZE];
                            mNode.parseMapToByte(tempMap);

                            int dim = tempMap.length * tempMap[0].length;
                            final byte[] message = new byte[dim];
                            for (int i = 0; i < tempMap.length; i++) {
                                for (int j = 0; j < tempMap[0].length; j++) {
                                    message[(i * tempMap[0].length) + j] = tempMap[i][j];
                                }
                            }
                            for (String idTemp : nearDeviceMap.keySet()) {
                                BluetoothDevice dev = nearDeviceMap.get(idTemp);
                                final ConnectBLETask client = Utility.createBroadCastNextServerIdClient(dev, new String(mGattCharacteristicNextServerId.getValue()), context, value);
                                client.startClient();
                            }
                            if (isNearToMe) {
                                String idNewServer = new String("" + (Utility.getBit(value[0], 0) + Utility.getBit(value[0], 1) * 2 + Utility.getBit(value[0], 2) * 4 + Utility.getBit(value[0], 3) * 8));
                                Utility.updateServerToAsk(mBluetoothAdapter, nearDeviceMap, idNewServer, new Listeners.OnNewServerDiscoveredListener() {
                                    @Override
                                    public void OnNewServerDiscovered(ScanResult server) {
                                        Log.d(TAG, "OUD: " + "Nuovo server scoperto!");
                                        final ConnectBLETask clientNuovoServ = Utility.createBroadcastRoutingTableClient(server.getDevice(), new String(mGattDescriptorRoutingTable.getValue()), context, message, getId());
                                        clientNuovoServ.startClient();
                                    }
                                });
                            }
                        } else {
                            Log.d(TAG, "OUD: " + "Nnuuova table nella rete value :" + value.length);
                            final String senderId = Utility.getStringId(value[0]);
                            byte[] correctMap = new byte[value.length - 2];
                            for (int i = 0; i < value.length - 2; i++) {
                                correctMap[i] = value[i + 2];
                            }
                            String previousMsg = messageMap.get(senderId);
                            if (previousMsg == null) previousMsg = "";

                            messageMap.put(senderId, previousMsg + new String(correctMap));
                            if (Utility.getBit(value[0], 0) != 0) {
                                Log.d(TAG, "OUD: " + "NOT LAST MESSAGE");
                                characteristic.setValue(value);
                                mGattServer.sendResponse(device, requestId, 0, 0, null);
                                return;
                            }
                            Log.d(TAG, "OUD: " + "LAST MESSAGE");
                            byte[][] map = Utility.buildMapFromString(messageMap.get(senderId));
                            mNode.printMapStatus();
                            mNode = ServerNode.buildRoutingTable(map, getId(), mNode.getClientList());

                            byte[] clientRoutingTable = new byte[ServerNode.MAX_NUM_SERVER + 1];
                            mNode.parseClientMapToByte(clientRoutingTable);

                            mGattCharacteristicClientOnline.setValue(clientRoutingTable);

                            BluetoothGattCharacteristic chara = characteristic.getService().getCharacteristic(Constants.ClientOnlineCharacteristicUUID);
                            for (BluetoothDevice dev : mNode.getClientList()) {
                                if (chara == null) break;
                                if (dev == null) continue;
                                boolean res = mGattServer.notifyCharacteristicChanged(dev, chara, false);
                                Log.d(TAG, "OUD: i've notified new client Online " + res);
                            }

                            mGattServer.sendResponse(device, requestId, 0, 0, null);
                            for (String idTemp : nearDeviceMap.keySet()) {
                                BluetoothDevice dev = nearDeviceMap.get(idTemp);
                                ConnectBLETask client = Utility.createBroadcastRoutingTableClient(dev, new String(mGattDescriptorRoutingTable.getValue()), context, messageMap.get(senderId).getBytes(), getId());
                                client.startClient();
                            }
                            messageMap.put(senderId, "");
                        }
                    } else {
                        mGattServer.sendResponse(device, requestId, 6, 0, null);
                    }
                } else {
                    final String valueReceived;
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
                        valueReceived = new String((correct_message));
                        Log.d(TAG, "OUD: " + valueReceived);
                        final int[] infoSorg = Utility.getByteInfo(sorgByte);
                        final int[] infoDest = Utility.getByteInfo(destByte);
                        //Log.d(TAG, "OUD: " + "id sorg: " + infoSorg[0] + "" + infoSorg[1]);
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
                                if ((infoDest[0] + "").equals(getId())) {
                                    BluetoothDevice dest = mNode.getClient("" + infoDest[1]);
                                    boolean res = mGattServer.notifyCharacteristicChanged(dest, characteristic, false);
                                    Log.d(TAG, "OUD: " + "Notification sent? --> " + res);
                                }
                            } catch (IllegalArgumentException e) {
                                Log.d(TAG, "OUD: " + e.getMessage());
                            }
                            return;
                        }
                        mGattServer.sendResponse(device, requestId, 0, 0, null);
                        final String message;
                        String messageReceived = previousMsg + valueReceived;
                        String[] infoMessage = messageReceived.split(";;");
                        infoMessage[1] = "" + (Integer.parseInt(infoMessage[1]) + 1);
                        message = infoMessage[0] + ";;" + infoMessage[1] + ";;" + infoMessage[2];
                        Log.d(TAG, "OUD: " + messageReceived);
                        messageMap.remove(senderId);
                        Handler mHandler = new Handler(Looper.getMainLooper());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Messaggio ricevuto dall'utente " + senderId + ", messaggio: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
                        if ((infoDest[0] + "").equals(getId())) {
                            //sono io il destinatario
                            if (infoDest[1] == 0) {
                                Log.d(TAG, "OUD: messaggio broadcoast");
                                return;
                            }
                            BluetoothDevice dest = mNode.getClient("" + infoDest[1]);
                            if (dest == null) {
                                Log.d(TAG, "OUD: " + "null");
                                return;
                            }
                            try {
                                characteristic.setValue(value);
                                boolean res = mGattServer.notifyCharacteristicChanged(dest, characteristic, false);
                                Log.d(TAG, "OUD: " + "Notification sent? --> " + res);
                            } catch (IllegalArgumentException e) {
                                Log.d(TAG, "OUD: " + e.getMessage());
                            }
                            return;
                        } else {
                            Log.d(TAG, "OUD: Non sono il destinatario");
                            final ServerNode dest = mNode.getServerToSend(infoDest[0] + "", getId(), mNode.getLastRequest() + 1);
                            Log.d(TAG, "OUD: next-hop : " + dest.getId());
                            final BluetoothDevice near = nearDeviceMap.get(dest.getId());
                            Log.d(TAG, "OUD: next-hop : " + near.getName());
                            final Server server = new Server(near, near.getName());
                            final ConnectBLETask connectBLETask = new ConnectBLETask(server, context, new BluetoothGattCallback() {
                                @Override
                                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                                        Log.i(TAG, "OUD: " + "Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                                        gatt.discoverServices();
                                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                        Log.i(TAG, "OUD: " + "Disconnected from GATT client " + gatt.getDevice().getName());
                                    }
                                    super.onConnectionStateChange(gatt, status, newState);
                                }

                                @Override
                                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                    Log.d(TAG, "OUD: " + "GATT: " + gatt.toString());
                                    BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                                    if (service == null) return;
                                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicUUID);
                                    if (characteristic == null) return;
                                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.DescriptorUUID);
                                    boolean res = gatt.readDescriptor(descriptor);
                                    Log.d(TAG, "OUD: " + "Read Server id: " + res);
                                    super.onServicesDiscovered(gatt, status);
                                }

                                @Override
                                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                    super.onCharacteristicWrite(gatt, characteristic, status);
                                }

                                @Override
                                public void onDescriptorRead(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Utility.sendMessage(message, gatt, infoSorg, infoDest, new Listeners.OnMessageSentListener() {
                                                @Override
                                                public void OnMessageSent(String message) {
                                                    Log.d(TAG, "OUD: OnMessageSent: messaggio inviato");
                                                }

                                                @Override
                                                public void OnCommunicationError(String error) {

                                                }
                                            });
                                        }
                                    }, 500);
                                    super.onDescriptorRead(gatt, descriptor, status);

                                }

                            });
                            connectBLETask.startClient();
                        }
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
                    } else {
                        mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                        mNode.setClientOnline("" + current_id, device);
                        String value = "" + (mNode.nextId(null));
                        mGattDescriptorNextId.setValue(value.getBytes());


                        Log.d(TAG, "OUD: " + "NextId: " + value);
                    }
                } else if (descriptor.getUuid().equals(Constants.RoutingTableDescriptorUUID)) {
                    Log.d(TAG, "OUD: " + "Descr : " + new String(descriptor.getValue()));
                    mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                } else { //richiesta preliminare dell'id del server per connettermici come client
                    mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                }
                //Log.d(TAG, "OUD: " + new String(mGattDescriptor.getValue()));

                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d(TAG, "OUD: " + "I've been asked to write descriptor from " + device.getName() + "    " + device.getAddress());
                Log.d(TAG, "OUD: " + "I've been asked to write descriptor " + descriptor.getUuid());
                descriptor.setValue(value);
                if (responseNeeded) {
                    boolean res = mGattServer.sendResponse(device, requestId, 0, 0, value);
                    Log.d(TAG, "OUD: " + res);
                } else Log.d(TAG, "OUD: " + "response not needed");
                if (descriptor.getUuid().equals(Constants.ClientOnline_Configuration_UUID)) {
                    int currentid = -1;
                    BluetoothDevice[] clientList = mNode.getClientList();
                    for (int i = 0; i < clientList.length; i++) {
                        if (clientList[i] == null) continue;
                        if (clientList[i].equals(device)) {
                            currentid = i;
                        }
                    }

                    byte[] val = mGattCharacteristicClientOnline.getValue();
                    val[Integer.parseInt(id)] = Utility.setBit(val[Integer.parseInt(id)], currentid);
                    mGattCharacteristicClientOnline.setValue(val);

                    BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                    for (BluetoothDevice dev : mNode.getClientList()) {
                        if (dev == null) continue;
                        boolean res = mGattServer.notifyCharacteristicChanged(dev, characteristic, false);
                        Log.d(TAG, "OUD: i've notified new client Online " + res);
                    }

                    for (String idTemp : nearDeviceMap.keySet()) {
                        BluetoothDevice dev = nearDeviceMap.get(idTemp);
                        ConnectBLETask client = Utility.createBroadcastNewClientOnline(dev, Integer.parseInt(getId()), currentid, context);
                        client.startClient();
                    }
                } else if (descriptor.getUuid().equals(Constants.DescriptorClientOnlineUUID)) {
                    byte[] val = mGattCharacteristicClientOnline.getValue();
                    int[] infoNewCLient = Utility.getByteInfo(descriptor.getValue()[0]);
                    if (Utility.getBit(val[infoNewCLient[0]], infoNewCLient[1]) == 1) {
                        Log.d(TAG, "OUD: " + "Client gia settato");
                    } else {
                        val[infoNewCLient[0]] = Utility.setBit(val[infoNewCLient[0]], infoNewCLient[1]);
                        mGattCharacteristicClientOnline.setValue(val);
                        for (BluetoothDevice dev : mNode.getClientList()) {
                            if (dev == null) continue;
                            boolean res = mGattServer.notifyCharacteristicChanged(dev, mGattCharacteristicClientOnline, false);
                            Log.d(TAG, "OUD: i've notified new client Online " + res);
                        }
                        for (String idTemp : nearDeviceMap.keySet()) {
                            BluetoothDevice dev = nearDeviceMap.get(idTemp);
                            ConnectBLETask client = Utility.createBroadcastNewClientOnline(dev, infoNewCLient[0], infoNewCLient[1], context);
                            client.startClient();
                        }
                        mNode.getServer("" + infoNewCLient[0]).setClientOnline("" + infoNewCLient[1], null);
                    }

                }
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
        Log.d(TAG, "OUD: setId: " + id);
        this.id = id;
    }

    public String getId() {
        return this.id;
    }


    public void initializeId(final int offset) {
        if (offset >= nearDeviceMap.size()) {
            //TODO: senti @Andrea per fare restart Init Activity
            Log.e(TAG, "initializeId: offset >= nearDeviceMap.size()");
            return;
        }
        Set<String> set = nearDeviceMap.keySet();

        String[] arr = new String[set.size()];
        arr = set.toArray(arr);
        BluetoothDevice dev = nearDeviceMap.get(arr[offset]);
        if (dev == null) {
            Log.d(TAG, "OUD: " + "device null");
            return; //vedi sopra
        }
        final Server u = new Server(dev, dev.getName());
        ConnectBLETask client = new ConnectBLETask(u, context, new BluetoothGattCallback() {
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
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
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
                    if (characteristic.getValue().length == 0) {
                        Log.d(TAG, "OUD: length = 0");
                        setId("2");
                    } else {
                        Log.d(TAG, "OUD: " + "Read: " + new String(characteristic.getValue()));
                        setId(new String(characteristic.getValue()));
                    }

                    mNode = new ServerNode(id);
                    for (String temp : nearDeviceMap.keySet()) {
                        Log.d(TAG, "OUD: " + "vicini : " + temp);
                        mNode.addNearServer(new ServerNode(temp));
                    }
                    mGattDescriptor.setValue(id.getBytes());
                    mGattDescriptorRoutingTable.setValue("0".getBytes());
                    mGattCharacteristic.addDescriptor(mGattDescriptor);
                    mGattDescriptorNextId.setValue("1".getBytes());
                    mGattCharacteristicClientOnline.setValue(new byte[17]); //16 max server nella rete + 1 di scarto
                    boolean ret = mGattCharacteristicNextServerId.setValue(("" + (Integer.parseInt(getId()) + 1)).getBytes());
                    Log.d(TAG, "OUD: Set Value: --> " + new String(mGattCharacteristicNextServerId.getValue()));
                    mGattCharacteristic.addDescriptor(mGattDescriptorNextId);
                    mGattCharacteristic.addDescriptor(mGattClientConfigurationDescriptor);
                    mGattCharacteristicRoutingTable.addDescriptor(mGattDescriptorRoutingTable);
                    mGattCharacteristicClientOnline.addDescriptor(mGattClientOnlineConfigurationDescriptor);
                    mGattCharacteristicClientOnline.addDescriptor(mGattClientOnlineDescriptor);
                    mGattService.addCharacteristic(mGattCharacteristic);
                    mGattService.addCharacteristic(mGattCharacteristicNextServerId);
                    mGattService.addCharacteristic(mGattCharacteristicRoutingTable);
                    mGattService.addCharacteristic(mGattCharacteristicClientOnline);
                    // I START OPEN THE GATT SERVER
                    mGattServer = mBluetoothManager.openGattServer(context, mGattServerCallback);
                    //mGattServer.addService(mGattServiceRoutingTable);
                    mGattServer.addService(mGattService);
                    characteristic.setValue(("" + (Integer.parseInt(getId()) + 1)).getBytes());
                    gatt.beginReliableWrite();
                    boolean res = gatt.writeCharacteristic(characteristic);
                    Log.d(TAG, "OUD: " + "Write Characteristic :--> " + res);
                    gatt.executeReliableWrite();
                } else {
                    initializeId(offset + 1);
                }
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(Constants.CharacteristicNextServerIdUUID)) {
                    Log.d(TAG, "OUD: " + "I wrote a characteristic nextServerId! " + new String(characteristic.getValue()));
                    BluetoothGattService serv = gatt.getService(Constants.ServiceUUID);
                    if (serv == null) return;
                    BluetoothGattCharacteristic characteristic1 = serv.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                    if (characteristic1 == null) return;
                    byte[] message = mNode.parseNewServer();
                    Log.d(TAG, "Message len: " + message.length);
                    characteristic1.setValue(message);
                    gatt.beginReliableWrite();
                    boolean res = gatt.writeCharacteristic(characteristic1);
                    Log.d(TAG, "OUD: " + "Write Characteristic :--> " + res);
                    gatt.executeReliableWrite();
                } else if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(Constants.RoutingTableCharacteristicUUID)) {
                    Log.d(TAG, "OUD: " + "i wrote a characterisic routing table!");
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
        client.startClient();
    }


    public void startServer() {
        // I CREATE A SERVICE WITH 1 CHARACTERISTIC AND 1 DESCRIPTOR
        Log.d(TAG, "OUD: " + "dev found:" + nearDeviceMap.size());


        if (nearDeviceMap != null && nearDeviceMap.keySet().size() != 0) {
            Log.d(TAG, "OUD: " + "startServer: Finding next id");
            initializeId(0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "OUD: size r.t. +" + mNode.getRoutingTable().size());
                }
            }, 2000);
        } else {
            setId("1");
            mNode = new ServerNode(id);
            mGattCharacteristicNextServerId.setValue("2".getBytes());
            Log.d(TAG, "OUD: startServer: en");
            mGattDescriptorRoutingTable.setValue("1".getBytes());
            mGattDescriptor.setValue(id.getBytes());
            mGattCharacteristicClientOnline.setValue(new byte[17]);
            this.mGattCharacteristic.addDescriptor(mGattDescriptor);
            mGattDescriptorNextId.setValue("1".getBytes());
            this.mGattCharacteristic.addDescriptor(mGattDescriptorNextId);
            this.mGattCharacteristic.addDescriptor(mGattClientConfigurationDescriptor);
            mGattCharacteristicClientOnline.addDescriptor(mGattClientOnlineConfigurationDescriptor);
            mGattCharacteristicClientOnline.addDescriptor(mGattClientOnlineDescriptor);
            this.mGattCharacteristicRoutingTable.addDescriptor(mGattDescriptorRoutingTable);
            this.mGattService.addCharacteristic(mGattCharacteristic);
            this.mGattService.addCharacteristic(mGattCharacteristicNextServerId);
            this.mGattService.addCharacteristic(mGattCharacteristicRoutingTable);
            this.mGattService.addCharacteristic(mGattCharacteristicClientOnline);
            // I START OPEN THE GATT SERVER
            this.mGattServer = mBluetoothManager.openGattServer(context, mGattServerCallback);
            this.mGattServer.addService(this.mGattService);

            Log.d(TAG, "OUD: " + "startServer: Inizializzato servizi e tutto");
        }
    }

    public void stopServer() {
        this.mGattServer.clearServices();
        this.mGattServer.close();
    }

    public void insertMapDevice(HashMap<String, BluetoothDevice> nearDeviceMap) {
        for (String id : nearDeviceMap.keySet()) {
            Log.d(TAG, "OUD: id: " + id + "Device: " + nearDeviceMap.get(id).getName());
        }
        this.nearDeviceMap = nearDeviceMap;
    }
}
