package dte.masteriot.mdp.emergencies.Services;


import android.util.Log;
import com.google.android.gms.maps.model.LatLng;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

    private URL buildRouteURL(LatLng source, LatLng dest)
            throws UnsupportedEncodingException, MalformedURLException {
        //TODO: Por qué los parámetros longitud, latitud van al revés?
        URL builtURL;
        try {
            String urlString = "http://www.yournavigation.org/api/1.0/gosmore.php?format=kml" +
                    "&flat=" + source.latitude + "&flon=" + source.longitude +
                    "&tlat=" + dest.latitude + "&tlon=" + dest.longitude +
                    "&v=motorcar&fast=1&layer=mapnik&instructions=1";
            builtURL = new URL(urlString);
            Log.d(TAG, "URL de la ruta: " +builtURL);
        }
        catch(Exception e){
            Log.d(TAG, "No se ha podido construir la URL: " + e.getMessage());
            builtURL = new URL("");
        }
        return builtURL;
    }

    private ArrayList<LatLng> getRouteFromXML(InputStream is){
        //XMLPullParser
        ArrayList<LatLng> route = new ArrayList<>();
        try {
            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);
            String aux;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String elementName = null;
                elementName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("coordinates".equals(elementName)) {
                            String coordinates = parser.nextText();
                            route = parseCoordinatesString(coordinates);
                        }
                        break;
                }
                eventType = parser.next();
            }

        } catch (Exception e) {
            Log.d(TAG, "getRouteFromXML: " + e.getMessage());
        }
        return route;
    }

    private ArrayList<LatLng> parseCoordinatesString(String coordinates) {
        ArrayList<LatLng> route = new ArrayList<>();
        String[] lines = coordinates.split("\n");
        //Log.d(TAG, "n lines: " + lines.length);
        for(String line: lines){
            // Aquí dice que solo hay una linea
            line = line.trim();
            if(line.length() > 0) {
                String[] latLng = line.split(","); //Este split no hace falta - si lo quitamos, hay que intercambiar las posiciones de latLng al construir point
                LatLng point = new LatLng(Double.parseDouble(latLng[1]),
                        Double.parseDouble(latLng[0]));
                try{
                    route.add(point);
                }
                catch(Exception e){
                    Log.d(TAG, "Exception: " +  e.getMessage());
                }
            }
        }
        return route;
    }
    public List<LatLng> getRoute(LatLng src, LatLng dst) throws java.io.IOException {
            URL apiURL = buildRouteURL(src, dst);
            HttpURLConnection urlConnection = (HttpURLConnection) apiURL.openConnection();
            InputStream is = urlConnection.getInputStream();
            return getRouteFromXML(is);
    }

}
