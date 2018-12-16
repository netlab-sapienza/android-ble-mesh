package it.drone.mesh.roles.server;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.LinkedList;

import it.drone.mesh.roles.common.Utility;

public class ServerNode {
    public static final int MAX_NUM_SERVER = 16;
    private static String TAG = ServerNode.class.getSimpleName();
    private String id;
    private int lastRequest;
    //private Device device;
    private LinkedList<ServerNode> nearServers;
    private LinkedList<ServerNode> routingTable;
    private byte clientByte;
    private BluetoothDevice[] clientList;
    public static final int CLIENT_LIST_SIZE = 7;
    public static final int SERVER_PACKET_SIZE = 11;
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
        clientByte = 0b00000000;
    }

    public ServerNode getServer(String serverId) {
        for (ServerNode s : routingTable) {
            if (s.getId().equals(serverId)) return s;
        }
        for (ServerNode s : routingTable) {
            LinkedList<ServerNode> temp = s.getNearServerList();
            for (ServerNode n : temp) {
                if (n.getId().equals(serverId)) return n;
            }
        }
        return null;
    }

    public ServerNode getServerToSend(String serverId, String idAsker, int numRequest) {
        if (lastRequest != numRequest) {
            lastRequest = numRequest;
        } else return null;

        for (ServerNode s : routingTable) {
            if (s.getId().equals(serverId)) return s;
        }
        for (ServerNode s : routingTable) {
            for (ServerNode t : s.getRoutingTable()) {
                if (t.getId().equals(serverId)) return s;
            }
        }
        for (ServerNode s : routingTable) {
            if (s.getId().equals(idAsker)) continue;
            ServerNode toSend = s.getServerToSend(serverId, this.id, numRequest);
            if (toSend != null) {
                ServerNode newServer = new ServerNode(serverId);
                addFarServer(newServer, s);
                Log.d(TAG, "OUD: " + "Next hop: " + toSend.getId());
                return s;
            }
        }
        return null; //quindi broadcasta
    }

    public void setClientOnline(String id, BluetoothDevice device) {    //PASSARE SOLO LA PARTE DI ID RELATIVA AL CLIENT
        clientByte = Utility.setBit(clientByte,Integer.parseInt(id));
        clientList[Integer.parseInt(id)] = device;
        Log.d(TAG, "OUD: ho aggiunto il client " + id);
    }

    public void setClientOffline(String id) {
        clientList[Integer.parseInt(id)] = null;
    }

    public BluetoothDevice getClient(String id) {
        return clientList[Integer.parseInt(id)];
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
        return clientList[Integer.parseInt(id)] != null;
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
        if (dev == null) {
            for (int i = 1; i < CLIENT_LIST_SIZE; i++) {
                if (clientList[i] == null) return i;
            }
            return -1;
        }
        for (int i = 1; i < CLIENT_LIST_SIZE; i++) {
            if (dev.equals(clientList[i])) {
                Log.d(TAG, "OUD: " + "Utente già presente");
                return -1;
            }
        }
        for (int i = 1; i < CLIENT_LIST_SIZE; i++) {
            if (clientList[i] == null) {
                if (i > 1) return -1;
                return i;
            }
        }
        //Log.d(TAG, "OUD: " + "Lista piena");
        return -1;
    }

    public void printStatus() {
        Log.d(TAG, "OUD: " + "I'm node " + id);
        Log.d(TAG, "OUD: " + "My clients are: ");
        String s = "";
        for (int i = 0; i < clientList.length; i++) {
            if (clientList[i] != null) s += i + (i == clientList.length - 1 ? "" : ",");
            else s += "null,";
        }
        Log.d(TAG, "OUD: " + "[" + s + "]");

        Log.d(TAG, "OUD: " + "I have " + nearServers.size() + " near servers");
        int size = nearServers.size();
        s = "";
        for (int i = 0; i < size; i++) {
            s += nearServers.get(i).getId() + (i == size - 1 ? "" : ",");
        }
        Log.d(TAG, "OUD: " + "[" + s + "]");
    }

    public void parseMapToByte(byte[][] destArrayByte) {
        for (ServerNode s : nearServers) {
            int index = Integer.parseInt(s.getId());
            if (Utility.getBit(destArrayByte[index][0], 0) == 1 || (Utility.getBit(destArrayByte[index][0], 1)) == 1 || (Utility.getBit(destArrayByte[index][0], 2)) == 1 || (Utility.getBit(destArrayByte[index][0], 3)) == 1)
                continue;
            byte[] tempArrayByte = new byte[SERVER_PACKET_SIZE];
            int clientId = Integer.parseInt(s.getId());
            int serverId = 0;
            byte firstByte = Utility.byteNearServerBuilder(serverId, clientId);
            tempArrayByte[0] = firstByte;
            byte secondByte = 0b00000000;
            LinkedList<ServerNode> nearTemp = s.getNearServerList();
            for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
                if (s.clientList[i] != null) secondByte = Utility.setBit(secondByte, i);
            }
            tempArrayByte[1] = secondByte;
            int tempIndex = 2;
            int size = nearTemp.size();
            for (int i = 0; i < size; i++) {
                byte nearByte;
                if (i + 1 < size) {
                    nearByte = Utility.byteNearServerBuilder(Integer.parseInt(nearTemp.get(i).getId()), Integer.parseInt(nearTemp.get(i + 1).getId()));
                } else {
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

    public void parseClientMapToByte(byte[] destArrayByte) {
        for (ServerNode s : nearServers) {
            int index = Integer.parseInt(s.getId());
            boolean alreadyDone = false;
            for (int i = 0; i < 8; i++) {
                if (Utility.getBit(destArrayByte[index], i) != 0) alreadyDone = true;
            }
            if (alreadyDone) continue;
            destArrayByte[index] = s.clientByte;
            destArrayByte[index] = Utility.setBit(destArrayByte[index],0);
            s.parseClientMapToByte(destArrayByte);
        }

    }

    public static ServerNode buildRoutingTable(byte[][] mapByte, String id, BluetoothDevice[] clientList) {
        Log.d(TAG, "OUD: " + "MapByte è una " + mapByte.length + " x " + mapByte[0].length);
        for (int i = 0; i < 16; i++) {
            Log.d(TAG, "buildRoutingTable: I: " + i);
            for (int j = 0; j < SERVER_PACKET_SIZE; j++) {
                Log.d(TAG, "buildRoutingTable: J: " + j);
                Utility.printByte(mapByte[i][j]);
            }
        }
        ServerNode[] arrayNode = new ServerNode[MAX_NUM_SERVER]; //perchè al max 16 server
        for (int i = 1; i < 16; i++) {
            if (Utility.getBit(mapByte[i][0], 0) == 1 || (Utility.getBit(mapByte[i][0], 1)) == 1 || (Utility.getBit(mapByte[i][0], 2)) == 1 || (Utility.getBit(mapByte[i][0], 3)) == 1) {
                arrayNode[i] = new ServerNode("" + i);
            }
        }
        for (int i = 0; i < 16; i++) {
            if (arrayNode[i] != null) {
                Log.d(TAG, "OUD: " + i);
                byte clientByte = mapByte[i][1];

                for (int k = 0; k < 8; k++) {
                    if (Utility.getBit(clientByte, k) == 1)
                        if (!id.equals("" + i)) arrayNode[i].setClientOnline("" + k, null);
                        else arrayNode[i].setClientOnline("" + k, clientList[i]);
                }

                for (int k = 2; k < SERVER_PACKET_SIZE; k++) {
                    byte nearServerByte = mapByte[i][k];
                    int[] infoNearServer = Utility.getIdServerByteInfo(nearServerByte);
                    if (infoNearServer[0] != 0) {
                        arrayNode[i].addNearServer(arrayNode[infoNearServer[0]]);
                    } else break;
                    if (infoNearServer[1] != 0) {
                        arrayNode[i].addNearServer(arrayNode[infoNearServer[1]]);
                    } else break;
                }
            }
        }
        arrayNode[Integer.parseInt(id)].printStatus();
        return arrayNode[Integer.parseInt(id)];
    }

    public byte[] parseNewServer() {
        Log.d(TAG, "OUD: " + "Near Server :" + nearServers.size());
        byte[] res = new byte[16];
        res[0] = Utility.byteNearServerBuilder(0, Integer.parseInt(this.id));
        res[1] = Utility.setBit(res[1], 0);
        for (int i = 0; i < CLIENT_LIST_SIZE; i++) {
            if (clientList[i] != null) res[1] = Utility.setBit(res[2], i + 1);
        }
        for (int i = 3; i < 16; i++) {
            if (nearServers.size() <= i - 3) break;
            else if (nearServers.size() == i - 2) {
                byte temp = Utility.byteNearServerBuilder(Integer.parseInt(nearServers.get(i - 3).getId()), 0);
                res[i] = temp;
            } else {
                byte temp = Utility.byteNearServerBuilder(Integer.parseInt(nearServers.get(i - 3).getId()), Integer.parseInt(nearServers.get(i - 2).getId()));
                res[i] = temp;
            }
        }
        return res;
    }

    public boolean updateRoutingTable(byte[] value) {
        boolean res = false;
        byte idByte = value[0];
        int index = Utility.getBit(idByte, 0) + Utility.getBit(idByte, 1) * 2 + Utility.getBit(idByte, 2) * 4 + Utility.getBit(idByte, 3) * 8;
        ServerNode nuovoServer = new ServerNode("" + index);
        for (int i = 0; i < 8; i++) {
            if (Utility.getBit(value[2], i) == 1) nuovoServer.setClientOnline("" + i, null);
        }
        for (int i = 3; i < 16; i++) {
            int tempId = Utility.getBit(value[i], 4) + Utility.getBit(value[i], 5) * 2 + Utility.getBit(value[i], 6) * 4 + Utility.getBit(value[i], 7) * 8;
            if (tempId == 0) break;
            if (("" + tempId).equals(this.id)) {
                addNearServer(nuovoServer);
                res = true;
            } else {
                ServerNode tempServer = getServer("" + tempId);
                if (tempServer != null) tempServer.addNearServer(nuovoServer);
            }
            tempId = Utility.getBit(value[i], 0) + Utility.getBit(value[i], 1) * 2 + Utility.getBit(value[i], 2) * 4 + Utility.getBit(value[i], 3) * 8;
            if (tempId == 0) break;
            if (("" + tempId).equals(this.id)) {
                addNearServer(nuovoServer);
                res = true;
            } else {
                ServerNode tempServer = getServer("" + tempId);
                if (tempServer != null) tempServer.addNearServer(nuovoServer);
            }
        }

        for (int i = 0; i < 16; i++) {
            ServerNode n = getServer("" + i);
            if (n != null) n.printStatus();
        }
        return res;
    }

    public BluetoothDevice[] getClientList() {
        return clientList;
    }

    public int getLastRequest() {
        return lastRequest;
    }
}
