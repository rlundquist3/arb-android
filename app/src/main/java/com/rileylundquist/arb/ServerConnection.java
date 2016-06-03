package com.rileylundquist.arb;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

    private final String BASE_URL = "https://arb-server.herokuapp.com/";

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

    public String getData(String collection) throws IOException {
        try {
            Log.d(TAG, "attempting connection");
            URL url = new URL(BASE_URL + collection + "/");
            Log.d(TAG, url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(TAG, conn.toString());
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            int response = conn.getResponseCode();
            Log.d(TAG, "Response code: " + response);

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

            Log.d(TAG, jObj.toString());
            return jObj.toString();
//            try {
//                return jObj.getString("text");
//            } catch (JSONException e) {
//                e.printStackTrace();
//                return "Error fetching data";
//            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return "Error fetching data";
    }

    public String getAbout() throws IOException {
        try {
            Log.d(TAG, "attempting connection");
            URL url = new URL(BASE_URL + "about/");
            Log.d(TAG, url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Log.d(TAG, conn.toString());
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            int response = conn.getResponseCode();
            Log.d(TAG, "Response code: " + response);

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

    public boolean sendEmail(String name, String email, String body) throws IOException {
        try {
            Log.d(TAG, "attempting connection");
            URL url = new URL(BASE_URL + "mail/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
//            conn.connect();
//
//            int response = conn.getResponseCode();
//            Log.d(TAG, "Response code: " + response);

            StringBuilder result = null;
            try {
                OutputStream out = new BufferedOutputStream(conn.getOutputStream());

                JSONObject data = new JSONObject();
                try {
                    data.put("name", name);
                    data.put("email", email);
                    data.put("body", body);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                out.write(data.toString().getBytes());

                int response = conn.getResponseCode();
                Log.d(TAG, "Response code: " + response);

                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
