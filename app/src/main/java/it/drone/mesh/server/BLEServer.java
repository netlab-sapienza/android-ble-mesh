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
import android.os.Looper;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import it.drone.mesh.advertiser.AdvertiserService;
import it.drone.mesh.common.Constants;
import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.listeners.ServerScanCallback;
import it.drone.mesh.models.Server;
import it.drone.mesh.models.ServerList;
import it.drone.mesh.tasks.AcceptBLETask;
import it.drone.mesh.tasks.ConnectBLETask;

import static it.drone.mesh.common.Constants.MAX_ATTEMPTS_UNTIL_SERVER;
import static it.drone.mesh.common.Constants.SCAN_PERIOD_MAX;
import static it.drone.mesh.common.Constants.SCAN_PERIOD_MIN;
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
    private int attemptsUntilServer = 1;
    private int randomValueScanPeriod;
    private Listeners.OnDebugMessageListener debugMessageListener;
    private Listeners.OnEnoughServerListener enoughServerListener;
    private byte[] lastServerIdFound = new byte[2];

    private BLEServer(Context context) {
        randomValueScanPeriod = ThreadLocalRandom.current().nextInt(SCAN_PERIOD_MIN, SCAN_PERIOD_MAX) * 1000;
        this.context = context;
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        this.acceptBLETask = new AcceptBLETask(mBluetoothAdapter, mBluetoothManager, context);
        connectionRejectedListener = () -> {
            Log.d(TAG, "Connection Rejected, stopping service");
            stopServer();
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

    public void setLastServerIdFound(byte[] lastServerIdFound) {
        this.lastServerIdFound = lastServerIdFound;
    }

    private void askIdToNearServer(final int offset) {
        final int size = ServerList.getServerList().size();
        if (offset >= size) {
            if (attemptsUntilServer < MAX_ATTEMPTS_UNTIL_SERVER && nearDeviceMap.size() == 0) {
                long sleepPeriod = randomValueScanPeriod * attemptsUntilServer;
                debugMessageListener.OnDebugMessage("Attempt " + attemptsUntilServer + ": Can't find any server, I'll retry after " + sleepPeriod + " milliseconds");
                new Handler(Looper.getMainLooper()).postDelayed(() -> startScanning(), sleepPeriod);
                attemptsUntilServer++;
            } else if (isServiceStarted) {
                context.startService(new Intent(context, AdvertiserService.class));
                acceptBLETask.addConnectionRejectedListener(connectionRejectedListener);
                acceptBLETask.insertMapDevice(nearDeviceMap);
                acceptBLETask.addRoutingTableUpdatedListener(message -> debugMessageListener.OnDebugMessage("RoutingTable updated: \n" + message));
                acceptBLETask.setLastServerIdFound(lastServerIdFound);
                acceptBLETask.startServer();
            }
        } else {
            final Server newServer = ServerList.getServer(offset);
            debugMessageListener.OnDebugMessage( "OUD: " + "Try reading ID of : " + newServer.getUserName());
            final ConnectBLETask connectBLE = new ConnectBLETask(newServer, context);
            BluetoothGattCallback callback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "OUD: Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (!connectBLE.getJobDone()) {
                            connectBLE.restartClient();
                            Log.d(TAG, "OUD: Retry reading ID");
                        }
                        Log.i(TAG, "OUD: Disconnected from GATT client " + gatt.getDevice().getName());
                    }
                    super.onConnectionStateChange(gatt, status, newState);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.d(TAG, "OUD: " + "I discovered a service" + gatt.getServices() + " from " + gatt.getDevice().getName());
                    for (BluetoothGattService service : gatt.getServices()) {
                        Log.d(TAG, "OUD: onServicesDiscovered: " + service.getUuid().toString());
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
                    connectBLE.setJobDone();
                    askIdToNearServer(offset + 1);
                    super.onServicesDiscovered(gatt, status);
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    Log.d(TAG, "OUD: nDescriptorRead: descrittore uuid: " + descriptor.getUuid().toString() + ", status: " + status);
                    if (status == 0) {
                        Log.d(TAG, "OUD: " + "I read a descriptor UUID: " + descriptor.getUuid().toString());
                        if (descriptor.getUuid().toString().equals(Constants.DescriptorUUID.toString())) {
                            nearDeviceMap.put(new String(descriptor.getValue(), Charset.defaultCharset()), newServer.getBluetoothDevice());
                            Log.d(TAG, "OUD: Server inserito correttamente nella mappa");
                            BluetoothGattDescriptor nextId = descriptor.getCharacteristic().getDescriptor(Constants.NEXT_ID_UUID);
                            boolean res = gatt.readDescriptor(nextId);
                            Log.d(TAG, "OUD: Descrittore letto " + res);
                        } else if (descriptor.getUuid().equals(Constants.NEXT_ID_UUID)) {
                            int nextId = Integer.parseInt(new String(descriptor.getValue()));
                            if (nextId == 1) {
                                enoughServerListener.OnEnoughServer(newServer);
                                connectBLE.setJobDone();
                                return;
                            }
                            connectBLE.setJobDone();
                            askIdToNearServer(offset + 1);
                        }
                    }
                    else {
                        connectBLE.setJobDone();
                        askIdToNearServer(offset + 1);
                    }
                    super.onDescriptorRead(gatt, descriptor, status);
                }
            };
            connectBLE.setCallback(callback);
            connectBLE.startClient();
        }
    }

    private void startScanning() {
        if (!isServiceStarted) return;
        isScanning = true;
        debugMessageListener.OnDebugMessage("Start scanning");
        if (mScanCallback == null) {
            ServerList.cleanUserList();
            // Will stop the scanning after a set time.
            new Handler().postDelayed(() -> initializeServer(), SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new ServerScanCallback(new ServerScanCallback.OnServerFoundListener() {
                @Override
                public void OnServerFound(String message) {
                    debugMessageListener.OnDebugMessage("OnServerFound: " + message);
                }

                @Override
                public void OnErrorScan(String message, int errorCodeCallback) {
                    debugMessageListener.OnDebugErrorMessage("OnServerFound: " + message);
                }
            });

            mBluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), mScanCallback);

        } else {
            debugMessageListener.OnDebugMessage( "startScanning: Scanning already started ");
        }
    }

    private void initializeServer() {
        if (!isServiceStarted) return;
        if(acceptBLETask == null) this.acceptBLETask = new AcceptBLETask(mBluetoothAdapter, mBluetoothManager, context);
        debugMessageListener.OnDebugMessage("Stopping Scanning");
        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        isScanning = false;
        Log.d(TAG, "initializeServer: size: " + ServerList.getServerList().size());
        askIdToNearServer(0);
    }


    public void startServer() {
        debugMessageListener.OnDebugMessage("startServer: Scan the background, search servers to ask ");
        isServiceStarted = true;
        startScanning();
    }

    public void stopServer() {
        if (isServiceStarted) {
            isServiceStarted = false;
            if (acceptBLETask != null) {
                acceptBLETask.stopServer();
                acceptBLETask.removeConnectionRejectedListener(connectionRejectedListener);
                acceptBLETask = new AcceptBLETask(mBluetoothAdapter,mBluetoothManager,context);
                nearDeviceMap.clear();
                attemptsUntilServer = 1;
                lastServerIdFound = new byte[2];
                context.stopService(new Intent(context, AdvertiserService.class));
            }
            debugMessageListener.OnDebugMessage("stopServer: Service stopped");
            if (isScanning) {
                debugMessageListener.OnDebugMessage( "stopServer: Stopping Scanning");

                // Stop the scan, wipe the callback(or maybe not).
                mBluetoothLeScanner.stopScan(mScanCallback);
                //mScanCallback = null;
                isScanning = false;
            }
        } else {
            debugMessageListener.OnDebugMessage("stopServer: Service never started. ");
        }
    }

    public void setOnDebugMessageListener(Listeners.OnDebugMessageListener l) {
        this.debugMessageListener= l;
    }

    public void addConnectionRejectedListener(AcceptBLETask.OnConnectionRejectedListener connectionRejectedListeners) {
        if(acceptBLETask != null) this.acceptBLETask.addConnectionRejectedListener(connectionRejectedListeners);
    }

    public void removeConnectionRejectedListener(AcceptBLETask.OnConnectionRejectedListener connectionRejectedListener) {
        if(acceptBLETask != null) this.acceptBLETask.removeConnectionRejectedListener(connectionRejectedListener);
    }

    public void addRoutingTableUpdatedListener(AcceptBLETask.OnRoutingTableUpdatedListener routingTableUpdatedListener) {
        if(acceptBLETask != null) this.acceptBLETask.addRoutingTableUpdatedListener(routingTableUpdatedListener);
    }

    public void removeRoutingTableUpdatedListener(AcceptBLETask.OnRoutingTableUpdatedListener routingTableUpdatedListener) {
        if(acceptBLETask != null) this.acceptBLETask.removeRoutingTableUpdatedListener(routingTableUpdatedListener);
    }

    public void addOnMessageReceivedWithInternet(Listeners.OnMessageWithInternetListener listener) {
        if(acceptBLETask != null) this.acceptBLETask.addOnMessageReceivedWithInternet(listener);
    }

    public void addServerInitializedListener(Listeners.OnServerInitializedListener l) {
        Log.d(TAG, "OUD: " + (acceptBLETask==null));
        if(acceptBLETask != null) this.acceptBLETask.addServerInitializedListener(l);
    }

    public void removeServerInitializedListener(Listeners.OnServerInitializedListener l) {
        if(acceptBLETask != null) this.acceptBLETask.removeServerInitializedListener(l);
    }

    public String getId() {
        if(acceptBLETask != null) return acceptBLETask.getId();
        else return null;
    }

    public void sendMessage(String message, String dest, boolean internet, Listeners.OnMessageSentListener listener) {
        if (acceptBLETask != null)
            acceptBLETask.sendMessage(message,getId(), dest, internet, listener);
        else listener.OnCommunicationError("Server not initialized");
    }

    public void addOnMessageReceivedListener(Listeners.OnMessageReceivedListener l) {
        if (acceptBLETask != null) this.acceptBLETask.addOnMessageReceived(l);
    }

    public void setEnoughServerListener(Listeners.OnEnoughServerListener enoughServerListener) {
        this.enoughServerListener = enoughServerListener;
    }
}
