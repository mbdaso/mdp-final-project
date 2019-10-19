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

    private static final String URL_CAMERAS = "http://informo.madrid.es/informo/tmadrid/CCTV.kml";
    private TextView text;

    ArrayList<String> nameURLS_ArrayList = new ArrayList<>();
    ArrayList<String> camerasURLS_ArrayList = new ArrayList<>();
    ArrayList<LatLng> coorURLS_ArrayList = new ArrayList<>();

    ListView lv;
    XmlPullParserFactory parserFactory;
   // ArrayAdapter adaptador;
    private ImageView im;
  //  LatLng coor;
   // String auxcoor;
    Integer pos=0;
    int numEmergencies = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main);
        text =  (TextView) findViewById(R.id.textView);
        text.setText("Number of Emergencies: "+numEmergencies);
        im = findViewById(R.id.imageView);
        im.setImageResource(R.mipmap.upmiot); //To check how to show this image greater

        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute( URL_CAMERAS );
    }

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {

        private String contentType = "";

        @Override
        @SuppressWarnings( "deprecation" )
        protected String doInBackground(String... urls) {
            String response = "";

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
                                response+=cameraURL + "\n";
                            } else if ("Data".equals(elementName)) {
                                aux=parser.getAttributeValue(null,"name");
                             //   Log.v("EEEEE", aux );
                                if (aux.equals("Nombre")){
                                    String aux1;
                                    parser.nextTag();
                                    aux1=parser.nextText();
                                    Log.v("aux1", aux1);
                                    nameURLS_ArrayList.add(aux1);
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
            lv = (ListView) findViewById(R.id.lv);
            ArrayAdapter arrayAdapter = new ArrayAdapter( MainActivity.this,
                    android.R.layout.simple_list_item_checked ,
                    nameURLS_ArrayList );
            lv.setAdapter(arrayAdapter);

            lv.setChoiceMode( ListView.CHOICE_MODE_SINGLE );

            lv.setClickable(true);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Object o = lv.getItemAtPosition(position);
                    String str=(String)o;//As you are using Default String Adapter
                    Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();
                    pos=position;
                    CargaImagenes task = new CargaImagenes();
                    task.execute( camerasURLS_ArrayList.get(position) );
                }
            });
        }
    }

    class CargaImagenes extends AsyncTask<String, Void, Bitmap>{

        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();


        }

        @Override
        protected Bitmap doInBackground(String... params) {
            // TODO Auto-generated method stub
            Log.i("doInBackground" , "Entra en doInBackground");
            String url = params[0];
            Bitmap imagen = descargarImagen(url);
            return imagen;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            ImageView im = (ImageView)((AppCompatActivity) MainActivity.this).findViewById(R.id.imageView);
            im.setImageBitmap(result);
            im.setOnClickListener( new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent (v.getContext(), MapsActivity.class);
                    Bundle args = new Bundle();
                    args.putParcelable("coordinates", coorURLS_ArrayList.get(pos));
                    args.putString("cameraName", nameURLS_ArrayList.get(pos));
                    intent.putExtra("bundle",args);
                    startActivity(intent);
                }
            });
        }
        private Bitmap descargarImagen (String imageHttpAddress){
            URL imageUrl = null;
            Bitmap imagen = null;
            try{
                imageUrl = new URL(imageHttpAddress);
                HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                conn.connect();
                imagen = BitmapFactory.decodeStream(conn.getInputStream());
            }catch(IOException ex){
                ex.printStackTrace();
            }

            return imagen;
        }
    }
}