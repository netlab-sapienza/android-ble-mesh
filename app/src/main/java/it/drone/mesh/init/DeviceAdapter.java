package it.drone.mesh.init;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import it.drone.mesh.R;
import it.drone.mesh.listeners.Listeners;
import it.drone.mesh.models.Device;
import it.drone.mesh.roles.common.RoutingTable;
import it.drone.mesh.roles.common.Utility;
import it.drone.mesh.tasks.AcceptBLETask;
import it.drone.mesh.tasks.ConnectBLETask;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private static final String TEST_MESSAGE = "I AM A TEST MESSAGE";
    private ArrayList<Device> devices;
    private final static String TAG = DeviceAdapter.class.getSimpleName();

    private ConnectBLETask connectBLETask;
    private AcceptBLETask acceptBLETask;

    DeviceAdapter() {
        RoutingTable routingTable = RoutingTable.getInstance();
        this.devices = routingTable.getDeviceList();
        routingTable.subscribeToUpdates(new RoutingTable.OnRoutingTableUpdateListener() {
            @Override
            public void OnDeviceAdded(Device device) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void OnDeviceRemoved(Device device) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
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
        deviceViewHolder.input.setText(device.getInput());
        deviceViewHolder.output.setText(device.getOutput());
        deviceViewHolder.testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Il formato messaggi per la beta è startTime,num_hop al posto di TEST_MESSAGE
                // all'inizio num_hop = 0
                sendMessage(device.getId(), System.currentTimeMillis() + ";;0;;" + TEST_MESSAGE, new Listeners.OnMessageSentListener() {
                    @Override
                    public void OnMessageSent(final String message) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                device.writeInput(message);
                                deviceViewHolder.input.setText(device.getInput());
                                notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void OnCommunicationError(final String error) {
                        new Handler(Looper.myLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                deviceViewHolder.input.setText(String.format("%s%s", deviceViewHolder.input.getText(), error));
                                notifyDataSetChanged();
                            }
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
    private void sendMessage(String destinationId, String message, Listeners.OnMessageSentListener listener) {
        // INIZIO LOGICA BETA
        String myId;
        if (connectBLETask != null)
            myId = connectBLETask.getId();
        else if (acceptBLETask != null)
            myId = acceptBLETask.getId();
        else
            myId = "MY_ID_UNAVAILABLE";

        String[] info = message.split(";;");
        try {
            // HOP dovrebbe venire sempre 0 ma sulla specifica ci sta scritto che ci deve essere, quindi ¯\_(ツ)_/¯
            ArrayList<String> list = new ArrayList<>();
            ArrayList<String> list2 = new ArrayList<>();
            for (String s: Arrays.asList("MY_ID", "DESTINATION_ID", "START_TIME", "HOP")) {
                list.add(s);
            }
            for (String s : Arrays.asList(myId, destinationId, info[0], info[1])) {
                list2.add(s);
            }
            Utility.saveData(list, Utility.BETA_FILENAME_SENT, list2);
        } catch (IOException e) {
            Log.e(TAG, "sendMessage: OUD: Levate sto OUD e controllate la stacktrace");
            e.printStackTrace();
        }
        // FINE LOGICA BETA

        if (connectBLETask != null) {
            boolean res = connectBLETask.sendMessage(message, destinationId, listener);
            Log.d(TAG, "OUD: " + "Messaggio inviato: " + res);
        } else if (acceptBLETask != null) {
            // TODO: 14/12/18 logica sendMessageAcceptBLETask
            //acceptBLETask.sendMessage();
            Log.e(TAG, "sendMessage: missing logic sendMessageAcceptBLETask");
        } else {
            Log.e(TAG, "sendMessage: connect accept tasks tutti e due null");
        }

    }

    public ConnectBLETask getConnectBLETask() {
        return connectBLETask;
    }

    void setConnectBLETask(final ConnectBLETask connectBLETask) {
        this.connectBLETask = connectBLETask;
        this.connectBLETask.addReceivedListener(new Listeners.OnMessageReceivedListener() {
            @Override
            public void OnMessageReceived(String idMitt, String message) {
                for (Device device : devices)
                    if (device.getId().equals(idMitt))
                        device.writeOutput(message);
                notifyDataSetChanged();

                // INIZIO LOGICA BETA
                String myId = connectBLETask.getId();
                String[] info = message.split(",");
                try {
                    Utility.saveData((ArrayList<String>) Arrays.asList("MY_ID", "SENDER_ID", "DELIVERY_TIME", "HOP"), Utility.BETA_FILENAME_RECEIVED, (ArrayList<String>) Arrays.asList(myId, idMitt, info[0], info[1]));
                } catch (IOException e) {
                    Log.e(TAG, "sendMessage: OUD : Levate sto OUD e controllate la stacktrace");
                    e.printStackTrace();
                }
                // FINE LOGICA BETA
            }
        });
    }

    public AcceptBLETask getAcceptBLETask() {
        return acceptBLETask;
    }

    void setAcceptBLETask(AcceptBLETask acceptBLETask) {
        this.acceptBLETask = acceptBLETask;
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView id, input, output;
        Button testButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.device_id);
            input = itemView.findViewById(R.id.inputText);
            output = itemView.findViewById(R.id.outputText);
            testButton = itemView.findViewById(R.id.button_test_message);
        }
    }

}
