package it.drone.mesh.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.listeners.ServerScanCallback;
import it.drone.mesh.models.Server;
import it.drone.mesh.models.ServerList;
import it.drone.mesh.tasks.ConnectBLETask;

import static it.drone.mesh.common.Utility.SCAN_PERIOD;

public class BLEClient {

    private static final String TAG = BLEClient.class.getSimpleName();
    private static final long HANDLER_PERIOD = 5000;
    private static BLEClient singleton;
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private ConnectBLETask connectBLETask;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ServerScanCallback mScanCallback;
    private boolean isScanning = false;
    private BluetoothDevice serverDevice;
    private boolean isServiceStarted = false;
    private boolean hasInternet = false;

    private BLEClient(Context context) {
        this.context = context;
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    }

    public static synchronized BLEClient getInstance(Context context) {
        if (singleton == null)
            singleton = new BLEClient(context);
        return singleton;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public BluetoothManager getmBluetoothManager() {
        return mBluetoothManager;
    }

    public ConnectBLETask getConnectBLETask() {
        return connectBLETask;
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
        tryConnection(0);
    }

    private void tryConnection(final int offset) {
        if (isServiceStarted) {
            final int size = ServerList.getServerList().size();
            if (offset >= size) {
                if (connectBLETask != null || serverDevice != null) {
                    Log.d(TAG, "Something went wrong ");
                    connectBLETask = null;
                    serverDevice = null;
                    startScanning();
                } else {
                    Log.d(TAG, "Unable to find server available");
                    startScanning();
                }
            } else {
                final Server newServer = ServerList.getServer(offset);
                Log.d(TAG, "OUD: " + "tryConnection with: " + newServer.getUserName());
                final ConnectBLETask connectBLE = new ConnectBLETask(newServer, context);
                connectBLE.addReceivedListener(new Listeners.OnMessageReceivedListener() {
                    @Override
                    public void OnMessageReceived(final String idMitt, final String message) {
                        Log.d(TAG, "OnMessageReceived: Messaggio ricevuto dall'utente " + idMitt + ": " + message);
                    }
                });
                connectBLE.startClient();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (connectBLE.hasCorrectId()) {
                            connectBLETask = connectBLE;
                            serverDevice = newServer.getBluetoothDevice();
                            Log.d(TAG, "You're a client and your id is " + connectBLETask.getId());
                        } else {
                            Log.d(TAG, "OUD: " + "id non assegnato, passo al prossimo server");
                            tryConnection(offset + 1);
                        }
                    }
                }, HANDLER_PERIOD);
            }
        }
    }

    private void startClient() {
        isServiceStarted = true;
        Log.d(TAG, "startClient: Scan the background,search servers to join");
        startScanning();
    }

    private void stopClient() {
        connectBLETask.stopClient();
        connectBLETask = null;
        isServiceStarted = false;

        Log.d(TAG, "stopClient: Service stopped");
        if (isScanning) {
            Log.d(TAG, "stopClient: Stopping Scanning");
            // Stop the scan, wipe the callback.
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanCallback = null;
            isScanning = false;
        }
    }

    public boolean getHasInternet() {
        return hasInternet;
    }

    public void setHasInternet(boolean hasInternet) {
        this.hasInternet = hasInternet;
    }
}
