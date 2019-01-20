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
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import it.drone.mesh.common.Constants;
import it.drone.mesh.common.RoutingTable;
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

    private BluetoothGattDescriptor mGattClientWithInternetDescriptor;

    private BluetoothGattCharacteristic mGattCharacteristicRoutingTable;
    private BluetoothGattDescriptor mGattDescriptorRoutingTable;
    private BluetoothManager mBluetoothManager;
    private HashMap<String, String> messageMap;
    private Context context;
    private ServerNode mNode;
    private String id;
    private HashMap<String, BluetoothDevice> nearDeviceMap;
    private ArrayList<OnConnectionRejectedListener> connectionRejectedListeners;
    private ArrayList<OnRoutingTableUpdatedListener> routingTableUpdatedListeners;
    private ArrayList<Listeners.OnMessageWithInternetListener> messageReceivedWithInternetListeners;
    private ArrayList<Listeners.OnServerInitializedListener> serverInitializedListeners;
    private ArrayList<Listeners.OnMessageReceivedListener> messageReceivedListeners;
    private RoutingTable routingTable;


    public AcceptBLETask(final BluetoothAdapter mBluetoothAdapter, BluetoothManager mBluetoothManager, final Context context) {
        this.mBluetoothManager = mBluetoothManager;
        this.context = context;
        connectionRejectedListeners = new ArrayList<>();
        routingTableUpdatedListeners = new ArrayList<>();
        messageReceivedWithInternetListeners = new ArrayList<>();
        serverInitializedListeners = new ArrayList<>();
        messageReceivedListeners = new ArrayList<>();
        messageMap = new HashMap<>();
        nearDeviceMap = null;
        routingTable = RoutingTable.getInstance();
        mGattService = new BluetoothGattService(Constants.ServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mGattCharacteristic = new BluetoothGattCharacteristic(Constants.CharacteristicUUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        mGattDescriptor = new BluetoothGattDescriptor(Constants.DescriptorUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattDescriptorNextId = new BluetoothGattDescriptor(Constants.NEXT_ID_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientConfigurationDescriptor = new BluetoothGattDescriptor(Constants.Client_Configuration_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientOnlineConfigurationDescriptor = new BluetoothGattDescriptor(Constants.ClientOnline_Configuration_UUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientOnlineDescriptor = new BluetoothGattDescriptor(Constants.DescriptorClientOnlineUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        mGattClientWithInternetDescriptor = new BluetoothGattDescriptor(Constants.DescriptorClientWithInternetUUID, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

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
                            boolean isNearToMe = mNode.updateRoutingTable(value);
                            byte idByte = value[0];
                            int serverId = Utility.getBit(idByte, 0) + Utility.getBit(idByte, 1) * 2 + Utility.getBit(idByte, 2) * 4 + Utility.getBit(idByte, 3) * 8;
                            routingTable.addDevice(serverId, 0);
                            Log.d(TAG, "OUD : isNear ? : " + isNearToMe);
                            mNode.printMapStatus();
                            for (OnRoutingTableUpdatedListener l : routingTableUpdatedListeners) {
                                l.OnRoutingTableUpdated(mNode.getMapStringStatus());
                            }
                            mGattServer.sendResponse(device, requestId, 0, 0, value);
                            mGattDescriptorRoutingTable.setValue(("" + (Integer.parseInt(new String(mGattDescriptorRoutingTable.getValue())) + 1)).getBytes()); //incrementiamo la versione della routing table

                            byte[] clientRoutingTable = new byte[ServerNode.MAX_NUM_SERVER + 2];
                            mNode.parseClientMapToByte(clientRoutingTable);
                            mGattCharacteristicClientOnline.setValue(clientRoutingTable);  //Aggiorno client Char del nuovo server Online

                            for (BluetoothDevice dev : mNode.getClientList()) {
                                if (dev == null) continue;
                                boolean res = mGattServer.notifyCharacteristicChanged(dev, mGattCharacteristicClientOnline, false);
                                Log.d(TAG, "OUD: i've notified new server Online " + res);
                            }

                            byte[][] tempMap = new byte[16][ServerNode.SERVER_PACKET_SIZE];
                            mNode.parseMapToByte(tempMap);

                            int dim = tempMap.length * tempMap[0].length;
                            final byte[] message = new byte[dim];
                            for (int i = 0; i < tempMap.length; i++) { //passaggio della routing table da mapByte a array byte
                                System.arraycopy(tempMap[i], 0, message, (i * tempMap[0].length), tempMap[0].length);
                            }
                            for (String idTemp : nearDeviceMap.keySet()) {    //broadcast del nuovo server nella rete
                                BluetoothDevice dev = nearDeviceMap.get(idTemp);
                                final ConnectBLETask client = Utility.createBroadCastNextServerIdClient(dev, new String(mGattCharacteristicNextServerId.getValue()), context, value);
                                client.startClient();
                            }
                            if (isNearToMe) { //passaggio della table al nuovo server
                                String idNewServer = "" + (Utility.getBit(value[0], 0) + Utility.getBit(value[0], 1) * 2 + Utility.getBit(value[0], 2) * 4 + Utility.getBit(value[0], 3) * 8);
                                Utility.updateServerToAsk(mBluetoothAdapter, nearDeviceMap, idNewServer, new Listeners.OnNewServerDiscoveredListener() {
                                    @Override
                                    public void OnNewServerDiscovered(BluetoothDevice server) {
                                        Log.d(TAG, "OUD: " + "Nuovo server scoperto!");
                                        final ConnectBLETask clientNuovoServ = Utility.createBroadcastRoutingTableClient(server, new String(mGattDescriptorRoutingTable.getValue()), context, message, getId());
                                        clientNuovoServ.startClient();
                                    }

                                    @Override
                                    public void OnNewServerNotFound() {
                                        Log.e(TAG, "OUD: " + "NON IMPLEMENTATO");
                                    }
                                });
                            }
                        } else {
                            Log.d(TAG, "OUD: " + "Nuova table nella rete value :" + value.length);
                            final String senderId = Utility.getStringId(value[0]);
                            byte[] correctMap = new byte[value.length - 2];
                            System.arraycopy(value, 2, correctMap, 0, value.length - 2);
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
                            String stringMap = messageMap.get(senderId);
                            if(stringMap == null) return;
                            byte[][] map = Utility.buildMapFromString(stringMap);

                            mNode.printMapStatus();
                            mNode = ServerNode.buildRoutingTable(map, getId(), mNode.getClientList(), routingTable);

                            for (OnRoutingTableUpdatedListener l : routingTableUpdatedListeners)
                                l.OnRoutingTableUpdated(mNode.getMapStringStatus());

                            byte[] clientRoutingTable = new byte[ServerNode.MAX_NUM_SERVER + 2];
                            mNode.parseClientMapToByte(clientRoutingTable);

                            mGattCharacteristicClientOnline.setValue(clientRoutingTable);

                            BluetoothGattCharacteristic chara = characteristic.getService().getCharacteristic(Constants.ClientOnlineCharacteristicUUID);
                            for (BluetoothDevice dev : mNode.getClientList()) {  //ancora non viene utilizzato
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
                } else { //messaggio normale con/senza internet
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
                        System.arraycopy(value, 2, correct_message, 0, value.length - 2);
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
                        String clientInternet = "";
                        if (Utility.getBit(destByte, 0) == 1) {
                            //Messaggio con internet
                            if (mNode.hasInternet()) {
                                Log.d(TAG, "Ho io Internet continua");
                            } else {
                                for (int i = 0; i < 8; i++) {
                                    if (Utility.getBit(mNode.getClientByteInternet(), i) == 1) {
                                        //il mio client ha internet devo notificarlo
                                        clientInternet = "" + i;
                                        break;
                                    }
                                }
                            }
                        }

                        if (Utility.getBit(sorgByte, 0) != 0) {
                            Log.d(TAG, "OUD: " + "NOT last message");
                            mGattServer.sendResponse(device, requestId, 0, 0, null);
                            try {
                                characteristic.setValue(value);
                                if (((infoDest[0] + "").equals(getId()) && infoDest[1] != 0) || !clientInternet.equals("")) {
                                    BluetoothDevice dest;
                                    if (!clientInternet.equals(""))
                                        dest = mNode.getClient(clientInternet);
                                    else dest = mNode.getClient("" + infoDest[1]);
                                    boolean res = mGattServer.notifyCharacteristicChanged(dest, characteristic, false);
                                    Log.d(TAG, "OUD: " + "Notification sent? --> " + res);
                                }
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        mGattServer.sendResponse(device, requestId, 0, 0, null);
                        final String message;
                        String messageReceived = previousMsg + valueReceived;
                        if (Utility.getBit(destByte, 0) == 0) {
                            String[] infoMessage = messageReceived.split(";;");
                            int hop;
                            try {
                                hop = Integer.parseInt(infoMessage[1]);
                                infoMessage[1] = "" + hop + 1;
                                message = infoMessage[0] + ";;" + hop + ";;" + infoMessage[2];
                            } catch (NumberFormatException e) {
                                Log.d(TAG, "OUD: Errore, messaggio malformato, non lo propago. Message: " + messageReceived);
                                return;
                            }
                        } else message = messageReceived;
                        Log.d(TAG, "OUD: " + messageReceived);
                        messageMap.remove(senderId);

                        Handler mHandler = new Handler(Looper.getMainLooper());
                        mHandler.post(() -> Toast.makeText(context, "Messaggio ricevuto dall'utente " + senderId + " per " + Utility.getStringId(destByte) + ", messaggio: " + messageReceived, Toast.LENGTH_LONG).show());
                        if (Utility.getBit(destByte, 0) == 1) {
                            //Messaggio con internet
                            if (mNode.hasInternet()) {
                                Log.d(TAG, "Ho io Internet continua");
                                for (Listeners.OnMessageWithInternetListener listener : messageReceivedWithInternetListeners)
                                    listener.OnMessageWithInternet(senderId, message);

                            } else if (!clientInternet.equals("")) {
                                for (int i = 0; i < 8; i++) {
                                    if (Utility.getBit(mNode.getClientByteInternet(), i) == 1) {
                                        //il mio client ha internet devo notificarlo
                                        BluetoothDevice dest = mNode.getClient(clientInternet);
                                        if (dest == null) return;
                                        try {
                                            characteristic.setValue(value);
                                            boolean res = mGattServer.notifyCharacteristicChanged(dest, characteristic, false);
                                            Log.d(TAG, "OUD: " + "Notification sent? --> " + res);
                                        } catch (IllegalArgumentException e) {
                                            Log.d(TAG, "OUD: " + e.getMessage());
                                        }
                                        break;
                                    }
                                }
                            } else {
                                //non sono io che ho internet
                                sendMessage(message, infoDest[0] + "" + infoDest[1], true, new Listeners.OnMessageSentListener() {
                                    @Override
                                    public void OnMessageSent(String messageSent) {
                                        Log.d(TAG, "OUD: OnMessageSent: messaggio inviato");
                                    }

                                    @Override
                                    public void OnCommunicationError(String error) {
                                        Log.d(TAG, "OUD: Errore nel rigirare il messaggio, " + error);
                                    }
                                });
                            }
                        } else if ((infoDest[0] + "").equals(getId())) { //messaggio senza internet
                            if (infoDest[1] == 0) {
                                Log.d(TAG, "OUD: messaggio per me ");
                                String[] messageSplitted = message.split(";;");
                                int hop = -1;
                                long timestamp = -1;
                                try {
                                    hop = Integer.parseInt(messageSplitted[1]);
                                    timestamp = Long.parseLong(messageSplitted[0]);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "OUD: " + "Messaggio malformato, non propagabile");
                                }
                                for (Listeners.OnMessageReceivedListener l : messageReceivedListeners) {
                                    l.OnMessageReceived(infoSorg[0] + "" + infoSorg[1], messageSplitted[2], hop, timestamp);
                                }
                                return;
                            }
                            BluetoothDevice dest = mNode.getClient("" + infoDest[1]);
                            if (dest == null) {
                                Log.d(TAG, "OUD: " + "null, exiting");
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
                            sendMessage(message, infoDest[0] + "" + infoDest[1], false, new Listeners.OnMessageSentListener() {
                                @Override
                                public void OnMessageSent(String message) {
                                    Log.d(TAG, "OUD: messaggio \"" + message + "\"rigirato con successo");
                                }

                                @Override
                                public void OnCommunicationError(String error) {
                                    Log.d(TAG, "OUD: Errore nel rigirare il messaggio, " + error);
                                }
                            });
                        }
                    }
                }
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d(TAG, "OUD: " + "I've been asked to read descriptor from " + device.getName());
                if (descriptor.getUuid().toString().equals((mGattDescriptorNextId.getUuid().toString()))) {  //richiesta da un client di aggiungersi alla piconet
                    int current_id = mNode.nextId(device);
                    if (current_id == -1) {
                        mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, descriptor.getValue());
                    } else {
                        mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                    }
                } else if (descriptor.getUuid().equals(Constants.RoutingTableDescriptorUUID)) { //richiesta da altri server di leggere la versione della table in modo da decidere se inviarla o meno
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
                if (descriptor.getUuid().equals(Constants.ClientOnline_Configuration_UUID)) { //abilitazione di notifiche per ricevere messaggi
                    int currentid = mNode.nextId(device);
                    mNode.setClientOnline("" + currentid, device);
                    String nextId = "" + (mNode.nextId(null));
                    mGattDescriptorNextId.setValue(nextId.getBytes());
                    Log.d(TAG, "OUD: " + "NextId: " + value);
                    routingTable.addDevice(Integer.parseInt(getId()), currentid);

                    byte[] val = mGattCharacteristicClientOnline.getValue();
                    val[Integer.parseInt(id)] = Utility.setBit(val[Integer.parseInt(id)], currentid);
                    mGattCharacteristicClientOnline.setValue(val);

                    BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                    for (BluetoothDevice dev : mNode.getClientList()) { //notifico i miei che ho un nuovo client
                        if (dev == null) continue;
                        boolean res = mGattServer.notifyCharacteristicChanged(dev, characteristic, false);
                        Log.d(TAG, "OUD: i've notified new client Online " + res);
                    }

                    for (String idTemp : nearDeviceMap.keySet()) { // mando nella rete il nuovo client
                        BluetoothDevice dev = nearDeviceMap.get(idTemp);
                        ConnectBLETask client = Utility.createBroadcastNewClientOnline(dev, Integer.parseInt(getId()), currentid, context);
                        client.startClient();
                    }
                    for (OnRoutingTableUpdatedListener l : routingTableUpdatedListeners) {
                        l.OnRoutingTableUpdated("Aggiunto mio nuovo client con id " + currentid);
                    }
                } else if (descriptor.getUuid().equals(Constants.DescriptorClientOnlineUUID)) { //Un altro server mi scrive un nuovo client nella rete
                    byte[] val = mGattCharacteristicClientOnline.getValue();
                    int[] infoNewCLient = Utility.getByteInfo(descriptor.getValue()[0]);
                    if (Utility.getBit(val[infoNewCLient[0]], infoNewCLient[1]) == 1) {
                        Log.d(TAG, "OUD: " + "Client gia settato");
                    } else {
                        routingTable.addDevice(infoNewCLient[0], infoNewCLient[1]);
                        val[infoNewCLient[0]] = Utility.setBit(val[infoNewCLient[0]], infoNewCLient[1]);
                        mGattCharacteristicClientOnline.setValue(val);
                        for (BluetoothDevice dev : mNode.getClientList()) {//notifico i miei che ho un nuovo client
                            if (dev == null) continue;
                            boolean res = mGattServer.notifyCharacteristicChanged(dev, mGattCharacteristicClientOnline, false);
                            Log.d(TAG, "OUD: i've notified new client Online " + res);
                        }
                        for (String idTemp : nearDeviceMap.keySet()) { // mando nella rete il nuovo client
                            BluetoothDevice dev = nearDeviceMap.get(idTemp);
                            ConnectBLETask client = Utility.createBroadcastNewClientOnline(dev, infoNewCLient[0], infoNewCLient[1], context);
                            client.startClient();
                        }
                        mNode.getServer("" + infoNewCLient[0]).setClientOnline("" + infoNewCLient[1], null);
                        Log.d(TAG, "OUD: new client online with id " + infoNewCLient[0] + infoNewCLient[1]);
                        for (OnRoutingTableUpdatedListener l : routingTableUpdatedListeners) {
                            l.OnRoutingTableUpdated("È entrato nella rete il nuovo client " + infoNewCLient[0] + "" + infoNewCLient[1]);
                        }
                    }

                } else if (descriptor.getUuid().equals(Constants.DescriptorClientWithInternetUUID)) { //il client ha internet
                    String clientId = new String(value);
                    mNode.setClientInternet(Integer.parseInt(clientId.substring(0, 1)));
                    // TODO: 14/01/19 Propagare la notizia che si ha internet 
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
            Log.e(TAG, "initializeId: offset >= nearDeviceMap.size()");
            for (OnConnectionRejectedListener listener : connectionRejectedListeners)
                listener.OnConnectionRejected();
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
                    for (String temp : nearDeviceMap.keySet()) { //aggiungo i server trovati nell'init search e li mettp nella mia mappa
                        Log.d(TAG, "OUD: " + "vicini : " + temp);
                        mNode.addNearServer(new ServerNode(temp));
                    }
                    if (Utility.isDeviceOnline(context))
                        mNode.setHasInternet(true);
                    mGattDescriptor.setValue(id.getBytes());
                    mGattDescriptorRoutingTable.setValue("0".getBytes());
                    mGattCharacteristic.addDescriptor(mGattDescriptor);
                    mGattDescriptorNextId.setValue("1".getBytes());
                    mGattCharacteristicClientOnline.setValue(new byte[18]); //16 max server nella rete + 2 per internet
                    mGattCharacteristicNextServerId.setValue(("" + (Integer.parseInt(getId()) + 1)).getBytes());
                    Log.d(TAG, "OUD: Set Value: --> " + new String(mGattCharacteristicNextServerId.getValue()));
                    mGattCharacteristic.addDescriptor(mGattDescriptorNextId);
                    mGattCharacteristic.addDescriptor(mGattClientConfigurationDescriptor);
                    mGattCharacteristic.addDescriptor(mGattClientWithInternetDescriptor);
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

                    for (Listeners.OnServerInitializedListener l : serverInitializedListeners) {
                        l.OnServerInitialized();
                    }

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
        } else {
            setId("1");
            mNode = new ServerNode(id);
            if (Utility.isDeviceOnline(context))
                mNode.setHasInternet(true);
            mGattCharacteristicNextServerId.setValue("2".getBytes());
            Log.d(TAG, "OUD: startServer: i'm the first");
            routingTable.addDevice(1, 0);
            mGattDescriptorRoutingTable.setValue("1".getBytes());
            mGattDescriptor.setValue(id.getBytes());
            byte[] mapClientOnline = new byte[18];
            mapClientOnline[1] = Utility.setBit(mapClientOnline[1], 0);
            mGattCharacteristicClientOnline.setValue(mapClientOnline);
            mGattDescriptorNextId.setValue("1".getBytes());
            mGattCharacteristic.addDescriptor(mGattDescriptor);
            mGattCharacteristic.addDescriptor(mGattDescriptorNextId);
            mGattCharacteristic.addDescriptor(mGattClientConfigurationDescriptor);
            mGattCharacteristic.addDescriptor(mGattClientWithInternetDescriptor);
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

            for (Listeners.OnServerInitializedListener l : serverInitializedListeners) {
                l.OnServerInitialized();
            }

            Log.d(TAG, "OUD: " + "startServer: Inizializzato servizi e tutto");
        }
    }

    public void stopServer() {
        if (mGattServer != null) {
            this.mGattServer.clearServices();
            this.mGattServer.close();
            this.mGattServer = null;
        }
    }

    public void insertMapDevice(HashMap<String, BluetoothDevice> nearDeviceMap) {
        for (String id : nearDeviceMap.keySet()) {
            Log.d(TAG, "OUD: id: " + id + "Device: " + nearDeviceMap.get(id).getName());
        }
        this.nearDeviceMap = nearDeviceMap;
    }

    // TODO: 19/01/19 DA TESTARE
    public void sendMessage(String message, String dest, boolean internet, Listeners.OnMessageSentListener listener) {
        int infoSorg[] = new int[2];
        infoSorg[0] = Integer.parseInt(getId());
        int infoDest[] = Utility.getIdArrayByString(dest);
        if (infoDest[0] == Integer.parseInt(getId())) {
            if (infoDest[1] == 0) {
                Log.e(TAG, "OUD: sendMessage: Messaggio per me stessso");
                listener.OnCommunicationError("You cannot send message to yourself!!");
                return;
            }
            //messaggio al mio client devo notificarlo
            byte[][] finalMessage = Utility.messageBuilder(Utility.byteMessageBuilder(infoSorg[0], infoSorg[1]), Utility.byteMessageBuilder(infoDest[0], infoDest[1]), message, internet);
            for (int i = 0; i < finalMessage.length; i++) {
                BluetoothDevice dev = mNode.getClient("" + infoDest[1]);
                mGattCharacteristic.setValue(finalMessage[i]);
                boolean res = mGattServer.notifyCharacteristicChanged(dev, mGattCharacteristic, false);
                Log.d(TAG, "OUD: i've notified new server Online " + res);
            }
            listener.OnMessageSent(message);
        } else {
            //non sono io il destinatario
            final ServerNode nodeDest;

            if(internet) nodeDest = mNode.getNearestServerWithInternet(mNode.getLastRequest() + 1, getId());
            else nodeDest = mNode.getServerToSend(infoDest[0] + "", getId(), mNode.getLastRequest() + 1);

            if (nodeDest == null) {
                Log.e(TAG, "next hop null");
                return;
            }
            Log.d(TAG, "OUD: next-hop : " + nodeDest.getId());
            final BluetoothDevice near = nearDeviceMap.get(nodeDest.getId());
            if (near == null) {
                Log.e(TAG, "near server null");
                return;
            }
            Log.d(TAG, "OUD: next-hop : " + near.getName());
            final Server server = new Server(near, (near.getName() == null ? "Unknown" : near.getName()));

            byte[][] finalMessage = Utility.messageBuilder(Utility.byteMessageBuilder(infoSorg[0], infoSorg[1]), Utility.byteMessageBuilder(infoDest[0], infoDest[1]), message, internet);

            final int[] indexHolder = new int[1]; //si inizializza automaticamente a 0 il primo elemento
            final boolean[] resultHolder = new boolean[1];

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
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (indexHolder[0] >= finalMessage.length || !resultHolder[0]) {
                            if (resultHolder[0]) {
                                listener.OnMessageSent("Messaggio inviato con successo");
                            } else listener.OnCommunicationError("Not every package is sent");
                        } else {
                            resultHolder[0] = Utility.sendPacket(finalMessage[indexHolder[0]], gatt, null);
                            indexHolder[0] += 1;
                        }
                    }
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onDescriptorRead(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    resultHolder[0] = Utility.sendPacket(finalMessage[indexHolder[0]], gatt, null);
                    indexHolder[0] += 1;
                    super.onDescriptorRead(gatt, descriptor, status);
                }

            });
            connectBLETask.startClient();
        }
    }
    /*
    public void sendMessageOld(String message, String dest, boolean internet, Listeners.OnMessageSentListener listener) {
        int infoSorg[] = new int[2];
        infoSorg[0] = Integer.parseInt(getId());
        //infoSorg[1] = 0;
        int infoDest[] = Utility.getIdArrayByString(dest);
        if (infoDest[0] == Integer.parseInt(getId())) {
            if (infoDest[1] == 0) {
                Log.e(TAG, "OUD: sendMessage: Messaggio per me stessso?!?!");
                return;
            }
            //messaggio al mio client devo notificarlo
            byte[][] finalMessage = Utility.messageBuilder(Utility.byteMessageBuilder(infoSorg[0], infoSorg[1]), Utility.byteMessageBuilder(infoDest[0], infoDest[1]), message, internet);
            for (int i = 0; i < finalMessage.length; i++) {
                BluetoothDevice dev = mNode.getClient("" + infoDest[1]);
                mGattCharacteristic.setValue(finalMessage[i]);
                boolean res = mGattServer.notifyCharacteristicChanged(dev, mGattCharacteristic, false);
                Log.d(TAG, "OUD: i've notified new server Online " + res);
            }
            listener.OnMessageSent(message);
        } else {
            //non sono io il destinatario
            final ServerNode nodeDest = mNode.getServerToSend(infoDest[0] + "", getId(), mNode.getLastRequest() + 1);
            if (nodeDest == null) {
                Log.e(TAG, "next hop null");
                return;
            }
            Log.d(TAG, "OUD: next-hop : " + nodeDest.getId());
            final BluetoothDevice near = nearDeviceMap.get(nodeDest.getId());
            if (near == null) {
                Log.e(TAG, "near server null");
                return;
            }
            Log.d(TAG, "OUD: next-hop : " + near.getName());
            final Server server = new Server(near, (near.getName() == null ? "" : near.getName()));
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
                    new Handler(Looper.getMainLooper()).postDelayed(() -> Utility.sendMessage(message, gatt, infoSorg, infoDest, false, new Listeners.OnMessageSentListener() {
                        @Override
                        public void OnMessageSent(String messageSent) {
                            Log.d(TAG, "OUD: OnMessageSent: messaggio inviato");
                            listener.OnMessageSent(messageSent);
                        }

                        @Override
                        public void OnCommunicationError(String error) {

                        }
                    }), 500);
                    super.onDescriptorRead(gatt, descriptor, status);

                }

            });
            connectBLETask.startClient();
        }
    }
    */

    public void addConnectionRejectedListener(OnConnectionRejectedListener connectionRejectedListeners) {
        this.connectionRejectedListeners.add(connectionRejectedListeners);
    }

    public void removeConnectionRejectedListener(OnConnectionRejectedListener connectionRejectedListener) {
        this.connectionRejectedListeners.remove(connectionRejectedListener);
    }

    public void addRoutingTableUpdatedListener(OnRoutingTableUpdatedListener routingTableUpdatedListener) {
        this.routingTableUpdatedListeners.add(routingTableUpdatedListener);
    }

    public void removeRoutingTableUpdatedListener(OnRoutingTableUpdatedListener routingTableUpdatedListener) {
        this.routingTableUpdatedListeners.remove(routingTableUpdatedListener);
    }

    public void addServerInitializedListener(Listeners.OnServerInitializedListener l) {
        this.serverInitializedListeners.add(l);
    }

    public void removeServerInitializedListener(Listeners.OnServerInitializedListener l) {
        this.serverInitializedListeners.remove(l);
    }

    public void addOnMessageReceivedWithInternet(Listeners.OnMessageWithInternetListener listener) {
        this.messageReceivedWithInternetListeners.add(listener);
    }

    public void addOnMessageReceived(Listeners.OnMessageReceivedListener l) {
        this.messageReceivedListeners.add(l);
    }

    public interface OnConnectionRejectedListener {
        void OnConnectionRejected();
    }

    public interface OnRoutingTableUpdatedListener {
        void OnRoutingTableUpdated(String id);
    }
}
