package dte.masteriot.mdp.emergencies;

import com.google.android.gms.maps.model.LatLng;

class MqttChannel {

    LatLng position;
    String publishTopic;
    String subscriptionTopic;
    int associatedCamera;
    MqttChannel(String channel_number, LatLng position, String publishAPIkey, String subscriberAPIkey, int associatedCamera){
        publishTopic =  "channels/" + channel_number + "/publish/fields/field1/" + publishAPIkey;
        subscriptionTopic =  "channels/" + channel_number + "/subscribe/fields/field1/" + subscriberAPIkey;
        this.position = position;
        this.associatedCamera = associatedCamera;
    }

}
