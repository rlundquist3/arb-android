package com.rileylundquist.arb;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by riley on 8/2/16.
 */
public class ArbItem {

    private String mName;
    private String mScientificName;
    private String mDescription;
    private LatLng mLocation;
    private String mImage;

    public ArbItem(String name, String scientificName, String description, LatLng location, String image) {
        mName = name;
        mScientificName = scientificName;
        mDescription = description;
        mLocation = location;
        mImage = image;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getScientificName() {
        return mScientificName;
    }

    public void setScientificName(String mScientificName) {
        this.mScientificName = mScientificName;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public LatLng getLocation() {
        return mLocation;
    }

    public void setLocation(LatLng mLocation) {
        this.mLocation = mLocation;
    }

    public String getImage() {
        return mImage;
    }

    public void setImage(String mImage) {
        this.mImage = mImage;
    }
}
