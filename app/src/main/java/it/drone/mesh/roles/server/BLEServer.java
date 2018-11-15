package it.drone.mesh.roles.server;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import it.drone.mesh.R;
import it.drone.mesh.roles.common.RoutingTable;
import it.drone.mesh.roles.common.exceptions.NotEnabledException;
import it.drone.mesh.roles.common.exceptions.NotSupportedException;

import static it.drone.mesh.roles.common.Utility.isBLESupported;

/**
 * BLEServer ha il compito di offrire un servizio che implementi le seguenti funzioni:
 * 1) Fornire identità nella sottorete
 * 2) Permettere lo scambio di messaggi nella sottorete e nelle altre reti
 * 3) ....
 */

public class BLEServer {

    private static BLEServer singleton;
    // potrebbero venir riutilizzati, quindi non convertire a local
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private String id;
    private RoutingTable routingTable;

    private BLEServer(Context context) throws NotSupportedException, NotEnabledException {
        bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null)
            mBluetoothAdapter = bluetoothManager.getAdapter();
        else
            throw new NotSupportedException(context.getResources().getString(R.string.bt_null));

        if (!isBLESupported(context)) {
            throw new NotSupportedException(context.getResources().getString(R.string.bt_not_supported));
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            throw new NotEnabledException(context.getResources().getString(R.string.bt_not_enabled_leaving));
        }
    }

    public static synchronized BLEServer getInstance(Context context) throws NotSupportedException, NotEnabledException {
        if (singleton == null)
            singleton = new BLEServer(context);
        return singleton;
    }

    public boolean initializeService() {
        // TODO: 13/11/18 inizializza il server

        return Math.random() % 2 == 0;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RoutingTable getRoutingTable() {

        return this.routingTable;
    }


    public boolean sendMessage(String message) {
        // TODO: 13/11/18
        return true;
    }

    public void scan() {

    }

}
