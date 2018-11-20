package it.drone.mesh.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

public class Utility {
    public static String TAG = Utility.class.getSimpleName();
    public static int PACK_LEN = 18;
    public static int SINGLE_PACK_MESSAGE_LEN = 17;

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

    public static byte firstByteMessageBuilder(int serverId, int clientId) {
        byte b = 0b00000000;

        Integer server = new Integer(serverId);
        Integer client = new Integer(clientId);

        byte serv = server.byteValue();
        byte clie = client.byteValue();

        b |= serv;
        b = (byte) (b << 4);
        clie = (byte) (clie << 1);
        b |= clie;
        b = setBit(b, 0);
        return b;
    }

    public static byte[][] messageBuilder(byte firstByte, String message) {
        byte[] sInByte = message.getBytes();

        int numPacks = (int) Math.floor(sInByte.length / SINGLE_PACK_MESSAGE_LEN);
        byte[][] finalMessage = new byte[numPacks + 1][PACK_LEN];
        int lastLen = sInByte.length % SINGLE_PACK_MESSAGE_LEN;

        for (int i = 0; i < numPacks + 1; i++) {
            if (i == numPacks) {
                byte[] pack = new byte[lastLen + 1];
                firstByte = clearBit(firstByte, 0);
                pack[0] = firstByte;
                for (int j = 0; j < lastLen; j++) {
                    pack[j + 1] = sInByte[j + (i * SINGLE_PACK_MESSAGE_LEN)];
                }
                finalMessage[i] = pack;
                break;
            }
            byte[] pack = new byte[PACK_LEN];
            pack[0] = firstByte;
            for (int j = 0; j < SINGLE_PACK_MESSAGE_LEN; j++) {
                pack[j + 1] = sInByte[j + (i * SINGLE_PACK_MESSAGE_LEN)];
            }
            finalMessage[i] = pack;
        }
        return finalMessage;
    }

    public static int[] getFirstByteInfo(byte firstByte) {
        int[] ret = new int[3];
        ret[2] = getBit(firstByte, 0);
        ret[1] = getBit(firstByte, 1) + getBit(firstByte, 2) * 2 + getBit(firstByte, 3) * 4;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret;
    }

    public static String getStringId(byte firstByte) {
        int[] ret = new int[2];
        ret[1] = getBit(firstByte, 1) + getBit(firstByte, 2) * 2 + getBit(firstByte, 3) * 4;
        ret[0] = getBit(firstByte, 4) + getBit(firstByte, 5) * 2 + getBit(firstByte, 6) * 4 + getBit(firstByte, 7) * 8;
        return ret[0] + "" + ret[1];
    }

    public static boolean sendMessage(String message, BluetoothGatt gatt, int[] info) {
        byte[][] finalMessage = messageBuilder(firstByteMessageBuilder(info[0], info[1]), message);

        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                            for (int i = 0; i < finalMessage.length; i++) {
                                chars.setValue(finalMessage[i]);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + new String(finalMessage[i]));
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                                return true;
                            }
                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");
        return false;
    }
}
