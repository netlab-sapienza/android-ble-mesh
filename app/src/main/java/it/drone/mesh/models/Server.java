package it.drone.mesh.models;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public class Server {
    private BluetoothDevice bluetoothDevice;
    private String userName;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGatt bluetoothGatt;
    private String id;

    public Server(BluetoothDevice mBluetoothDevice, BluetoothServerSocket mBluetoothServerSocket, BluetoothSocket mBluetoothSocket) {
        this.bluetoothDevice = mBluetoothDevice;
        this.bluetoothServerSocket = mBluetoothServerSocket;
        this.bluetoothSocket = mBluetoothSocket;
    }

    public Server(BluetoothDevice mBluetoothDevice) {
        this.bluetoothDevice = mBluetoothDevice;
        this.bluetoothServerSocket = null;
        this.bluetoothSocket = null;
        this.bluetoothGattServer = null;
        this.bluetoothGatt = null;
    }

    public Server(BluetoothDevice mBluetoothDevice, String name) {
        this.bluetoothDevice = mBluetoothDevice;
        this.userName = name;
        this.bluetoothServerSocket = null;
        this.bluetoothSocket = null;
        this.bluetoothGattServer = null;
        this.bluetoothGatt = null;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice b) {
        this.bluetoothDevice = b;
    }

    public BluetoothServerSocket getBluetoothServerSocket() {
        return bluetoothServerSocket;
    }

    public void setBluetoothServerSocket(BluetoothServerSocket bluetoothServerSocket) {
        this.bluetoothServerSocket = bluetoothServerSocket;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public BluetoothGattServer getBluetoothGattServer() {
        return bluetoothGattServer;
    }

    public void setBluetoothGattServer(BluetoothGattServer bluetoothGattServer) {
        this.bluetoothGattServer = bluetoothGattServer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
