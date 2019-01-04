package it.drone.mesh.init;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import it.drone.mesh.R;
import it.drone.mesh.advertiser.AdvertiserService;
import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.listeners.ServerScanCallback;
import it.drone.mesh.models.Server;
import it.drone.mesh.models.ServerList;
import it.drone.mesh.tasks.AcceptBLETask;
import it.drone.mesh.tasks.ConnectBLETask;

import static it.drone.mesh.common.Constants.MAX_ATTEMPTS_UNTIL_SERVER;
import static it.drone.mesh.common.Constants.REQUEST_ENABLE_BT;
import static it.drone.mesh.common.Constants.SCAN_PERIOD_MAX;
import static it.drone.mesh.common.Constants.SCAN_PERIOD_MIN;
import static it.drone.mesh.common.Utility.SCAN_PERIOD;

public class InitActivity extends Activity {

    private static final String TAG = InitActivity.class.getSimpleName();

    private static final long HANDLER_PERIOD = 5000;
    private static final int PERMISSION_REQUEST_WRITE = 564;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    private Button startServices;
    private TextView debugger, whoAmI, myId;
    private DeviceAdapter deviceAdapter;

    ServerScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean isServiceStarted = false;

    private ConnectBLETask connectBLETask;

    private HashMap<String, BluetoothDevice> nearDeviceMap = new HashMap<>();

    private AcceptBLETask acceptBLETask;

    private int attemptsUntilServer = 1;
    private long randomValueScanPeriod;
    private AcceptBLETask.OnConnectionRejectedListener connectionRejectedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        startServices = findViewById(R.id.startServices);
        debugger = findViewById(R.id.debugger);
        whoAmI = findViewById(R.id.whoami);
        myId = findViewById(R.id.myid);
        randomValueScanPeriod = ThreadLocalRandom.current().nextInt(SCAN_PERIOD_MIN, SCAN_PERIOD_MAX) * 1000;

        askPermissions(savedInstanceState);

        RecyclerView recyclerDeviceList = findViewById(R.id.recy_scan_results);
        deviceAdapter = new DeviceAdapter();
        recyclerDeviceList.setAdapter(deviceAdapter);
        recyclerDeviceList.setVisibility(View.VISIBLE);

        connectionRejectedListener = new AcceptBLETask.OnConnectionRejectedListener() {
            @Override
            public void OnConnectionRejected() {
                writeErrorDebug("Connection Rejected, stopping service");
                startServices.performClick();
            }
        };

        startServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isServiceStarted) {
                    startServices.setText(R.string.start_service);
                    isServiceStarted = false;
                    if (acceptBLETask != null) {
                        acceptBLETask.stopServer();
                        acceptBLETask.removeConnectionRejectedListener(connectionRejectedListener);
                        acceptBLETask = null;
                    } else if (connectBLETask != null) {
                        connectBLETask.stopClient();
                        connectBLETask = null;
                    }
                    whoAmI.setText(R.string.whoami);
                    myId.setText(R.string.myid);
                    writeDebug("Service stopped");
                    attemptsUntilServer = 1;
                    deviceAdapter.cleanView();
                } else {
                    initializeService();
                    startServices.setText(R.string.stop_service);
                    isServiceStarted = true;
                    writeDebug("Service started");
                }

            }
        });
    }

    /**
     * Controlla che l'app sia eseguibile e inizia lo scanner
     */
    private void initializeService() {
        writeDebug("Start initializing server");
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        startScanning();
    }

    /**
     * Start scanning for BLE Servers
     */
    public void startScanning() {
        if (mScanCallback == null) {
            writeDebug("Starting Scanning");
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
                    writeDebug(message);
                }

                @Override
                public void OnErrorScan(String message, int errorCodeCallback) {
                    writeErrorDebug(message);
                }
            });

            mBluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), mScanCallback);

            writeDebug(getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds));
        } else {
            writeDebug(getString(R.string.already_scanning));
        }
    }

    /**
     * Stop scanning for BLE Servers and start link in the mesh network
     */
    public void stopScanning() {
        writeDebug("Stopping Scanning");
        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        tryConnection(0);
    }

    /**
     * Funzione ricorsiva per provare a connettersi come client ai server trovati; Se non ce ne sono o nessuno è disponibile
     * diventi tu stesso Server
     *
     * @param offset ---> indice nell'ServerList dei vari server, con offset > size si diventa server
     */
    public void tryConnection(final int offset) {
        final int size = ServerList.getServerList().size();
        if (offset >= size) {
            if (attemptsUntilServer < MAX_ATTEMPTS_UNTIL_SERVER) {
                long sleepPeriod = randomValueScanPeriod * attemptsUntilServer;
                writeDebug("Attempt " + attemptsUntilServer + ": Can't find any server, I'll retry after " + sleepPeriod + " milliseconds");
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        startScanning();
                    }
                }, sleepPeriod);
                attemptsUntilServer++;
            } else {
                startService(new Intent(this, AdvertiserService.class));
                writeDebug("Start Server");
                acceptBLETask = new AcceptBLETask(mBluetoothAdapter, mBluetoothManager, this);
                acceptBLETask.addConnectionRejectedListener(connectionRejectedListener);
                acceptBLETask.insertMapDevice(nearDeviceMap);
                acceptBLETask.startServer();
                deviceAdapter.setAcceptBLETask(acceptBLETask);
                new Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                whoAmI.setText(R.string.server);
                                myId.setText(acceptBLETask.getId()); // TODO: 03/01/19 @Andrea gli devi dare almeno 5 sec di tempo per fare in modo che l'id venga assegnato.
                            }
                        }, HANDLER_PERIOD);
            }
            return;
        }
        final Server newServer = ServerList.getServer(offset);
        Log.d(TAG, "OUD: " + "tryConnection with: " + newServer.getUserName());
        final ConnectBLETask connectBLE = new ConnectBLETask(newServer, this);
        connectBLE.addReceivedListener(new Listeners.OnMessageReceivedListener() {
            @Override
            public void OnMessageReceived(final String idMitt, final String message) {
                writeDebug("Messaggio ricevuto dall'utente " + idMitt + ": " + message);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        deviceAdapter.notifyDataSetChanged();
                        // Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        connectBLE.startClient();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "OUD: Run ");
                if (connectBLE.hasCorrectId()) {
                    writeDebug("Id trovato: " + connectBLE.getId());
                    try {
                        writeDebug("Id assegnato correttamente");
                        connectBLETask = connectBLE;
                        writeDebug("OUD: " + "You're a client and your id is " + connectBLETask.getId());
                        deviceAdapter.setConnectBLETask(connectBLETask);
                        myId.setText(connectBLETask.getId());
                        whoAmI.setText(R.string.client);
                    } catch (Exception e) {
                        Log.d(TAG, "OUD: " + "id non assegnato con eccezione");
                        e.printStackTrace();
                        tryConnection(offset + 1);
                    }
                } else {
                    if (connectBLE.getServerId() != null) {
                        nearDeviceMap.put(connectBLE.getServerId(), newServer.getBluetoothDevice());
                        Log.d(TAG, "OUD: " + "Inserito server " + connectBLE.getServerId() + " nella mappa");
                    }
                    Log.d(TAG, "OUD: " + "id non assegnato senza eccezione");
                    tryConnection(offset + 1);
                }
            }
        }, HANDLER_PERIOD);
        writeDebug("Assegnazione id tra 5 secondi");
    }


    private void writeDebug(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (debugger.getLineCount() == debugger.getMaxLines())
                    debugger.setText(String.format("%s\n", message));
                else
                    debugger.setText(String.format("%s%s\n", String.valueOf(debugger.getText()), message));
            }
        });
        Log.d(TAG, message);
    }

    private void writeErrorDebug(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (debugger.getLineCount() == debugger.getMaxLines())
                    debugger.setText(String.format("%s\n", message));
                else
                    debugger.setText(String.format("%s%s\n", String.valueOf(debugger.getText()), message));
            }
        });
        Log.e(TAG, message);
    }


    private void askPermissions(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothAvailability(savedInstanceState);
                askPermissionsStorage();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            // fix per API < 23
        } else if (PermissionChecker.PERMISSION_GRANTED == PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            checkBluetoothAvailability(savedInstanceState);
        } else {
            // permission not granted, we must decide what to do
            Toast.makeText(this, "Permissions not granted API < 23", Toast.LENGTH_LONG).show();
        }


    }

    private void askPermissionsStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                writeDebug("Write storage permissions granted");
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE);
            }
            // fix per API < 23
        } else if (PermissionChecker.PERMISSION_GRANTED == PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            writeDebug("Write storage permissions granted");
        } else {
            // permission not granted, we must decide what to do
            Toast.makeText(this, "Permissions not granted API < 23", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Cattura la risposta asincrona di richiesta dei permessi e se è tutto ok passa a controllare il bluetooth
     *
     * @param requestCode  codice richiesta ( per coarse location = PERMISSION_REQUEST_COARSE_LOCATION )
     * @param permissions  permessi richiesti. NB If request is cancelled, the result arrays are empty.
     * @param grantResults int [] rappresentati gli esiti delle richieste
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: OK");
                    checkBluetoothAvailability();
                    askPermissionsStorage();
                } else {
                    Log.e(TAG, "onRequestPermissionsResult: Permission denied");
                }
                break;
            case PERMISSION_REQUEST_WRITE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: OK");
                    writeDebug("Write storage permissions granted");
                } else {
                    writeDebug("Write storage permissions denied");
                }
        }
    }

    /**
     * Controlla che il cellulare supporti l'app e il multiple advertisement. Maschera per onActivityResult e onRequestPermissionsResult
     */
    private void checkBluetoothAvailability() {
        checkBluetoothAvailability(null);
    }

    /**
     * Controlla che il cellulare supporti l'app e il multiple advertisement.
     *
     * @param savedInstanceState se l'app era già attiva non devo reinizializzare tutto
     */
    private void checkBluetoothAvailability(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null)
                mBluetoothAdapter = mBluetoothManager.getAdapter();

            // Is Bluetooth supported on this device?
            if (mBluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {

                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        writeDebug("Everything is supported and enabled");
                    } else {
                        writeDebug("Your device does not support multiple advertisement, you can be only client");
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not supported.
                writeDebug(getString(R.string.bt_not_supported));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceStarted) {/*
            if (connectBLETask != null)
                connectBLETask.stopClient();
            if (acceptBLETask != null)
                acceptBLETask.stopServer();
            isServiceStarted = false;
            */
            // se esplode tornare allo stato precedente
            startServices.performClick();
        }
    }
}
