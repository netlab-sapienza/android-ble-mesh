package it.drone.mesh.roles.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import it.drone.mesh.utility.Constants;

/**
 * Tutte i metodi e le variabile condivise da server e client (quelle per la scansione per esempio) vengono messi qua.
 * Per il momento sia server che client devono avere accesso alla possiblità di eseguire scansioni, in futuro potrebbe cambiare
 */


public class Utility {
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;
    // Gestione Scan devices
    private boolean isScanning = false;
    /**
     * La callback popola la lista con i risultati che trova
     */
    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            ScanResultList.getInstance().addResult(result);
            ScanResultList.getInstance().printList();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public static boolean isBLESupported(Context context) {
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Avvia la scansione. È consigliato chiamare prima un {@code isScanning()} per essere sicuri dell'esito corretto della scansione
     *
     * @param mBluetoothAdapter bluetoothAdapter per avviare la scansione
     */

    public void startScan(final BluetoothAdapter mBluetoothAdapter) {
        ScanResultList.getInstance().cleanList();
        if (!isScanning) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan(mBluetoothAdapter);
                }
            }, SCAN_PERIOD);

            mBluetoothAdapter.getBluetoothLeScanner().startScan(buildScanFilters(), buildScanSettings(), bleScanCallback);
            isScanning = true;
        }
    }

    /**
     * Blocca la scansione corrente, deve rimanere public perchè in caso di chiusura attività o dell'app anche la scansione deve bloccarsi
     *
     * @param mBluetoothAdapter riferimento al bluetoothAdapter
     */
    public void stopScan(BluetoothAdapter mBluetoothAdapter) {
        if (isScanning) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(bleScanCallback);
            isScanning = false;
        }
    }

    /**
     * @return true se è in corso una scansione
     */

    public boolean isScanning() {
        return isScanning;
    }


    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
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
