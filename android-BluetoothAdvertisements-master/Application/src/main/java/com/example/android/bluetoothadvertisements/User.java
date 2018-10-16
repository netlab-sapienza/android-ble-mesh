package com.example.android.bluetoothadvertisements;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public class User {
    private BluetoothDevice bluetoothDevice;
    private String userName;
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGatt bluetoothGatt;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public User(BluetoothDevice mBluetoothDevice, BluetoothServerSocket mBluetoothServerSocket, BluetoothSocket mBluetoothSocket){
            this.bluetoothDevice = mBluetoothDevice;
            this.bluetoothServerSocket = mBluetoothServerSocket;
            this.bluetoothSocket = mBluetoothSocket;
    }
    public User(BluetoothDevice mBluetoothDevice){
        this.bluetoothDevice = mBluetoothDevice;
        this.bluetoothServerSocket = null;
        this.bluetoothSocket = null;
        this.bluetoothGattServer = null;
        this.bluetoothGatt = null;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
    public void setBluetoothDevice(BluetoothDevice b){
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

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGattServer(BluetoothGattServer bluetoothGattServer) {
        this.bluetoothGattServer = bluetoothGattServer;
    }

    public BluetoothGattServer getBluetoothGattServer() {
        return bluetoothGattServer;
    }
}
