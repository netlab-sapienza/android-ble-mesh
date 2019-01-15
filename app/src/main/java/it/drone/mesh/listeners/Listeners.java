package it.drone.mesh.listeners;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

import it.drone.mesh.models.Device;

public class Listeners {
    public interface OnMessageReceivedListener {
        // void OnMessageReceived(String idMitt, String message, int hopNumber, long deliveryTime);

        // TODO: 14/01/2019 fare i cambi necessari ed eliminare questa funzione
        void OnMessageReceived(String idMitt, String message);
    }


    public interface OnMessageWithInternetListener {
        void OnMessageWithInternetListener(String idMitt, String message);
    }

    public interface OnMessageSentListener {
        void OnMessageSent(String message);

        void OnCommunicationError(String error);
    }

    public interface OnScanCompletedListener {
        void OnScanCompleted(ArrayList<Device> devicesFound);
    }

    public interface OnNewServerDiscoveredListener {
        void OnNewServerDiscovered(BluetoothDevice server);
    }

    public interface OnDebugMessageListener {
        void OnDebugMessage(String message);

        void OnDebugErrorMessage(String message);
    }
}
