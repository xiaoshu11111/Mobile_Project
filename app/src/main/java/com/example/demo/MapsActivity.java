package com.example.demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.snapshot.StringNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Handle the event object on the map.
public class MapsActivity extends AppCompatActivity
        implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnInfoWindowClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SensorEventListener {

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    ImageView list,refresh;

    private boolean mPermissionDenied = false;

    private static final int LOCATION_REQUEST_CODE =101;

    private GoogleMap mMap;

    private Marker mSelectedMarker;
    private static final String TAG = "firebase";

    SensorManager sensorManager;
    Sensor sensor;
    int darkMode = 0;
    int previous = 0;

    // Pause the sensor.
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    //Resume the sensor.
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, sensorManager.SENSOR_DELAY_NORMAL);
    }

    //Change the monitor mode based on the data from sensor.
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            if (sensorEvent.values[0] < 13333) {
                previous = darkMode;
                darkMode = 1;
            } else if (sensorEvent.values[0] < 26666){
                previous = darkMode;
                darkMode = 2;
            } else {
                previous = darkMode;
                darkMode = 3;
            }
            if (previous != 0) {
                if(previous != darkMode) {
                    finish();
                    startActivity(getIntent());
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
        }

        //the function to display the content of the view.
        private void render(Marker marker, View view) {
            String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null) {
                snippetUi.setText(snippet);
            } else {
                snippetUi.setText("");
            }
        }
    }

    //initialize the class and connect the code to the items.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        refresh = findViewById(R.id.refresh);
        list = findViewById(R.id.list);
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.settings:
                        Intent intent1 = new Intent(getApplicationContext(), setting.class);
                        startActivity(intent1);

                        break;
                    case R.id.add:
                        Intent intent = new Intent(getApplicationContext(), AddEvent.class);
                        startActivity(intent);

                        break;
                    case R.id.profile:
                        Intent intent2 = new Intent(getApplicationContext(), Profile.class);
                        startActivity(intent2);
                        break;
                }
                return true;
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), List_events.class);
                startActivity(intent);
            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                startActivity(getIntent());
            }
        });



        //initialize the sensor manager.
        sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensor.TYPE_LIGHT);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        fetchLastLocation();

    }

    // set the map ready.
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        if (darkMode == 1) {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));
        } else if (darkMode == 2) {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json2));
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        // Setting an info window adapter allows us to change the both the contents and look of the
        // info window.
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

//        Log.d(TAG, "address1: "+currentLocation.getLatitude() +","+currentLocation.getLongitude());
        final LatLng current = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));

        //create the reference to firebase.
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference myRef = database.child("events/");

        //listen to any data change on the map and update it.
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot eventSnapShot : dataSnapshot.getChildren()) {
                    String adate = (String) eventSnapShot.child("date").getValue();
                    String atime = (String) eventSnapShot.child("start_time").getValue();
                    int date = Integer.parseInt(adate);
                    int time = Integer.parseInt(atime);

                    Date c = Calendar.getInstance().getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyMMdd");
                    String formattedDate = df.format(c);
                    SimpleDateFormat tf = new SimpleDateFormat("HHmm");
                    String formattedTime = tf.format(c);
                    int current_date = Integer.parseInt(formattedDate);
                    int current_time = Integer.parseInt(formattedTime);

                    if (date < current_date || (date==current_date && time<=current_time)){

                    } else {
                        String eventId = (String) eventSnapShot.getKey();
                        String[] latlng = ((String) eventSnapShot.child("location").getValue()).split(",");
                        Double latitude = Double.parseDouble(latlng[0]);
                        Double longitude = Double.parseDouble(latlng[1]);
                        String name = (String) eventSnapShot.child("name").getValue();
                        String description = (String) eventSnapShot.child("description").getValue();
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .title(name)
                                .snippet(description));
                        marker.setTag(eventId);
//                    Log.d(TAG, "added Marker of event " + name);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}

        });
//        mMap.addMarker(new MarkerOptions()
//                .position(new LatLng(-37.806546, 144.958259))
//                .title("Garbage audition")
//                .snippet("Those are some interesting garbage!\n" +
//                        "Time: 10/21 2pm\n" +
//                        "Going people: 30"));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                } else {
                    Toast.makeText(MapsActivity.this,"Location permission missing",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onMapClick(final LatLng point) {
        // Any showing info window closes when the map is clicked.
        // Clear the currently selected marker.
        mSelectedMarker = null;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        // The user has re-tapped on the marker which was already showing an info window.
        if (marker.equals(mSelectedMarker)) {
            // The showing info window has already been closed - that's the first thing to happen
            // when any marker is clicked.
            // Return true to indicate we have consumed the event and that we do not want the
            // the default behavior to occur (which is for the camera to move such that the
            // marker is centered and for the marker's info window to open, if it has one).
            mSelectedMarker = null;
            return true;
        }

        mSelectedMarker = marker;

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur.
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        String eventId = (String) marker.getTag();
//        Toast.makeText(this, "Click Info Window", Toast.LENGTH_SHORT).show();

        Intent intent1 = new Intent(getApplicationContext(), View_Event.class);
        intent1.putExtra("eventId", eventId);
        startActivity(intent1);
    }

    private void fetchLastLocation(){
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Log.d(TAG, "location: "+location);
                    currentLocation = location;
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

//                            Log.d(TAG, "address1: "+latitude +","+longitude);
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MapsActivity.this);
                } else {
                    Log.d(TAG, "location is null");
                }
            }
        });
    }

    private boolean isPastEvent(String eventId) {
        boolean result = false;
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference eventRef = database.child("events/"+eventId);

        eventRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot eventSnapShot : dataSnapshot.getChildren()) {
                    String eventId = (String) eventSnapShot.getKey();
                    String[] latlng = ((String) eventSnapShot.child("location").getValue()).split(",");
                    Double latitude = Double.parseDouble(latlng[0]);
                    Double longitude = Double.parseDouble(latlng[1]);
                    String name = (String) eventSnapShot.child("name").getValue();
                    String description = (String) eventSnapShot.child("description").getValue();
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .title(name)
                            .snippet(description));
                    marker.setTag(eventId);
//                    Log.d(TAG, "added Marker of event " + name);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}

        });
        return result;
    }
}
