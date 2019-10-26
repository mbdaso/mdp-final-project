package dte.masteriot.mdp.emergencies.AsyncTasks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

import dte.masteriot.mdp.emergencies.Activities.MapsActivity;
import dte.masteriot.mdp.emergencies.Services.YOURSRoute;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class MapRouteTask extends AsyncTask<LatLng, Void, List<LatLng>> {
    MapsActivity mapsActivity;
    YOURSRoute yoursRoute = new YOURSRoute();

    public MapRouteTask(MapsActivity mapsActivity_){
        mapsActivity = mapsActivity_;
    }

    @Override
    @SuppressWarnings( "deprecation" )
    protected List<LatLng> doInBackground(LatLng ... srcdst) {
        LatLng src = srcdst[0];
        LatLng dst = srcdst[1];
        List<LatLng> route;
        try {
            route = yoursRoute.getRoute(src, dst);

        }
        catch(Exception e){
            Log.d(TAG, "MapRouteTask: " + e. getMessage());
            route = Arrays.asList();
        }
        return route;
    }

    @Override
    protected void onPostExecute(List<LatLng> route) {
        mapsActivity.drawMapRoutePolyline(route);
    }
}