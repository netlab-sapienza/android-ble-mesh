package it.drone.mesh.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;

import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.listeners.ServerScanCallback;
import it.drone.mesh.models.Server;
import it.drone.mesh.models.ServerList;
import it.drone.mesh.tasks.ConnectBLETask;

import static it.drone.mesh.common.Utility.SCAN_PERIOD;

/**
 * This class implements the functionality needed by a BLE client in our BLE network. It masks the complexity of the job done in the lower tier, the ConnectBLETask
 */
public class BLEClient {

    private static final String TAG = BLEClient.class.getSimpleName();

    private static final long HANDLER_PERIOD = 5000;
    private static BLEClient singleton;
    private Context context;
    private ConnectBLETask connectBLETask;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ServerScanCallback mScanCallback;
    private boolean isScanning = false;
    private BluetoothDevice serverDevice;
    private boolean isServiceStarted = false;
    private LinkedList<OnClientOnlineListener> listeners;
    private byte[] lastServerIdFound = new byte[2];
    private Listeners.OnConnectionLost OnConnectionListener;

    private BLEClient(Context context) {
        this.context = context;
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        assert mBluetoothManager != null;
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        listeners = new LinkedList<>();
    }

    public static synchronized BLEClient getInstance(Context context) {
        if (singleton == null)
            singleton = new BLEClient(context);
        return singleton;
    }

    public ConnectBLETask getConnectBLETask() {
        return connectBLETask;
    }

    public void addOnClientOnlineListener(OnClientOnlineListener list) {
        this.listeners.add(list);
    }

    public void setLastServerIdFound(byte[] lastServerIdFound) {
        if (lastServerIdFound != null) {
            Log.d(TAG, "OUD: " + "setLastServerIdFound: 1: " + (int) lastServerIdFound[0] + ", 2:" + (int) lastServerIdFound[1]);
            Utility.printByte(lastServerIdFound[0]);
            this.lastServerIdFound[0] = lastServerIdFound[0];
            this.lastServerIdFound[1] = lastServerIdFound[1];
            Utility.printByte(this.lastServerIdFound[0]);
        } else Log.d(TAG, "OUD: " + "setLastServerIdFound: è null");
    }

