package it.drone.mesh.roles.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.List;

import it.drone.mesh.R;
import it.drone.mesh.roles.common.RoutingTable;
import it.drone.mesh.roles.common.ScanResultList;
import it.drone.mesh.roles.common.exceptions.NotEnabledException;
import it.drone.mesh.roles.common.exceptions.NotSupportedException;

import static it.drone.mesh.roles.common.Utility.SCAN_PERIOD;
import static it.drone.mesh.roles.common.Utility.buildScanFilters;
import static it.drone.mesh.roles.common.Utility.buildScanSettings;
import static it.drone.mesh.roles.common.Utility.isBLESupported;

/**
 * BLEServer ha il compito di offrire un servizio che implementi le seguenti funzioni:
 * 1) Fornire identità nella sottorete
 * 2) Permettere lo scambio di messaggi nella sottorete e nelle altre reti
 * 3) ....
 */

public class BLEServer {

    private final static String TAG = BLEServer.class.getSimpleName();

    private static BLEServer singleton;
    // potrebbero venir riutilizzati, quindi non convertire a local
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGattServer mGattServer;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private BluetoothGattDescriptor mGattDescriptor;
    private BluetoothGattDescriptor mGattDescriptorNextId;


    private String id;
    private RoutingTable routingTable;

    private boolean isScanning = false;
    /**
     * La callback popola la lista con i risultati che trova
     */
    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            ScanResultList.getInstance().addResult(result);
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

    private BLEServer(Context context) throws NotSupportedException, NotEnabledException {
        bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            mBluetoothAdapter = bluetoothManager.getAdapter();
        else
            throw new NotSupportedException(context.getResources().getString(R.string.bt_null));

        if (!isBLESupported(context)) {
            throw new NotSupportedException(context.getResources().getString(R.string.bt_not_supported));
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            throw new NotEnabledException(context.getResources().getString(R.string.bt_not_enabled_leaving));
        }
    }

    public static synchronized BLEServer getInstance(Context context) throws NotSupportedException, NotEnabledException {
        if (singleton == null)
            singleton = new BLEServer(context);
        return singleton;
    }

    public void initializeService(Context context) {

        BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "OUD: " + "I'm the server, I've connected to " + device.getName());
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "OUD: " + "onConnectionStateChange: DISCONNECTED from" + device.getName());
                }
                super.onConnectionStateChange(device, status, newState);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.d(TAG, "OUD: " + "I've been asked to read from " + device.getName());
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                // TODO: 16/11/18 questa fa il broadcast da fare
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                // TODO: 16/11/18
                /*
                Log.d(TAG, "OUD: " + "I've been asked to read descriptor from " + device.getName());
                if (descriptor.getUuid().toString().equals((mGattDescriptorNextId.getUuid().toString()))) {
                    mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                    int next_id = Integer.parseInt(new String(mGattDescriptorNextId.getValue())) + 1;
                    String value = "" + next_id;
                    mGattDescriptorNextId.setValue(value.getBytes());
                    Log.d(TAG, "OUD: " + "NextId: " + value);
                } else mGattServer.sendResponse(device, requestId, 0, 0, descriptor.getValue());
                Log.d(TAG, "OUD: " + new String(mGattDescriptor.getValue()));
                */
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                super.onExecuteWrite(device, requestId, execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
            }
        };

        // TODO: 16/11/18 usare campi della classe BLE
        String id = "1";
        String next_id = "1";

        mGattDescriptor.setValue(id.getBytes());
        this.mGattCharacteristic.addDescriptor(mGattDescriptor);
        mGattDescriptorNextId.setValue(next_id.getBytes());
        this.mGattCharacteristic.addDescriptor(mGattDescriptorNextId);
        this.mGattService.addCharacteristic(mGattCharacteristic);

        this.mGattServer = bluetoothManager.openGattServer(context, callback);
        this.mGattServer.addService(this.mGattService);


        // TODO: 16/11/18 scannare gli altri server per prendere l'id corretto
        // startScan(context);
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RoutingTable getRoutingTable() {

        return this.routingTable;
    }



    public boolean sendMessage(String message) {
        // TODO: 13/11/18
        return true;
    }

    /**
     * Avvia la scansione. È consigliato chiamare prima un {@code isScanning()} per essere sicuri dell'esito corretto della scansione
     */

    public void startScan(final Context context) {
        ScanResultList.getInstance().cleanList();
        if (!isScanning) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan(context);
                }
            }, SCAN_PERIOD);

            mBluetoothAdapter.getBluetoothLeScanner().startScan(buildScanFilters(), buildScanSettings(), bleScanCallback);
            isScanning = true;
        }
    }

    /**
     * Blocca la scansione corrente, deve rimanere public perchè in caso di chiusura attività o dell'app anche la scansione deve bloccarsi
     */

    public void stopScan(Context context) {
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


}
