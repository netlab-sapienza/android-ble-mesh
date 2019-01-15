package it.drone.mesh.listeners;

import android.bluetooth.le.ScanResult;

import java.util.ArrayList;

import it.drone.mesh.models.Device;

public class Listeners {
    public interface OnMessageReceivedListener {
        void OnMessageReceived(String idMitt, String message);
    }

    public interface OnMessageSentListener {
        void OnMessageSent(String message);
        void OnCommunicationError(String error);
    }

    public interface OnScanCompletedListener {
        void OnScanCompleted(ArrayList<Device> devicesFound);
    }

    public interface OnNewServerDiscoveredListener {
        void OnNewServerDiscovered(ScanResult server);
    }

    public interface OnDebugMessageListener {
        void OnDebugMessage(String message);

        void OnDebugErrorMessage(String message);
    }
}
