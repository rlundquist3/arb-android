package com.rileylundquist.arb;

import android.*;
import android.Manifest;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.realtime.internal.event.ObjectChangedDetails;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleMap.OnCameraChangeListener, GoogleMap.OnMarkerClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        AboutFragment.OnFragmentInteractionListener, ContactFragment.OnFragmentInteractionListener,
        DetailFragment.OnFragmentInteractionListener, LocationListener {

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;

    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;
    protected Location mLastLocation;
    protected Marker mCurrLocationMarker;
    protected LatLng latLng;

    private DatabaseReference mRootRef;
    private DatabaseReference mBenchesRef;
    private DatabaseReference mBirdSignsRef;
    private DatabaseReference mHerbaceousRef;
    private DatabaseReference mHerpSignsRef;

    private final String DEBUG_STRING = "MAP";
    private final LatLng ARB_CENTER = new LatLng(42.293469, -85.701);
    private final LatLngBounds ARB_AREA = new LatLngBounds(new LatLng(42.285, -85.71), new LatLng(42.30, -85.69));
    private final int DEFAULT_ZOOM = 15;

    private SupportMapFragment mapFragment;

    private List trailLines = new ArrayList<Polyline>();
    private List boundaryLines = new ArrayList<Polyline>();
    private List benches = new ArrayList<Marker>();
    private boolean trailsOn = false;
    private boolean boundaryOn = false;
    private boolean benchesOn = false;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //place marker at current position
            mMap.clear();
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            mCurrLocationMarker = mMap.addMarker(markerOptions);
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000); //5 seconds
        mLocationRequest.setFastestInterval(3000); //3 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(0.1F); //1/10 meter
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "onConnectionFailed", Toast.LENGTH_SHORT).show();
    }

    private enum Fragments {
        MAP, ABOUT, CONTACT
    }

    private Fragments currentFragment = Fragments.MAP;

    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        ImageView logo = (ImageView) findViewById(R.id.arb_logo);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(AppIndex.API)
//                .addApi(LocationServices.API)
//                .build();
//        createLocationRequest();
        buildGoogleApiClient();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mBenchesRef = mRootRef.child("benches");
        mBirdSignsRef = mRootRef.child("bird_signs");
        mHerbaceousRef = mRootRef.child("herbaceous");
        mHerpSignsRef = mRootRef.child("herp_signs");

        startLocationUpdates();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.show_map) {
            goToMap();
        } else if (id == R.id.show_trails) {
            if (trailsOn)
                hideTrails();
            else if (!trailLines.isEmpty())
                showTrails();
        } else if (id == R.id.show_boundary) {
            if (boundaryOn)
                hideBoundary();
            else if (!boundaryLines.isEmpty())
                showBoundary();
        } else if (id == R.id.show_benches) {
            if (benchesOn)
                hideBenches();
            else if (!benches.isEmpty())
                showBenches();
        } else if (id == R.id.show_bird_signs) {
            showCollection("bird_signs");
        } else if (id == R.id.show_herbaceous) {
            showCollection("herbaceous");
        } else if (id == R.id.show_herp_signs) {
            showCollection("herp_signs");
        } else if (id == R.id.nav_about) {
            goToAbout();
        } else if (id == R.id.nav_contact) {
            goToContact();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
        mMap.setOnCameraChangeListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ARB_CENTER, DEFAULT_ZOOM));
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
//        if (checkPermission(LOCATION_SERVICE, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
//            Log.d("X", "location enabled");
//            mMap.setMyLocationEnabled(true);
//            mMap.getUiSettings().setMyLocationButtonEnabled(true);
//        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
//                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
//            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        //Temporary for testing BottomSheet
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(ARB_CENTER));

        setupTrails();
        showTrails();
        setupBoundary();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        createLocationRequest();
    }

    protected void startLocationUpdates() {
        if (getPackageManager().checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, getPackageName()) == PackageManager.PERMISSION_GRANTED ||
                getPackageManager().checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, getPackageName()) == PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
