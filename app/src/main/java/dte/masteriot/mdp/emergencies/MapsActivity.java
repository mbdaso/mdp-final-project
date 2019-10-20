package dte.masteriot.mdp.emergencies;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //Map to be loaded in the activity
    private GoogleMap mMap;
    //Positions of the camera selected and the current position of the user (if allowed)
    LatLng camPos, currPos;
    //Name of the camera selected
    String cameraName;
    //RadioButtons to change the map type
    RadioButton rbMap, rbSatellite, rbHybrid;
    //Markers for camera position and user current position
    Marker camMarker, currPositionMarker;
    //TAG to use in logs
    private String TAG = "MapsActivity";
    //Variables for the different types of permissions requests
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;
    private static final int PERMISSION_REQUEST_BOTH_LOCATION = 3;
    //Objects to handle the current location of the user
    private FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest;
    boolean requestingLocationUpdates;
    private LocationCallback locationCallback;
    Location mCurrentLocation;

    //Bounds of the map with the markers
    LatLngBounds bounds;
    //Parameters received from previous activity
    Bundle args;
    //Object of the route to be drawn
    private YOURSRoute yoursRoute = new YOURSRoute();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtains the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Obtains arguments in bundle
        args = this.getIntent().getParcelableExtra("bundle");
        camPos = (LatLng) args.getParcelable("coordinates");
        cameraName = args.getString("cameraName");
        //Handles location object
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                //It obtains last location of the user (if allowed)
                mCurrentLocation = locationResult.getLastLocation();
                //It sets current position marker, sets map bounds and draws the map route between the two points
                currPos = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                addCurrPosMarker(currPos);
                setMapBounds(currPos, camPos);
                drawMapRoute(currPos, camPos);
                //It stops receiving location updates
                stopLocationUpdates();
                //fusedLocationClient.removeLocationUpdates(locationCallback);
            }


        };
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //It retrieves location updates if the user allows it
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Before using Location services, location permissions are checked
        checkLocationPermissions();
        //It loads RadioButtons on the map
        rbMap = (RadioButton) findViewById(R.id.rbMap);
        rbSatellite =(RadioButton) findViewById(R.id.rbSatellite);
        rbHybrid =(RadioButton) findViewById(R.id.rbHybrid);
        //Hybrid type is set by default
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //It adds selected camera's position marker to the map
        camMarker = mMap.addMarker(new MarkerOptions().position(camPos).title(cameraName));
        camMarker.showInfoWindow();
    }

    //It changes the map type to which is selected by the RadioButton clicked
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.rbMap:
                if (checked) {
                  //  Log.e(TAG, "RB Map");
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                }
                break;
            case R.id.rbSatellite:
                if (checked) {
                   // Log.e(TAG, "RB Satellite");
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                }
                break;
            case R.id.rbHybrid:
                if (checked) {
                   // Log.e(TAG, "RB Hybrid");
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                break;
        }
    }

    private void checkLocationPermissions() {
        //If Android version is M (6.0 API 23) or newer, check if it has Location permissions to request them to the user in case it is needed
        /*
        Android offers two location permissions: ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION. The permission you choose determines
        the accuracy of the location returned by the API. You only need to request one of the Android location permissions, depending on the level of accuracy you need:
        android.permission.ACCESS_COARSE_LOCATION – Allows the API to use WiFi or mobile cell data (or both) to determine the device's location. The API returns the
        location with an accuracy approximately equivalent to a city block.
        android.permission.ACCESS_FINE_LOCATION – Allows the API to determine as precise a location as possible from the available location providers,
        including the Global Positioning System (GPS) as well as WiFi and mobile cell data.
        */
        //It requests the missing permissions to the user. In case both are granted, the app gets user's current location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Option 1: requesting both permissions
            if((this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    && (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) ){
                //if both permissions are already granted we get the current location
                getCurrentLocation();
            } else if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //If the missing permission is COARSE, we request permission for COARSE location
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            } else if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //if the missing permission is FINE, we request permission for FINE location
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            } else {
                //if both permissions are missing, we request both permissions
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_BOTH_LOCATION);
            }
            //Option 2: requesting only FINE permission, because it is more precise than COARSE- look at explanation above
          /*  if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            } else {
                getCurrentLocation();
            } */

        }

        /*else { //If Android version is older, permissions are requested on installation, but we should check if the user granted or denied the permissions ¿?
           // permissionCoarseGranted = Manifest.permission.ACCESS_COARSE_LOCATION;
            // permissionFineGranted = Manifest.permission.ACCESS_FINE_LOCATION;
        }*/
    }

    //The response of the user invokes this method. It checks if the user either granted or denied the permissions
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Check if permission request response is for location permissions
        int length = grantResults.length;
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //User granted permissions. It gets his/her current location
                    Log.i( TAG, "/+++++++++++++++++++++");
                    Log.d("TAG", "coarse location permission granted");
                    Log.i( TAG, "/+++++++++++++++++++++");

                    //Get current location
                    getCurrentLocation();

                } else {
                    //User denied Location permissions. His/her current location cannot be retrieved
                    requestingLocationUpdates = false;
                    Log.e(TAG, "User denied coarse location permissions");
                }
                return;
            }

            case PERMISSION_REQUEST_FINE_LOCATION:{
                if (length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //User granted permissions. It gets his/her current location
                    //It can be considered to warn the user
                    Log.i( TAG, "/+++++++++++++++++++++");
                    Log.d("TAG", "fine location permission granted");
                    Log.i( TAG, "/+++++++++++++++++++++");

                    //Get current location
                    getCurrentLocation();

                } else {
                    //User denied Location permissions. His/her current location cannot be retrieved
                    //It can be considered to warn the user
                    requestingLocationUpdates = false;
                    Log.e(TAG, "User denied fine location permissions");
                }
                return;

            }
            case PERMISSION_REQUEST_BOTH_LOCATION:{

                if(length > 0){
                    for(int i = 0; i < grantResults.length ; i++){
                        //If any of the permissions is denied, the location cannot be retrieved
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            requestingLocationUpdates = false;
                            Log.e(TAG, "User denied some location permissions");
                            return;
                        }
                    }
                    //In case both permissions are granted the location is retrieved
                    requestingLocationUpdates = true;
                    getCurrentLocation();
                }

            }
        }
    }

    public void getCurrentLocation(){
        requestingLocationUpdates = true;
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null
                        if (location != null) {
                            // Logic to handle location object
                            currPos = new LatLng(location.getLatitude(), location.getLongitude());
                            addCurrPosMarker(currPos);
                            setMapBounds(currPos, camPos);
                            drawMapRoute(currPos, camPos);

                        } else {
                            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                        }
                    }
                });


    }
    //It adds to Map the current position marker
    public void addCurrPosMarker(LatLng position){
        currPositionMarker = mMap.addMarker(new MarkerOptions().position(position)
                .title("CURRENT POSITION")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        currPositionMarker.showInfoWindow();

    }

    public void setMapBounds(LatLng currPos, LatLng camPos){
        if (currPos.latitude < camPos.latitude) {
            bounds = new LatLngBounds(currPos, camPos);
        } else {
            bounds = new LatLngBounds(currPos, camPos);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));

    }

    public void drawMapRoute(LatLng source, LatLng dest){
        //Pintamos la ruta

        MapRouteTask task = new MapRouteTask();
        task.execute(source, dest);

    }
    private class MapRouteTask extends AsyncTask<LatLng, Void, List<LatLng>> {
        @Override
        @SuppressWarnings( "deprecation" )
        protected List<LatLng> doInBackground(LatLng ... srcdst) {
            LatLng src = srcdst[0];
            LatLng dst = srcdst[1];
            List<LatLng> route;
            try {
                URL apiURL = yoursRoute.buildRouteURL(src, dst);
                HttpURLConnection urlConnection = (HttpURLConnection) apiURL.openConnection();
                InputStream is = urlConnection.getInputStream();
                route = yoursRoute.getRouteFromXML(is);
            }
            catch(Exception e){
                Log.d(TAG, "MapRouteTask: " + e. getMessage());
                route = Arrays.asList();
            }
            return route;
        }

        @Override
        protected void onPostExecute(List<LatLng> route) {

            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
            for(LatLng point: route){
                options.add(point);
            }

            Polyline line = mMap.addPolyline(options);
            Log.d(TAG, "Ruta pintada " + route.get(0) + "hasta " + route.get(route.size() - 1));
        }
    }
}
