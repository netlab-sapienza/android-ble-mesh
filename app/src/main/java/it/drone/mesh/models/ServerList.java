package it.drone.mesh.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Dovrebbe rappresentare tutti i server in rete trovati durante la scansione e durante l'uso
 */
public class ServerList {

    private static final String TAG = ServerList.class.getSimpleName();
    private static ArrayList<Server> servers = new ArrayList<>();


    public static Server getServer(int index) throws NoSuchElementException {
        Server u = servers.get(index);
        if (u == null)
            throw new NoSuchElementException("Server non presente all'interno della lista");
        else
            return u;
    }

    public static Server getServer(String serverName) throws NoSuchElementException {
        for (Server i : servers) {
            if (i.getUserName().equals(serverName))
                return i;
        }
        throw new NoSuchElementException("Server non presente all'interno della lista");
    }

    public static void addServer(Server server) {
        servers.add(server);
        Log.i(TAG, "added Server: " + server.getUserName());
    }

    public static List<Server> getServerList() {
        return servers;
    }

    public static void cleanUserList() {
        servers.clear();
    }

    public static Server removeServer(String name) {
        for (Server i : servers) {
            if (i.getUserName().equals(name)) {
                servers.remove(i);
                Log.i(TAG, "removed Server: " + i.getUserName());
                return i;
            }
        }

        Log.e(TAG, "removeUser: Server not found, Lista: \n" + printList());
        return null;
    }

    public static String printList() {
        StringBuilder res = new StringBuilder();
        for (Server i : servers)
            res.append("Username: ").append(i.getUserName()).append("ID mistico da decidere : none\n");

        return res.toString();
    }

}