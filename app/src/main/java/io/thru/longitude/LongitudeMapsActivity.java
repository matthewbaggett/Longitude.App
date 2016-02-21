package io.thru.longitude;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.telephony.TelephonyManager;
import android.util.Log;

public class LongitudeMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private List<MarkerOptions> mMapMarkers = new ArrayList<MarkerOptions>();
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;

    public static String baseUrl = "http://api.longitude.thru.io";

    private GoogleApiClient mGoogleApiClient;

    private String mAuthKey = "";

    public void setAuthKey(String authKey){
        mAuthKey = authKey;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_longitude_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if(this.mAuthKey.equals("")){
            Intent LoginIntent = new Intent(this, LongitudeLoginActivity.class);
            startActivity(LoginIntent);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getFriendLocations();
        getOwnLocation();
        try{
            mMap.setMyLocationEnabled(true);
        }catch(SecurityException se){
            Log.d("LongitudeGPS", "Not allowed GPS :c");
        }
        updateProfile();
    }

    public void updateProfile(){
        String mPhoneNumber = updateProfileGetPhoneNumber();
        Log.i("UpdateProfile", "PhoneNumber: " + mPhoneNumber);
        String mUsername = updateProfileGetUsername();
        Log.i("UpdateProfile", "Username: " + mUsername);

        // TODO: Push this data back into API to fill in the blanks
    }

    private String updateProfileGetPhoneNumber(){
        TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return tMgr.getLine1Number();
    }

    private String updateProfileGetUsername(){
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");

            if (parts.length > 1)
                return parts[0];
        }
        return null;
    }

    public void getFriendLocations() {
        try {
            URL url = new URL(baseUrl + "/friends");
            new RetrieveFriendsTask(this).execute(url);
        }catch(MalformedURLException mue){
            Log.e("Longitude", "Malformed url", mue);
        }
    }

    public void getOwnLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        long t = 3;
        float d = 10;
        try {
            Log.i("LongitudeGPS", "Requesting Location Update");
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    t,
                    d,
                    this
            );
            Log.i("LongitudeGPS", "Requesting Location Update Requested");

        }catch(SecurityException se){
            Log.d("LongitudeGPS", "Not allowed GPS :c");
        }

        Location lastLocation = getLastLocationWrapper();
        Log.i("LongitudeGPS", "Last Location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
        updateOwnLocation(lastLocation);
    }

    public void updateOwnLocation(Location location){
        new UpdateLocationTask(this).execute(location);
    }

    private Location getLastLocationWrapper(){
        try {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocationGPS != null) {
                    Log.i("LongitudeGPS", "LastLocationWrapper source is GPS_PROVIDER");
                    return lastKnownLocationGPS;
                } else {
                    Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    Log.i("LongitudeGPS", "LastLocationWrapper source is PASSIVE_PROVIDER");
                    return loc;
                }
            } else {
                return null;
            }
        }catch(SecurityException se){
            Log.d("LongitudeGPS", "Not allowed GPS :c");
        }
        return null;
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        Log.d("LongitudeGPS", "Location Changed: (" + location.getLatitude() + ", " + location.getLongitude() + ")");
        LatLng friendLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        this.MapAddGoogleMapMarker(new MarkerOptions().position(friendLatLng).title("Your Location"));
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("LongitudeGPS", "GPS Provider disabled: " + provider.toString());
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("LongitudeGPS", "GPS Provider enabled: " + provider.toString());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("LongitudeGPS", "onStatusChanged! " + provider.toString());
    }

    public void MapAddGoogleMapMarker(MarkerOptions markerOptions){
        mMapMarkers.add(markerOptions);
        mMap.addMarker(markerOptions);
    }

    public void MapZoomToFit(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(MarkerOptions marker : mMapMarkers){
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        int padding = 20; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }
}
