package it.drone.mesh.roles.server;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.LinkedList;

import it.drone.mesh.roles.common.Utility;

public class ServerNode {
    private String TAG = this.getClass().getSimpleName();
    private String id;
    private int lastRequest;
    //private Device device;
    private LinkedList<ServerNode> nearServers;
    private LinkedList<ServerNode> routingTable;
    private BluetoothDevice[] clientList;
    private static final int CLIENT_LIST_SIZE = 7;
    private static final int SERVER_PACKET_SIZE = 10;
    /*
    public ServerNode(String id, BluetoothDevice device) {
        this.id = id;
    }
    */

    public ServerNode(String id) {
        this.id = id;
        nearServers = new LinkedList<>();
        routingTable = new LinkedList<>();
        clientList = new BluetoothDevice[CLIENT_LIST_SIZE];
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            clientList[i] = null;
        }
    }

    public ServerNode getServer(String serverId) {
        for (ServerNode s : nearServers) {
            if (s.getId().equals(serverId)) return s;
        }
        for (ServerNode s : nearServers) {
            LinkedList<ServerNode> temp = s.getNearServerList();
            for (ServerNode n : temp) {
                if (n.getId().equals(serverId)) return n;
            }
        }
        return null;
    }

    public ServerNode getServerToSend(String serverId, String idAsker, int numRequest) {
        if(lastRequest != numRequest) {
            lastRequest = numRequest;
        }
        else return null;

        for (ServerNode s : routingTable) {
            if(s.getId().equals(serverId)) return s;
        }
        for (ServerNode s : routingTable) {
            for (ServerNode t : s.getRoutingTable()) {
                if(t.getId().equals(serverId)) return s;
            }
        }
        for (ServerNode s : routingTable) {
            if(s.getId().equals(idAsker)) continue;
            ServerNode toSend = s.getServerToSend(serverId, this.id, numRequest);
            if(toSend != null) {
                ServerNode newServer = new ServerNode(serverId);
                addFarServer(newServer, s);
                Log.d(TAG, "OUD: " + "Next hop: " + toSend.getId());
                return s;
            }
        }
        return null; //quindi broadcasta
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

    public LinkedList<ServerNode> getRoutingTable() {
        return routingTable;
    }

    public void addNearServer(ServerNode s) {
        if (!nearServers.contains(s)) {
            nearServers.add(s);
            s.addNearServer(this);
            routingTable.add(s);
        }
    }

    public void addFarServer(ServerNode newServer, ServerNode nearServer) {
        for (ServerNode s : nearServers) {
            if (s.equals(nearServer)) {
                nearServer.getRoutingTable().add(newServer);
                break;
            }
        }
    }

    private LinkedList<ServerNode> getNearServerList() {
        return this.nearServers;
    }

    public String getId() {
        return this.id;
    }

    public boolean isClientOnline(String id) {
        return clientList[Integer.parseInt(id) - 1] != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            ServerNode temp = (ServerNode) o;
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
        //Log.d(TAG, "OUD: " + "Lista piena");
        return -1;
    }

    public boolean isFull() {
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            if(clientList[i] == null) return false;
        }
        return true;
    }

    public void printStatus() {
        Log.d(TAG, "OUD: " + "I'm node " + id);
        Log.d(TAG, "OUD: " + "This is my current situation: ");
        Log.d(TAG, "OUD: " + "[");
        for (int i = 0; i < clientList.length; i++) {
            if(clientList[i] != null) Log.d(TAG, "OUD: " + i + (i == clientList.length - 1 ? "" : ","));
            else Log.d(TAG, "OUD: " + "null,");

        }

        Log.d(TAG, "OUD: " + "]");
        Log.d(TAG, "OUD: " + "I have " + nearServers.size() + " near servers");
        Log.d(TAG, "OUD: " + "[");
        int size = nearServers.size();
        for (int i = 0; i < size; i++) {
            Log.d(TAG, "OUD: " + nearServers.get(i).getId() + (i == size - 1 ? "" : ","));
        }
        Log.d(TAG, "OUD: " + "]\n");
    }

    public void parseMapToByte(byte[][] destArrayByte) {
        for(ServerNode s: nearServers){
            int index = Integer.parseInt(s.getId());
            if (Utility.getBit(destArrayByte[index][0],0) == 1 || (Utility.getBit(destArrayByte[index][0],1)) == 1 || (Utility.getBit(destArrayByte[index][0],2)) == 1 || (Utility.getBit(destArrayByte[index][0],3)) == 1) continue;
            byte[] tempArrayByte = new byte[SERVER_PACKET_SIZE];
            int clientId = Integer.parseInt(s.getId());
            int serverId = 0;
            byte firstByte = Utility.byteNearServerBuilder(serverId, clientId);
            tempArrayByte[0] = firstByte;
            byte secondByte = 0b00000000;
            LinkedList<ServerNode> nearTemp = s.getNearServerList();
            for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
                if (clientList[i] != null) secondByte = Utility.setBit(secondByte, i+1);
            }
            tempArrayByte[1] = secondByte;
            int tempIndex = 2;
            int size = nearTemp.size();
            for (int i = 0; i < size; i++) {
                byte nearByte;
                if (i + 1 < size) {
                    nearByte = Utility.byteNearServerBuilder(Integer.parseInt(nearTemp.get(i).getId()), Integer.parseInt(nearTemp.get(i+1).getId()));
                }
                else {
                    nearByte = Utility.byteNearServerBuilder(Integer.parseInt(nearTemp.get(i).getId()), 0);
                }
                tempArrayByte[tempIndex] = nearByte;
                tempIndex++;
                i++;
            }
            for (int i = 0; i < SERVER_PACKET_SIZE; i++) {
                destArrayByte[index][i] = tempArrayByte[i];
            }
            s.parseMapToByte(destArrayByte);
        }

    }

    public static ServerNode buildRoutingTable(byte[][] mapByte, String id) {
        ServerNode[] arrayNode = new ServerNode[Utility.DEST_PACK_MESSAGE_LEN];
        for (int i = 1;i < Utility.DEST_PACK_MESSAGE_LEN;i++){
            if (Utility.getBit(mapByte[i][0],0) == 1 || (Utility.getBit(mapByte[i][0],1)) == 1 || (Utility.getBit(mapByte[i][0],2)) == 1 || (Utility.getBit(mapByte[i][0],3)) == 1) {
                arrayNode[i] = new ServerNode(""+i);
            }
        }
        for (int i = 0; i < Utility.DEST_PACK_MESSAGE_LEN; i++) {
            if (arrayNode[i] != null) {
                byte clientByte = mapByte[i][1] ;
                for(int k = 0;k < 8;k++) {
                    if (Utility.getBit(clientByte, k) == 1 ) arrayNode[i].setClientOnline("" + k, null); // TODO: 04/12/18 VEDERE COME PASSARSI IL DEVICE
                }
                for (int k = 2;k < SERVER_PACKET_SIZE;k++) {
                    byte nearServerByte = mapByte[i][k];
                    int[] infoNearServer = Utility.getIdServerByteInfo(nearServerByte);
                    if (infoNearServer[0] != 0) {
                        arrayNode[i].addNearServer(arrayNode[infoNearServer[0]]);
                    }
                    else break;
                    if (infoNearServer[1] != 0) {
                        arrayNode[i].addNearServer(arrayNode[infoNearServer[1]]);
                    }
                    else break;
                }
            }
        }
        return arrayNode[Integer.parseInt(id)];
    }
}
