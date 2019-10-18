package dte.masteriot.mdp.emergencies;

import com.google.android.gms.maps.model.LatLng;

public class MQTTchannel {

    LatLng position;
    String publishTopic;
    String subscriptionTopic;

    public MQTTchannel(String channel_number, LatLng position0, String publishAPIkey, String subscriberAPIkey){
        publishTopic =  "paho.mqtt.java.example.channels/" + channel_number + "/publish/fields/field1/" + publishAPIkey;
        subscriptionTopic =  "paho.mqtt.java.example.channels/" + channel_number + "/publish/fields/field1/" + subscriberAPIkey;
        position = position0;
    }
}
