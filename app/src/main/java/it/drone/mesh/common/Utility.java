package it.drone.mesh.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.models.Server;
import it.drone.mesh.server.ServerNode;
import it.drone.mesh.tasks.ConnectBLETask;

/**
 * Tutte i metodi e le variabile condivise da server e client (quelle per la scansione per esempio) vengono messi qua.
 * Per il momento sia server che client devono avere accesso alla possiblità di eseguire scansioni, in futuro potrebbe cambiare
 */

public class Utility {
    // Stops scanning after 5 seconds.
    public static final long SCAN_PERIOD = 5000;

    public final static String BETA_FILENAME_SENT = "sent_messages.txt";
    public final static String BETA_FILENAME_RECEIVED = "received_messages.txt";
    public static int PACK_LEN = 18;
    public static int DEST_PACK_MESSAGE_LEN = 16;
    private static String TAG = Utility.class.getSimpleName();

    public static int getBit(byte val, int offset) {
        return (val >> offset) & 1;
    }

    public static byte setBit(byte val, int offset) {
        val |= 1 << offset;
        return val;
    }

    public static byte clearBit(byte val, int offset) {
        val = (byte) (val & ~(1 << offset));
        return val;
    }

    public static void printByte(byte b) {
        String s = "";
        for (int i = 7; i > -1; i--) {
            s += getBit(b, i);
        }
        Log.d(TAG, "OUD: " + s);
    }

    public static byte byteMessageBuilder(int serverId, int clientId) {
        byte b = 0b00000000;

        Integer server = serverId;
        Integer client = clientId;

        byte serv = server.byteValue();
        byte clie = client.byteValue();

        b |= serv;
        b = (byte) (b << 4);
        clie = (byte) (clie << 1);
        b |= clie;
        b = setBit(b, 0);
        return b;
    }

    public static byte byteNearServerBuilder(int server1Id, int server2Id) {
        byte b = 0b00000000;

        Integer server = server1Id;
        Integer client = server2Id;

        byte server1 = server.byteValue();
        byte server2 = client.byteValue();

        b |= server1;
        b = (byte) (b << 4);

        b |= server2;

        return b;
    }

    public static byte[][] messageBuilder(byte firstByte, byte destByte, byte[] sInByte, boolean internet) {
        //  Log.d(TAG, "OUD: messageBuilder: length message :" + sInByte.length);
        byte[][] finalMessage;
        int numPacks = (int) Math.floor((float) sInByte.length / DEST_PACK_MESSAGE_LEN);

        int lastLen = sInByte.length % DEST_PACK_MESSAGE_LEN;
        int numPackToSend = (lastLen == 0) ? numPacks : numPacks + 1;

        finalMessage = new byte[numPackToSend][PACK_LEN];
        Log.d(TAG, "OUD: numPack: " + numPackToSend);
        // Log.d(TAG, "OUD: messageBuilder:Entrata foqr");
        Utility.printByte(firstByte);
        Utility.printByte(destByte);
        if (!internet)
            destByte = Utility.clearBit(destByte, 0); //perchè il bytemssagebuilder setta sempre l'ultimo bit del byte a 1
        for (int i = 0; i < numPackToSend; i++) {
            if (i == numPackToSend - 1) {
                byte[] pack = new byte[lastLen + 2];
                firstByte = clearBit(firstByte, 0);
                pack[0] = firstByte;
                pack[1] = destByte;
                System.arraycopy(sInByte, (i * DEST_PACK_MESSAGE_LEN), pack, 2, lastLen);
                finalMessage[i] = pack;
            } else {
                byte[] pack = new byte[PACK_LEN];
                pack[0] = firstByte;
                pack[1] = destByte;
                System.arraycopy(sInByte, (i * DEST_PACK_MESSAGE_LEN), pack, 2, DEST_PACK_MESSAGE_LEN);
                finalMessage[i] = pack;
            }
        }
        //Log.d(TAG, "OUD: messageBuilder:Fine for");
        return finalMessage;
    }

    public static byte[][] messageBuilder(byte firstByte, byte destByte, String message, boolean internet) {
        return messageBuilder(firstByte, destByte, message.getBytes(), internet);
    }

    public static int[] getByteInfo(byte firstByte) {
        int[] ret = new int[3];
        ret[2] = getBit(firstByte, 0);
        ret[1] = getBit(firstByte, 1) + getBit(firstByte, 2) * 2 + getBit(firstByte, 3) * 4;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret;
    }

