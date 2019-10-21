package dte.masteriot.mdp.emergencies.Model;

import com.google.android.gms.maps.model.LatLng;

public class Camera {
    public String URL;
    public String name;
    public LatLng position;
    public double valCont = -1;
    public void setValCont (int valCont){
        this.valCont = valCont;
    }
    public Camera(String name, String URL, LatLng position){
        this.name =  name;
        this.URL = URL;
        this.position = position;
    }
}
