package it.drone.mesh.utility;

import android.os.ParcelUuid;

/**
 * Constants for use in the Bluetooth Advertisements sample
 */
public class Constants {

    /**
     * UUID identified with this app - set as Service UUID for BLE Advertisements.
     * <p>
     * Bluetooth requires a certain format for UUIDs associated with Services.
     * The official specification can be found here:
     * {https://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery}
     */

    /*public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Service_UUID_client = ParcelUuid
            .fromString("9999b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Characteristic_UUID = ParcelUuid
            .fromString("1111b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Descriptor_UUID = ParcelUuid
            .fromString("2222b81d-0000-1000-8000-00805f9b34fb");*/

    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("00001814-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Service_UUID_client = ParcelUuid
            .fromString("00002A14-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Characteristic_UUID = ParcelUuid
            .fromString("1111b81d-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Descriptor_UUID = ParcelUuid
            .fromString("2222b81d-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    // Strings for data exchange activity
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
}

