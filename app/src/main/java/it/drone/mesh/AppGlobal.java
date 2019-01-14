package it.drone.mesh;

import android.app.Application;
import android.util.Log;

public class AppGlobal extends Application {
    private final static String TAG = AppGlobal.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Application Started");
    }
}


