package it.drone.mesh.tasks;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import it.drone.mesh.models.User;
import it.drone.mesh.roles.common.Constants;

public class ConnectBtTask extends AsyncTask<Void, Void, BluetoothSocket> {
    private final static String TAG = ConnectBtTask.class.getSimpleName();

    private User mmUser;

    public ConnectBtTask(User user) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        mmUser = user;
    }

    @Override
    protected BluetoothSocket doInBackground(Void... s) {
        // Cancel discovery because it otherwise slows down the connection.
        //mBluetoothAdapter.cancelDiscovery();
        BluetoothSocket tmp = null;
        try {
            tmp = mmUser.getBluetoothDevice().createInsecureRfcommSocketToServiceRecord(Constants.Service_UUID.getUuid());
        } catch (IOException e) {
            Log.d(TAG, "OUD: " + e.toString());
        }
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            tmp.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                tmp.close();
            } catch (IOException closeException) {
                Log.d(TAG, "OUD: " + "Could not close the client socket", closeException);
            }
            return null;
        }
        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        mmUser.setBluetoothSocket(tmp);
        return mmUser.getBluetoothSocket();
    }

    @Override
    protected void onPostExecute(BluetoothSocket result) {
        final BluetoothSocket mBluetoothSocket = result;
        if (result == null)
            return;
        try {
            mmUser.getBluetoothServerSocket().close();
        } catch (IOException e) {
            Log.d(TAG, "OUD: " + "couldn't close socket", e);
        }
        processBtConnect(result);
    }

    public void processBtConnect(final BluetoothSocket socket) {
        TimerTask timerTask = new TimerTask() {
            OutputStream tmpOut = null;
            byte[] buffer = new byte[20];
            int bytes;

            @Override
            public void run() {
                try {
                    tmpOut = mmUser.getBluetoothSocket().getOutputStream();
                } catch (IOException closeException) {
                    Log.d(TAG, "OUD: " + "Couldn't get an Inputstream", closeException);
                }
                try {
                    new Random().nextBytes(buffer);
                    tmpOut.write(buffer);
                } catch (IOException closeException) {
                    Log.d(TAG, "OUD: " + "Couldn't print anything", closeException);
                }
                // output nel logger per ora
                Log.d(TAG, "OUD: " + "processBtConnect: " + new String(buffer) + "to" + socket.getRemoteDevice().toString());
            }
        };

        Timer timer = new Timer();
        //DELAY: the time to the first execution
        //PERIODICAL_TIME: the time between each execution of your task.
        timer.schedule(timerTask, 2000L, 4000L);

    }
}