    public static int[] getIdServerByteInfo(byte firstByte) {
        int[] ret = new int[2];
        ret[1] = getBit(firstByte, 0) + getBit(firstByte, 1) * 2 + getBit(firstByte, 2) * 4 + getBit(firstByte, 3) * 8;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret;
    }

    public static String getStringId(byte firstByte) {
        int[] ret = new int[2];
        ret[1] = getBit(firstByte, 1) + getBit(firstByte, 2) * 2 + getBit(firstByte, 3) * 4;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret[0] + "" + ret[1];
    }

    public static int[] getIdArrayByString(String id) {
        int[] res = new int[2];
        Log.d(TAG, "getIdArrayByString: id: " + id);
        if (id.length() == 1) {
            res[0] = Integer.parseInt(id);
            return res;
        }
        res[0] = (id.length() == 2) ? (Integer.parseInt(id.substring(0, 1))) : (Integer.parseInt(id.substring(0, 2)));
        res[1] = (id.length() == 2) ? (Integer.parseInt(id.substring(1, 2))) : (Integer.parseInt(id.substring(2, 3)));
        return res;
    }

    public static boolean sendPacket(byte[] packet, BluetoothGatt gatt, Listeners.OnPacketSentListener onPacketSent) {
        BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
        if (service == null) return false;
        BluetoothGattCharacteristic chars = service.getCharacteristic(Constants.CharacteristicUUID);
        if (chars == null) return false;
        chars.setValue(packet);
        gatt.beginReliableWrite();
        boolean res = gatt.writeCharacteristic(chars);
        gatt.executeReliableWrite();
        Log.d(TAG, "OUD: " + new String(packet));
        Log.d(TAG, "OUD: " + "sent? -> " + res);
        if (onPacketSent != null) {
            if (res) onPacketSent.OnPacketSent(packet);
            else onPacketSent.OnPacketError("Error sending packet");
        }
        return res;
    }

    public static boolean sendRoutingTablePacket(byte[] packet, BluetoothGatt gatt, Listeners.OnPacketSentListener onPacketSent) {
        BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
        if (service == null) return false;
        BluetoothGattCharacteristic chars = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
        if (chars == null) return false;
        chars.setValue(packet);
        gatt.beginReliableWrite();
        boolean res = gatt.writeCharacteristic(chars);
        gatt.executeReliableWrite();
        for (int i = 0; i < packet.length; i++) {
            Utility.printByte(packet[i]);
        }
        Log.d(TAG, "OUD: " + "sent? -> " + res);
        if (onPacketSent != null) {
            if (res) onPacketSent.OnPacketSent(packet);
            else onPacketSent.OnPacketError("Error sending packet");
        }
        return res;
    }

    /**
     * Use this check to determine whether BLE is supported on the device. Then
     * you can selectively disable BLE-related features.
     *
     * @param context application Context
     * @return true if BLE is supported
     */
    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    public static List<ScanFilter> buildScanFilters() {
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
    public static ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        //builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        return builder.build();
    }

