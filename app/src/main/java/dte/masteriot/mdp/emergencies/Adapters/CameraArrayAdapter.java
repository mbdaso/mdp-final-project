package dte.masteriot.mdp.emergencies.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.R;

public class CameraArrayAdapter extends ArrayAdapter {

    private ArrayList<Camera> items;
    private Context mContext;

    public CameraArrayAdapter(Context context, ArrayList<Camera> countries) {
        super(context, 0, countries);
        items = countries;
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View newView = convertView;

        // This approach can be improved for performance
        if (newView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            newView = inflater.inflate(R.layout.camera_item_list, parent, false);
        }
        //-----

        TextView textView = (TextView) newView.findViewById(R.id.listElemName);
        //TextView poptextView = (TextView) newView.findViewById(R.id.countPopView);
        //ImageView imageView = (ImageView) newView.findViewById(R.id.imgCountry);

        Camera camera = items.get(position);

        textView.setText(camera.name);
        //poptextView.setText(camera.getPopulation());
        //imageView.setImageResource(camera.getImageResource());

        return newView;
    }
}


