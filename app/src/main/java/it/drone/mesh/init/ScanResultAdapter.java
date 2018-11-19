package it.drone.mesh.init;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import it.drone.mesh.R;

public class ScanResultAdapter extends ArrayAdapter<ScanResult> {

    public ScanResultAdapter(Context context, int resource, List<ScanResult> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {



        return super.getView(position, convertView, parent);
    }


}
