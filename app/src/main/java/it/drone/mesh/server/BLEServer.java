package it.drone.mesh.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.HashMap;

import it.drone.mesh.advertiser.AdvertiserService;
import it.drone.mesh.common.Constants;
import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.ServerScanCallback;
import it.drone.mesh.models.Server;
import it.drone.mesh.models.ServerList;
import it.drone.mesh.tasks.AcceptBLETask;
import it.drone.mesh.tasks.ConnectBLETask;

import static it.drone.mesh.common.Utility.SCAN_PERIOD;

/**
 * BLEServer ha il compito di offrire un servizio che implementi le seguenti funzioni:
 * 1) Fornire identità nella sottorete
 * 2) Permettere lo scambio di messaggi nella sottorete e nelle altre reti
 * 3) ....
 */

public class BLEServer {

    private final static String TAG = BLEServer.class.getSimpleName();

    private static BLEServer singleton;
    private final Context context;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private AcceptBLETask acceptBLETask;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ServerScanCallback mScanCallback;
    private HashMap<String, BluetoothDevice> nearDeviceMap = new HashMap<>();
    private AcceptBLETask.OnConnectionRejectedListener connectionRejectedListener;
    private boolean isServiceStarted = false;
    private boolean isScanning = false;


    private BLEServer(Context context) {
        this.context = context;
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        this.acceptBLETask = new AcceptBLETask(mBluetoothAdapter, mBluetoothManager, context);
        connectionRejectedListener = new AcceptBLETask.OnConnectionRejectedListener() {
            @Override
            public void OnConnectionRejected() {
                Log.d(TAG, "Connection Rejected, stopping service");
                stopServer();
            }
        };
    }

    public static synchronized BLEServer getInstance(Context context) {
        if (singleton == null)
            singleton = new BLEServer(context);
        return singleton;
    }

    public AcceptBLETask getAcceptBLETask() {
        return acceptBLETask;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public BluetoothManager getmBluetoothManager() {
        return mBluetoothManager;
    }

    public void startScanning() {
        isScanning = true;
        if (mScanCallback == null) {
            ServerList.cleanUserList();
            // Will stop the scanning after a set time.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new ServerScanCallback(new ServerScanCallback.OnServerFoundListener() {
                @Override
                public void OnServerFound(String message) {
                    Log.d(TAG, "OnServerFound: " + message);
                }

                @Override
                public void OnErrorScan(String message, int errorCodeCallback) {
                    Log.e(TAG, "OnServerFound: " + message);
                }
            });

            mBluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), mScanCallback);

        } else {
            Log.d(TAG, "startScanning: Scanning already started ");
        }
    }

    public void stopScanning() {
        Log.d(TAG, "Stopping Scanning");
        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        isScanning = false;
        askIdToNearServer(0);
    }

    private void askIdToNearServer(final int offset) {
        final int size = ServerList.getServerList().size();
        if (offset >= size) {
            context.startService(new Intent(context, AdvertiserService.class));
            acceptBLETask.addConnectionRejectedListener(connectionRejectedListener);
            acceptBLETask.insertMapDevice(nearDeviceMap);
            acceptBLETask.addRoutingTableUpdatedListener(new AcceptBLETask.OnRoutingTableUpdatedListener() {
                @Override
                public void OnRoutingTableUpdated(final String message) {
                    Log.d(TAG, "OnRoutingTableUpdated: \n" + message);
                }
            });
            acceptBLETask.startServer();
        } else {
            final Server newServer = ServerList.getServer(offset);
            Log.d(TAG, "OUD: " + "Try reading ID of : " + newServer.getUserName());
            final ConnectBLETask connectBLE = new ConnectBLETask(newServer, context, new BluetoothGattCallback() {
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
                    Log.d(TAG, "OUD: " + "I discovered a service" + gatt.getServices() + " from " + gatt.getDevice().getName());
                    for (BluetoothGattService service : gatt.getServices()) {
                        if (service.getUuid().toString().equals(Constants.ServiceUUID.toString())) {
                            if (service.getCharacteristics() != null) {
                                for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                    if (chars.getUuid().equals(Constants.CharacteristicUUID)) {
                                        BluetoothGattDescriptor desc = chars.getDescriptor(Constants.DescriptorUUID);
                                        boolean res = gatt.readDescriptor(desc);
                                        Log.d(TAG, "OUD: " + "descrittore ID letto ? " + res);
                                        super.onServicesDiscovered(gatt, status);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    askIdToNearServer(offset + 1);
                    super.onServicesDiscovered(gatt, status);
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    if (status == 0) {
                        Log.d(TAG, "OUD: " + "I read a descriptor UUID: " + descriptor.getUuid().toString());
                        if (descriptor.getUuid().toString().equals(Constants.DescriptorUUID.toString())) {
                            nearDeviceMap.put(new String(descriptor.getValue(), Charset.defaultCharset()), newServer.getBluetoothDevice());
                            Log.d(TAG, "Server inserito correttamente nella mappa");
                        }
                    }
                    askIdToNearServer(offset + 1);
                    super.onDescriptorRead(gatt, descriptor, status);
                }
            });
            connectBLE.startClient();
        }
    }

    public void startServer() {
        Log.d(TAG, "startServer: Scan the background,search servers to ask ");
        isServiceStarted = true;
        startScanning();
    }

    public void stopServer() {
        if (isServiceStarted) {
            isServiceStarted = false;
            if (acceptBLETask != null) {
                acceptBLETask.stopServer();
                acceptBLETask.removeConnectionRejectedListener(connectionRejectedListener);
                acceptBLETask = null;
            }
            Log.d(TAG, "stopServer: Service stopped");
            if (isScanning) {
                Log.d(TAG, "stopServer: Stopping Scanning");
                // Stop the scan, wipe the callback.
                mBluetoothLeScanner.stopScan(mScanCallback);
                mScanCallback = null;
                isScanning = false;
            }
        } else {
            Log.d(TAG, "stopServer: Service never started. ");
        }
    }

}
