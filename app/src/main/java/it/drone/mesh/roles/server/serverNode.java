package it.drone.mesh.roles.server;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.LinkedList;

public class serverNode {
    private String TAG = this.getClass().getSimpleName();
    private String id;
    private int numRequest;
    //private BluetoothDevice device;
    private LinkedList<serverNode> nearServers;
    private BluetoothDevice[] clientList;
    private int CLIENT_LIST_SIZE = 7;


    public serverNode(String id, BluetoothDevice device) {
        this.id = id;
    }

    public serverNode(String id) {
        this.id = id;
        nearServers = new LinkedList<>();
        clientList = new BluetoothDevice[CLIENT_LIST_SIZE];
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            clientList[i] = null;
        }
    }

    public serverNode getServer(String serverId) {
        for (serverNode s : nearServers) {
            if (s.getId().equals(serverId)) return s;
        }
        for (serverNode s : nearServers) {
            LinkedList<serverNode> temp = s.getNearServerList();
            for (serverNode n : temp) {
                if (n.getId().equals(serverId)) return n;
            }
        }
        return null;
    }

    public serverNode getServerToSend(String serverId, int numRequest) {
        // TODO: 22/11/18 :'(
        return null;
    }

    public void setClientOnline(String id, BluetoothDevice device) {    //PASSARE SOLO LA PARTE DI ID RELATIVA AL CLIENT
        clientList[Integer.parseInt(id) - 1] = device;
    }

    public void setClientOffline(String id) {
        clientList[Integer.parseInt(id) - 1] = null;
    }

    public BluetoothDevice getClient(String id) {
        return clientList[Integer.parseInt(id) - 1];
    }

    public void addNearServer(serverNode s) {
        if (!nearServers.contains(s)) nearServers.add(s);
    }

    public void addFarServer(serverNode newServer, serverNode knownServer) {
        for (serverNode s : nearServers) {
            if (s.equals(knownServer)) {
                knownServer.getNearServerList().add(newServer);
                break;
            }
        }
    }

    private LinkedList<serverNode> getNearServerList() {
        return this.nearServers;
    }

    private String getId() {
        return this.id;
    }

    public boolean isClientOnline(String id) {
        return clientList[Integer.parseInt(id) - 1] != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            serverNode temp = (serverNode) o;
            return temp.getId().equals(this.id);
        }
        return false;
    }

    public int nextId(BluetoothDevice dev) {
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            if (dev.equals(clientList[i])) {
                Log.d(TAG, "OUD: " + "Utente già presente");
                return -1;
            }
        }
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            if (clientList[i] == null) return i + 1;
        }
        Log.d(TAG, "OUD: " + "Lista piena");
        return -1;
    }
    
    public boolean isFull() {
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            if(clientList[i] == null) return false;
        }
        return true;
    }
}
