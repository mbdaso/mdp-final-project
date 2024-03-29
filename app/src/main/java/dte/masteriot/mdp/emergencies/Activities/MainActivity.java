package dte.masteriot.mdp.emergencies.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import dte.masteriot.mdp.emergencies.Adapters.CameraArrayAdapter;
import dte.masteriot.mdp.emergencies.AsyncTasks.DownloadCameraList;
import dte.masteriot.mdp.emergencies.AsyncTasks.DownloadJSONChannels;
import dte.masteriot.mdp.emergencies.AsyncTasks.ImageLoader;
import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.Model.MqttChannel;
import dte.masteriot.mdp.emergencies.R;
import dte.masteriot.mdp.emergencies.Services.MqttService;

public class MainActivity extends AppCompatActivity {

    // Keys for accessing ThingSpeak:(Cristina)
    private static final String UserAPIKey = "O2LF267YHMV61A0N";
    private static final String MQTTAPIKey = "T4DBW5CS51EWBCGL";
    // Keys for accessing ThingSpeak:
   /* private static final String UserAPIKey = "0IFUPHEW12KUX7JW";
    private static final String MQTTAPIKey = "ZX09Q7X687ORLM2I";
    */
    boolean sortedByCont = false;

    private TextView text;
    private CameraArrayAdapter cameraAdapter;
    //Camera variables
    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";
    private ArrayList<Camera> cameraArrayList;

    //MQTT variables
    private ArrayList<MqttChannel> mqttChannelArrayList;

    private MqttService mqttService;

    private int numEmergencies = 0;
    private boolean[] firedEmer = {false, false, false, false}; //Emergencies fired in Madrid
    //private Bitmap lastImageBitmap;
    private int lastImagePos = -1;
    private static final int START_MAPS_ACTIVITY = 7;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        addToHistory("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //It gets the UI elements of the main activity
        text = findViewById(R.id.textView);
        if (savedInstanceState == null) {
            DownloadCameraList task1 = new DownloadCameraList(this);
            task1.execute(URL_CAMERAS);
        }
    }

