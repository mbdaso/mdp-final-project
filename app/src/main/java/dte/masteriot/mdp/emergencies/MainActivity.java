package dte.masteriot.mdp.emergencies;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //URL from which the list of cameras will be retrieved
    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";
    private TextView text;
    List<Camera> cameraArrayList = new ArrayList<Camera>();


    ListView lv;
    XmlPullParserFactory parserFactory;

    private ImageView im;

    int pos=0;


    //MQTT variables
    List<MqttChannel> mqttChannels = new ArrayList<>();
    private static final String UserAPIKey = "0IFUPHEW12KUX7JW";
    private static final String MQTTAPIKey = "ZX09Q7X687ORLM2I";
    final String serverUri = "tcp://mqtt.thingspeak.com:1883";
    final String URL_CHANNELS_JSON = "https://api.thingspeak.com/channels.json?api_key=" + UserAPIKey;

    String clientId = "Emergencies_collector1";
    JSONChannel[] channels = new JSONChannel[4];
    boolean[] firedEmer = {false, false, false, false}; //Emergencies fired in Madrid
    MqttAndroidClient mqttAndroidClient;

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
        DownloadWebPageTask task1 = new DownloadWebPageTask(), task2 = new DownloadWebPageTask();
        task1.execute( URL_CAMERAS );
    }



    void connectToMQTTChannels(){
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
                        cameraArrayList.get(mqttChannel.associatedCamera).setValCont(Integer.valueOf(payload)); //Set cont value
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

    void updateEmergencies(int nTopic, boolean emergency){
        firedEmer[nTopic] = emergency;
        int numEmergencies = 0;
        for (int i = 0; i<4; i++) {
            numEmergencies += firedEmer[i] ? 1 : 0;
        }
        text.setText(String.format("Number of Emergencies: %d", numEmergencies));

        // TODO add list modifier (putting background in red)

    }

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {

        private String contentType = "";
        Gson gson = new Gson();
        ArrayList<String> nameURLS_ArrayList = new ArrayList<>();
        ArrayList<String> camerasURLS_ArrayList = new ArrayList<>();
        ArrayList<LatLng> coorURLS_ArrayList = new ArrayList<>();
        @Override
        @SuppressWarnings( "deprecation" )
        protected String doInBackground(String... urls) {
            String response = "";

            HttpURLConnection urlConnection;
            try {
                URL url = new URL( urls[0] );
                urlConnection = (HttpURLConnection) url.openConnection();
                contentType = urlConnection.getContentType();
                InputStream is = urlConnection.getInputStream();
                if(contentType.contains("xml")) {
                    parserFactory = XmlPullParserFactory.newInstance();

                    XmlPullParser parser = parserFactory.newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(is, null);
                    String aux;
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        String elementName;
                        elementName = parser.getName();
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                if ("description".equals(elementName)) {
                                    String cameraURL = parser.nextText();
                                    cameraURL = cameraURL.substring(cameraURL.indexOf("http:"));
                                    cameraURL = cameraURL.substring(0, cameraURL.indexOf(".jpg") + 4);
                                    camerasURLS_ArrayList.add(cameraURL);
                                 //   response += cameraURL + "\n";
                                } else if ("Data".equals(elementName)) {
                                    aux = parser.getAttributeValue(null, "name");

                                    if (aux.equals("Nombre")) {
                                        String name;
                                        parser.nextTag();
                                        name = parser.nextText();
                                        Log.v("aux1", name);
                                        nameURLS_ArrayList.add(name);
                                    }

                                } else if ("coordinates".equals(elementName)) {
                                    String coorURL = parser.nextText();
                                    String lat = coorURL.substring((coorURL.indexOf(",")) + 1, coorURL.length() - 4);
                                    String lon = coorURL.substring(0, coorURL.indexOf(","));
                                    coorURLS_ArrayList.add(new LatLng(Double.valueOf(lat), Double.valueOf(lon)));

                                }
                                break;
                        }
                        eventType = parser.next();
                    }
                    for (int i = 0; i< nameURLS_ArrayList.size(); i++) {
                        cameraArrayList.add(new Camera(nameURLS_ArrayList.get(i), camerasURLS_ArrayList.get(i), coorURLS_ArrayList.get(i)));
                    }
                } else if (contentType.contains("json")){
                    channels = gson.fromJson(new InputStreamReader(is), JSONChannel[].class);
                    response = "GSON PARSED";
                }
            } catch (Exception e) {
                response = e.toString();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (contentType.contains("xml")) {
                lv = (ListView) findViewById(R.id.lv);
                ArrayAdapter arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_checked, nameURLS_ArrayList);
                lv.setAdapter(arrayAdapter);

                lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

                lv.setClickable(true);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                        Object o = lv.getItemAtPosition(position);
                        String str = (String) o;//As you are using Default String Adapter
                        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                        pos = position;
                        ImageLoader task = new ImageLoader();
                        task.execute(camerasURLS_ArrayList.get(position));

                    }
                });
                DownloadWebPageTask task1 = new DownloadWebPageTask();
                task1.execute( URL_CHANNELS_JSON);

            }else if(contentType.contains("json")) {
                for (JSONChannel channel : channels) {
                    String write_api_key = channel.api_keys[0].write_flag ? channel.api_keys[0].api_key : channel.api_keys[1].api_key;
                    String read_api_key = channel.api_keys[0].write_flag ? channel.api_keys[1].api_key : channel.api_keys[0].api_key;
                    LatLng position = new LatLng(Double.valueOf(channel.latitude), Double.valueOf(channel.longitude));
                    int storePos =  100000;
                    //Calculate nearest camera and store its index
                    for (int i = 0; i < cameraArrayList.size(); i++) {
                        double distance = Math.pow(position.latitude - cameraArrayList.get(i).position.latitude, 2) + Math.pow(position.longitude - cameraArrayList.get(i).position.longitude, 2);
                        if (distance < storePos) storePos = i;
                    }
                    mqttChannels.add(new MqttChannel(Integer.toString(channel.id), position, write_api_key, read_api_key, storePos));

                }

                connectToMQTTChannels();
            }
        }
    }

    class ImageLoader extends AsyncTask<String, Void, Bitmap>{

      //  ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            // TODO Auto-generated method stub

            String url = params[0];
            //Bitmap imagen = descargarImagen(url);

            URL imageUrl = null;
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
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            ImageView im = (ImageView)((AppCompatActivity) MainActivity.this).findViewById(R.id.imageView);
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
     /*   private Bitmap descargarImagen (String imageHttpAddress){
            URL imageUrl;
            Bitmap imagen = null;
            try{
                imageUrl = new URL(imageHttpAddress);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.connect();
                imagen = BitmapFactory.decodeStream(conn.getInputStream());
            }catch(IOException ex){
                ex.printStackTrace();
            }

            return imagen;
        }*/
    }
}