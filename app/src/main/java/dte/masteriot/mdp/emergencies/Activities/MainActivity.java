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

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import dte.masteriot.mdp.emergencies.Adapters.CameraArrayAdapter;
import dte.masteriot.mdp.emergencies.AsyncTasks.DownloadCameraList;
import dte.masteriot.mdp.emergencies.AsyncTasks.DownloadJSONChannels;
import dte.masteriot.mdp.emergencies.AsyncTasks.ImageLoader;
import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.Model.MqttChannel;
import dte.masteriot.mdp.emergencies.R;
import dte.masteriot.mdp.emergencies.Services.MqttService;

public class MainActivity extends AppCompatActivity {
    private TextView text;
    private CameraArrayAdapter cameraAdapter;
    //Camera variables
    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";
    private ArrayList<Camera> cameraArrayList;

    //MQTT variables
    private ArrayList<MqttChannel> mqttChannelArrayList; //TODO:
    // ivan/cristina (?)
    private static final String UserAPIKey = "O2LF267YHMV61A0N";
    private static final String MQTTAPIKey = "T4DBW5CS51EWBCGL";

  /*ivan/cristina (?)
  private static final String UserAPIKey = "0IFUPHEW12KUX7JW";
  private static final String MQTTAPIKey = "ZX09Q7X687ORLM2I";
*/
    //Martín
    /*private static final String UserAPIKey = "JI1AKBOFIB3AKH92";
    private static final String MQTTAPIKey = "A0ECZ80BBI8FKPPB";
    */
    private final String serverUri = "tcp://mqtt.thingspeak.com:1883";
    private MqttService mqttService;

    private int numEmergencies = 0;
    private boolean[] firedEmer = {false, false, false, false}; //Emergencies fired in Madrid
    private Bitmap lastImageBitmap;
    private int lastImagePos = -1;
    private static final int START_MAPS_ACTIVITY = 7;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        addToHistory("onCreate");
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main);
        //It gets the UI elements of the main activity
        text = findViewById(R.id.textView);
        ImageView im = findViewById(R.id.imageView);
        if (savedInstanceState != null) {
            numEmergencies = savedInstanceState.getInt("numEmergencies");
            cameraArrayList = savedInstanceState.getParcelableArrayList("cameraArrayList");
            mqttChannelArrayList = savedInstanceState.getParcelableArrayList("mqttChannelArrayList");
            printCameraList();
            //TODO: guardar mqttService en un bundle
            startMqttService();
            if(lastImagePos != -1){ //Recuperar bitmap del bundle
//                Bitmap bitmap=getArguments().getByteArray("bitByte");
//                return new AlertDialog().Builder(getActivity());
                //https://stackoverflow.com/questions/33797036/how-to-send-the-bitmap-into-bundle
            }
        }else {
            im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger
            DownloadCameraList task1 = new DownloadCameraList(this);
            task1.execute( URL_CAMERAS );
        }
        text.setText("Number of Emergencies:" + numEmergencies);
    }

    protected void onStop() {
        super.onStop();
        try {
            mqttService.stop();
        }
        catch(Exception e){
            System.err.println(e);
        }
    }

    protected void onSaveInstanceState(Bundle outState){
        addToHistory("onSavedInstanceState");
        outState.putInt("numEmergencies", numEmergencies);
        outState.putParcelableArrayList("cameraArrayList", cameraArrayList);
        outState.putParcelableArrayList("mqttChannelArrayList", mqttChannelArrayList);
        super.onSaveInstanceState(outState);
    }

    private void addToHistory(String mainText){
        System.out.println("LOG: " + mainText);
    }

    @SuppressLint("DefaultLocale")
    public void updateEmergencies(int nTopic, boolean emergency){
        firedEmer[nTopic] = emergency;
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
                task.execute(position); //position = posicion en ListView
            }
        });
    }

    public void downloadJSONChannels(){
        DownloadJSONChannels task1 = new DownloadJSONChannels(this);
        final String URL_CHANNELS_JSON = "https://api.thingspeak.com/channels.json?api_key=" + UserAPIKey;
        task1.execute(URL_CHANNELS_JSON);
    }

    public void setImageBitmapAndListener(Bitmap bitmap, final int pos) {
        ImageView im = findViewById(R.id.imageView);
        im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger
        lastImageBitmap = bitmap;
        lastImagePos = pos;
        im.setImageBitmap(bitmap);
        im.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent (v.getContext(), MapsActivity.class);
                Bundle args = new Bundle();
                args.putParcelable("coordinates", getCameraArrayList().get(pos).position);
                args.putString("cameraName", getCameraArrayList().get(pos).name);
                args.putDouble("valCont", getCameraArrayList().get(pos).valCont);
                intent.putExtra("bundle",args);
                startActivityForResult(intent, START_MAPS_ACTIVITY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        addToHistory("onActivityResult");
        // Check which request we're responding to
        if (requestCode == START_MAPS_ACTIVITY) {
            addToHistory("requestcode = start maps activity");
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                try {
                    mqttService.connect();
                }
                catch (Exception e){
                    addToHistory("Exception in onActivityResult: " + e.getMessage());
                }
            }
        }
    }

    public void startMqttService() {
        mqttService = new MqttService(this, serverUri, UserAPIKey, MQTTAPIKey, mqttChannelArrayList);
        mqttService.start();
    }

    public void setContaminationValue(int associatedCamera, Double value) {
        cameraArrayList.get(associatedCamera).setValCont(value); //Set cont value
        cameraAdapter.notifyDataSetChanged();
    }
}