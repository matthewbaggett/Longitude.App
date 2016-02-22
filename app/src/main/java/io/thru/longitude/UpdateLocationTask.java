package io.thru.longitude;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class UpdateLocationTask extends AsyncTask<android.location.Location, Integer, Integer> {

    LongitudeMapsActivity caller;

    UpdateLocationTask(LongitudeMapsActivity caller) {
        this.caller = caller;
    }

    protected Integer doInBackground(android.location.Location... locations){
        int count = locations.length;
        int processedLocations = 0;
        for (int i = 0; i < count; i++) {
            processedLocations++;
            sendUpdatedLocation(locations[i]);

            // Escape early if cancel() is called
            if (isCancelled()) break;
        }
        return processedLocations;
    }

    protected void onPostExecute(Long parsedUrlsCount){

    }

    public void sendUpdatedLocation(android.location.Location location)
    {
        URL url;
        try {
            url = new URL(LongitudeMapsActivity.baseUrl + "/location");

            HttpClient httpclient = new DefaultHttpClient();

            // Prepare a request object
            Log.d("Longitude", "Go go go: " + url.toString());

            HttpPut httpPut = new HttpPut(url.toString());

            // Build the request object
            JSONObject locationUpdateRequest = new JSONObject();
            try{
                locationUpdateRequest.put("authKey", LongitudeMapsActivity.mAuthKey);
                locationUpdateRequest.put("deviceId", LongitudeMapsActivity.mDeviceID);
                locationUpdateRequest.put("location", location.getLatitude() + "," + location.getLongitude());
            } catch (Exception ex) {

            }

            // Execute the request
            HttpResponse response;
            try {
                String message = locationUpdateRequest.toString();
                httpPut.setEntity(new StringEntity(message, "UTF8"));
                httpPut.setHeader("Content-type", "application/json");
                response = httpclient.execute(httpPut);
                // Examine the response status
                Log.i("LongitudeApiLocation", response.getStatusLine().toString());

                // Get hold of the response entity
                HttpEntity entity = response.getEntity();
                // If the response does not enclose an entity, there is no need
                // to worry about connection release

                if (entity != null) {

                    // A Simple JSON Response Read
                    InputStream instream = entity.getContent();
                    String result = convertStreamToString(instream);
                    Log.d("LongitudeApiLocation", result);
                    // now you have the string representation of the JSON request
                    instream.close();

                    // Parse JSON
                    JSONObject locationUpdateResponse = new JSONObject(result);
                    String locationUpdateResponseStatus = locationUpdateResponse.getString("Status");
                    if(locationUpdateResponseStatus.toLowerCase().equals("okay")){
                        Log.d("LongitudeApiLocation", "hooray, location update response is good!");
                    }
                }

            } catch (Exception e) {
                Log.d("LongitudeApiLocation", "Exception: " + e.toString());
            }
        }catch(MalformedURLException mue){

        }


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
