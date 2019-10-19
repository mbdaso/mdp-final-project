package dte.masteriot.mdp.emergencies;

import com.google.android.gms.maps.model.LatLng;

public class Camera {
    String URL;
    String name;
    LatLng position;
    double valCont = -1;
    void setValCont (int valCont){
        this.valCont = valCont;
    }
    Camera(String name, String URL, LatLng position){
        this.name =  name;
        this.URL = URL;
        this.position = position;
    }
}
