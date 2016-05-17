package com.rileylundquist.arb;

import android.os.Message;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.google.android.gms.drive.internal.StringListResponse;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by riley on 5/17/16.
 */
public class DataReader {
    public List readTrailData(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readTrails(reader);
        }
        finally {
            reader.close();
        }
    }

    public List readTrails(JsonReader reader) throws IOException {
        List trails = new ArrayList<PolylineOptions>();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("trails")) {
                reader.beginObject();
                while (reader.hasNext()) {
                    name = reader.nextName();
                    if (name.equals("rte") && reader.peek() != JsonToken.NULL) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                name = reader.nextName();
                                if (name.equals("rtept") && reader.peek() != JsonToken.NULL)
                                    trails.add(readTrailPoints(reader));
                            }
                            reader.endObject();
                        }
                        reader.endArray();
                    }
                }
                reader.endObject();
            }
            else
                reader.skipValue();
        }
        reader.endObject();

        return trails;
    }

    public PolylineOptions readTrailPoints(JsonReader reader) throws IOException {
        PolylineOptions trail = new PolylineOptions();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            double lat = 0, lon = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("@lat"))
                    lat = reader.nextDouble();
                else if (name.equals("@lon"))
                    lon = reader.nextDouble();
            }
            reader.endObject();
            trail.add(new LatLng(lat, lon));
        }
        reader.endArray();

        Log.d("DATA", trail.getPoints().toString());

        return trail;
    }

    public List readBoundaryData(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readBoundary(reader);
        }
        finally {
            reader.close();
        }
    }

    public List readBoundary(JsonReader reader) throws IOException {
        List boundary = new ArrayList<PolylineOptions>();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("boundary")) {
                reader.beginObject();
                while (reader.hasNext()) {
                    name = reader.nextName();
                    if (name.equals("pt") && reader.peek() != JsonToken.NULL)
                        boundary.add(readBoundaryPoints(reader));
                }
                reader.endObject();
            }
            else
                reader.skipValue();
        }
        reader.endObject();

        return boundary;
    }

    public PolylineOptions readBoundaryPoints(JsonReader reader) throws IOException {
        PolylineOptions boundary = new PolylineOptions();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            double lat = 0, lon = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("@lat"))
                    lat = reader.nextDouble();
                else if (name.equals("@lon"))
                    lon = reader.nextDouble();
            }
            reader.endObject();
            boundary.add(new LatLng(lat, lon));
        }
        reader.endArray();

        Log.d("DATA", boundary.getPoints().toString());

        return boundary;
    }
}
