package com.rileylundquist.arb;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by riley on 7/1/16.
 */
public class ThingsNearby {

    private List mMarkers = new ArrayList<MarkerOptions>();
    private List mArbItems = new ArrayList<ArbItem>();
    private List mNearby = new ArrayList<ArbItem>(10);
    private List mDistances = new ArrayList();
    private DatabaseReference mRootRef;
    private DatabaseReference mHerbaceousRef;
    private DatabaseReference mWildflowersRef;
    private DatabaseReference mBirdSignsRef;
    private DatabaseReference mHerpSignsRef;
    private ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            LatLng coords = new LatLng(dataSnapshot.child("Latitude").getValue(Double.class),
                    dataSnapshot.child("Longitude").getValue(Double.class));
            String title = dataSnapshot.child("Title").getValue(String.class);
            String scientific = dataSnapshot.child("Scientific Name").getValue(String.class);
            String description = dataSnapshot.child("Comment").getValue(String.class);
            String photo = dataSnapshot.child("Photo").getValue(String.class);
            mArbItems.add(new ArbItem(title, scientific, description, coords, photo));

//            mMarkers.add(new MarkerOptions().position(coords)
//                    .visible(false)
//                    .title(dataSnapshot.child("Species").getValue(String.class))
//                    .snippet(dataSnapshot.child("Comment").getValue(String.class))
//            );
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            LatLng coords = new LatLng(dataSnapshot.child("Latitude").getValue(Double.class),
                    dataSnapshot.child("Longitude").getValue(Double.class));
            MarkerOptions item = (MarkerOptions) mMarkers.get(dataSnapshot.child("FID").getValue(Integer.class));
            item.position(coords);
            item.title(dataSnapshot.child("Species").getValue(String.class));
            item.snippet(dataSnapshot.child("Comment").getValue(String.class));
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            mMarkers.remove(dataSnapshot.child("FID").getValue(Integer.class));
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public ThingsNearby() {
        setup();
    }

    public ThingsNearby(List markers) {
        mMarkers = markers;
        setup();
    }

    /**
     * TODO: wildflowers & other data defined by trail rather than exact location
     * New trail data files will be stored in DB and trails node in DB will have
     * the file names. Then we'll be able to use the current location to check
     * whether the user is on/near any given trail and give info on what to look
     * for on that or nearby trails
     */
    public void update(GoogleMap map, Location location) {
        mNearby.clear();
        mDistances.clear();

        for (int i=0; i<mArbItems.size(); i++) {
            ArbItem item = (ArbItem) mArbItems.get(i);
            mDistances.add(new DistanceItem(i, distance(location, item.getLocation())));
        }

//        for (int i=0; i<mMarkers.size(); i++) {
//            MarkerOptions item = (MarkerOptions) mMarkers.get(i);
//            mDistances.add(new DistanceItem(i, distance(location, item)));
//        }
        Arrays.sort(new List[]{mDistances});
        for (int i=0; i<10; i++)
            mNearby.add(mArbItems.get(((DistanceItem) mDistances.get(i)).getIndex()));
    }

    public List getNearby() {
        return mNearby;
    }

    private void setup() {
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mHerbaceousRef = mRootRef.child("herbaceous");
        mWildflowersRef = mRootRef.child("wildflowers");
        mBirdSignsRef = mRootRef.child("bird-signs");
        mHerpSignsRef = mRootRef.child("herp-signs");
        mHerbaceousRef.addChildEventListener(childEventListener);
        mBirdSignsRef.addChildEventListener(childEventListener);
        mHerpSignsRef.addChildEventListener(childEventListener);
    }

    private double distance(Location location, LatLng itemLocation) {
        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());

        return Math.sqrt(Math.pow(itemLocation.latitude - current.latitude, 2) +
                        Math.pow(itemLocation.longitude - current.longitude, 2));
    }

    public class DistanceItem {

        private int index;
        private double distance;

        public DistanceItem(int index, double distance) {
            this.index = index;
            this.distance = distance;
        }

        public int compareTo(DistanceItem item) {
            if (distance < item.getDistance())
                return -1;
            else if (distance > item.getDistance())
                return 1;
            else
                return 0;
        }

        public int getIndex() {
            return index;
        }

        public double getDistance() {
            return distance;
        }
    }
}
