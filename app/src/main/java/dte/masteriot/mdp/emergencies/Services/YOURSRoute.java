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
    * http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&flat=52.215676&flon=5.963946&tlat=52.2573&tlon=6.1799&v=motorcar&fast=1&layer=mapnik&instructions=1
    * http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&
    flat=52.215676&flon=5.963946&tlat=52.2573&tlon=6.1799&v=motorcar&fast=1&layer=mapnik&instructions=1
    * */
    /*Esta url va bien
     * http://www.yournavigation.org/api/1.0/gosmore.php?format=kml&%20flat=40.452162&flon=-3.725778&tlat=52.2573&tlon=6.1799&v=motorcar&fast=1&layer=mapnik&instructions=1
     * */

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
