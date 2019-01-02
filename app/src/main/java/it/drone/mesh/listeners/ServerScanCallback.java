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

        for (Server temp : ServerList.getServerList()) {
            if (temp.getBluetoothDevice().getName().equals(result.getDevice().getName()))
                return;
        }
        ServerList.addServer(new Server(result.getDevice(), result.getDevice().getName()));
        listener.OnServerFound("Ho trovato un nuovo server");
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        switch (errorCode) {
            case SCAN_FAILED_ALREADY_STARTED:
                listener.OnErrorScan("Scan already started", errorCode);
                Log.e(TAG, "Scan already started");
                break;
            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                listener.OnErrorScan("Scan failed application registration failed", errorCode);
                Log.e(TAG, "Scan failed application registration failed");
                break;
            case SCAN_FAILED_FEATURE_UNSUPPORTED:
                listener.OnErrorScan("Scan failed,this feature is unsupported", errorCode);
                Log.e(TAG, "Scan failed,this feature is unsupported");
                break;
            case SCAN_FAILED_INTERNAL_ERROR:
                listener.OnErrorScan("Scan failed internal error", errorCode);
                Log.e(TAG, "Scan failed internal error");
                break;
            default:
                listener.OnErrorScan("", errorCode);
                Log.e(TAG, "Scan failed unidentified errorCode " + errorCode);
        }
    }

    public interface OnServerFoundListener {
        void OnServerFound(String id);

        void OnErrorScan(String message, int errorCodeCallback);
    }
}