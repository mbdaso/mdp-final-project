package dte.masteriot.mdp.emergencies.Model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class MqttChannel implements Parcelable {

    public LatLng position;
    public String publishTopic;
    public String subscriptionTopic;
    private Camera associatedCamera;
    private Double reportedValue;

    public Double getReportedValue() {
        return reportedValue;
    }

    public void setReportedValue(Double reportedValue) {
        this.reportedValue = reportedValue;
    }

    public MqttChannel(String channel_number, LatLng position, String publishAPIkey, String subscriberAPIkey){
        publishTopic =  "channels/" + channel_number + "/publish/fields/field1/" + publishAPIkey;
        subscriptionTopic =  "channels/" + channel_number + "/subscribe/fields/field1/" + subscriberAPIkey;
        this.position = position;
    }

    public void setAssociatedCamera(Camera associatedCamera) {
        this.associatedCamera = associatedCamera;
    }

    public Camera getAssociatedCamera() {
        return associatedCamera;
    }

    protected MqttChannel(Parcel in) {
        position = in.readParcelable(LatLng.class.getClassLoader());
        publishTopic = in.readString();
        subscriptionTopic = in.readString();
        associatedCamera = in.readParcelable(Camera.class.getClassLoader());
        reportedValue = in.readDouble();
    }

    public static final Creator<MqttChannel> CREATOR = new Creator<MqttChannel>() {
        @Override
        public MqttChannel createFromParcel(Parcel in) {
            return new MqttChannel(in);
        }

        @Override
        public MqttChannel[] newArray(int size) {
            return new MqttChannel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(position, i);
        parcel.writeString(publishTopic);
        parcel.writeString(subscriptionTopic);
        parcel.writeParcelable(associatedCamera, i);
        if(reportedValue !=null) {
            parcel.writeDouble(reportedValue);
        }
    }
}
