package com.rileylundquist.arb;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap;

    private final LatLng ARB_CENTER = new LatLng(42.293469, -85.701);
    private final LatLngBounds ARB_AREA = new LatLngBounds(new LatLng(42.285, -85.71), new LatLng(42.30, -85.69));
    private final int DEFAULT_ZOOM = 15;

    private final String DEBUG_STRING = "MAP";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
        mMap.addMarker(new MarkerOptions().position(ARB_CENTER).title("Arb Center"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ARB_CENTER, DEFAULT_ZOOM));
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        if (checkPermission(LOCATION_SERVICE, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
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
}
