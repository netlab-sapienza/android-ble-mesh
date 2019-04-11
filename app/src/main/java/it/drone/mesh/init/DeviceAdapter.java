package it.drone.mesh.init;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Selection;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import it.drone.mesh.R;
import it.drone.mesh.client.BLEClient;
import it.drone.mesh.common.Constants;
import it.drone.mesh.common.RoutingTable;
import it.drone.mesh.common.Utility;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.models.Device;
import it.drone.mesh.server.BLEServer;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private static final String TEST_MESSAGE = "I AM A TEST MESSAGE";
    private ArrayList<Device> devices;
    private final static String TAG = DeviceAdapter.class.getSimpleName();

    private BLEClient client;
    private BLEServer server;
    private long offset;

    DeviceAdapter() {
        offset = Constants.NO_OFFSET;
        RoutingTable routingTable = RoutingTable.getInstance();
        this.devices = routingTable.getDeviceList();
        routingTable.subscribeToUpdates(new RoutingTable.OnRoutingTableUpdateListener() {
            @Override
            public void OnDeviceAdded(Device device) {
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }

            @Override
            public void OnDeviceRemoved(Device device) {
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        });

    }


    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        return new DeviceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final DeviceViewHolder deviceViewHolder, int i) {
        final Device device = devices.get(i);
        deviceViewHolder.id.setText(device.getId());

        SpannableString spannableInput = new SpannableString(device.getInput());
        Selection.setSelection(spannableInput, spannableInput.length());
        deviceViewHolder.input.setText(spannableInput, TextView.BufferType.SPANNABLE);

        SpannableString spannableOutput = new SpannableString(device.getOutput());
        Selection.setSelection(spannableOutput, spannableOutput.length());
        deviceViewHolder.output.setText(spannableOutput, TextView.BufferType.SPANNABLE);
   
        deviceViewHolder.testButton.setOnClickListener(view -> {
            if (deviceViewHolder.messageToSend.getText().length() > 0) {
                String messageToSend = deviceViewHolder.messageToSend.getText().toString();
                deviceViewHolder.messageToSend.setText("");
                deviceViewHolder.messageToSend.clearFocus();
                DeviceAdapter.this.sendMessage(device.getId(), (System.currentTimeMillis() + (offset == Constants.NO_OFFSET ? 0 : offset)) + ";;0;;" + messageToSend, false, new Listeners.OnMessageSentListener() {
                    @Override
                    public void OnMessageSent(final String message) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            device.writeInput(message);
                            deviceViewHolder.input.setText(device.getInput());
                            notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void OnCommunicationError(final String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            deviceViewHolder.input.setText(String.format("%s%s", deviceViewHolder.input.getText(), error));
                            notifyDataSetChanged();
                        });
                    }
                });
            }
        });
    }


    @Override
    public int getItemCount() {
        return devices.size();
    }

    /**
     * Invia il messaggio messagge al device identificato come destinationID
     *
     * @param destinationId Id della device da raggiungere
     * @param message       messaggio da inviare
     * @param listener      Listener di risposta
     */
    private void sendMessage(String destinationId, String message, boolean internet, Listeners.OnMessageSentListener listener) {
        String myId;
        if (client != null && client.getId() != null)
            myId = client.getId();
        else if (server != null && server.getId() != null)
            myId = server.getId();
        else
            myId = "MY_ID_UNAVAILABLE";

        String[] info = message.split(";;");
        try {
            Utility.saveData(Arrays.asList("MY_ID", "DESTINATION_ID", "START_TIME", "HOP"), Utility.BETA_FILENAME_SENT, Arrays.asList(myId, destinationId, info[0], info[1]));
        } catch (IOException e) {
            Log.e(TAG, "sendMessage: OUD: Levate sto OUD e controllate la stacktrace");
            e.printStackTrace();
        }

        if (client != null) {
            client.sendMessage(message, destinationId, internet, listener);
            //Log.d(TAG, "OUD: " + "Messaggio inviato: " + res);
        } else if (server != null) {
            server.sendMessage(message, destinationId, internet, listener);
        } else {
            Log.e(TAG, "sendMessage: client e server tutti e due null");
        }

    }

    /*public ConnectBLETask getConnectBLETask() {
        return connectBLETask;
    }*/

    void setClient(final Context context) {
        client = BLEClient.getInstance(context);
        client.addReceivedListener((idMitt, message, numHop, sentTimeStamp) -> {
            for (Device device : devices) {
                if (device.getId().equals(idMitt)) {
                    device.writeOutput("Time: " + ((System.currentTimeMillis() + (offset == Constants.NO_OFFSET ? 0 : offset)) - sentTimeStamp) + ", Message: " + message + ", Hop: " + numHop);
                }
            }

            new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);

            String myId = client.getId();
            try {
                Utility.saveData(Arrays.asList("MY_ID", "SENDER_ID", "DELIVERY_TIME", "HOP"), Utility.BETA_FILENAME_RECEIVED, Arrays.asList(myId, idMitt, System.currentTimeMillis() - sentTimeStamp, numHop));
            } catch (IOException e) {
                Log.e(TAG, "sendMessage: OUD : Levate sto OUD e controllate la stacktrace");
                e.printStackTrace();
            }
        });
    }

    void setServer(final Context context) {
        server = BLEServer.getInstance(context);
        server.addOnMessageReceivedListener((idMitt, message, hop, sendTimeStamp) -> {
            for (Device device : devices) {
                if (device.getId().equals(idMitt)) {
                    device.writeOutput("Time: " + ((System.currentTimeMillis() + (offset == Constants.NO_OFFSET ? 0 : offset)) - sendTimeStamp) + ", Message: " + message + ", NumHop: " + hop);
                    new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
                }
            }
        });
    }

    public void cleanView() {
        RoutingTable.getInstance().cleanRoutingTable();
        devices.clear();
        notifyDataSetChanged();
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }


    class DeviceViewHolder extends RecyclerView.ViewHolder {
        EditText messageToSend;
        TextView id, input, output;
        Button testButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.device_id);
            input = itemView.findViewById(R.id.inputText);
            output = itemView.findViewById(R.id.outputText);
            testButton = itemView.findViewById(R.id.button_test_message);
            messageToSend = itemView.findViewById(R.id.message_to_send);
        }
    }

}
