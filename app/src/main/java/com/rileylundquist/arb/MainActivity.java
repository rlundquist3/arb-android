package com.rileylundquist.arb;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleMap.OnCameraChangeListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnPolylineClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        AboutFragment.OnFragmentInteractionListener, ContactFragment.OnFragmentInteractionListener,
        DetailFragment.OnFragmentInteractionListener, GuidelinesFragment.OnFragmentInteractionListener,
        ItemFragment.OnListFragmentInteractionListener, LocationListener {

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;

    protected LocationRequest mLocationRequest;
    protected Location mLastLocation;

    private DatabaseReference mRootRef;
    private DatabaseReference mBenchesRef;

    private final String DEBUG_STRING = "MAP";
    private final LatLng ARB_CENTER = new LatLng(42.293469, -85.701);
    private final LatLngBounds ARB_AREA = new LatLngBounds(new LatLng(42.285, -85.71), new LatLng(42.30, -85.69));
    private final int DEFAULT_ZOOM = 15;

    private final int PEEK_HEIGHT = 220;

    private SupportMapFragment mMapFragment;
    private ItemFragment mItemFragment;
    private FrameLayout mBottomSheet;

    private ThingsNearby mThingsNearby = new ThingsNearby();
    private List mNearbyItems = new ArrayList<ArbItem>();
    private List mNearbyMarkers = new ArrayList<Marker>();
    private List mNearbyDisplayed = new ArrayList<Marker>();
    private List trailLines = new ArrayList<Polyline>();
    private List mTrailNames = new ArrayList<String>();
    private List boundaryLines = new ArrayList<Polyline>();
    private List benches = new ArrayList<Marker>();
    private boolean trailsOn = true;
    private boolean boundaryOn = false;
    private boolean benchesOn = false;
    private boolean nearbyOn = false;

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        checkLocationPermission();

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

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

    @Override
    public boolean onMyLocationButtonClick() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude()), DEFAULT_ZOOM));
        return false;
    }

    @Override
    public void onPolylineClick(Polyline polyline) {

    }

    @Override
    public void onListFragmentInteraction(Marker item) {

    }

    private enum Fragments {
        MAP, ABOUT, CONTACT, GUIDELINES
    }

    private Fragments currentFragment = Fragments.MAP;

    private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mBenchesRef = mRootRef.child("benches");

        buildGoogleApiClient();

        handleIntent(getIntent());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        ImageView logo = (ImageView) findViewById(R.id.arb_logo);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

//        mItemFragment = new ItemFragment(mNearbyMarkers);
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.replace(R.id.bottom_sheet, mItemFragment);
//        transaction.addToBackStack(null);
//        transaction.commit();
//
        mBottomSheet = (FrameLayout) findViewById(R.id.bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.setPeekHeight(PEEK_HEIGHT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
        }
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

//        SearchManager searchManager =
//                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        SearchView searchView =
//                (SearchView) menu.findItem(R.id.search).getActionView();
//        searchView.setSearchableInfo(
//                searchManager.getSearchableInfo(getComponentName()));

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
        } else if (id == R.id.search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivityForResult(intent, 1);
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
        } else if (id == R.id.show_nearby) {
            if (nearbyOn)
                hideNearby();
            else
                showNearby();
        } else if (id == R.id.nav_about) {
            goToAbout();
        } else if (id == R.id.nav_guidelines) {
            goToGuidelines();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);

            }
        } else {
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        setupTrails();
        setupBoundary();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mThingsNearby.update(mMap, location);
//        mNearbyMarkers = mThingsNearby.getNearby();
        mNearbyItems = mThingsNearby.getNearby();

        //Find out how to just update data set
        mItemFragment = new ItemFragment(mNearbyMarkers);

        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            behavior.setPeekHeight(PEEK_HEIGHT);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.bottom_sheet, mItemFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction2 = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.rileylundquist.arb/http/host/path")
        );
        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction2);
    }

    @Override
    public void onStop() {
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction2 = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.rileylundquist.arb/http/host/path")
        );
        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction2);

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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        DetailFragment detailFragment = new DetailFragment();

//        detailFragment.setTitle(marker.getTitle());
//        detailFragment.setDescription(marker.getSnippet());
//        detailFragment.findImage();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.bottom_sheet, detailFragment.newInstance(marker.getTitle(), marker.getSnippet()));
        transaction.addToBackStack(null);
        transaction.commit();

        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            behavior.setPeekHeight(PEEK_HEIGHT);
        }

        return true;
    }

    private void setupTrails() {
        Log.d("DATA", "attempting to load trail data");
        DataReader reader = new DataReader();
        try {
            List trails = reader.readTrailData(getResources().openRawResource(R.raw.arb_trails));
            for (Object trail : trails) {
                trailLines.add(mMap.addPolyline(((PolylineOptions) trail)
                        .color(getResources().getColor(R.color.k_aqua))));

//                mTrailNames.add("Trail " + Integer.toString(trailLines.size()));
//                mMap.addMarker(new MarkerOptions()
//                        .title((String) mTrailNames.get(mTrailNames.size()-1))
//                        .position(((Polyline) trailLines.get(trailLines.size()-1)).getPoints().get(0)));
            }
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
                boundaryLines.add(mMap.addPolyline(((PolylineOptions) b).color(getResources().getColor(R.color.k_lime))));
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

    private void showBenches() {
        for (Object b : benches)
            ((Marker) b).setVisible(true);
        benchesOn = true;
    }

    private void hideBenches() {
        for (Object b : benches)
            ((Marker) b).setVisible(false);
        benchesOn = false;
    }

    private void showNearby() {
        mNearbyDisplayed = mNearbyMarkers;

        for (Object b : mNearbyDisplayed)
            ((Marker) b).setVisible(true);
        nearbyOn = true;

        BottomSheetBehavior behavior = BottomSheetBehavior.from(mBottomSheet);
        if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN)
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

//        ItemFragment itemFragment = new ItemFragment(mNearbyDisplayed);
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.replace(R.id.bottom_sheet, itemFragment);
//        transaction.addToBackStack(null);
//        transaction.commit();
//
//        FrameLayout bottomSheet = (FrameLayout) findViewById(R.id.bottom_sheet);
//        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
//        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideNearby() {
        for (Object b : mNearbyDisplayed)
            ((Marker) b).setVisible(false);
        nearbyOn = false;

//        FrameLayout bottomSheet = (FrameLayout) findViewById(R.id.bottom_sheet);
//        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
//        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void goToAbout() {
        AboutFragment aboutFragment = new AboutFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, aboutFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        currentFragment = Fragments.ABOUT;
    }

    private void goToGuidelines() {
        GuidelinesFragment guidelinesFragment = new GuidelinesFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, guidelinesFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        currentFragment = Fragments.GUIDELINES;
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

    public GoogleMap getmMap() {
        return mMap;
    }
}
