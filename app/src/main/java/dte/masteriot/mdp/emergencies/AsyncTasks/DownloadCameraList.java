package dte.masteriot.mdp.emergencies.AsyncTasks;

import android.os.AsyncTask;
import android.util.Log;


import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import dte.masteriot.mdp.emergencies.Activities.MainActivity;
import dte.masteriot.mdp.emergencies.Model.Camera;

/*AsyncTask to retrieve the list of cameras*/
public class DownloadCameraList extends AsyncTask<String, Void, Void> {
    MainActivity mainActivity;

    public DownloadCameraList(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }
    private String contentType = "";

    ArrayList<String> nameURLS_ArrayList = new ArrayList<>();
    ArrayList<String> camerasURLS_ArrayList = new ArrayList<>();
    ArrayList<LatLng> coorURLS_ArrayList = new ArrayList<>();
    ArrayList<Camera> cameraArrayList = new ArrayList<>();
    XmlPullParserFactory parserFactory;

    @Override
    protected Void doInBackground(String... urls) {
        HttpURLConnection urlConnection;

        try {
            //It gets the URL object for the url to parse to get the cameras' list
            URL url = new URL(urls[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            contentType = urlConnection.getContentType();
            InputStream is = urlConnection.getInputStream();
            parserFactory = XmlPullParserFactory.newInstance();

            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);
            String aux;

            int eventType = parser.getEventType();
            /*It parses the content and assigns the value to the different arrays (camera's image url,
            * camera's name, camera's position
            */
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String elementName;
                elementName = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if ("description".equals(elementName)) {
                        String cameraURL = parser.nextText();
                        cameraURL = cameraURL.substring(cameraURL.indexOf("http:"));
                        cameraURL = cameraURL.substring(0, cameraURL.indexOf(".jpg") + 4);
                        camerasURLS_ArrayList.add(cameraURL);

                    } else if ("Data".equals(elementName)) {
                        aux = parser.getAttributeValue(null, "name");
                        if (aux.equals("Nombre")) {
                            String name;
                            parser.nextTag();
                            name = parser.nextText();
                            Log.v("aux1", name);
                            nameURLS_ArrayList.add(name);
                        }

                    } else if ("coordinates".equals(elementName)) {
                        String coorURL = parser.nextText();
                        String lat = coorURL.substring((coorURL.indexOf(",")) + 1, coorURL.length() - 4);
                        String lon = coorURL.substring(0, coorURL.indexOf(","));
                        coorURLS_ArrayList.add(new LatLng(Double.valueOf(lat), Double.valueOf(lon)));
                    }
                }
                eventType = parser.next();
            }
            //Once the content is parsed, the array of Camera objects is built
            for (int i = 0; i < nameURLS_ArrayList.size(); i++) {
                cameraArrayList.add(new Camera(nameURLS_ArrayList.get(i), camerasURLS_ArrayList.get(i), coorURLS_ArrayList.get(i)));
            }

        } catch (Exception e) {
            System.err.println(e.toString());
        }

        return null;
    }
    @Override
    protected void onPostExecute(Void voids) {
        mainActivity.setCameraArrayList(cameraArrayList);
        mainActivity.printCameraList();
        mainActivity.downloadJSONChannels();
    }
}
