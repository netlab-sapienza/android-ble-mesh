package it.drone.mesh.init;

import android.Manifest;
import android.app.Activity;
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
import java.util.concurrent.TimeUnit;

import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.listeners.ServerScanCallback;
import it.drone.mesh.R;
import it.drone.mesh.advertiser.AdvertiserService;
import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.roles.common.Constants;
import it.drone.mesh.roles.common.Utility;
import it.drone.mesh.roles.common.exceptions.NotEnabledException;
import it.drone.mesh.roles.common.exceptions.NotSupportedException;
import it.drone.mesh.roles.server.BLEServer;
import it.drone.mesh.tasks.AcceptBLETask;
import it.drone.mesh.tasks.ConnectBLETask;

import static it.drone.mesh.roles.common.Constants.REQUEST_ENABLE_BT;
import static it.drone.mesh.roles.common.Utility.SCAN_PERIOD;

public class InitActivity extends Activity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private static final long HANDLER_PERIOD = 5000;
    private final static String TAG = InitActivity.class.getSimpleName();


    Button startServices;
    BLEServer server;
    TextView debugger, whoami, myid;
    RecyclerView recyclerDeviceList;
    DeviceAdapter deviceAdapter;

    ServerScanCallback mScanCallback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean isServiceStarted = false;

    private ConnectBLETask connectBLETask;

    private HashMap<String, BluetoothDevice> nearDeviceMap = new HashMap<>();

    private AcceptBLETask acceptBLETask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        startServices = findViewById(R.id.startServices);
        debugger = findViewById(R.id.debugger);
        whoami = findViewById(R.id.whoami);
        myid = findViewById(R.id.myid);

        askPermissions(savedInstanceState);

        startServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isServiceStarted) {
                    startServices.setText(R.string.start_service);
                    isServiceStarted = false;
                    if (acceptBLETask != null)
                        acceptBLETask.stopServer();
                    else if (connectBLETask != null)
                        connectBLETask.stopClient();
                    whoami.setText(R.string.whoami);
                    myid.setText(R.string.myid);
                    writeDebug("Service stopped");
                } else {
                    initializeService();
                    startServices.setText(R.string.stop_service);
                    isServiceStarted = true;
                    writeDebug("Service started");
                }

            }
        });

        recyclerDeviceList = findViewById(R.id.recy_scan_results);
        deviceAdapter = new DeviceAdapter(getApplicationContext());
        recyclerDeviceList.setAdapter(deviceAdapter);
        recyclerDeviceList.setVisibility(View.VISIBLE);
    }

    /**
     * Controlla che l'app sia eseguibile e inizia lo scanner
     */
    private void initializeService() {
        writeDebug("Start initializing server");

        // questa inizializzione potrebbe essere ridondante
        try {
            server = BLEServer.getInstance(this);
        } catch (NotSupportedException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } catch (NotEnabledException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
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
            UserList.cleanUserList();
            //tempResult.clear();
            // Will stop the scanning after a set time.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new ServerScanCallback(new ServerScanCallback.OnServerFoundListerner() {
                @Override
                public void OnServerFound(String message) {
                    writeDebug(message);
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
        askIdNearServer(0);
    }

    /**
     * Since now the UserList is populated, the device starts asking for an Id
     * Funzione ricorsiva per chiedere a tutti i Server il proprio Id.
     *
     * @param offset ---> indice nell'UserList dei vari server, con offset > size finisce la ricorsività
     */

    private void askIdNearServer(final int offset) {
        final int size = UserList.getUserList().size();

        if (offset >= size) {
            tryConnection(0); //finito di leggere gli id passa a connettersi
            return;
        }

        final User newUser = UserList.getUser(offset);
        writeDebug("askNearServer with: " + newUser.getUserName());
        final ConnectBLETask connectBLETask = new ConnectBLETask(newUser, this, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    writeDebug("Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    writeDebug("Disconnected from GATT client " + gatt.getDevice().getName());
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothGattService service = gatt.getService(Constants.ServiceUUID);
                if (service == null) {
                    askIdNearServer(offset + 1);
                    return;
                }
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.CharacteristicUUID);
                if (characteristic == null) {
                    askIdNearServer(offset + 1);
                    return;
                }
                BluetoothGattDescriptor desc = characteristic.getDescriptor(Constants.DescriptorUUID);
                if (desc == null) {
                    askIdNearServer(offset + 1);
                    return;
                }
                boolean res = gatt.readDescriptor(desc);
                Log.d(TAG, "OUD: " + "Read Server id: " + res);
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    String val = new String(descriptor.getValue());
                    if (val.length() > 0) {
                        nearDeviceMap.put(val, gatt.getDevice());
                        writeDebug("id aggiunto :" + val);
                    }
                }
                super.onDescriptorRead(gatt, descriptor, status);
                askIdNearServer(offset + 1);
            }
        });
        connectBLETask.startClient();
    }

    /**
     * Funzione ricorsiva per provare a connettersi come client ai server trovati; Se non ce ne sono o nessuno è disponibile
     * diventi tu stesso Server
     *
     * @param offset ---> indice nell'UserList dei vari server, con offset > size si diventa server
     */
    public void tryConnection(final int offset) {
        final int size = UserList.getUserList().size();
        if (offset >= size) {

            startService(new Intent(this, AdvertiserService.class));
            writeDebug("Start Server");
            acceptBLETask = new AcceptBLETask(mBluetoothAdapter, mBluetoothManager, this);
            acceptBLETask.insertMapDevice(nearDeviceMap);
            acceptBLETask.startServer();
            deviceAdapter.setAcceptBLETask(acceptBLETask);
            whoami.setText(R.string.server);
            myid.setText(acceptBLETask.getId());
            return;
        }
        User newUser = UserList.getUser(offset);
        Log.d(TAG, "OUD: " + "tryConnection with: " + newUser.getUserName());
        final ConnectBLETask connectBLE = new ConnectBLETask(newUser, this);
        connectBLE.addReceivedListener(new Listeners.OnMessageReceivedListener() {
            @Override
            public void OnMessageReceived(final String idMitt, final String message) {
                writeDebug("Messaggio ricevuto dall'utente " + idMitt + ": " + message);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        connectBLE.startClient();
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
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
                        myid.setText(connectBLETask.getId());
                        whoami.setText(R.string.client);
                    } catch (Exception e) {
                        Log.d(TAG, "OUD: " + "id non assegnato con eccezione");
                        tryConnection(offset + 1);
                    }
                } else {
                    Log.d(TAG, "OUD: " + "id non assegnato senza eccezione");
                    tryConnection(offset + 1);
                }
            }
        }, HANDLER_PERIOD);
        writeDebug("Assegnazione id 5 secondi");
    }


    private void writeDebug(final String message) {
        InitActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                if (debugger.getLineCount() == debugger.getMaxLines())
                    debugger.setText(String.format("%s\n", message));
                else
                    debugger.setText(String.format("%s%s\n", String.valueOf(debugger.getText()), message));
            }
        });
        Log.d(TAG, "OUD:" + message);
    }


    private void askPermissions(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothAvailability(savedInstanceState);
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
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: OK");
                    checkBluetoothAvailability();
                } else {
                    Log.e(TAG, "onRequestPermissionsResult: Permission denied");
                }
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

}
