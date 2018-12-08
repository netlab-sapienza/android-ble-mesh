package it.drone.mesh.init;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import it.drone.mesh.R;
import it.drone.mesh.models.Device;
import it.drone.mesh.roles.common.exceptions.NotEnabledException;
import it.drone.mesh.roles.common.exceptions.NotSupportedException;
import it.drone.mesh.roles.server.BLEServer;

public class InitActivity extends Activity {


    Button startServices;
    BLEServer server;
    TextView debugger;

    RecyclerView recyclerDeviceList;
    DeviceAdapter deviceAdapter;
    ArrayList<Device> devices = new ArrayList<>();

    private final static String TAG = InitActivity.class.getSimpleName();
    private boolean isServiceStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        startServices = findViewById(R.id.startServices);
        debugger = findViewById(R.id.debugger);

        startServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isServiceStarted) {
                    // TODO: 07/12/2018 stop service
                    startServices.setText(R.string.start_service);
                    isServiceStarted = false;
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
        deviceAdapter = new DeviceAdapter(devices, getApplicationContext());
        recyclerDeviceList.setAdapter(deviceAdapter);
        recyclerDeviceList.setVisibility(View.VISIBLE);
    }


    private void initializeService() {
        writeDebug("Start initializing server");
        try {
            server = BLEServer.getInstance(this);
        } catch (NotSupportedException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (NotEnabledException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        writeDebug("Adding mockup device...");

        addDevice(new Device("00", System.currentTimeMillis(), "0 db"));
    }

    private void writeDebug(String message) {
        if (debugger.getLineCount() == debugger.getMaxLines())
            debugger.setText(String.format("%s\n", message));
        else
            debugger.setText(String.format("%s%s\n", String.valueOf(debugger.getText()), message));
    }

    private void addDevice(Device device) {
        this.devices.add(device);
        deviceAdapter.notifyDataSetChanged();
    }
}
