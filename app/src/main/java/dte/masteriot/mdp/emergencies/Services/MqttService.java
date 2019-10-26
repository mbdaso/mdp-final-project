package dte.masteriot.mdp.emergencies.Services;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

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
import java.util.UUID;

import dte.masteriot.mdp.emergencies.Activities.MainActivity;
import dte.masteriot.mdp.emergencies.Model.MqttChannel;

public class MqttService implements Parcelable {
    private static final String TAG = "Pepe";
    private final String MQTTAPIKey;
    private final String userAPIKey;

    private MainActivity mainActivity;
    private MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mqttConnectOptions;
    private String serverUri;
    private ArrayList<MqttChannel> mqttChannelArrayList;

    public MqttService(MainActivity mainActivity_,
                       String serverUri, String userAPIKey,
                       String MQTTAPIKey,
                       ArrayList<MqttChannel> mqttChannelArrayList){
        mainActivity = mainActivity_;
        this.serverUri = serverUri;
        this.userAPIKey = userAPIKey;
        this.MQTTAPIKey = MQTTAPIKey;
        this.mqttChannelArrayList = mqttChannelArrayList;
    }


    public static final Creator<MqttService> CREATOR = new Creator<MqttService>() {
        @Override
        public MqttService createFromParcel(Parcel in) {
            return new MqttService(in);
        }

        @Override
        public MqttService[] newArray(int size) {
            return new MqttService[size];
        }
    };

    private void subscribeToTopics(){
        String[] topics = new String[mqttChannelArrayList.size()];
        int[] QoS;
        QoS = new int[mqttChannelArrayList.size()];
        int i = 0;
        for (MqttChannel channel : mqttChannelArrayList) {
            Log.d(TAG, "Subscribing to ");
            topics[i] = channel.subscriptionTopic;
            QoS[i] = 0;
            i++;
        }
        try {
            mqttAndroidClient.subscribe(topics, QoS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to " + asyncActionToken.getTopics().length + " topics!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to subscribe");
                }

            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void start(){
        mqttAndroidClient = setupMqttClient();

        mqttConnectOptions = setupMqttOptions();

        this.connect();
    }

    public void connect() {
        try {
            System.out.println("Connecting to " + serverUri);
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
                    System.out.println("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private MqttConnectOptions setupMqttOptions() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);

        mqttConnectOptions.setUserName( "Emergencies_Collector" );
        mqttConnectOptions.setPassword( MQTTAPIKey.toCharArray() );
        return mqttConnectOptions;
    }

    private MqttAndroidClient setupMqttClient() {
        String clientId = UUID.randomUUID().toString();
        Log.d(TAG, "Connecting with clientId 0" + clientId);
        final MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(mainActivity.getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Log.d(TAG, "Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopics();
                } else {
                    Log.d(TAG, "Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message){
                String payload = new String(message.getPayload());
                Log.d(TAG, "Incoming message: " + payload);
                int i = 0;
                for(MqttChannel mqttChannel : mqttChannelArrayList){
                    if(mqttChannel.subscriptionTopic.equals(topic)){
                        mainActivity.updateEmergencies(i, Double.valueOf(payload) >= 100);
                        mainActivity.setContaminationValue(mqttChannel.associatedCamera, Double.valueOf(payload));
                        break;
                    }
                    i++;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
        return mqttAndroidClient;
    }

    public void stop() throws MqttException{
        mqttAndroidClient.close();
        mqttAndroidClient.disconnect();
        Log.d(TAG, "Disconnected from " + serverUri + " succesfully");
    }

    private MqttService(Parcel in) {
        MQTTAPIKey = in.readString();
        userAPIKey = in.readString();
        serverUri = in.readString();
        mqttChannelArrayList = in.createTypedArrayList(MqttChannel.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(MQTTAPIKey);
        dest.writeString(userAPIKey);
        dest.writeString(serverUri);
        dest.writeTypedList(mqttChannelArrayList);
    }
}
