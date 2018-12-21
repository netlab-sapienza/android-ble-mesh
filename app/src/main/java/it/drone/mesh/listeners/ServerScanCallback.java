package it.drone.mesh.listeners;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.List;

import it.drone.mesh.models.Server;
import it.drone.mesh.models.ServerList;

/**
 * Custom ScanCallback object - Every result is an user on the mesh network
 */
public class ServerScanCallback extends ScanCallback {

    private final static String TAG = ServerScanCallback.class.getName();
    private OnServerFoundListener listener;

    public ServerScanCallback(OnServerFoundListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        for (Server temp : ServerList.getUserList()) {
            if (temp.getBluetoothDevice().getName().equals(result.getDevice().getName()))
                return;
        }
        Server newServer = new Server(result.getDevice(), result.getDevice().getName());
        ServerList.addUser(newServer);
        listener.OnServerFound("Ho trovato un nuovo server");
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.d(TAG, "OUD: " + "Scan failed with error: " + errorCode);
    }

    public interface OnServerFoundListener {
        void OnServerFound(String id);
    }
}