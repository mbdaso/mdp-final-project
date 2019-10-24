package dte.masteriot.mdp.emergencies.Activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import dte.masteriot.mdp.emergencies.Adapters.CameraArrayAdapter;
import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.Model.JSONChannel;
import dte.masteriot.mdp.emergencies.Model.MqttChannel;
import dte.masteriot.mdp.emergencies.R;

public class MainActivity extends AppCompatActivity {

    private TextView text;
    private ImageView im;

    CameraArrayAdapter cameraAdapter;

    //Camera variables
    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";
    ArrayList<Camera> cameraArrayList = new ArrayList<>();

    //MQTT variables
    List<MqttChannel> mqttChannels = new ArrayList<>();
    private static final String UserAPIKey = "0IFUPHEW12KUX7JW";
    private static final String MQTTAPIKey = "ZX09Q7X687ORLM2I";
    final String serverUri = "tcp://mqtt.thingspeak.com:1883";
    final String URL_CHANNELS_JSON = "https://api.thingspeak.com/channels.json?api_key=" + UserAPIKey;

    boolean[] firedEmer = {false, false, false, false}; //Emergencies fired in Madrid
    MqttAndroidClient mqttAndroidClient;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main);
        //It gets the UI elements of the main activity
        text = findViewById(R.id.textView);
        text.setText("Number of Emergencies: 0");
        im = findViewById(R.id.imageView);
        im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger

        //It downloads the camera list from the predefined URL
        DownloadCameraList task1 = new DownloadCameraList(this);
        task1.execute( URL_CAMERAS );
    }



    public void connectToMQTTChannels(){
        String clientId = "Emergencies_collector";

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopics();
                } else {
                    addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message){
                String payload = new String(message.getPayload());
                addToHistory("Incoming message: " + payload);
                int i = 0;
                for(MqttChannel mqttChannel : mqttChannels){
                    if(mqttChannel.subscriptionTopic.equals(topic)){
                        updateEmergencies(i, Double.valueOf(payload) >= 100);
                        cameraArrayList.get(mqttChannel.associatedCamera).setValCont(Double.valueOf(payload)); //Set cont value
                        cameraAdapter.notifyDataSetChanged();
                        break;
                    }
                    i++;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        // mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setCleanSession(true);

        mqttConnectOptions.setUserName( "Emergencies_Collector" );
        mqttConnectOptions.setPassword( MQTTAPIKey.toCharArray() );

        try {
            addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private void addToHistory(String mainText){
        System.out.println("LOG: " + mainText);
    }

    public void subscribeToTopics(){
        String[] topics = new String[mqttChannels.size()];
        int[] QoS;
        QoS = new int[]{0, 0, 0, 0};
        int i = 0;
        for (MqttChannel channel : mqttChannels) {
            topics[i] = channel.subscriptionTopic;
            i++;
        }
            try {
                mqttAndroidClient.subscribe(topics, QoS, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        addToHistory("Subscribed to " + asyncActionToken.getTopics().length + " topics!");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        addToHistory("Failed to subscribe");
                    }

                });

            } catch (MqttException ex) {
                System.err.println("Exception whilst subscribing");
                ex.printStackTrace();
            }
        }

    @SuppressLint("DefaultLocale")
    void updateEmergencies(int nTopic, boolean emergency){
        firedEmer[nTopic] = emergency;
        int numEmergencies = 0;
        for (int i = 0; i<4; i++) {
            numEmergencies += firedEmer[i] ? 1 : 0;
        }
        text.setText(String.format("Number of Emergencies: %d", numEmergencies));
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadWebPageTask extends AsyncTask<String, Void, Void> {
        Gson gson = new Gson();
        JSONChannel[] channels = new JSONChannel[4];

        @Override
        protected Void doInBackground(String... urls) {
            HttpURLConnection urlConnection;

            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream is = urlConnection.getInputStream();

                channels = gson.fromJson(new InputStreamReader(is), JSONChannel[].class);
                System.out.println("GSON PARSED");
            } catch (Exception e) {
                System.err.println(e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            for (JSONChannel channel : channels) {
                String write_api_key = channel.api_keys[0].write_flag ? channel.api_keys[0].api_key : channel.api_keys[1].api_key;
                String read_api_key = channel.api_keys[0].write_flag ? channel.api_keys[1].api_key : channel.api_keys[0].api_key;
                LatLng position = new LatLng(Double.valueOf(channel.latitude), Double.valueOf(channel.longitude));
                int storePos = 0;
                double min_distance = 1000000;
                //Calculate nearest camera and store its index
                for (int i = 0; i < cameraArrayList.size(); i++) {
                    /* * * * * * * * * * * * * * * * * * * * * * * *
                     * For measuring distance we consider in madrid: *
                     *           1 latitude degree -> 111km          *
                     *           1 longitude degree -> 85km          *
                     * * * * * * * * * * * * * * * * * * * * * * * * */
                    double distance = Math.pow((position.latitude - cameraArrayList.get(i).position.latitude) * 111, 2)
                            + Math.pow((position.longitude - cameraArrayList.get(i).position.longitude) * 85, 2);
                    if (distance < min_distance) {
                        min_distance = distance;
                        storePos = i;
                    }
                }
                mqttChannels.add(new MqttChannel(Integer.toString(channel.id), position, write_api_key, read_api_key, storePos));
            }
            connectToMQTTChannels();
        }
    }
    public void setCameraArrayList(ArrayList<Camera> cameraArrayList){
        this.cameraArrayList = cameraArrayList;
    }
    public void paintCameraList(){
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
                MainActivity.ImageLoader task = new MainActivity.ImageLoader();
                task.execute(position);
            }
        });
        MainActivity.DownloadWebPageTask task1 = new MainActivity.DownloadWebPageTask();
        task1.execute( URL_CHANNELS_JSON);
    }
    @SuppressLint("StaticFieldLeak")
    private class ImageLoader extends AsyncTask<Integer, Void, Bitmap>{
        int pos;

        @Override
        protected Bitmap doInBackground(Integer... params) {
            pos = params[0];
            String url = cameraArrayList.get(pos).URL;

            URL imageUrl;
            Bitmap imagen = null;
            try{
                imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.connect();
                imagen = BitmapFactory.decodeStream(conn.getInputStream());
            }catch(IOException ex){
                ex.printStackTrace();
            }

            return imagen;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            im.setImageBitmap(result);
            im.setOnClickListener( new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent (v.getContext(), MapsActivity.class);
                    Bundle args = new Bundle();
                    args.putParcelable("coordinates", cameraArrayList.get(pos).position);
                    args.putString("cameraName", cameraArrayList.get(pos).name);
                    args.putDouble("valCont", cameraArrayList.get(pos).valCont);
                    intent.putExtra("bundle",args);
                    startActivity(intent);
                }
            });
        }
    }
}