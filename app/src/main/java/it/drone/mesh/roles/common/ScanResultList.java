package it.drone.mesh.roles.common;

import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.ArrayList;

public class ScanResultList {
    private final static String TAG = ScanResultList.class.getSimpleName();

    private static ScanResultList singleton;

    // potrebbe essere necessario cambiare la struttura dati per introdurre gli ID
    private ArrayList<ScanResult> list;

    private ScanResultList() {
        list = new ArrayList<>();
    }

    /**
     * Abbiamo bisogno di una unica ScanResultList da modificare, quindi usiamo il pattern singleton
     *
     * @return istanza di ScanResultList
     */

    public static synchronized ScanResultList getInstance() {
        if (singleton == null)
            singleton = new ScanResultList();
        return singleton;
    }

    public void addResult(ScanResult result) {
        list.add(result);
    }

    public ScanResult getScanResult(int index) {
        return list.get(index);
    }

    public void cleanList() {
        list.clear();
    }


    public void printList() {
        for (ScanResult i : list) {
            Log.i(TAG, "Address device = " + i.getDevice().getAddress());
            Log.i(TAG, "Name device = " + i.getDevice().getName());
        }
    }
}