    private void startScanning() {
        isScanning = true;
        if (mScanCallback == null) {
            ServerList.cleanUserList();
            // Will stop the scanning after a set time.
            new Handler().postDelayed(this::initializeClient, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new ServerScanCallback(new ServerScanCallback.OnServerFoundListener() {
                @Override
                public void OnServerFound(String message) {
                    Log.d(TAG, "OUD: OnServerFound: " + message);
                }

                @Override
                public void OnErrorScan(String message, int errorCodeCallback) {
                    Log.e(TAG, "OUD: OnServerFound: " + message);
                }
            });

            mBluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), mScanCallback);

        } else {
            Log.d(TAG, "OUD: startScanning: Scanning already started ");
        }
    }

    private void initializeClient() {
        Log.d(TAG, "OUD: Stopping Scanning");
        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        isScanning = false;
        new Handler(Looper.getMainLooper()).postDelayed(() -> tryConnection(0), 1000);
    }

    private void tryConnection(final int offset) {
        if (isServiceStarted) {
            final int size = ServerList.getServerList().size();
            if (offset >= size) {
                if (connectBLETask != null || serverDevice != null) {
                    Log.d(TAG, "OUD: Something went wrong ");
                    connectBLETask = null;
                    serverDevice = null;
                    startScanning();
                } else {
                    Log.d(TAG, "OUD: Unable to find server available");
                    startScanning();
                }
            } else {
                final Server newServer = ServerList.getServer(offset);
                Log.d(TAG, "OUD: " + "tryConnection with: " + newServer.getUserName());
                final ConnectBLETask connectBLE = new ConnectBLETask(newServer, context);

                if (lastServerIdFound == null)
                    Log.d(TAG, "OUD: " + "setLastServerIdFound: è null nella try connection " + offset);
                Utility.printByte(lastServerIdFound[0]);
                Utility.printByte(lastServerIdFound[1]);
                if (lastServerIdFound[0] != (byte) 0)
                    connectBLE.setLastServerIdFound(lastServerIdFound);
                //connectBLE.addReceivedListener((idMitt, message, hop, sendTimeStamp) -> Log.d(TAG, "OnMessageReceived: Messaggio ricevuto dall'utente " + idMitt + ": " + message));
                connectBLE.setOnConnectionLostListener(() -> {
                    OnConnectionListener.OnConnectionLost();
                });
                connectBLE.startClient();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (connectBLE.hasCorrectId()) {
                        connectBLETask = connectBLE;
                        for (OnClientOnlineListener l : listeners) {
                            l.onClientOnline();
                        }
                        serverDevice = newServer.getBluetoothDevice();
                        Log.d(TAG, "OUD: You're a client and your id is " + connectBLETask.getId());
                    } else {
                        connectBLE.setJobDone();
                        Log.d(TAG, "OUD: " + "id non assegnato, passo al prossimo server");
                        tryConnection(offset + 1);
                    }
                }, HANDLER_PERIOD);
            }
        }
    }

    public void startClient() {
        isServiceStarted = true;
        Log.d(TAG, "OUD: startClient: Scan the background,search servers to join");
        startScanning();
    }

    /**
     * @param newServer the server to connect, without scan etcetc.
     */
    public void startClient(Server newServer) {
        final ConnectBLETask connectBLE = new ConnectBLETask(newServer, context);
        //connectBLE.addReceivedListener((idMitt, message, hop, sendTimeStamp) -> Log.d(TAG, "OnMessageReceived: Messaggio ricevuto dall'utente " + idMitt + ": " + message));
        connectBLE.startClient();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (connectBLE.hasCorrectId()) {
                connectBLETask = connectBLE;
                for (OnClientOnlineListener l : listeners) {
                    l.onClientOnline();
                }
                serverDevice = newServer.getBluetoothDevice();
                Log.d(TAG, "OUD: You're a client and your id is " + connectBLETask.getId());
            } else {
                Log.d(TAG, "OUD: " + "È andata male, proviamo col metodo classico");
                startScanning();
            }
        }, HANDLER_PERIOD);
    }

    public void stopClient() {
        if (connectBLETask != null) {
            connectBLETask.stopClient();
            connectBLETask = null;
        }
        isServiceStarted = false;

        Log.d(TAG, "OUD: stopClient: Service stopped");
        if (isScanning) {
            Log.d(TAG, "OUD: stopClient: Stopping Scanning");
            // Stop the scan, wipe the callback.
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanCallback = null;
            isScanning = false;
        }
    }

    public void addReceivedListener(Listeners.OnMessageReceivedListener onMessageReceivedListener) {
        if (connectBLETask != null)
            this.connectBLETask.addReceivedListener(onMessageReceivedListener);
    }

    public void removeReceivedListener(Listeners.OnMessageReceivedListener onMessageReceivedListener) {
        if (connectBLETask != null)
            this.connectBLETask.removeReceivedListener(onMessageReceivedListener);
    }

    public void addReceivedWithInternetListener(Listeners.OnMessageWithInternetListener l) {
        if (connectBLETask != null) this.connectBLETask.addReceivedWithInternetListener(l);
    }

    public void addDisconnectedServerListener(Listeners.OnDisconnectedServerListener l) {
        if (connectBLETask != null) this.connectBLETask.addDisconnectedServerListener(l);
    }

    public void removeReceivedWithInternetListener(Listeners.OnMessageWithInternetListener l) {
        if (connectBLETask != null) this.connectBLETask.removeReceivedWithInternetListener(l);
    }

    public void sendMessage(String message, String dest, boolean internet, Listeners.OnMessageSentListener onMessageSentListener) {
        if (connectBLETask != null)
            connectBLETask.sendMessage(message, dest, internet, onMessageSentListener);
        else onMessageSentListener.OnCommunicationError("Client not initialized");
    }

    public String getId() {
        if (connectBLETask != null) return connectBLETask.getId();
        else return null;
    }

    public interface OnClientOnlineListener {
        void onClientOnline();
    }

    public void setOnConnectionLostListener(Listeners.OnConnectionLost l) {
        OnConnectionListener = l;
    }
}
