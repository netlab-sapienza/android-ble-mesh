package it.drone.mesh.scanner;

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
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import it.drone.mesh.ConnectionActivity;
import it.drone.mesh.R;
import it.drone.mesh.advertiser.AdvertiserService;
import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.roles.common.Constants;
import it.drone.mesh.roles.common.Utility;
import it.drone.mesh.tasks.AcceptBLETask;
import it.drone.mesh.tasks.ConnectBLETask;


/**
 * Scans for Bluetooth Low Energy Advertisements matching a filter and displays them to the user.
 */
public class ScannerFragment extends ListFragment {


    private static final String TAG = ScannerFragment.class.getSimpleName();

    /**
     * Stops scanning after 5 seconds.
     */
    private static final long SCAN_PERIOD = 5000;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }

        }
    };
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private ScanResultAdapter mAdapter;
    private Handler mHandler;
    private ArrayList<User> usersFound = new ArrayList<>();
    private ConnectBLETask connectBLE;
    private String clientId;
    private LinkedList<ScanResult> tempResult = new LinkedList<>();
    private LinkedList<String> idList = new LinkedList<>();


    /**
     * Must be called after object creation by MainActivity.
     *
     * @param btAdapter the local BluetoothAdapter
     */
    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.mBluetoothAdapter = btAdapter;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public void setBluetoothManager(BluetoothManager btManager) {
        this.mBluetoothManager = btManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        // Use getActivity().getApplicationContext() instead of just getActivity() because this
        // object lives in a fragment and needs to be kept separate from the Activity lifecycle.
        //
        // We could get a LayoutInflater from the ApplicationContext but it messes with the
        // default theme, so generate it from getActivity() and pass it in separately.
        mAdapter = new ScanResultAdapter(getActivity().getApplicationContext(),
                LayoutInflater.from(getActivity()));
        mHandler = new Handler();
        connectBLE = null;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().getApplicationContext().registerReceiver(mReceiver, filter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = super.onCreateView(inflater, container, savedInstanceState);

        setListAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getString(R.string.empty_list));

        // Trigger refresh on app's 1st load
        //startScanning();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.scanner_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.refresh:
                startScanning();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // create a new activity to open a connection with the clicked item
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final User user = usersFound.get(position);
        final BluetoothDevice device = user.getBluetoothDevice();
        if (device == null) {
            Log.wtf(TAG, "The device is null");
            return;
        }

        final Intent intent = new Intent(this.getContext(), ConnectionActivity.class);
        intent.putExtra(Constants.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(Constants.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(Constants.EXTRAS_DEVICE_ID, clientId);

        startActivity(intent);

    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "OUD: " + "Starting Scanning");
            UserList.cleanUserList();
            tempResult.clear();
            // Will stop the scanning after a set time.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), mScanCallback);

            String toastText = getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), R.string.already_scanning, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(TAG, "OUD: " + "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        askIdNearServer(0);
        // Even if no new results, update 'last seen' times.
        mAdapter.notifyDataSetChanged();
    }

    private void askIdNearServer(final int offset) {
        final int size = UserList.getUserList().size();

        if (offset >= size) {
            tryConnection(0); //finito di leggere gli id passa a connettersi
            return;
        }

        final User newUser = UserList.getUser(offset);
        Log.d(TAG, "OUD: " + "askNearServer with: " + newUser.getUserName());
        final ConnectBLETask connectBLETask = new ConnectBLETask(newUser, getContext(), new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "OUD: Connected to GATT client. Attempting to start service discovery from " + gatt.getDevice().getName());
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "OUD: Disconnected from GATT client " + gatt.getDevice().getName());
                }
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
                        idList.add(val);
                        Log.d(TAG, "OUD: " + "id aggiunto :" + val);
                    }
                }
                super.onDescriptorRead(gatt, descriptor, status);
                askIdNearServer(offset + 1);
            }
        });
        connectBLETask.startClient();
    }

    public void tryConnection(final int offset) {
        if (connectBLE != null) {
            Log.d(TAG, "OUD: " + "Sei già un client con id " + connectBLE.getId());
            return;
        }
        final int size = UserList.getUserList().size();
        if (offset >= size) {
            Context c = getActivity();
            if (c != null) {
                c.startService(new Intent(c, AdvertiserService.class));
                Log.d(TAG, "OUD: " + "startAdvertising: StART Server");
                AcceptBLETask acceptBLETask = new AcceptBLETask(mBluetoothAdapter, mBluetoothManager, getContext());
                acceptBLETask.setStartServerList(tempResult);
                acceptBLETask.insertIdInMap(idList);
                acceptBLETask.startServer();
            }
            return;
        }
        final User newUser = UserList.getUser(offset);
        Log.d(TAG, "OUD: " + "tryConnection with: " + newUser.getUserName());
        final ConnectBLETask connectBLETask = new ConnectBLETask(newUser, getContext(), new Utility.OnMessageReceivedListener() {
            @Override
            public void OnMessageReceived(final String message) {
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Messaggio ricevuto dall'utente " + message, Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });
        connectBLETask.startClient();
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "OUD: Run ");
                if (connectBLETask.hasCorrectId()) {
                    String tempId = connectBLETask.getId();
                    Log.d(TAG, "OUD: " + "id trovato dopo 5 secondi di attesa : " + tempId);
                    int parsed;
                    try {
                        parsed = Integer.parseInt(tempId);
                        Log.d(TAG, "OUD: " + "id assegnato correttamente");
                        clientId = parsed + "";
                        connectBLE = connectBLETask;
                        mAdapter.add(tempResult.get(offset));
                        mAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.d(TAG, "OUD: " + "id non assegnato con eccezione");
                        tryConnection(offset + 1);
                    }
                } else {
                    Log.d(TAG, "OUD: " + "id non assegnato senza eccezione");
                    tryConnection(offset + 1);
                }
            }
        }, 5000);
        Log.d(TAG, "OUD: tra 5 secondo parte handler");
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            // TO DO:
            // SETUP AS IN onScanResult BUT FOR A BATCH OF results
            // NEVER REALLY RAN EVER
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //mAdapter.notifyDataSetChanged();
            Log.d(TAG, "OUD: " + result.toString());

            for (User temp : UserList.getUserList()) {
                if (temp.getBluetoothDevice().getName().equals(result.getDevice().getName()))
                    return;
            }
            tempResult.add(result);
            final User newUser = new User(result.getDevice(), result.getDevice().getName());
            usersFound.add(newUser);
            UserList.addUser(newUser);
            //mAdapter.add(result);
            Log.d(TAG, "onScanResult: Nuovo SERVER");

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "OUD: " + "Scan failed with error: " + errorCode);
        }

    }

}