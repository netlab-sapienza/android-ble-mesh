package it.drone.mesh.tasks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import it.drone.mesh.common.Constants;
import it.drone.mesh.common.RoutingTable;
import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.models.Server;

public class ConnectBLETask {
    private final static String TAG = ConnectBLETask.class.getName();

    private Server server;
    private BluetoothGattCallback mGattCallback;
    private BluetoothGatt mGatt;
    private Context context;
    private String id;
    private HashMap<String, String> messageMap;
    private ArrayList<Listeners.OnMessageReceivedListener> receivedListeners;
    private RoutingTable routingTable;

    public ConnectBLETask(Server server, Context context, BluetoothGattCallback callback) {
        // GATT OBJECT TO CONNECT TO A GATT SERVER
        this.context = context;
        this.server = server;
        this.mGattCallback = callback;
        this.id = null;
        receivedListeners = new ArrayList<>();
        routingTable = RoutingTable.getInstance();
    }

    public ConnectBLETask(Server server, final Context context) {
        // GATT OBJECT TO CONNECT TO A GATT SERVER
        this.context = context;
        this.server = server;
        this.id = null;
        this.messageMap = new HashMap<>();
        receivedListeners = new ArrayList<>();
        routingTable = RoutingTable.getInstance();
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT client " + gatt.getDevice().getName());
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Log.d(TAG, "OUD: " + "GATT: " + gatt.toString());
                Log.d(TAG, "OUD: " + "I discovered a service" + gatt.getServices());
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().toString().equals(Constants.ServiceUUID.toString())) {
                        if (service.getCharacteristics() != null) {
                            for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                if (chars.getUuid().equals(Constants.CharacteristicUUID)) {
                                    //provare anche con indication value
                                    BluetoothGattDescriptor desc = chars.getDescriptor(Constants.DescriptorUUID);
                                    boolean res = gatt.readDescriptor(desc);
                                    Log.d(TAG, "OUD: " + "descrittore id letto ? " + res);

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
                if (characteristic.getUuid().equals(Constants.ClientOnlineCharacteristicUUID)) {
                    routingTable.cleanRoutingTable();
                    byte[] value = characteristic.getValue();
                    for (int i = 0; i < value.length; i++) {
                        boolean flag = false;
                        for (int j = 0; j < 8; j++) {
                            if (Utility.getBit(value[i], j) == 1) flag = true;
                        }
                        if (flag) {
                            Log.d(TAG, "OUD: SERVER ONLINE ID: " + i);
                            for (int j = 1; j < 8; j++) {
                                if (Utility.getBit(value[i], j) == 1) {
                                    Log.d(TAG, "OUD: client : " + j);
                                    routingTable.addDevice(i, j);
                                }
                            }
                        }
                    }
                    return;
                }
                Log.d(TAG, "OUD: " + "Characteristic changed");
                byte[] value = characteristic.getValue();
                final String valueReceived;
                byte[] correct_message = new byte[value.length - 2];

                byte sorgByte = value[0];
                byte destByte = value[1];
                final int[] infoDest = Utility.getByteInfo(destByte);

                if (id.equals("" + infoDest[0] + infoDest[1]))
                    Log.d(TAG, "OUD: " + "sono il destinatario corretto");
                else {
                    Log.d(TAG, "OUD: " + "sono il destinatario sbagliato");
                    return;
                }

                for (int i = 0; i < value.length - 2; i++) {
                    correct_message[i] = value[i + 2];
                }

                final String senderId = Utility.getStringId(sorgByte);

                valueReceived = new String(correct_message);
                Log.d(TAG, "OUD: " + valueReceived);

                String previousMsg = messageMap.get(senderId);

                if (previousMsg == null) previousMsg = "";
                messageMap.put(senderId, previousMsg + valueReceived);

                Log.d(TAG, "OUD: " + id + " : Notifica dal server,il mittente " + senderId + " mi ha inviato: " + valueReceived);
                if (Utility.getBit(sorgByte, 0) != 0) {
                    Log.d(TAG, "OUD: " + "NOT last message");
                } else {
                    Log.d(TAG, "OUD: " + "YES last message");
                    for (Listeners.OnMessageReceivedListener listener : receivedListeners)
                        listener.OnMessageReceived("" + senderId, messageMap.get(senderId));
                    messageMap.remove(senderId);

                    /*
                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Messaggio ricevuto dall'utente " + senderId  + ", messaggio: " + messageMap.get(senderId), Toast.LENGTH_SHORT).show();
                            messageMap.remove(senderId);
                        }
                    });
                    */

                }

                super.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == 0) {
                    Log.d(TAG, "OUD: " + "I read a descriptor UUID: " + descriptor.getUuid().toString());
                    if (descriptor.getUuid().toString().equals(Constants.DescriptorUUID.toString())) {
                        setId(new String(descriptor.getValue(), Charset.defaultCharset()));
                        Log.d(TAG, "OUD: " + "id : " + getId());
                        boolean res = gatt.readDescriptor(gatt.getService(Constants.ServiceUUID).getCharacteristic(Constants.CharacteristicUUID).getDescriptor(Constants.NEXT_ID_UUID));
                        Log.d(TAG, "OUD: " + "read next id descriptor : " + res);
                    } else if (descriptor.getUuid().toString().equals(Constants.NEXT_ID_UUID.toString())) {
                        setId(getId() + new String(descriptor.getValue(), Charset.defaultCharset()));
                        Log.d(TAG, "OUD: " + "id : " + getId());
                        BluetoothGattDescriptor configDesc = gatt.getService(Constants.ServiceUUID).getCharacteristic(Constants.CharacteristicUUID).getDescriptor(Constants.Client_Configuration_UUID);
                        configDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        boolean res = gatt.writeDescriptor(configDesc);
                        Log.d(TAG, "OUD: " + "Writing descriptor?" + configDesc.getUuid() + " --->" + res);
                        BluetoothGattCharacteristic chars = gatt.getService(Constants.ServiceUUID).getCharacteristic(Constants.CharacteristicUUID);
                        gatt.setCharacteristicNotification(chars, true);

                    } else {
                        Log.d(TAG, "OUD: " + "Nessun operazione con tale descrittore : " + descriptor.getUuid().toString());
                    }
                } else if (status == 6) Log.d(TAG, "OUD: " + "id gia inizializzato");
                else Log.d(TAG, "OUD: " + "status non conosciuto");
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.d(TAG, "OUD: " + "I wrote a descriptor");
                if (descriptor.getUuid().equals(Constants.Client_Configuration_UUID)) {
                    Log.d(TAG, "OUD: onDescriptorWrite: cc");
                    BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                    if (service == null) {
                        Log.d(TAG, "OUD: nulll");
                        return;
                    }
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.ClientOnlineCharacteristicUUID);
                    if (characteristic == null) {
                        Log.d(TAG, "OUD: nulll");
                        return;
                    }
                    BluetoothGattDescriptor desc = characteristic.getDescriptor(Constants.ClientOnline_Configuration_UUID);
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean res = gatt.writeDescriptor(desc);
                    Log.d(TAG, "OUD: " + "Writing descriptor?" + desc.getUuid() + " --->" + res);
                    gatt.setCharacteristicNotification(characteristic, true);
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


    /**
     * Send message a un client nella rete
     *
     * @param message  Messaggio da inviare
     * @param dest     Id del Client Destinatario in formato stringa o se ti è piu comodo un altro formato si può cambiare
     * @param listener listener con callback specifica quando il messaggio è stato inviato
     */
    public boolean sendMessage(String message, String dest, Listeners.OnMessageSentListener listener) {
        return Utility.sendMessage(message, this.mGatt, Utility.getIdArrayByString(getId()), Utility.getIdArrayByString(dest), listener);
    }

    public boolean startClient() {
        this.mGatt = server.getBluetoothDevice().connectGatt(context, false, mGattCallback);
        server.setBluetoothGatt(this.mGatt);
        server.getBluetoothGatt().requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        server.getBluetoothGatt().connect();
        setId("");
        Log.d(TAG, "OUD: " + "startClient: " + mGatt.getDevice().getName());
        //boolean ret = this.mGatt.discoverServices();
        //Log.d(TAG, "OUD: " + "DiscoverServices -> " + ret);
        return true;
    }

    public void stopClient() {
        this.mGatt.disconnect();
        this.mGatt = null;
    }

    public boolean hasCorrectId() {
        return this.id != null && this.id.length() >= 2;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<Listeners.OnMessageReceivedListener> getReceivedListeners() {
        return receivedListeners;
    }

    public void addReceivedListener(Listeners.OnMessageReceivedListener onMessageReceivedListener) {
        this.receivedListeners.add(onMessageReceivedListener);
    }

    public void removeReceivedListener(Listeners.OnMessageReceivedListener onMessageReceivedListener) {
        this.receivedListeners.remove(onMessageReceivedListener);
    }
}