    public static ConnectBLETask createBroadcastRoutingTableClient(BluetoothDevice device, final String routingId, Context context, final byte[] value, final String id) {
        boolean[] resultHolder = new boolean[1];
        int[] indexHolder = new int[1];

        final int[] infoSorg = new int[2];
        infoSorg[0] = Integer.parseInt(id);

        byte[][] finalMessage = messageBuilder(byteMessageBuilder(infoSorg[0], infoSorg[1]), byteNearServerBuilder(0, 0), new String(value), false);

        ConnectBLETask client =  new ConnectBLETask(new Server(device, device.getName()), context);
        BluetoothGattCallback callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "OUD: " + "Connected to GATT client. Attempting to start service discovery Routing Table from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "OUD: " + "Disconnected from GATT client " + gatt.getDevice().getName());
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                if (characteristic == null) return;
                BluetoothGattDescriptor desc = characteristic.getDescriptor(Constants.RoutingTableDescriptorUUID);
                boolean res = gatt.readDescriptor(desc);
                Log.d(TAG, "OUD: " + "Read desc routing: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "OUD: " + "i wrote a characteristic !");
                    if (indexHolder[0] >= finalMessage.length || !resultHolder[0]) {
                        if (resultHolder[0]) {
                            client.setJobDone();
                            Log.d(TAG, "OUD: sendRoutingTable: Messaggio inviato con successo");
                        } else
                            Log.d(TAG, "OUD: sendRoutingTable: Error sending packet " + indexHolder[0]);
                    } else {
                        resultHolder[0] = Utility.sendRoutingTablePacket(finalMessage[indexHolder[0]], gatt, null);
                        indexHolder[0] += 1;
                    }
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                //Log.d(TAG, "OUD: onDescriptorRead: " + new String(descriptor.getValue()) + "length: " + descriptor.getValue().length + "routingId: " + routingId);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (Integer.parseInt(routingId) > Integer.parseInt(new String(descriptor.getValue()))) {
                        descriptor.setValue(routingId.getBytes());
                        gatt.writeDescriptor(descriptor);
                    } else return;
                }
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.d(TAG, "OUD: onDescriptorWrite: status :" + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattCharacteristic characteristic1 = descriptor.getCharacteristic();
                    if (characteristic1 == null) return;
                    resultHolder[0] = Utility.sendRoutingTablePacket(finalMessage[indexHolder[0]], gatt, null);
                    indexHolder[0] += 1;
                }
            }
        };
        client.setCallback(callback);
        return client;
    }

    public static byte[][] buildMapFromString(String mapString) {
        byte[][] res = new byte[ServerNode.MAX_NUM_SERVER][ServerNode.SERVER_PACKET_SIZE];
        byte[] mapByte = mapString.getBytes();
        int j = -1;
        int counter = 0;
        for (int i = 0; i < mapByte.length; i++) {
            if (i % ServerNode.SERVER_PACKET_SIZE == 0) {
                j++;
                counter = 0;
            }
            res[j][counter++] = mapByte[i];
        }
        return res;
    }

    public static ConnectBLETask createBroadCastNextServerIdClient(BluetoothDevice device, Context context, final byte[] value) {
        Server u = new Server(device, device.getName());
        return new ConnectBLETask(u, context, new BluetoothGattCallback() {
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
                Log.d(TAG, "OUD: " + "ho scoperto serviceszeze");
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) {
                    Log.d(TAG, "OUD: " + "IL SERVICE ERA NULL");
                    return;
                }
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                if (characteristic == null) {
                    Log.d(TAG, "OUD: " + "LA CARATTERISTICA ERA NULL");
                    return;
                }
                String temp = new String(value);
                BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                if (characteristic1 == null) return;
                characteristic1.setValue(temp);
                gatt.beginReliableWrite();
                boolean res = gatt.writeCharacteristic(characteristic1);
                Log.d(TAG, "OUD: " + "write charac? " + res);
                gatt.executeReliableWrite();
                super.onServicesDiscovered(gatt, status);
            }


            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: " + "i wrote a characteristic !");
                if (characteristic.getUuid().equals(Constants.RoutingTableCharacteristicUUID)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "OUD: " + "I wrote a new server on a server");
                    } else {
                        Log.d(TAG, "OUD: " + "Error1: " + status);
                    }
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
    }

    public static void updateServerToAsk(BluetoothAdapter mBluetoothAdapter, final HashMap<String, BluetoothDevice> nearMapDevice, final String nuovoId, final Listeners.OnNewServerDiscoveredListener listener) {
        final BluetoothLeScanner mBluetoothScan = mBluetoothAdapter.getBluetoothLeScanner();
        final ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                super.onScanResult(callbackType, result);
                if (nearMapDevice.values().contains(result.getDevice())) {
                    Log.d(TAG, "OUD: " + "risultato già presente");
                    return;
                } else {
                    nearMapDevice.put(nuovoId, result.getDevice());
                }

                //listener.OnNewServerDiscovered(result);
                Log.d(TAG, "OUD: " + "ho aggiunto " + result.getDevice().getName());
            }
        };

        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            Log.d(TAG, "OUD: " + "Stopping Scanning");
            // Stop the scan, wipe the callback.
            mBluetoothScan.stopScan(mScanCallback);
            // TODO: 18/01/19 GESTIRE IL CASO IN CUI nearMapDevice.get(nuovoId) È NULL, BISOGNA COMUNICARE AGLI ALTRI SERVER CHE NON LO VEDO
            if (nearMapDevice.get(nuovoId) == null) listener.OnNewServerNotFound();
            else listener.OnNewServerDiscovered(nearMapDevice.get(nuovoId));
        }, 4000);
        //ServerList.cleanUserList();
        mBluetoothScan.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
    }

    /**
     * To save metrics and values of characteristics this should be passed
     * Saves the data in a file in the download directory of the phone.
     *
     * @param header   first description string on top of the file
     * @param fileName name of the file
     * @param data     actual data to be saved
     */
    public static void saveData(List<String> header, String fileName, List data) throws IOException {
        // Convert arrays to delimited strings
        String header_str = TextUtils.join("\t", header);
        String data_str = TextUtils.join("\t", data);

        File root = new File(Environment.
                getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getPath());

        File dataFile = new File(root, fileName);
        // If file doesn't exist
        if (!dataFile.exists()) {
            // Add headers
            FileWriter writer = new FileWriter(dataFile, true);
            writer.append(header_str);
            writer.append('\n');
            writer.append('\n');
            writer.flush();
            writer.close();
        }
        // Add actual data
        FileWriter writer = new FileWriter(dataFile, true);
        writer.append(data_str);
        writer.append('\n');
        writer.flush();
        writer.close();
    }

    public static ConnectBLETask createBroadcastNewClientOnline(BluetoothDevice device, final int serverId, final int clientId, Context context) {
        return new ConnectBLETask(new Server(device, device.getName()), context, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "OUD: " + "Connected to GATT client. Attempting to start service discovery Routing Table from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "OUD: " + "Disconnected from GATT client " + gatt.getDevice().getName());
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.ClientOnlineCharacteristicUUID);
                if (characteristic == null) return;
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.DescriptorClientOnlineUUID);
                if (descriptor == null) return;
                byte[] val = new byte[2];
                val[0] = Utility.byteMessageBuilder(serverId, clientId);
                descriptor.setValue(val);
                boolean res = gatt.writeDescriptor(descriptor);

                Log.d(TAG, "OUD: " + "write descriptor client online: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS)
                    Log.d(TAG, "OUD: i wrote new client online descriptor");

                super.onDescriptorWrite(gatt, descriptor, status);
            }
        });
    }

    public static ConnectBLETask createBroadcastClientWithInternet(BluetoothDevice device, final String clientId, Context context) {
        return new ConnectBLETask(new Server(device, device.getName()), context, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "OUD: " + "Connected to GATT client. Attempting to start service discovery Routing Table from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "OUD: " + "Disconnected from GATT client " + gatt.getDevice().getName());
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicUUID);
                if (characteristic == null) return;
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.DescriptorClientWithInternetUUID);
                if (descriptor == null) return;
                descriptor.setValue(clientId.getBytes());
                boolean res = gatt.writeDescriptor(descriptor);

                Log.d(TAG, "OUD: " + "write descriptor client with internet: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS)
                    Log.d(TAG, "OUD: i wrote new client online descriptor");
                super.onDescriptorWrite(gatt, descriptor, status);
            }
        });
    }

    public static boolean isDeviceOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static ConnectBLETask createBroadcastSomeoneDisconnectedClient(BluetoothDevice device, byte[] message, Context context) {
        return new ConnectBLETask(new Server(device, device.getName()), context, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "OUD: " + "Connected to GATT client. Attempting to start service discovery Routing Table from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "OUD: " + "Disconnected from GATT client " + gatt.getDevice().getName());
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) return;
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicUUID);
                if (characteristic == null) return;
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.DescriptorCheckAliveUUID);
                if (descriptor == null) return;
                descriptor.setValue(message);
                boolean res = gatt.writeDescriptor(descriptor);

                Log.d(TAG, "OUD: " + "write descriptor client with internet: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS && descriptor.getUuid().equals(Constants.DescriptorCheckAliveUUID)) {
                    Log.d(TAG, "OUD: i wrote server disconnected descriptor");
                    String suspectedId = Utility.getStringId(message[0]);
                    String suspectedServerId = (suspectedId.length() == 2) ? suspectedId.substring(0, 1) : suspectedId.substring(0, 2);
                    String suspectedClientId = (suspectedId.length() == 2) ? suspectedId.substring(1, 2) : suspectedId.substring(2, 3);
                    if (suspectedClientId.equals("0")) { //scriviamo anche sulla caratteristica next server id per mettere id di quello morto
                        BluetoothGattCharacteristic chara = gatt.getService(Constants.ServiceUUID).getCharacteristic(Constants.CharacteristicNextServerIdUUID);
                        chara.setValue(suspectedServerId.getBytes());
                        gatt.writeCharacteristic(gatt.getService(Constants.ServiceUUID).getCharacteristic(Constants.CharacteristicNextServerIdUUID));
                    }
                }

                super.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: I wrote characteristic NEXT SERVER ID" + new String(characteristic.getValue()));
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
    }
}