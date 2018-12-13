package it.drone.mesh.roles.common;

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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import it.drone.mesh.models.Device;
import it.drone.mesh.models.User;
import it.drone.mesh.roles.server.ServerNode;
import it.drone.mesh.tasks.ConnectBLETask;

/**
 * Tutte i metodi e le variabile condivise da server e client (quelle per la scansione per esempio) vengono messi qua.
 * Per il momento sia server che client devono avere accesso alla possiblità di eseguire scansioni, in futuro potrebbe cambiare
 */

public class Utility {
    // Stops scanning after 5 seconds.
    public static final long SCAN_PERIOD = 5000;

    public static String TAG = Utility.class.getSimpleName();
    public static int PACK_LEN = 18;
    public static int DEST_PACK_MESSAGE_LEN = 16;

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

    public static byte[][] messageBuilder(byte firstByte, byte destByte, String message) {
        byte[] sInByte = message.getBytes();
        //  Log.d(TAG, "OUD: messageBuilder: length message :" + sInByte.length);
        byte[][] finalMessage;
        int numPacks = (int) Math.floor(sInByte.length / DEST_PACK_MESSAGE_LEN);

        int lastLen = sInByte.length % DEST_PACK_MESSAGE_LEN;
        int numPackToSend = (lastLen == 0) ? numPacks : numPacks + 1;

        finalMessage = new byte[numPackToSend][PACK_LEN];
        Log.d(TAG, "OUD: numPack: " + numPackToSend);
        // Log.d(TAG, "OUD: messageBuilder:Entrata foqr");
        Utility.printByte(firstByte);
        Utility.printByte(destByte);
        for (int i = 0; i < numPackToSend; i++) {
            if (i == numPackToSend - 1) {
                byte[] pack = new byte[lastLen + 2];
                firstByte = clearBit(firstByte, 0);
                pack[0] = firstByte;
                pack[1] = destByte;
                for (int j = 0; j < lastLen; j++) {
                    pack[j + 2] = sInByte[j + (i * DEST_PACK_MESSAGE_LEN)];
                }
                finalMessage[i] = pack;
            } else {
                byte[] pack = new byte[PACK_LEN];
                pack[0] = firstByte;
                pack[1] = destByte;
                for (int j = 0; j < DEST_PACK_MESSAGE_LEN; j++) {
                    pack[j + 2] = sInByte[j + (i * DEST_PACK_MESSAGE_LEN)];
                }
                finalMessage[i] = pack;
            }
        }
        //Log.d(TAG, "OUD: messageBuilder:Fine for");

        return finalMessage;
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
        res[0] = (id.length() == 2 )? (Integer.parseInt(id.substring(0,1))):(Integer.parseInt(id.substring(0,2)));
        res[1] = (id.length() == 2 )? (Integer.parseInt(id.substring(1,2))):(Integer.parseInt(id.substring(2,3)));
        return res;
    }

