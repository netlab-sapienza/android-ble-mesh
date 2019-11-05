package it.drone.mesh.common;

import android.util.Log;

public class ByteUtility {

    private static final String TAG = ByteUtility.class.getSimpleName();

    /**
     * @param val    byte to read
     * @param offset the offset of the bit to return
     * @return 1 if the Nth bit is 1 or 0 otherwise
     */
    public static int getBit(byte val, int offset) {
        return (val >> offset) & 1;
    }

    /**
     * @param val    byte to read
     * @param offset offset the offset of the bit to return
     * @return the byte with the Nth bit set to 1
     */
    public static byte setBit(byte val, int offset) {
        val |= 1 << offset;
        return val;
    }

    /**
     * @param val    byte to read
     * @param offset offset the offset of the bit to return
     * @return the byte with the Nth bit set to 0
     */
    public static byte clearBit(byte val, int offset) {
        val = (byte) (val & ~(1 << offset));
        return val;
    }

    /**
     * @param b just print the byte in the logs
     */
    public static void printByte(byte b) {
        String s = "";
        for (int i = 7; i > -1; i--) {
            s += getBit(b, i);
        }
        Log.d(TAG, "OUD: " + s);
    }
}