    protected void onPause() {
        super.onPause();
        try {
            if(mqttService != null)
                mqttService.stop();
        } catch (MqttException e) {
            System.err.println("Exception in onPause -> stop: " + e.getMessage());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        addToHistory("onSavedInstanceState");
        numEmergencies = savedInstanceState.getInt("numEmergencies");
        cameraArrayList = savedInstanceState.getParcelableArrayList("cameraArrayList");
        mqttChannelArrayList = savedInstanceState.getParcelableArrayList("mqttChannelArrayList");
        firedEmer = savedInstanceState.getBooleanArray("firedEmer");
        mqttService = savedInstanceState.getParcelable("mqttService");
        sortedByCont = savedInstanceState.getBoolean("sortedByCont");
        printCameraList();
        startMqttService();

        lastImagePos = savedInstanceState.getInt("lastImagePos", -1);
        if (lastImagePos != -1) {
            ImageLoader task = new ImageLoader(this);

            task.execute(lastImagePos);
        }
    }

    protected void onResume() {
        super.onResume();
        if (mqttService != null) {
            try {
                mqttService.connect();
            } catch (Exception e) {
                System.err.println("Exception in onResume: " + e.getMessage());
            }
        }
        text.setText(String.format("Number of Emergencies:%d", numEmergencies));

    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        addToHistory("onSavedInstanceState");
        outState.putInt("numEmergencies", numEmergencies);
        outState.putParcelableArrayList("cameraArrayList", cameraArrayList);
        outState.putParcelableArrayList("mqttChannelArrayList", mqttChannelArrayList);
        outState.putBooleanArray("firedEmer", firedEmer);
        outState.putParcelable("mqttService", mqttService);
        outState.putInt("lastImagePos", lastImagePos);
        outState.putBoolean("sortedByCont", sortedByCont);

        super.onSaveInstanceState(outState);
    }

    private void addToHistory(String mainText){
        System.out.println("LOG: " + mainText);
    }

    //Updates number of emergencies detected
    @SuppressLint("DefaultLocale")
    public void updateEmergencies(int nTopic, double value){
        boolean emergency = value > 100;
        firedEmer[nTopic] = emergency;
        mqttChannelArrayList.get(nTopic).setReportedValue(value);
        numEmergencies = 0;
        for (int i = 0; i<4; i++) {
            numEmergencies += firedEmer[i] ? 1 : 0;
        }
        text.setText(String.format("Number of Emergencies: %d", numEmergencies));
    }

    @SuppressLint("StaticFieldLeak")
    public void setCameraArrayList(ArrayList<Camera> cameraArrayList){
        this.cameraArrayList = cameraArrayList;
    }

    public ArrayList<Camera> getCameraArrayList(){return cameraArrayList;}

    public void setMqttChannels(ArrayList<MqttChannel> mqttChannelArrayList){
        this.mqttChannelArrayList = mqttChannelArrayList;
    }

    public void printCameraList(){
        final ListView lv;

        lv = findViewById(R.id.lv);
        cameraAdapter = new CameraArrayAdapter(MainActivity.this, cameraArrayList);
        lv.setAdapter(cameraAdapter);

        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setClickable(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Camera o = (Camera) lv.getItemAtPosition(position);
                String str = o.name;//As you are using Default String Adapter
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                ImageLoader task = new ImageLoader(MainActivity.this); //ImageLoader task
                task.execute(position); //position = position in ListView
            }
        });
    }

    public void downloadJSONChannels(){
        DownloadJSONChannels task1 = new DownloadJSONChannels(this);
        final String URL_CHANNELS_JSON = "https://api.thingspeak.com/channels.json?api_key=" + UserAPIKey;
        task1.execute(URL_CHANNELS_JSON);
    }

    public void setImagePosAndListener(Bitmap bitmap, final int pos) {
        ImageView im = findViewById(R.id.imageView);
        im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger
        //lastImageBitmap = bitmap;
        lastImagePos = pos;
        im.setImageBitmap(bitmap);
        im.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent (v.getContext(), MapsActivity.class);
                Bundle args = new Bundle();
                args.putParcelable("coordinates", getCameraArrayList().get(pos).position);
                args.putString("cameraName", getCameraArrayList().get(pos).name);
                args.putDouble("valCont", getCameraArrayList().get(pos).valCont);
                //Enhancement: to add the marker of the channel
                args.putParcelable("closestChannel", mqttChannelArrayList.get(getCameraArrayList().get(pos).getClosestChannel()));
                intent.putExtra("bundle",args);
                startActivityForResult(intent, START_MAPS_ACTIVITY);
            }
        });
    }

    public void startMqttService() {
        String serverUri = "tcp://mqtt.thingspeak.com:1883";
        if(mqttService == null)
            mqttService = new MqttService(this, serverUri, UserAPIKey, MQTTAPIKey, mqttChannelArrayList);
        mqttService.start();
    }

    public void setContaminationValue(Camera associatedCamera, Double value) {

        for(Camera camera : cameraArrayList)
            if (associatedCamera.name.equals(camera.name)) {
                camera.setValCont(value); //Set pollution value
                cameraAdapter.notifyDataSetChanged();
                break;
            }

        if(sortedByCont) {
            Collections.sort(cameraArrayList, new Comparator<Camera>() {
                @Override
                public int compare(Camera camera, Camera t1) {
                    if (camera.valCont > t1.valCont) return -1;
                    else if (camera.valCont < t1.valCont) return 1;
                    else return 0;
                }
            });
            sortedByCont = true;
            cameraAdapter.notifyDataSetChanged();
        }
    }

    //Implements alphabetic sorting
    public void sort_by_alphabetic(View view) {
        Collections.sort(cameraArrayList, new Comparator<Camera>() {
            @Override
            public int compare(Camera camera, Camera t1) {
                return (camera.name.compareTo(t1.name));
            }
        });
        cameraAdapter.notifyDataSetChanged();

    }
    //Implements pollution value sorting
    public void sort_by_emergencies(View view) {
        Collections.sort(cameraArrayList, new Comparator<Camera>() {
            @Override
            public int compare(Camera camera, Camera t1) {
                if(camera.valCont > t1.valCont) return -1;
                else if (camera.valCont < t1.valCont) return 1;
                else return 0;
            }
        });
        sortedByCont = true;
        cameraAdapter.notifyDataSetChanged();
    }
}