    public static boolean sendMessage(String message, BluetoothGatt gatt, int[] infoSorg, int[] infoDest, OnMessageSentListener listener) {
        byte[][] finalMessage = messageBuilder(byteMessageBuilder(infoSorg[0], infoSorg[1]), byteMessageBuilder(infoDest[0], infoDest[1]), message);
        boolean result = true;
        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().equals(Constants.ServiceUUID)) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().equals(Constants.CharacteristicUUID)) {
                            for (int i = 0; i < finalMessage.length; i++) {
                                chars.setValue(finalMessage[i]);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                result = res && result;
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + new String(finalMessage[i]));
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                            }

                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
        return false;
    }

    public static boolean sendRoutingTable(String message, BluetoothGatt gatt, int[] infoSorg, int[] infoDest) {
        // Log.d(TAG, "OUD: PRE messagBuilder ok");
        byte[][] finalMessage = messageBuilder(byteMessageBuilder(infoSorg[0], infoSorg[1]), byteNearServerBuilder(infoDest[0], infoDest[1]), message);
        // Log.d(TAG, "OUD: messagBuilder ok");
        boolean result = true;
        BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
        if (service == null) {
            Log.d(TAG, "OUD: " + "il service era NULL");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
        if (characteristic == null) {
            Log.d(TAG, "OUD: " + "la caratteristica era NULL");
            return false;
        }

        for (int i = 0; i < finalMessage.length; i++) {
            characteristic.setValue(finalMessage[i]);
            gatt.beginReliableWrite();
            boolean res = gatt.writeCharacteristic(characteristic);
            result = res && result;
            gatt.executeReliableWrite();
            Log.d(TAG, "OUD: " + "Inviato? -> " + res);
            try {
                Thread.sleep(300);
            } catch (Exception e) {
                Log.d(TAG, "OUD: " + "Andata male la wait");
            }
            if (i == finalMessage.length - 1) return result;
        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
        return false;
    }


    /**
     * Use this check to determine whether BLE is supported on the device. Then
     * you can selectively disable BLE-related features.
     *
     * @param context
     * @return
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
        User u = new User(device, device.getName());
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
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                Log.d(TAG, "OUD: onDescriptorRead: " + new String(descriptor.getValue()) + "length: " + descriptor.getValue().length + "routingId: " + routingId);
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
                    final String temp = new String(value);
                    BluetoothGattCharacteristic characteristic1 = descriptor.getCharacteristic();
                    if (characteristic1 == null) return;
                    final int[] infoSorg = new int[2];
                    final int[] infoDest = new int[2];
                    infoSorg[0] = Integer.parseInt(id);
                    infoSorg[1] = 0;
                    infoDest[0] = 0;
                    infoDest[1] = 0;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (Utility.sendRoutingTable(temp, gatt, infoSorg, infoDest))
                                Log.d(TAG, "OUD: " + "Routing table inviata con successo!");
                        }
                    }, 500);

                }
            }
        });
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

    public static ConnectBLETask createBroadCastNextServerIdClient(BluetoothDevice device, final String nextId, Context context, final byte[] value) {
        User u = new User(device, device.getName());
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
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicNextServerIdUUID);
                if (characteristic == null) {
                    Log.d(TAG, "OUD: " + "LA CARATTERISTICA ERA NULL");
                    return;
                }
                boolean res = gatt.readCharacteristic(characteristic);
                Log.d(TAG, "OUD: " + "Read Characteristic nextServerID: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: Status == " + status + ", value: " + new String(characteristic.getValue()) + " Length: " + characteristic.getValue().length);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    String temp = new String(characteristic.getValue());
                    if (Integer.parseInt(nextId) > Integer.parseInt(temp)) {
                        characteristic.setValue(nextId);
                        gatt.beginReliableWrite();
                        boolean res = gatt.writeCharacteristic(characteristic);
                        Log.d(TAG, "OUD: " + "Write Characteristic :--> " + res);
                        gatt.executeReliableWrite();
                    }

                }
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Log.d(TAG, "OUD: " + "i wrote a characteristic !");
                if(characteristic.getUuid().equals(Constants.RoutingTableCharacteristicUUID)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "OUD: " + "I wrote a new server on a server");
                    }
                    else {
                        Log.d(TAG, "OUD: " + "Error1: " + status);
                    }
                }
                else {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        String temp = new String(value);
                        BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                        if (service == null) return;
                        BluetoothGattCharacteristic characteristic1 = service.getCharacteristic(Constants.RoutingTableCharacteristicUUID);
                        if (characteristic1 == null) return;
                        characteristic1.setValue(temp);
                        gatt.beginReliableWrite();
                        boolean res = gatt.writeCharacteristic(characteristic1);
                        Log.d(TAG, "OUD: " + "write charac? " + res);
                        gatt.executeReliableWrite();
                    }
                    else {
                        Log.d(TAG, "OUD: " + "Error2: " + status);
                    }
                }
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        });
    }

    public static void updateServerToAsk(BluetoothAdapter mBluetoothAdapter, final LinkedList<ScanResult> serverToAsk, final HashMap<String, BluetoothDevice> nearMapDecice, final String nuovoId, final OnNewServerDiscoveredListener listener) {
        final BluetoothLeScanner mBluetoothScan = mBluetoothAdapter.getBluetoothLeScanner();
        final ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                super.onScanResult(callbackType, result);
                for (ScanResult temp : serverToAsk) {
                    if (temp.getDevice().equals(result.getDevice())) {
                        Log.d(TAG, "OUD: " + "result già presente");
                        return;
                    }
                }
                serverToAsk.add(result);
                listener.OnNewServerDiscovered(result);
                nearMapDecice.put(nuovoId, result.getDevice());
                Log.d(TAG, "OUD: " + "ho aggiunto " + result.getDevice().getName());
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
        }, 4500);
        //UserList.cleanUserList();
        mBluetoothScan.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
    }


    public interface OnMessageReceivedListener {
        public void OnMessageReceived(String idMitt, String message);
    }

    public interface OnMessageSentListener {
        public void OnMessageSent(String message);

        public void OnCommunicationError(String error);
    }

    public interface OnScanCompletedListener {
        public void OnScanCompleted(ArrayList<Device> devicesFound);
    }

    public interface OnNewServerDiscoveredListener {
        public void OnNewServerDiscovered(ScanResult server);
    }
}