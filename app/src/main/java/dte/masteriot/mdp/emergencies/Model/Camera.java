package dte.masteriot.mdp.emergencies.Model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class Camera implements Parcelable {
    public String URL;
    public String name;
    public LatLng position;
    public LatLng channelPosition;
    public double valCont = -1;


    public void setValCont (double valCont){
        this.valCont = valCont;
    }

    public Camera(String name, String URL, LatLng position){
        this.name =  name;
        this.URL = URL;
        this.position = position;

        Log.d("Pepe", name + " " + this.URL + " " + position.toString());
    }

    protected Camera(Parcel in) {
        URL = in.readString();
        name = in.readString();
        position = in.readParcelable(LatLng.class.getClassLoader());
        valCont = in.readDouble();
    }

    public static final Creator<Camera> CREATOR = new Creator<Camera>() {
        @Override
        public Camera createFromParcel(Parcel in) {
            return new Camera(in);
        }

        @Override
        public Camera[] newArray(int size) {
            return new Camera[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(URL);
        parcel.writeString(name);
        parcel.writeParcelable(position, i);
        parcel.writeDouble(valCont);
    }
}
