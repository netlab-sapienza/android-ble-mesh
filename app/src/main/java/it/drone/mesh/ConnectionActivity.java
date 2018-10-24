package it.drone.mesh;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import it.drone.mesh.models.User;
import it.drone.mesh.utility.Constants;

import static it.drone.mesh.utility.Constants.EXTRAS_DEVICE_ADDRESS;
import static it.drone.mesh.utility.Constants.EXTRAS_DEVICE_NAME;

public class ConnectionActivity extends Activity {

    private final static String TAG = ConnectionActivity.class.getSimpleName();
    private final static int DO_UPDATE_TEXT = 0;
    private final static int DO_THAT = 1;
    // Create a BroadcastReceiver for ACTION_FOUND.
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
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private Handler mHandler;
    private User user;

    private TextView outputText;
    private EditText inputText;
    private Button sendButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        outputText = findViewById(R.id.outputText);
        inputText = findViewById(R.id.inputText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(inputText.getText().toString());
                inputText.setText("");
            }
        });

        mDeviceName = getIntent().getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final int what = msg.what;
                Log.d(TAG, "OUD: " + "handleMessage: SONO ENTRATO NELL'HANDLER");
                switch (what) {
                    case DO_UPDATE_TEXT:
                        doUpdate();
                        break;
                    case DO_THAT:
                        break;
                }
            }
        };

        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter);

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.d(TAG, "OUD: " + "Unable to initialize BluetoothManager.");
                return;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "OUD: " + "Unable to obtain a BluetoothAdapter.");
            return;
        }

        user = UserList.getUser(mDeviceName);

        //ConnectBLETask connectBLETask = new ConnectBLETask(UserList.getUser(mDeviceName), this);
        //connectBLETask.startClient();

        //outputText.setText(user.getBluetoothDevice().getName());


        //ConnectTask connectTask = new ConnectTask(device);
        //connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void sendMessage(String message) {
        // Toast.makeText(this,message,Toast.LENGTH_SHORT).show();

        Log.d(TAG, "OUD: " + "sendMessage: Inizio invio messaggio");
        //final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        final BluetoothGatt gatt = /*UserList.getUser(mDeviceName).getBluetoothGatt();*/ user.getBluetoothGatt();
        boolean res1 = gatt.connect();
        Log.d(TAG, "OUD: " + "StateServer connesso ? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)));
        Log.d(TAG, "OUD: " + "StateGatt connesso? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT)));

        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo" + res1);
            if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                            chars.setValue(message);
                            gatt.beginReliableWrite();
                            boolean res = gatt.writeCharacteristic(chars);
                            gatt.executeReliableWrite();
                            Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");

    }

    private void doUpdate() {
        outputText.setText(String.valueOf(System.currentTimeMillis()));
    }

}
