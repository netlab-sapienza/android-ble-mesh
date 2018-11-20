package it.drone.mesh.roles.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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
import it.drone.mesh.roles.server.BLEServer;
import it.drone.mesh.utility.Constants;

import static it.drone.mesh.roles.common.Utility.SCAN_PERIOD;
import static it.drone.mesh.roles.common.Utility.buildScanFilters;
import static it.drone.mesh.roles.common.Utility.buildScanSettings;
import static it.drone.mesh.roles.common.Utility.isBLESupported;

/**
 * The first step in interacting with a BLE device is connecting to it— more specifically,
 * connecting to the GATT server on the device. To connect to a GATT server on a BLE device,
 * you use the connectGatt() method.
 * This method takes three parameters:
 * a Context object, autoConnect (boolean indicating whether to automatically connect to the BLE device
 * as soon as it becomes available), and a reference to a BluetoothGattCallback:
 * <p>
 * mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
 * <p>
 * This connects to the GATT server hosted by the BLE device,
 * and returns a BluetoothGatt instance, which
 * you can then use to conduct GATT client operations.
 * The caller (the Android app) is the GATT client.
 * The BluetoothGattCallback is used to deliver results to the client,
 * such as connection status, as well as any further GATT client operations.
 * <p>
 * src :  https://developer.android.com/guide/topics/connectivity/bluetooth-le#connect
 */

public class BLEClient {

    private static final String TAG = BLEClient.class.getSimpleName();

    private static BLEClient singleton;
    private BluetoothGatt mGatt;
    private boolean isScanning = false;
    private String id;
    private char serverId;

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


    // potrebbero venir riutilizzati, quindi non convertire a local
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BLEClient(Context context) throws NotSupportedException, NotEnabledException {
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

    public static BLEClient getInstance(Context context) throws NotEnabledException, NotSupportedException {
        if (singleton == null)
            singleton = new BLEClient(context);
        return singleton;
    }

    public String getId() {
        return this.id;
    }

    private void setId(String id) {
        this.id = id;
        this.serverId = id.charAt(0);
    }

    private void getIdFromServer(final Context context) {

        ScanResult candidate = ScanResultList.getInstance().removeFirst();
        if (candidate == null) {
            try {
                BLEServer.getInstance(context).initializeService(context);
            } catch (NotSupportedException e) {
                e.printStackTrace();
                // TODO: 16/11/18 richiesta che qualcun altro mi accolga nella rete perchè non posso essere server 
            } catch (NotEnabledException e) {
                e.printStackTrace();
                Log.e(TAG, "getIdFromServer: ");
                // TODO: 16/11/18 richiesta di attivazione bluetooth
            }
        } else {

            candidate.getDevice().connectGatt(context, false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                    if (service != null) {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicUUID);
                        if (characteristic != null) {
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.NEXT_ID_UUID);
                            if (descriptor != null) {
                                boolean ret = gatt.readDescriptor(descriptor);
                                Log.i(TAG, "OUD: descriptor readed? " + ret);
                            }
                        }

                    }
                }

                // TODO: 16/11/18 @Nero fai i compiti del client

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorRead(gatt, descriptor, status);
                    switch (status) {
                        case BluetoothGatt.GATT_SUCCESS:
                            setId(new String(descriptor.getValue()));
                            Log.i(TAG, "OUD: onDescriptorRead: SUCCESS: id = " + getId());
                            mGatt = gatt;
                            break;

                        case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                            Log.i(TAG, "OUD: onDescriptorRead: read not permitted");
                            getIdFromServer(context);
                            break;

                        default:
                            Log.e(TAG, "OUD: onDescriptorRead: status =  " + status);
                            getIdFromServer(context);
                    }
                }
            });
        }

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
            getIdFromServer(context);
        }

    }

    public RoutingTable getRoutingTable() {

        return null;
    }

    /**
     * @param idDest
     * @param message
     * @return
     */
    public boolean sendMessage(String idDest, String message) {
        return true;
    }

}
