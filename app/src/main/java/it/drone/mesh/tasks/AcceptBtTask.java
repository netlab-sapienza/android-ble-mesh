package it.drone.mesh.tasks;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import it.drone.mesh.common.Constants;
import it.drone.mesh.models.Server;

public class AcceptBtTask extends AsyncTask<Void, Void, BluetoothSocket> {
    private static final String TAG = AcceptBtTask.class.getSimpleName();

    private Server server;
    private BluetoothAdapter mBluetoothAdapter;


    public AcceptBtTask(Server server, BluetoothAdapter mBluetoothAdapter) {
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        this.server = server;
        this.mBluetoothAdapter = mBluetoothAdapter;
    }

    @Override
    protected BluetoothSocket doInBackground(Void... s) {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        //mBluetoothAdapter.cancelDiscovery();
        // Creates the Server Socket
        try {
            server.setBluetoothServerSocket(mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("HIRO-NET", Constants.ServiceUUID));
        } catch (IOException e) {
            Log.d(TAG, "OUD: " + "Couldn't create a Socket");
        }

        while (true) {
            try {
                socket = server.getBluetoothServerSocket().accept();
            } catch (IOException e) {
                Log.d(TAG, "OUD: " + "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                Log.d(TAG, "OUD: " + "Socket's accept()");
                // manageMyConnectedSocket(socket);
                try {
                    server.getBluetoothServerSocket().close();
                } catch (IOException e) {
                    Log.d(TAG, "OUD: " + "Socket's accept() method failed", e);
                }
                break;
            }
        }
        //processBtAccept(socket);
        return socket;
    }

    @Override
    protected void onPostExecute(BluetoothSocket result) {
        if (result == null)
            return;
        server.setBluetoothSocket(result);
        processBtAccept(result);
    }

    private void processBtAccept(final BluetoothSocket socket) {
        TimerTask timerTask = new TimerTask() {
            InputStream tmpIn = null;
            byte[] buffer = new byte[20];
            int bytes;

            @Override
            public void run() {
                try {
                    tmpIn = server.getBluetoothSocket().getInputStream();
                } catch (IOException closeException) {
                    Log.d(TAG, "OUD: " + "Couldn't get an Inputstream", closeException);
                }
                try {
                    bytes = tmpIn.available();
                    bytes = tmpIn.read(buffer);
                } catch (IOException closeException) {
                    Log.d(TAG, "OUD: " + "Couldn't print anything", closeException);
                }
                // output in logger
                Log.d(TAG, "OUD: " + "processBtAccept: " + new String(buffer) + "from" + socket.getRemoteDevice().toString());
            }
        };

        Timer timer = new Timer();

        //DELAY: the time to the first execution
        //PERIODICAL_TIME: the time between each execution of your task.
        timer.schedule(timerTask, 2000L, 4000L);
    }
}