package it.drone.mesh;

import android.app.Application;
import android.util.Log;

public class AppGlobal extends Application {
    private final static String TAG = AppGlobal.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        // Qui vanno istanziate tutte le strutture dati che necessitano di inizializzazione
        // non dovrebbero essercene per ora

        Log.i(TAG, "Application Started");

    }
}


