package it.drone.mesh.scanner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import it.drone.mesh.ConnectionActivity;
import it.drone.mesh.R;
import it.drone.mesh.models.User;
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

        getListView().setDivider(null);
        getListView().setDividerHeight(0);

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
        final BluetoothDevice device = usersFound.get(position).getBluetoothDevice();
        if (device == null) return;

        final Intent intent = new Intent(this.getContext(), ConnectionActivity.class);
        intent.putExtra(ConnectionActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(ConnectionActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        startActivity(intent);

    }

    /**
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(TAG, "Starting Scanning");

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
        Log.d(TAG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

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
            mAdapter.notifyDataSetChanged();

            // IF THE NEWLY DISCOVERED USER IS IN MY LIST OF USER, RETURNS
            for (User temp : usersFound) {
                if (temp.getBluetoothDevice().getName().equals(result.getDevice().getName()))
                    return;
            }
            // ADD THE USER
            final User newUser = new User(result.getDevice());
            newUser.setUserName(result.getDevice().getName());
            usersFound.add(newUser);
            mAdapter.add(result);

            // STARTS THE GATTSERVER
            AcceptBLETask acceptBLETask = new AcceptBLETask(newUser);
            acceptBLETask.startServer();
            // WAIT 600 MILLIS
            try {
                wait(600);
            } catch (Exception e) {
            }
            // STARTS THE GATT
            ConnectBLETask connectBLETask = new ConnectBLETask(newUser);
            connectBLETask.startClient();


//            CODE TO SET UP A TIMED THREAD
//
//            TimerTask timerTask = new TimerTask(){
//                @Override
//                public void run() {
//                    getActivity().runOnUiThread(new Runnable() {
//                        public void run() {
//                            Toast.makeText(getContext(), "SAY SOMETHING INSIDE HERE", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//            };
//          Timer timer = new Timer();
//          DELAY: the time to the first executioN
//          PERIODICAL_TIME: the time between each execution of your task.
//          timer.schedule(timerTask, 2000L, 4000L);


//          HERE IS THE BLUETOOTH CLASSIC PART:


            //AcceptBtTask acceptBtTask = new AcceptBtTask(newUser);
            //acceptBtTask.execute();
            //ConnectBtTask connectBtTask = new ConnectBtTask(newUser);
            //connectBtTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            Toast.makeText(getContext(), "It worked", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getActivity(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show();
        }

    }

    private class AcceptBLETask {
        private User mmUser;
        private BluetoothGattServer mGattServer;
        private BluetoothGattServerCallback mGattServerCallback;
        private BluetoothGattService mGattService;
        private BluetoothGattCharacteristic mGattCharacteristic;
        private BluetoothGattDescriptor mGattDescriptor;

        public AcceptBLETask(User user) {
            mmUser = user;
            mGattService = new BluetoothGattService(Constants.Service_UUID.getUuid(), 0);
            mGattCharacteristic = new BluetoothGattCharacteristic(Constants.Characteristic_UUID.getUuid(), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
            mGattDescriptor = new BluetoothGattDescriptor(Constants.Descriptor_UUID.getUuid(), BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            mGattServerCallback = new BluetoothGattServerCallback() {
                // DO SOMETHING WHEN THE CONNECTION UPDATES
                @Override
                public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                    Toast.makeText(getContext(), "I'm the server, I've connected to" + device.getName(), Toast.LENGTH_SHORT).show();
                    super.onConnectionStateChange(device, status, newState);
                }

                // DO SOMETHING WHEN A SERVICE IS ADDED
                @Override
                public void onServiceAdded(int status, BluetoothGattService service) {
                    Toast.makeText(getContext(), "I've added a service" + service.toString(), Toast.LENGTH_SHORT).show();
                    super.onServiceAdded(status, service);
                }

                // WHAT HAPPENS WHEN I GET A CHARACTERISTIC READ REQ
                @Override
                public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                    final BluetoothDevice tempdev = device;
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "I've been asked to read from " + tempdev.getName(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                }

                // WHAT HAPPENS WHEN I GET A CHARACTERISTIC WRITE REQ
                @Override
                public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                    final BluetoothDevice tempdev = device;
                    final String tempval = new String(value);
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "I've been asked to write from " + tempdev.getName() + " " + tempval, Toast.LENGTH_SHORT).show();
                            //if(tempGatt.getService(Constants.Service_UUID.getUuid())==null)
                            //return;
                        }
                    });
                    super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                }

                @Override
                public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                    Toast.makeText(getContext(), "I've been asked to read descriptor from " + device.getName(), Toast.LENGTH_SHORT).show();
                    super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                }

                @Override
                public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                    Toast.makeText(getContext(), "I've been asked to write descriptor from " + device.getName(), Toast.LENGTH_SHORT).show();
                    super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                }

                @Override
                public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                    final BluetoothDevice tempdev = device;
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "I'm writing from " + tempdev.getName(), Toast.LENGTH_SHORT).show();
                            //if(tempGatt.getService(Constants.Service_UUID.getUuid())==null)
                            //return;
                        }
                    });
                    super.onExecuteWrite(device, requestId, execute);
                }

                @Override
                public void onNotificationSent(BluetoothDevice device, int status) {
                    Toast.makeText(getContext(), "I've notified " + device.getName(), Toast.LENGTH_SHORT).show();
                    super.onNotificationSent(device, status);
                }

                @Override
                public void onMtuChanged(BluetoothDevice device, int mtu) {
                    super.onMtuChanged(device, mtu);
                }

                @Override
                public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                    super.onPhyUpdate(device, txPhy, rxPhy, status);
                }

                @Override
                public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                    super.onPhyRead(device, txPhy, rxPhy, status);
                }
            };

        }

        public void startServer() {
            // I CREATE A SERVICE WITH 1 CHARACTERISTIC AND 1 DESCRIPTOR
            this.mGattCharacteristic.addDescriptor(mGattDescriptor);
            this.mGattService.addCharacteristic(mGattCharacteristic);
            // I START OPEN THE GATT SERVER
            this.mGattServer = mBluetoothManager.openGattServer(getContext(), mGattServerCallback);
            this.mGattServer.addService(this.mGattService);
            try {
                wait(600);
            } catch (Exception e) {
            }
            mmUser.setBluetoothGattServer(this.mGattServer);
            return;
        }
    }

    private class ConnectBLETask {
        private User mmUser;
        private BluetoothGattCallback mGattCallback;
        private BluetoothGatt mGatt;

        public ConnectBLETask(User user) {
            // GATT OBJECT TO CONNECT TO A GATT SERVER
            mmUser = user;
            mGattCallback = new BluetoothGattCallback() {
                @Override
                public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                }

                @Override
                public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                    super.onPhyRead(gatt, txPhy, rxPhy, status);
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT client. Attempting to start service discovery");
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT client");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    final BluetoothGatt tempGatt = gatt;
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "I discovered a service" + tempGatt.getServices(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    for (BluetoothGattService service : tempGatt.getServices()) {
                        if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                            if (service.getCharacteristics() != null) {
                                for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                                    if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                                        chars.setValue("Test String");
                                        tempGatt.beginReliableWrite();
                                        tempGatt.writeCharacteristic(chars);
                                        tempGatt.executeReliableWrite();
                                    }
                                }
                            }
                        }
                    }

                    super.onServicesDiscovered(gatt, status);
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "I read a characteristic", Toast.LENGTH_SHORT).show();
                        }
                    });
                    super.onCharacteristicRead(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "I wrote a characteristic", Toast.LENGTH_SHORT).show();
                        }
                    });
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Characteristic changed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    super.onCharacteristicChanged(gatt, characteristic);
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    Toast.makeText(getContext(), "I read a descriptor", Toast.LENGTH_SHORT).show();
                    super.onDescriptorRead(gatt, descriptor, status);
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    Toast.makeText(getContext(), "I wrote a descriptor", Toast.LENGTH_SHORT).show();
                    super.onDescriptorWrite(gatt, descriptor, status);
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    Toast.makeText(getContext(), "I reliably wrote ", Toast.LENGTH_SHORT).show();
                    super.onReliableWriteCompleted(gatt, status);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    Toast.makeText(getContext(), "I read the remote rssi", Toast.LENGTH_SHORT).show();
                    super.onReadRemoteRssi(gatt, rssi, status);
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    super.onMtuChanged(gatt, mtu, status);
                }
            };
        }

        public void startClient() {
            this.mGatt = mmUser.getBluetoothDevice().connectGatt(getContext(), false, mGattCallback);
            try {
                wait(600);
            } catch (Exception e) {
            }
            mmUser.setBluetoothGatt(this.mGatt);
            //mmUser.getBluetoothGatt().requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            //mmUser.getBluetoothGatt().connect();
            try {
                wait(600);
            } catch (Exception e) {
            }
            //this.mGatt.discoverServices();
            return;
        }
    }


    private class AcceptBtTask extends AsyncTask<Void, Void, BluetoothSocket> {
        private User mmUser;

        public AcceptBtTask(User user) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            mmUser = user;
        }

        @Override
        protected BluetoothSocket doInBackground(Void... s) {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            //mBluetoothAdapter.cancelDiscovery();
            // Creates the Server Socket
            try {
                mmUser.setBluetoothServerSocket(mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("HIRO-NET", Constants.Service_UUID.getUuid()));
            } catch (IOException e) {
                Toast.makeText(getContext(), "Couldn't create a Socket", Toast.LENGTH_SHORT).show();
            }

            while (true) {
                try {
                    socket = mmUser.getBluetoothServerSocket().accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.e(TAG, "Socket's accept()");
                    // manageMyConnectedSocket(socket);
                    try {
                        mmUser.getBluetoothServerSocket().close();
                    } catch (IOException e) {
                        Log.e(TAG, "Socket's accept() method failed", e);
                    }
                    break;
                }
            }
            //processBtAccept(socket);
            return socket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket result) {
            if (result == null)
                return;
            mmUser.setBluetoothSocket(result);
            processBtAccept(result);
        }

        private void processBtAccept(final BluetoothSocket socket) {
            TimerTask timerTask = new TimerTask() {
                InputStream tmpIn = null;
                byte[] buffer = new byte[20];
                int bytes;

                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                tmpIn = mmUser.getBluetoothSocket().getInputStream();
                            } catch (IOException closeException) {
                                Log.e(TAG, "Couldn't get an Inputstream", closeException);
                            }
                            try {
                                bytes = tmpIn.available();
                                bytes = tmpIn.read(buffer);
                            } catch (IOException closeException) {
                                Log.e(TAG, "Couldn't print anything", closeException);
                            }
                            Toast.makeText(getContext(), new String(buffer) + "from" + socket.getRemoteDevice().toString(), Toast.LENGTH_SHORT).show();
                        }
                        //processConnect();
                    });
                }
            };

            Timer timer = new Timer();

            //DELAY: the time to the first execution
            //PERIODICAL_TIME: the time between each execution of your task.
            timer.schedule(timerTask, 2000L, 4000L);

        }
    }

    private class ConnectBtTask extends AsyncTask<Void, Void, BluetoothSocket> {
        private User mmUser;

        public ConnectBtTask(User user) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            mmUser = user;
        }

        @Override
        protected BluetoothSocket doInBackground(Void... s) {
            // Cancel discovery because it otherwise slows down the connection.
            //mBluetoothAdapter.cancelDiscovery();
            BluetoothSocket tmp = null;
            try {
                tmp = mmUser.getBluetoothDevice().createInsecureRfcommSocketToServiceRecord(Constants.Service_UUID.getUuid());
            } catch (IOException e) {
                Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
            }
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                tmp.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    tmp.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return null;
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            mmUser.setBluetoothSocket(tmp);
            return mmUser.getBluetoothSocket();
        }

        @Override
        protected void onPostExecute(BluetoothSocket result) {
            final BluetoothSocket mBluetoothSocket = result;
            if (result == null)
                return;
            try {
                mmUser.getBluetoothServerSocket().close();
            } catch (IOException e) {
                Log.e(TAG, "couldn't close socket", e);
            }
            processBtConnect(result);
        }

        public void processBtConnect(final BluetoothSocket socket) {
            TimerTask timerTask = new TimerTask() {
                OutputStream tmpOut = null;
                byte[] buffer = new byte[20];
                int bytes;

                @Override
                public void run() {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                tmpOut = mmUser.getBluetoothSocket().getOutputStream();
                            } catch (IOException closeException) {
                                Log.e(TAG, "Couldn't get an Inputstream", closeException);
                            }
                            try {
                                new Random().nextBytes(buffer);
                                tmpOut.write(buffer);
                            } catch (IOException closeException) {
                                Log.e(TAG, "Couldn't print anything", closeException);
                            }
                            Toast.makeText(getContext(), new String(buffer) + "to" + socket.getRemoteDevice().toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };

            Timer timer = new Timer();

            //DELAY: the time to the first execution
            //PERIODICAL_TIME: the time between each execution of your task.
            timer.schedule(timerTask, 2000L, 4000L);

        }
    }
}