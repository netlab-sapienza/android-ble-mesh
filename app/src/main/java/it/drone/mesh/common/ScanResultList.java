package it.drone.mesh.common;

import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.LinkedList;

public class ScanResultList {
    private final static String TAG = ScanResultList.class.getSimpleName();

    private static ScanResultList singleton;

    private LinkedList<ScanResult> list;

    private ScanResultList() {
        list = new LinkedList<>();
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

    public LinkedList<ScanResult> getList() {
        return list;
    }

    public ScanResult removeFirst() {
        return list.removeFirst();
    }


    public void addResult(ScanResult result) {
        list.add(result);
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
