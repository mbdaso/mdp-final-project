package dte.masteriot.mdp.emergencies.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

import dte.masteriot.mdp.emergencies.Adapters.CameraArrayAdapter;
import dte.masteriot.mdp.emergencies.Model.Camera;
import dte.masteriot.mdp.emergencies.Model.MqttChannel;
import dte.masteriot.mdp.emergencies.R;

public class MainActivity extends AppCompatActivity {

    private TextView text;

    private CameraArrayAdapter cameraAdapter;

    //Camera variables
    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";
    private ArrayList<Camera> cameraArrayList;

    //MQTT variables
    private ArrayList<MqttChannel> mqttChannelArrayList;

    private static final String UserAPIKey = "O2LF267YHMV61A0N";
    private static final String MQTTAPIKey = "T4DBW5CS51EWBCGL";
  /*  private static final String UserAPIKey = "0IFUPHEW12KUX7JW";
    private static final String MQTTAPIKey = "ZX09Q7X687ORLM2I";
*/
    private final String serverUri = "tcp://mqtt.thingspeak.com:1883";

    private int numEmergencies = 0;
    private boolean[] firedEmer = {false, false, false, false}; //Emergencies fired in Madrid
    private MqttAndroidClient mqttAndroidClient;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            connectToMQTTChannels();
        }else {
            im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger
            DownloadCameraList task1 = new DownloadCameraList(this);
            task1.execute( URL_CAMERAS );
        }
        text.setText("Number of Emergencies:" + numEmergencies);
    }

    protected void onStart() {
        super.onStart();
        //It downloads the camera list from the predefined URL
    }
    protected void onStop() {
        super.onStop();
        try {
            mqttAndroidClient.close();
            mqttAndroidClient.disconnect();
            addToHistory("Disconnected from " + serverUri + " succesfully");
        }catch(Exception e){
            System.err.println(e);
        }
    }

    protected void onSaveInstanceState(Bundle outState){
        outState.putInt("numEmergencies", numEmergencies);
        outState.putParcelableArrayList("cameraArrayList", cameraArrayList);
        outState.putParcelableArrayList("mqttChannelArrayList", mqttChannelArrayList);
        super.onSaveInstanceState(outState);
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
                for(MqttChannel mqttChannel : mqttChannelArrayList){
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
        String[] topics = new String[mqttChannelArrayList.size()];
        int[] QoS;
        QoS = new int[mqttChannelArrayList.size()];
        int i = 0;
        for (MqttChannel channel : mqttChannelArrayList) {
            topics[i] = channel.subscriptionTopic;
            QoS[i] = 0;
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
                ImageLoader task = new ImageLoader(MainActivity.this);
                task.execute(position);
            }
        });
    }

    public void downloadJSONChannels(){
        DownloadJSONChannels task1 = new DownloadJSONChannels(this);
        final String URL_CHANNELS_JSON = "https://api.thingspeak.com/channels.json?api_key=" + UserAPIKey;
        task1.execute(URL_CHANNELS_JSON);
    }
}