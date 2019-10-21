package dte.masteriot.mdp.emergencies.Model;

import com.google.android.gms.maps.model.LatLng;

public class MqttChannel {

    public LatLng position;
    public String publishTopic;
    public String subscriptionTopic;
    public int associatedCamera;
    public MqttChannel(String channel_number, LatLng position, String publishAPIkey, String subscriberAPIkey, int associatedCamera){
        publishTopic =  "channels/" + channel_number + "/publish/fields/field1/" + publishAPIkey;
        subscriptionTopic =  "channels/" + channel_number + "/subscribe/fields/field1/" + subscriberAPIkey;
        this.position = position;
        this.associatedCamera = associatedCamera;
    }

}
