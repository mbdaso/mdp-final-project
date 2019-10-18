package dte.masteriot.mdp.emergencies;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/* Cuando se abre el mapa, se solicita la ruta desde currentPosition a la cámara que se ha
seleccionado.
MapsActivity pasa las coordenadas destino a get_route
Ahora hay que:
    1. Usar el fichero KML como inputStream

*/

public class YOURSRoute {
    String TAG = "MapsActivity";

    private LatLng currentPosition = new LatLng(40.389877, -3.629053);

    private String BaseURL = "http://www.yournavigation.org/api/1.0/gosmore.php";
    //TODO: ask for transport type
    private String transport_type = "v=foot";
    private String flat; // latitude of the starting location.
    private String flon; // longitude of the starting location.
    private String tlat; // latitude of the end location.
    private String tlon; // longitude of the end location.
    private List<LatLng> route;
    /*PROBANDO RUTAS
    *
    * Origen 40.378729, -3.613838
    * Destino 40.021892, -3.626836 (al sur)
    * (longitud, latitud)
    * */

    /*fast = 1 selects the fastest route, 0 the shortest route. Default is: 1.
layer = determines which Gosmore instance is used to calculate the route. Provide mapnik for normal routing using car, bicycle or foot. Provide cn for using bicycle routing using cycle route networks only. Default is: mapnik.
format = specifies the format (KML or geoJSON) in which the route result is being sent back to the client. This can either be kml or geojson. Default is: kml.
geometry = enables/disables adding the route geometry in the output. Options are 1 to include the route geometry in the output or 0 to exclude it. Default is: 1.
distance = specifies which algorithm is used to calculate the route distance. This returned value is always in metric units (km), independent of the chosen language. Options are v for Vicenty, gc for simplified Great Circle, h for Haversine Law, cs for Cosine Law. Default is: v. Implemented using the geography class from Simon Holywell.
instructions = enbles/disables adding driving instructions in the output. Options are 1 to include driving directions, 0 to disable driving directions. Default is 0.
lang = specifies the language code in which the routing directions are returned. Distances in the instructions are given in metric or imperial units depending on the chosen language. Default is en_US (English). Options are:*/



    /*
    * http://www.yournavigation.org/api/1.0/gosmore.php?flat=-3.613838&flon=40.378729&tlat=-3.626836&tlon=40.021892
    *
    * */


    public List<LatLng> draw_route(LatLng source, LatLng dest) {

        String RequestURLString = "http://www.yournavigation.org/api/1.0/gosmore.php?" +
                "format=kml&flat=" + source.latitude + "&flon=" + source.longitude +
                "&tlat="+ dest.latitude + "&tlon=" + dest.longitude +
                "v=motorcar&fast=1&layer=mapnik&instructions=1";
        Log.d(TAG, RequestURLString);

        //URL RequestURL = new URL(RequestURLString);
/*
        //Descargar fichero de internet
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            InputStream is =
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);
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
                        }
                        else if ("Data".equals(elementName)){
                            Log.d(TAG, parser.getAttributeValue(null, "name"));
                            if ("Nombre".equals(parser.getAttributeValue(null, "name"))){
                                parser.nextTag();
                                cameraNames.add(parser.nextText());
                            }
                        }
                        break;
                } //switch
                eventType = parser.next();
            } //while
        } //Try
        catch (Exception e){
            Log.d(TAG, "Exception");
            String err = (e.getMessage()==null)?"SD Card failed":e.getMessage();
            Log.d("sdcard-err2:",err);
        } //Catch

*/

        return route;
    }
}

