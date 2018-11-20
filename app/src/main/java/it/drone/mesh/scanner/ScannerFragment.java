package it.drone.mesh.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.tasks.ConnectBLETask;
import it.drone.mesh.utility.Constants;


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
    private ConnectBLETask connectBLETask;
    private String id;
    private LinkedList<ScanResult> tempResult = new LinkedList<>();


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
        startScanning();

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
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

            String toastText = getString(R.string.scan_start_toast) + " "
                    + TimeUnit.SECONDS.convert(SCAN_PERIOD, TimeUnit.MILLISECONDS) + " "
                    + getString(R.string.seconds);
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), R.string.already_scanning, Toast.LENGTH_SHORT);
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

        tryConnection(0);
        // Even if no new results, update 'last seen' times.
        mAdapter.notifyDataSetChanged();
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

    public void tryConnection(final int offset) {
        final int size = UserList.getUserList().size();
        if (offset >= size) return; //TODO diventa server
        final User newUser = UserList.getUser(offset);
        Log.d(TAG, "OUD: " + "tryConnection with: " + newUser.getUserName());
        final ConnectBLETask connectBLETask = new ConnectBLETask(newUser, getContext());
        connectBLETask.startClient();
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            Log.d(TAG, "OUD: " + "Andata male la wait");
        }
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String tempId = connectBLETask.getId();
                Log.d(TAG, "OUD: " + "id trovato dopo 2 secondi di attesa : " + tempId);
                if (tempId != null) {
                    Log.d(TAG, "OUD: " + "id assegnato correttamente");
                    id = tempId;
                    mAdapter.add(tempResult.get(offset));
                    mAdapter.notifyDataSetChanged();
                } else {
                    tryConnection(offset + 1);
                }
            }
        }, 2000);
        Log.d(TAG, "OUD: " + "It worked");
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