package it.drone.mesh.roles.common;

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

import it.drone.mesh.utility.Constants;

/**
 * Tutte i metodi e le variabile condivise da server e client (quelle per la scansione per esempio) vengono messi qua.
 * Per il momento sia server che client devono avere accesso alla possiblità di eseguire scansioni, in futuro potrebbe cambiare
 */


public class Utility {
    // Stops scanning after 5 seconds.
    public static final long SCAN_PERIOD = 5000;


    public static boolean isBLESupported(Context context) {
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
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

}
