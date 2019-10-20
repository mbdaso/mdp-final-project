package dte.masteriot.mdp.emergencies;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //URL from which the list of cameras will be retrieved
    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";

    //TextView to show the number of emergencies
    private TextView text;

    //ArrayLists containing the names, URLs and coordinates of the cameras
    ArrayList<String> nameURLS_ArrayList = new ArrayList<>();
    ArrayList<String> camerasURLS_ArrayList = new ArrayList<>();
    ArrayList<LatLng> coorURLS_ArrayList = new ArrayList<>();

    //ListView that includes the list of cameras
    ListView lv;

    //Object to parse the file retrieved from the cameras' URL
    XmlPullParserFactory parserFactory;

    //ImageView to show either UPM MIOT logo or the selected camera image
    private ImageView im;

    //Position in the list of the camera selected
    int pos=0;

    //Number of emergencies (NO2 dangerous levels) detected
    int numEmergencies = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main);
        //It gets the UI elements of the main activity
        text =  (TextView) findViewById(R.id.textView);
        text.setText("Number of Emergencies: "+numEmergencies);
        im = findViewById(R.id.imageView);
        im.setImageResource(R.mipmap.upmiot); //To check how to show this image bigger

        //It downloads the camera list from the predefined URL
        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute( URL_CAMERAS );

    }

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {

        private String contentType = "";

        @Override
        @SuppressWarnings( "deprecation" )
        protected String doInBackground(String... urls) {
            String response = "";
            //Connects to the URL and parses the response to build the different ArrayLists with cameras' data
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL( urls[0] );
                urlConnection = (HttpURLConnection) url.openConnection();
                contentType = urlConnection.getContentType();
                InputStream is = urlConnection.getInputStream();
                parserFactory = XmlPullParserFactory.newInstance();
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
                            if ("description".equals(elementName)) {
                                String cameraURL = parser.nextText();
                                cameraURL = cameraURL.substring(cameraURL.indexOf("http:"));
                                cameraURL = cameraURL.substring(0, cameraURL.indexOf(".jpg") + 4);
                                camerasURLS_ArrayList.add(cameraURL);

                            } else if ("Data".equals(elementName)) {
                                aux=parser.getAttributeValue(null,"name");
                                if (aux.equals("Nombre")){
                                    String name;
                                    parser.nextTag();
                                    name=parser.nextText();
                                    nameURLS_ArrayList.add(name);
                                }

                            }else if("coordinates".equals(elementName)){
                                String coorURL=parser.nextText();
                                String lat= coorURL.substring((coorURL.indexOf(","))+1, coorURL.length()-4);
                                String lon = coorURL.substring(0, coorURL.indexOf(","));
                                coorURLS_ArrayList.add(new LatLng(Double.valueOf(lat).doubleValue(),Double.valueOf(lon).doubleValue()));

                            }
                            break;
                    }
                    eventType = parser.next();
                }

            } catch (Exception e) {
                response = e.toString();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            //It builds the adapter of the cameras' names list
            lv = (ListView) findViewById(R.id.lv);
            ArrayAdapter arrayAdapter = new ArrayAdapter( MainActivity.this, android.R.layout.simple_list_item_checked , nameURLS_ArrayList );
            lv.setAdapter(arrayAdapter);
            lv.setChoiceMode( ListView.CHOICE_MODE_SINGLE );
            lv.setClickable(true);
            //It sets a listener on the list
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Object o = lv.getItemAtPosition(position);
                    String str=(String)o;//As you are using Default String Adapter
                    //A toast with the name of the camera is shown
                    Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();
                    pos=position;
                    //It gets the image of the camera selected
                    ImageLoader task = new ImageLoader();
                    task.execute( camerasURLS_ArrayList.get(position) );

                }
            });
        }
    }

    //Async task class to retrieve the image of the camera from the Internet
    class ImageLoader extends AsyncTask<String, Void, Bitmap>{

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            // TODO Auto-generated method stub

            String url = params[0];
            URL imageUrl = null;
            Bitmap imagen = null;
            //It connects to the image url and gets the image
            try{
                imageUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.connect();
                imagen = BitmapFactory.decodeStream(conn.getInputStream());
            }catch(IOException ex){
                ex.printStackTrace();
            }

            return imagen;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            //It sets the image to the image view to be shown to the User
            ImageView im = (ImageView)((AppCompatActivity) MainActivity.this).findViewById(R.id.imageView);
            im.setImageBitmap(result);
            //It sets a listener on the image to load a map when clicked
            im.setOnClickListener( new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent (v.getContext(), MapsActivity.class);
                    //It passes the parameters needed in the new Maps activity
                    Bundle args = new Bundle();
                    args.putParcelable("coordinates", coorURLS_ArrayList.get(pos));
                    args.putString("cameraName", nameURLS_ArrayList.get(pos));
                    intent.putExtra("bundle",args);
                    startActivity(intent);
                }
            });
        }
    }
}