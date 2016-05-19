package com.rileylundquist.arb;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by riley on 5/18/16.
 */
public enum  ServerConnection {
    INSTANCE;

    private final String TAG = "SERVER";

    private final String ABOUT_URL = "https://arb-server.herokuapp.com/about/";

    private ServerConnection() {
//        ConnectivityManager connMgr = (ConnectivityManager)
//                getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
//        if (networkInfo != null && networkInfo.isConnected()) {
//
//        } else {
//
//        }
    }

    public String getAbout() throws IOException {
        //InputStream stream = null;
        Log.d(TAG, "getAbout called");
        try {
            Log.d(TAG, "attempting connection");
            URL url = new URL(ABOUT_URL);
            Log.d(TAG, url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(TAG, conn.toString());
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            Log.d(TAG, conn.toString());

            int response = conn.getResponseCode();
            Log.d(TAG, "Response code: " + response);

            //return readString(stream, 500);

            StringBuilder result = null;
            try {
                Log.d(TAG, "getting input stream");
                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                Log.d("JSON Parser", "result: " + result.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }

            conn.disconnect();

            JSONObject jObj = null;
            try {
                jObj = new JSONObject(result.toString());
            } catch (JSONException e) {
                Log.e("JSON Parser", "Error parsing data " + e.toString());
            }

            // return JSON Object
            Log.d("JSON", jObj.toString());

            try {
                return jObj.getString("text");
            } catch (JSONException e) {
                e.printStackTrace();
                return "Error fetching data";
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "Error fetching data";
    }

//    public String readString(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
//        Reader reader = null;
//        reader = new InputStreamReader(stream, "UTF-8");
//        char[] buffer = new char[len];
//        reader.read(buffer);
//        return new String(buffer);
//    }
}
