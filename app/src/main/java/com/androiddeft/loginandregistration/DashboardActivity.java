package com.androiddeft.loginandregistration;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private SessionHandler session;
    MapView map = null;
    MapController myMapController;
    MyLocationNewOverlay mLocationOverlay;

    User user;

    private static final String KEY_USERNAME = "username";
    private static final String KEY_LATLONG = "latlong";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_EMPTY = "";
    private String latlong_url = "https://yodonga.com/decab/updateloc.php";
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestwritepermission();
        requestlocationpermission();

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_dashboard);
        session = new SessionHandler(getApplicationContext());
        user = session.getUserDetails();
        TextView welcomeText = findViewById(R.id.welcomeText);
        map = (MapView) findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        myMapController = (MapController) map.getController();
        myMapController.setZoom(15);
        map.setMapOrientation(0);

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this),map);
        this.mLocationOverlay.enableMyLocation();
        this.mLocationOverlay.enableFollowLocation();
        map.getOverlays().add(this.mLocationOverlay);

        welcomeText.setText("Welcome "+user.getFullName()+"\nSession Expiry: "+user.getSessionExpiryDate());
        Button logoutBtn = findViewById(R.id.btnLogout);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.logoutUser();
                Intent i = new Intent(DashboardActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        updatelatlong();
    }

    private void updatelatlong(){

        JSONObject request = new JSONObject();
        try {
            //Populate the request parameters
            GeoPoint latlong = mLocationOverlay.getMyLocation();

            request.put(KEY_USERNAME, user.getUsername());
            request.put(KEY_LATLONG, latlong);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsArrayRequest = new JsonObjectRequest(Request.Method.POST, latlong_url, request, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //Check if user got registered successfully
                    if (response.getInt(KEY_STATUS) == 0) {
                        Log.d("STATE","Created.");

                    }else if(response.getInt(KEY_STATUS) == 1){
                        Log.d("STATE","Updated.");

                    }else{
                        Toast.makeText(getApplicationContext(),
                                response.getString(KEY_MESSAGE), Toast.LENGTH_SHORT).show();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //Display error message whenever an error occurs
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsArrayRequest);
    }

    public void requestwritepermission(){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }


    }

    public void requestlocationpermission(){
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}
