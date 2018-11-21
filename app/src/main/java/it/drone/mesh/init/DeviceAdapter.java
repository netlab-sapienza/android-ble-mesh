package it.drone.mesh.init;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import it.drone.mesh.R;
import it.drone.mesh.models.Device;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private ArrayList<Device> devices;

    public DeviceAdapter(ArrayList<Device> devices) {
        this.devices = devices;
    }


    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_scan_result, parent, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder deviceViewHolder, int i) {
        final Device device = devices.get(i);

        deviceViewHolder.id.setText(device.getId());
        deviceViewHolder.power.setText(device.getSignalPower());
        deviceViewHolder.lastTime.setText(device.getTimeSinceString());
        deviceViewHolder.input.setText(device.getInput());
        deviceViewHolder.output.setText(device.getOutput());
        deviceViewHolder.testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: 21/11/18 sendTestmessage
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView id, lastTime, power, input, output;
        Button testButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.device_id);
            lastTime = itemView.findViewById(R.id.last_time_device);
            power = itemView.findViewById(R.id.power_device);
            input = itemView.findViewById(R.id.inputText);
            output = itemView.findViewById(R.id.outputText);
            testButton = itemView.findViewById(R.id.button_test_message);
        }
    }

}
