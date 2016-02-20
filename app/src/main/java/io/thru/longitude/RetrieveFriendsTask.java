package io.thru.longitude;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class RetrieveFriendsTask extends AsyncTask<URL, Integer, Long> {

    private static final Friend[] NO_FRIENDS = {};

    protected Long doInBackground(URL... urls){
        int count = urls.length;
        long parsedUrls = 0;
        GoogleMap mMap = LongitudeMapsActivity.getGoogleMapObject();
        for (int i = 0; i < count; i++) {

            List<Friend> friends = connect(urls[i]);
            for(Friend friend : friends){
                Log.i("Friends", "Found " + friend.getFullName() + " at " + friend.getLocation().toString());
                LatLng friendLatLng = new LatLng(friend.getLocation().getX(), friend.getLocation().getY());
                mMap.addMarker(new MarkerOptions().position(friendLatLng).title(friend.getFullName()));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(friendLatLng));
            }
            parsedUrls++;

            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return parsedUrls;
    }

    public List<Friend> connect(URL url)
    {
        List<Friend> friends = new ArrayList<Friend>();
        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        Log.d("Longitude", "Go go go: " + url.toString());

        HttpPost httpPost = new HttpPost(url.toString());

        // Build the request object
        JSONObject friendsRequest = new JSONObject();
        try{
            friendsRequest.put("sessionKey", "sHXpjQOGzLbL71m");
        } catch (Exception ex) {

        }

        // Execute the request
        HttpResponse response;
        try {
            String message = friendsRequest.toString();
            httpPost.setEntity(new StringEntity(message, "UTF8"));
            httpPost.setHeader("Content-type", "application/json");
            response = httpclient.execute(httpPost);
            // Examine the response status
            Log.i("LongitudeApiStatus",response.getStatusLine().toString());

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release

            if (entity != null) {

                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result = convertStreamToString(instream);
                Log.d("LongitudeApiResponse", result);
                // now you have the string representation of the JSON request
                instream.close();

                // Parse JSON
                JSONObject friendsResponse = new JSONObject(result);
                JSONArray friendsToHydrate = friendsResponse.getJSONObject("Friends");
                Log.d("LongitudeApiResponse", friendsToHydrate.toString());

                // Make some friends and return them
                Iterator<JSONObject> iterator = friendsToHydrate.keys();

                while (iterator.hasNext()) {
                    JSONObject friendJSON = (JSONObject) iterator.next();
                    JSONObject nameJSON = (JSONObject) friendJSON.get("Name");
                    JSONObject locationJSON = (JSONObject) friendJSON.get("Location");
                    Friend friend = new Friend();
                    Location location = new Location();
                    friend.setFirstName(nameJSON.getString("Firstname"));
                    friend.setLastName(nameJSON.getString("Lastname"));
                    location.setLocation(locationJSON.getDouble("Lat"), locationJSON.getDouble("Long"));
                    friend.setLocation(location);
                    Log.d("Friend: Firstname", nameJSON.get("Firstname").toString());
                    friends.add(friend);
                }
            }

        } catch (Exception e) {
            Log.d("Longitude", "Exception: " + e.toString());
        }

        return friends;
    }

    private static String convertStreamToString(InputStream is) {
    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
