package it.drone.mesh.roles.common;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.List;

import it.drone.mesh.models.User;
import it.drone.mesh.models.UserList;

/**
 * Custom ScanCallback object - Every result is an user on the mesh network
 */
public class ServerScanCallback extends ScanCallback {

    private final static String TAG = ServerScanCallback.class.getName();
    private OnServerFoundListerner listener;

    public ServerScanCallback(OnServerFoundListerner listener) {
        this.listener = listener;
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType, result);

        for (User temp : UserList.getUserList()) {
            if (temp.getBluetoothDevice().getName().equals(result.getDevice().getName()))
                return;
        }

        final User newUser = new User(result.getDevice(), result.getDevice().getName());
        UserList.addUser(newUser);
        listener.OnServerFound("Ho trovato un nuovo server");
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.d(TAG, "OUD: " + "Scan failed with error: " + errorCode);
    }

    // TODO: 12/12/2018 migliorare e vedere se può passare messaggi più utili, per ora serve solo a scopo di debug
    public interface OnServerFoundListerner {
        void OnServerFound(String message);
    }

}