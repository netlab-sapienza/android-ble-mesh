package it.drone.mesh.server;

/**
 * BLEServer ha il compito di offrire un servizio che implementi le seguenti funzioni:
 * 1) Fornire identità nella sottorete
 * 2) Permettere lo scambio di messaggi nella sottorete e nelle altre reti
 * 3) ....
 */

public class BLEServer {

    private final static String TAG = BLEServer.class.getSimpleName();

    private static BLEServer singleton;

    private BLEServer() {

    }

    public static synchronized BLEServer getInstance() {
        if (singleton == null)
            singleton = new BLEServer();
        return singleton;
    }

}