//        if (currLocationMarker != null) {
//            currLocationMarker.remove();
//        }
//        latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.position(latLng);
//        markerOptions.title("Current Position");
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
//        currLocationMarker = mMap.addMarker(markerOptions);
//        Snackbar.make(mapFragment.getView(), "location updated: " + location.toString(), Snackbar.LENGTH_LONG).show();
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.d(DEBUG_STRING, "onCameraChange");

        double midLatitude = (ARB_AREA.northeast.latitude + ARB_AREA.southwest.latitude) / 2.0;
        double midLongitude = (ARB_AREA.northeast.longitude + ARB_AREA.southwest.longitude) / 2.0;

        // Reposition camera if leaving arb area
        if (cameraPosition.target.latitude > ARB_AREA.northeast.latitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(ARB_AREA.northeast.latitude, midLongitude)));
        if (cameraPosition.target.latitude < ARB_AREA.southwest.latitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(ARB_AREA.southwest.latitude, midLongitude)));
        if (cameraPosition.target.longitude > ARB_AREA.northeast.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(midLatitude, ARB_AREA.northeast.longitude)));
        if (cameraPosition.target.latitude < ARB_AREA.southwest.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(midLatitude, ARB_AREA.southwest.longitude)));
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.rileylundquist.arb/http/host/path")
        );
//        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);

        mBenchesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                LatLng coords = new LatLng(dataSnapshot.child("Latitude").getValue(Double.class),
                                            dataSnapshot.child("Longitude").getValue(Double.class));
                benches.add(mMap.addMarker(new MarkerOptions().position(coords).visible(false)));
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                LatLng coords = new LatLng(dataSnapshot.child("Latitude").getValue(Double.class),
                        dataSnapshot.child("Longitude").getValue(Double.class));
                Marker bench = (Marker) benches.get(dataSnapshot.child("FID").getValue(Integer.class));
                bench.setPosition(coords);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                benches.remove(dataSnapshot.child("FID").getValue(Integer.class));
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.rileylundquist.arb/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        DetailFragment detailFragment = new DetailFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.bottom_sheet, detailFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        FrameLayout bottomSheet = (FrameLayout) findViewById(R.id.bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        return true;
    }

    private void setupTrails() {
        Log.d("DATA", "attempting to load trail data");
        DataReader reader = new DataReader();
        try {
            List trails = reader.readTrailData(getResources().openRawResource(R.raw.arb_trails));
            for (Object trail : trails)
                trailLines.add(mMap.addPolyline(((PolylineOptions) trail).color(getResources().getColor(R.color.k_aqua))));
//            for (Object line : trailLines)
//                ((Polyline) line)
            hideTrails();
            trails.clear();
        } catch (FileNotFoundException e) {
            Log.d("IO", e.getClass().toString());
        } catch (IOException e) {
            Log.d("IO", e.getClass().toString());
        }
    }

    private void setupBoundary() {
        Log.d("DATA", "attempting to load boundary data");
        DataReader reader = new DataReader();
        try {
            List boundary = reader.readBoundaryData(getResources().openRawResource(R.raw.arb_boundary));
            for (Object b : boundary)
                boundaryLines.add(mMap.addPolyline(((PolylineOptions) b).color(getResources().getColor(R.color.k_purple))));
            hideBoundary();
            boundary.clear();
        } catch (FileNotFoundException e) {
            Log.d("IO", e.getClass().toString());
        } catch (IOException e) {
            Log.d("IO", e.getClass().toString());
        }
    }

    private void showTrails() {
        Log.d(DEBUG_STRING, "show trails");
        for (Object trail : trailLines)
            ((Polyline) trail).setVisible(true);
        trailsOn = true;
    }

    private void hideTrails() {
        for (Object trail : trailLines)
            ((Polyline) trail).setVisible(false);
        trailsOn = false;
    }

    private void showBoundary() {
        for (Object boundary : boundaryLines)
            ((Polyline) boundary).setVisible(true);
        boundaryOn = true;
    }

    private void hideBoundary() {
        for (Object boundary : boundaryLines)
            ((Polyline) boundary).setVisible(false);
        boundaryOn = false;
    }

    private void goToMap() {
        if (!currentFragment.equals(Fragments.MAP))
            getSupportFragmentManager().popBackStack();
        currentFragment = Fragments.MAP;
    }

    private void showCollection(String collection) {


    }

    private void showBenches() {
        for (Object b : benches)
            ((Marker) b).setVisible(true);
    }

    private void hideBenches() {
        for (Object b : benches)
            ((Marker) b).setVisible(false);
    }

    private void goToAbout() {
        AboutFragment aboutFragment = new AboutFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, aboutFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        currentFragment = Fragments.ABOUT;
    }

    private void goToContact() {
        ContactFragment contactFragment = new ContactFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, contactFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        currentFragment = Fragments.CONTACT;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(DEBUG_STRING, "fragment interaction");
    }

    private class GetCollectionTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                Log.d("S", "asyncTask trying server connection");
                return ServerConnection.INSTANCE.getData(params[0]);
            } catch (IOException e) {
                Log.e("ERR", "Unable to connect to server");
                return getString(R.string.connection_error);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(DEBUG_STRING, result);

        }
    }
}
