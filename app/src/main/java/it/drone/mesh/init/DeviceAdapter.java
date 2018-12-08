package it.drone.mesh.init;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import it.drone.mesh.R;
import it.drone.mesh.models.Device;
import it.drone.mesh.roles.common.Utility;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private static final String TEST_MESSAGE = "I AM A TEST MESSAGE";
    private ArrayList<Device> devices;
    private Context _applicationContext;
    private final static String TAG = DeviceAdapter.class.getSimpleName();

    public DeviceAdapter(ArrayList<Device> devices, Context _applicationContext) {
        this.devices = devices;
        this._applicationContext = _applicationContext;
    }


    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_result, parent, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final DeviceViewHolder deviceViewHolder, int i) {
        final Device device = devices.get(i);

        deviceViewHolder.id.setText(device.getId());
        deviceViewHolder.power.setText(device.getSignalPower());
        deviceViewHolder.lastTime.setText(device.getTimeSinceString());
        deviceViewHolder.input.setText(device.getInput());
        deviceViewHolder.output.setText(device.getOutput());
        deviceViewHolder.testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(device, deviceViewHolder.destinationId.getText().toString(), TEST_MESSAGE, new Utility.OnMessageSentListener() {
                    @Override
                    public void OnMessageSent(String message) {
                        device.writeInput(message);
                        deviceViewHolder.input.setText(device.getInput());
                    }

                    @Override
                    public void OnCommunicationError(String error) {
                        deviceViewHolder.input.setText(String.format("%s%s", deviceViewHolder.input.getText(), error));
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
     * @param sourceDevice  device
     * @param destinationId Id della device da raggiungere
     * @param message       messaggio da inviare
     * @param listener      Listener di risposta
     */
    private void sendMessage(Device sourceDevice, String destinationId, String message, Utility.OnMessageSentListener listener) {
        final BluetoothGatt gatt = null; // TODO: 08/12/2018 serve un metodo che dato destinationId torni gatt, routingTable?
        int[] infoSorg = new int[2];
        infoSorg[0] = Integer.parseInt("" + sourceDevice.getId().charAt(0));
        infoSorg[1] = Integer.parseInt("" + sourceDevice.getId().charAt(1));

        int[] infoDest = new int[2];
        infoDest[0] = Integer.parseInt(destinationId.substring(0, 1));    //id del destinatario Server
        infoDest[1] = Integer.parseInt(destinationId.substring(1, 2));    //id del destinatario Client

        message = message.substring(1, message.length());
        Utility.sendMessage(message, gatt, infoSorg, infoDest, listener);
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView id, lastTime, power, input, output;
        EditText destinationId;
        Button testButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.device_id);
            lastTime = itemView.findViewById(R.id.last_time_device);
            power = itemView.findViewById(R.id.power_device);
            input = itemView.findViewById(R.id.inputText);
            output = itemView.findViewById(R.id.outputText);
            testButton = itemView.findViewById(R.id.button_test_message);
            destinationId = itemView.findViewById(R.id.destination_id);
        }
    }

}
