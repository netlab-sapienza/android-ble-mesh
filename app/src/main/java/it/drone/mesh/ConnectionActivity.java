package it.drone.mesh;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.NoSuchElementException;

import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;
import it.drone.mesh.roles.common.Utility;

import static it.drone.mesh.roles.common.Constants.EXTRAS_DEVICE_ADDRESS;
import static it.drone.mesh.roles.common.Constants.EXTRAS_DEVICE_ID;
import static it.drone.mesh.roles.common.Constants.EXTRAS_DEVICE_NAME;

public class ConnectionActivity extends Activity {

    private final static String TAG = ConnectionActivity.class.getSimpleName();

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
    private String clientId;
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
        clientId = getIntent().getStringExtra(EXTRAS_DEVICE_ID);


        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //registerReceiver(mReceiver, filter);

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "OUD: " + "Unable to initialize BluetoothManager.");
                return;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "OUD: " + "Unable to obtain a BluetoothAdapter.");
            return;
        }

        try {
            user = UserList.getUser(mDeviceName);
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            Log.e(TAG, "Lista :" + UserList.printList());
        }


        //ConnectBLETask connectBLETask = new ConnectBLETask(UserList.getUser(mDeviceName), this);
        //connectBLETask.startClient();

        //outputText.setText(user.getBluetoothDevice().getName());


        //ConnectTask connectTask = new ConnectTask(device);
        //connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Invia il messaggio messagge al device selezionato nella schermata precedente
     *
     * @param message messaggio da inviare
     */
    private void sendMessage(String message) {
        Log.d(TAG, "OUD: " + "sendMessage: Inizio invio messaggio");
        //final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        final BluetoothGatt gatt = /*UserList.getUser(mDeviceName).getBluetoothGatt();*/ user.getBluetoothGatt();
        int[] infoSorg = new int[2];
        infoSorg[0] = Integer.parseInt("" + clientId.charAt(0));
        infoSorg[1] = Integer.parseInt("" + clientId.charAt(1));

        int[] infoDest = new int[2];
        infoDest[0] = Integer.parseInt(message.substring(0, 1));    //id del destinatario Server
        infoDest[1] = Integer.parseInt(message.substring(1, 2));    //id del destinatario Client

        message = message.substring(2, message.length());
        boolean res = Utility.sendMessage(message, gatt, infoSorg, infoDest, null);
        Log.d(TAG, "OUD: " + "sendMessage: Inviato ? " + res);
        /*
        ConnectBLETask connectBLETask = null;
        while (!(BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)) || !(BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT))) {
            connectBLETask = new ConnectBLETask(user, this);
            connectBLETask.startClient();
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                Log.d(TAG, "OUD: " + "Andata male la wait");
            }
            Log.d(TAG, "OUD: " + "Restauro connessione");
            Log.d(TAG, "OUD: " + "StateServer connesso ? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT_SERVER)));
            Log.d(TAG, "OUD: " + "StateGatt connesso? -> " + (BluetoothProfile.STATE_CONNECTED == mBluetoothManager.getConnectionState(user.getBluetoothDevice(), BluetoothProfile.GATT)));
        }

        if (connectBLETask != null) {
            while (!connectBLETask.getServiceDiscovered()) {
                Log.d(TAG, "OUD: " + "Wait for services");
            }
        }
        */
        /*
        byte[][] finalMessage = Utility.messageBuilder(Utility.byteMessageBuilder(4, 5), message);

        for (BluetoothGattService service : gatt.getServices()) {
            Log.d(TAG, "OUD: " + "sendMessage: inizio ciclo");
            if (service.getUuid().toString().equals(Constants.Service_UUID.toString())) {
                Log.d(TAG, "OUD: " + "sendMessage: service.equals");
                if (service.getCharacteristics() != null) {
                    for (BluetoothGattCharacteristic chars : service.getCharacteristics()) {
                        Log.d(TAG, "OUD:" + "Char: " + chars.toString());
                        if (chars.getUuid().toString().equals(Constants.Characteristic_UUID.toString())) {
                            for (int i = 0; i < finalMessage.length; i++) {
                                chars.setValue(finalMessage[i]);
                                gatt.beginReliableWrite();
                                boolean res = gatt.writeCharacteristic(chars);
                                gatt.executeReliableWrite();
                                Log.d(TAG, "OUD: " + new String(finalMessage[i]));
                                Log.d(TAG, "OUD: " + "Inviato? -> " + res);
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                    Log.d(TAG, "OUD: " + "Andata male la wait");
                                }
                            }
                        }
                    }
                }
            }

        }
        Log.d(TAG, "OUD: " + "sendMessage: end ");*/
    }

    /**
     * Aggiorna l'output con il messaggio nuovo
     * <p>
     * NB questo metodo viene invocato se e solo se c'è stata un'effettiva scrittura, quindi potrebbe non comparire subito
     *
     * @param message messaggio da aggiungere
     */
    private void addOutputMessage(String message) {
        outputText.setText(outputText.getText().toString().concat("\n").concat(message));
    }

    private void doUpdate() {
        outputText.setText(outputText.getText().toString().concat("\n").concat(String.valueOf(System.currentTimeMillis())));
    }

}
