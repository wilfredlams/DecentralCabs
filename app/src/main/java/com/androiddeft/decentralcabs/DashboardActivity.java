package com.androiddeft.decentralcabs;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    private SessionHandler session;
    MapView map = null;
    MapController myMapController;
    MyLocationNewOverlay mLocationOverlay;
    EditText chatbox;
    User user;
    ArrayList<Marker> Markers= new ArrayList<>();

    private static final String KEY_USERNAME = "username";
    private static final String KEY_LATLONG = "latlong";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_EMPTY = "";
    private String updateloc_url = "https://yodonga.com/decab/updateloc.php";
    private String listlatlong_url = "https://yodonga.com/decab/listlatlong.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestpermissions();

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

        }

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_dashboard);
        session = new SessionHandler(getApplicationContext());
        user = session.getUserDetails();
        map = (MapView) findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        myMapController = (MapController) map.getController();
        myMapController.setZoom(15);
        map.setMapOrientation(0);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.enableFollowLocation();
        Drawable currentDraw = ResourcesCompat.getDrawable(getResources(), R.drawable.user, null);
        Bitmap currentIcon = null;
        if (currentDraw != null) {
            currentIcon = ((BitmapDrawable) currentDraw).getBitmap();
        }
        map.getOverlays().add(this.mLocationOverlay);
        mLocationOverlay.setPersonIcon(currentIcon);
        mLocationOverlay.setDirectionArrow(currentIcon, currentIcon);

        map.getOverlays().add(0, mLocationOverlay);

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {

            public boolean singleTapConfirmedHelper(GeoPoint p) {
                for (Marker m : Markers) {
                    // m.closeInfoWindow();
                }
                return true;
            }

            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        ImageView logoutBtn = findViewById(R.id.btnLogout);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.logoutUser();
                Intent i = new Intent(DashboardActivity.this, LoginActivity.class);
                startActivity(i);
                handler.removeCallbacks(runnable);
                finish();
            }
        });

        ImageView centerBtn = findViewById(R.id.btnCenter);
        centerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myMapController.setCenter((mLocationOverlay.getMyLocation()));
                myMapController.animateTo(mLocationOverlay.getMyLocation());
            }
        });

        chatbox = findViewById(R.id.chatbox);

        chatbox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    processchatbox(chatbox.getText().toString());
                    chatbox.setText(null);
                    return true;
                }
                return false;
            }
        });

// Start the Runnable immediately
        handler.post(runnable);
    }

    public void processchatbox(String text) {


    }

    // Create the Handler
    private Handler handler = new Handler();

    // Define the code block to be executed
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Insert custom code here
            updatelatlong();
            drawmarkers();
            // Repeat every 2 seconds
            handler.postDelayed(runnable, 5000);
        }
    };


    private void updatelatlong() {

        double longitude = 0;
        double latitude  = 0;

        try {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        longitude = location.getLongitude();
        latitude = location.getLatitude();

        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject request = new JSONObject();
        try {

            String latlong = latitude + "," + longitude;

            request.put(KEY_USERNAME, user.getUsername());
            request.put(KEY_LATLONG, latlong);

            Log.d("STATE","Username:" + user.getUsername());
            Log.d("STATE","Latlong:" + latlong);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsArrayRequest = new JsonObjectRequest(Request.Method.POST, updateloc_url, request, new Response.Listener<JSONObject>(){
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //Check if latlong got registered successfully
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

    public void drawmarkers() {

        try{
        // Put line here to read all SQL items to an array

        URL url = new URL(listlatlong_url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            for (Marker m : Markers) {
                map.getOverlays().remove(m);
            }

            String[] array = new String[100];

            String line;
            int i=0;

            while((line = br.readLine()) != null){
                final String[] token = line.split(";");
                String[] latlong = token[1].split(",");

                if (!token[0].equals(user.getUsername())) {
                   double latitudeE6 = Double.parseDouble(latlong[0]);
                   double longitudeE6 = Double.parseDouble(latlong[1]);

                   Log.d("STATE", "GeoPoint is " + latitudeE6 + "," + longitudeE6);

                    GeoPoint Point = new GeoPoint(latitudeE6, longitudeE6);

                    final Marker Marker = new Marker(map);
                    Marker.setPosition(Point);
                    Marker.setTitle(token[0].toUpperCase());
                    Marker.setSubDescription(chathead(token[0]));
                    Marker.setIcon(getResources().getDrawable(R.drawable.aliens));
                    Marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                    Marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(org.osmdroid.views.overlay.Marker marker, MapView mapView) {
                            Marker.showInfoWindow();
                            chatbox.setText("@" + token[0] + ":");
                            chatbox.setSelection(chatbox.getText().length());

                            InputMethodManager inputMethodManager =
                                    (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputMethodManager.toggleSoftInputFromWindow(
                                    chatbox.getApplicationWindowToken(),
                                    InputMethodManager.SHOW_FORCED, 0);

                            return false;
                        }
                    });

                    map.getOverlays().add(Marker);
                    Markers.add(Marker);
                   }

                array[i] = line;
                i++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String chathead(String user){
        /*if(user == "1") {
            return "hi";
        }
        else {
            return "Bye";
        }*/

        return "Chat message from " + user + "<br> 1. 123 <br> 2. 123 <br> 3. 123 <br> 4. 123";
    }


    public void requestpermissions(){
        // Here, thisActivity is the current activity
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)){


            // Permission is not granted
            // Should we show an explanation?
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) && (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION))) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION},
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
