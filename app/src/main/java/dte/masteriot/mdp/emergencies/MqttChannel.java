package dte.masteriot.mdp.emergencies;

import com.google.android.gms.maps.model.LatLng;

class MqttChannel {

    LatLng position;
    String publishTopic;
    String subscriptionTopic;

    MqttChannel(String channel_number, LatLng position0, String publishAPIkey, String subscriberAPIkey){
        publishTopic =  "channels/" + channel_number + "/publish/fields/field1/" + publishAPIkey;
        subscriptionTopic =  "channels/" + channel_number + "/subscribe/fields/field1/" + subscriberAPIkey;
        position = position0;
    }
}
