package it.drone.mesh.common;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;

import it.drone.mesh.models.Device;

/**
 * This class represents the routing table
 */
public class RoutingTable {

    private final static String TAG = RoutingTable.class.getSimpleName();
    private static RoutingTable singleton;
    private LinkedList<OnRoutingTableUpdateListener> listeners;
    private ArrayList<Device> routingTable;

    private RoutingTable() {
        this.routingTable = new ArrayList<>();
        this.listeners = new LinkedList<>();
    }

    public static RoutingTable getInstance() {
        if (singleton == null)
            singleton = new RoutingTable();
        return singleton;
    }

    public ArrayList<Device> getDeviceList() {
        return this.routingTable;
    }

    public void addDevice(Device device) {
        if (routingTable.contains(device)) return;
        this.routingTable.add(device);
        for (OnRoutingTableUpdateListener listener : listeners)
            listener.OnDeviceAdded(device);
    }

    public boolean removeDevice(Device device) {
        boolean removed = this.routingTable.remove(device);
        if (removed) {
            for (OnRoutingTableUpdateListener listener : listeners)
                listener.OnDeviceRemoved(device);
        } else {
            Log.e(TAG, "removeDevice: Failed");
        }
        return removed;
    }

    public void subscribeToUpdates(OnRoutingTableUpdateListener listener) {
        this.listeners.add(listener);
    }

    public void unsubscribeToUpdates(OnRoutingTableUpdateListener listener) {
        this.listeners.remove(listener);
    }

    public void addDevice(int serverId, int clientId) {
        addDevice(new Device("" + serverId + "" + clientId));
    }

    public void cleanRoutingTable() {
        routingTable.clear();
    }

    public interface OnRoutingTableUpdateListener {
        void OnDeviceAdded(Device device);

        void OnDeviceRemoved(Device device);
    }
}
