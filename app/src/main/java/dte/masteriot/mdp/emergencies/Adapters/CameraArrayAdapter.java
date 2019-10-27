package dte.masteriot.mdp.emergencies.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;


import java.util.ArrayList;

import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.R;

public class CameraArrayAdapter extends ArrayAdapter {

    private ArrayList<Camera> items;
    private Context mContext;

    public CameraArrayAdapter(Context context, ArrayList<Camera> cameras) {
        super(context, 0, cameras);
        items = cameras;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View newView = convertView;

        // This approach can be improved for performance
        if (newView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            newView = inflater.inflate(R.layout.camera_item_list, parent, false);
        }
        //-----

        CheckedTextView textView = newView.findViewById(R.id.listOfCameras);
        //listOfCameras is the layout for a camera list item


        Camera camera = items.get(position);

        textView.setText(camera.name);
        if(camera.valCont >= 100)
        {
            // Set a background color for ListView regular row/item
            newView.setBackgroundColor(Color.RED);
        }
        else
        {
            // Set the background color for alternate row/item
            newView.setBackgroundColor(Color.WHITE);
        }

        return newView;
    }
}


