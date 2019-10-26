package dte.masteriot.mdp.emergencies.AsyncTasks;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import dte.masteriot.mdp.emergencies.Activities.MainActivity;
import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.Model.JSONChannel;
import dte.masteriot.mdp.emergencies.Model.MqttChannel;

public class DownloadJSONChannels extends AsyncTask<String, Void, JSONChannel[]> {
    final String TAG = "Pepe";
    Gson gson = new Gson();

    ArrayList<MqttChannel> mqttChannels = new ArrayList<>();
    MainActivity mainActivity;

    public DownloadJSONChannels(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected JSONChannel[] doInBackground(String... urls) {
        HttpURLConnection urlConnection;
        JSONChannel[] channels = new JSONChannel[4];
        try {
            URL url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream is = urlConnection.getInputStream();

            channels = gson.fromJson(new InputStreamReader(is), JSONChannel[].class);
            System.out.println("GSON PARSED");
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return channels;
    }

    @Override
    protected void onPostExecute(JSONChannel[] channels) {
        ArrayList<Camera> cameraArrayList= mainActivity.getCameraArrayList();
        for (JSONChannel channel : channels) {
            String write_api_key = channel.api_keys[0].write_flag ? channel.api_keys[0].api_key : channel.api_keys[1].api_key;
            String read_api_key = channel.api_keys[0].write_flag ? channel.api_keys[1].api_key : channel.api_keys[0].api_key;
            LatLng position = new LatLng(Double.valueOf(channel.latitude), Double.valueOf(channel.longitude));
            //Method 1: approximation calculation
            int storePosI = 0;
            double min_distanceI = 1000000;

            //Method 2: using API Location method
            int storePos = 0;
            float min_distance = 100000000;
            //Calculate nearest camera and store its index
            for (int i = 0; i < cameraArrayList.size(); i++) {
                /* * * * * * * * * * * * * * * * * * * * * * * *
                 * For measuring distance we consider in madrid: *
                 *           1 latitude degree -> 111km          *
                 *           1 longitude degree -> 85km          *
                 *    METHOD 1                                   *
                 * * * * * * * * * * * * * * * * * * * * * * * * */
                double distanceI = Math.pow((position.latitude - cameraArrayList.get(i).position.latitude) * 111, 2)
                        + Math.pow((position.longitude - cameraArrayList.get(i).position.longitude) * 85, 2);

                if(distanceI < min_distanceI){
                    min_distanceI = distanceI;
                    storePosI = i;
                }
                //Method 2: API method
                float[] results = new float[3];
                Location.distanceBetween(position.latitude, position.longitude, cameraArrayList.get(i).position.latitude, cameraArrayList.get(i).position.longitude, results);
                float distance = results[0];
                if (distance < min_distance) {
                    min_distance = distance;
                    storePos = i;
                }
            }
            mqttChannels.add(new MqttChannel(Integer.toString(channel.id), position, write_api_key, read_api_key, storePos));
            Log.e(TAG, "Channel "+channel.id+ " close to Camera "+cameraArrayList.get(storePos).name + " distance: "+ min_distance);
            Log.e(TAG, "Channel "+channel.id+ " close to Camera "+cameraArrayList.get(storePosI).name + " distanceI: "+ min_distanceI);
        }
        mainActivity.setMqttChannels(mqttChannels);
        mainActivity.startMqttService();
    }
}
