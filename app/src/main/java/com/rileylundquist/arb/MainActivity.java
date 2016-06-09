package com.rileylundquist.arb;

import android.content.ClipData;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.provider.ContactsContract;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.realtime.internal.event.ObjectChangedDetails;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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
        AboutFragment.OnFragmentInteractionListener, ContactFragment.OnFragmentInteractionListener,
        DetailFragment.OnFragmentInteractionListener {

    private GoogleApiClient client;
    private GoogleMap mMap;

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
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mBenchesRef = mRootRef.child("benches");
        mBirdSignsRef = mRootRef.child("bird_signs");
        mHerbaceousRef = mRootRef.child("herbaceous");
        mHerpSignsRef = mRootRef.child("herp_signs");
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
        if (checkPermission(LOCATION_SERVICE, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        //Temporary for testing BottomSheet
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(ARB_CENTER));

        setupTrails();
        showTrails();
        setupBoundary();
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
        client.connect();
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
        AppIndex.AppIndexApi.start(client, viewAction);


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
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
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
