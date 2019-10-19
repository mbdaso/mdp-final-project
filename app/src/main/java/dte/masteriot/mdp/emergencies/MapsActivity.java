package dte.masteriot.mdp.emergencies;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LatLng datos, currPos;
    String cameraName;
    RadioButton rbMap, rbSatellite, rbHybrid;
    Marker camMarker, currPositionMarker;
    LatLng currPosition;
    private String TAG = "MapsActivity";
    double valCont;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;

    boolean permissionCoarseGranted = false;
    boolean permissionFineGranted = false;

    private FusedLocationProviderClient fusedLocationClient;
    LatLngBounds bounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Bundle parametros = this.getIntent().getParcelableExtra("bundle");
        datos = (LatLng) parametros.getParcelable("coordinates");
        cameraName = parametros.getString("cameraName");
        valCont = parametros.getDouble("valCont");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermissions();

        rbMap = (RadioButton) findViewById(R.id.rbMap);
        rbSatellite =(RadioButton) findViewById(R.id.rbSatellite);
        rbHybrid =(RadioButton) findViewById(R.id.rbHybrid);


       // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(datos, 15));
       // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(datos, 15));


    }
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.map:
                if (checked)
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                break;
            case R.id.satellite:
                if (checked)
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                //  uno.setChecked(false);
                //  tres.setChecked(false);

                break;
            case R.id.hybrid:
                if (checked)
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                //  uno.setChecked(false);
                //  dos.setChecked(false);
                break;
        }
    }
    private void checkLocationPermissions() {
        //If Android version is M (6.0 API 23) or newer, check if it has Location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //If Location permissions are not granted for the app, ask user for it! Request response will be received in the onRequestPermissionsResult.
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
            else {
                //We change the value of permissions' variable
                permissionCoarseGranted = true;
                //initBT();
            }
            if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            }else{
                permissionFineGranted = true;
            }
            if(permissionCoarseGranted || permissionFineGranted){
                //If any of the permissions are granted we proceed with accessing to current location
                getCurrentLocation();

            }else{
                //If any of the permissions is not granted, we have to show a message informing that the current location won´t be shown
                Log.e(TAG, "User denied location permissions");
            }

        }
    }
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Check if permission request response is from Location
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //User granted permissions. Setup the scan settings
                    Log.i( TAG, "/+++++++++++++++++++++");
                    Log.d("TAG", "coarse location permission granted");
                    Log.i( TAG, "/+++++++++++++++++++++");

                    //Get current location
                    getCurrentLocation();

                } else {
                    //User denied Location permissions. Here you could warn the user that without
                    //Location permissions the app is not able to scan for BLE devices
                    //In this case we just close the app
                    //finish();
                    Log.e(TAG, "User denied coarse location permissions");
                }
                return;
            }
            case PERMISSION_REQUEST_FINE_LOCATION:{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //User granted permissions. Setup the scan settings
                    Log.i( TAG, "/+++++++++++++++++++++");
                    Log.d("TAG", "fine location permission granted");
                    Log.i( TAG, "/+++++++++++++++++++++");

                    //Get current location
                    getCurrentLocation();

                } else {
                    //User denied Location permissions. Here you could warn the user that without
                    //Location permissions the app is not able to scan for BLE devices
                    //In this case we just close the app
                    //finish();
                    Log.e(TAG, "User denied fine location permissions");
                }
                return;

            }
        }
    }
    public void getCurrentLocation(){
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                            // Logic to handle location object
                            currPos = new LatLng(location.getLatitude(), location.getLongitude());
                            if(valCont == -1)
                                camMarker = mMap.addMarker(new MarkerOptions().position(datos).title(cameraName)
                                        .snippet("Not measured"));
                            else
                                camMarker = mMap.addMarker(new MarkerOptions().position(datos).title(cameraName)
                                        .snippet(String.format("Last measured value: %ld NO2 µg/m3", valCont)));

                            //FALTA CAMBIAR EL COLOR A NARANJA
                            currPositionMarker = mMap.addMarker(new MarkerOptions().position(currPos).title("CURRENT POSITION"));
                            if (currPos.latitude < datos.latitude) {
                                bounds = new LatLngBounds(currPos, datos);
                            } else {
                                bounds = new LatLngBounds(currPos, datos);
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                        }
                    }
                });


    }
}
