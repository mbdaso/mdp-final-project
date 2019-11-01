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

    ArrayList<MqttChannel> mqttChannels = new ArrayList<MqttChannel>();
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

            mqttChannels.add(new MqttChannel(Integer.toString(channel.id), position, write_api_key, read_api_key));
        }

        //Now associate each camera to its closest channel
        for (Camera camera : cameraArrayList){
            float min_distance_channel = 10000000;
            for(MqttChannel mqttChannel : mqttChannels){
           /*     if(camera.getClosestChannel() == -1)
                    camera.setClosestChannel(mqttChannels.indexOf(mqttChannel));
                else {*/
                    float[] results = new float[3];
                    Location.distanceBetween(mqttChannel.position.latitude, mqttChannel.position.longitude,
                            camera.position.latitude, camera.position.longitude, results);
                    if (results[0] < min_distance_channel) {
                        min_distance_channel = results[0];
                        camera.setClosestChannel(mqttChannels.indexOf(mqttChannel));
                    }
             //   }

                if(mqttChannel.getAssociatedCamera() == null)
                    mqttChannel.setAssociatedCamera(camera);
                else{
                    float[] newDistanceToCamera = new float[3];
                    Location.distanceBetween(mqttChannel.position.latitude, mqttChannel.position.longitude,
                            camera.position.latitude, camera.position.longitude, newDistanceToCamera);

                    float[] currentDistanceToCamera = new float[3];
                    Location.distanceBetween(mqttChannel.getAssociatedCamera().position.latitude, mqttChannel.getAssociatedCamera().position.longitude,
                            mqttChannel.position.latitude, mqttChannel.position.longitude, currentDistanceToCamera);
                    if(newDistanceToCamera[0] < currentDistanceToCamera[0])
                        mqttChannel.setAssociatedCamera(camera);
                }
                /* * * * * * * * * * * * * * * * * * * * * * * *
                 * For measuring distance we consider in madrid: *
                 *           1 latitude degree -> 111km          *
                 *           1 longitude degree -> 85km          *
                 *    METHOD 1                                   *
                 * * * * * * * * * * * * * * * * * * * * * * * * */
                //Old method to get distance
                /*double distanceI = Math.pow((mqttChannel.position.latitude - camera.position.latitude) * 111, 2)
                        + Math.pow((mqttChannel.position.longitude - camera.position.longitude) * 85, 2);


                float min_distance = 100000000;
                //Method 2: API method
                float[] results = new float[3];
                Location.distanceBetween(mqttChannel.position.latitude, mqttChannel.position.longitude, camera.position.latitude, camera.position.longitude, results);
                float distance = results[0];
                if (distance < min_distance) {
                    min_distance = distance;
                }*/
            }
        }
        for(MqttChannel mqttChannel : mqttChannels)
            Log.d(TAG, "Channel "+ mqttChannels.indexOf(mqttChannel)+ " close to Camera "+ mqttChannel.getAssociatedCamera().name);

        mainActivity.setMqttChannels(mqttChannels);
        mainActivity.startMqttService();
    }
}
