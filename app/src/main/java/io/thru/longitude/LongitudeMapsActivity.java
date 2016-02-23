package io.thru.longitude;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.location.*;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
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
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.telephony.TelephonyManager;
import android.util.Log;

public class LongitudeMapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private List<MarkerOptions> mMapMarkers = new ArrayList<MarkerOptions>();
    protected LocationManager locationManager;

    private static final int LoginActivityComplete = 449;

    public static String baseUrl = "http://api.longitude.thru.io";


    public static String mAuthKey = "";
    public static String mDeviceID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        grantMeAllDangerousPermissions();
    }
    protected void shouldBeOkayToStartTheApplicationNow(){
        setContentView(R.layout.activity_longitude_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDeviceID = getDeviceID();

        if(this.mAuthKey.equals("")){
            Intent LoginIntent = new Intent(this, LongitudeLoginActivity.class);
            startActivityForResult(LoginIntent, LoginActivityComplete);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case LoginActivityComplete:
                Log.i("onActivityResult", "whoop whoop");
                getOwnLocation();
                getFriendLocations();
                storeAuthKey();
                break;
        }
    }

    protected void storeAuthKey(){
        //KeyStore ks = KeyStore.getInstance();

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
    }

    public String getDeviceID(){
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
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
        if(lastLocation != null) {
            Log.i("LongitudeGPS", "Last Location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
            updateOwnLocation(lastLocation);
        }else{
            Log.i("LongitudeGPS", "Last Location: UNAVAILABLE! :c");
        }
    }

    public void updateOwnLocation(Location location){
        if(! LongitudeMapsActivity.mAuthKey.isEmpty()){
            new UpdateLocationTask(this).execute(location);
        }else{
            Log.i("LongitudeGPS", "Not running UpdateLocationTask, there is no mAuthKey yet.");
        }
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
        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        updateOwnLocation(location);
        this.MapAddGoogleMapMarker(new MarkerOptions().position(myLatLng).title("Your Location"));
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
        if(mMapMarkers.size() > 0){
            for(MarkerOptions marker : mMapMarkers){
                builder.include(marker.getPosition());
            }

            LatLngBounds bounds = builder.build();
            int padding = 20; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }

    private void grantMeAllDangerousPermissions()
    {
        if (Build.VERSION.SDK_INT < 23) {
            shouldBeOkayToStartTheApplicationNow();
            return;
        }

        try
        {
            final int PERMISSIONS_REQUEST_CODE = 9613;

            // Scan manifest for dangerous permissions not already granted
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions == null)
                packageInfo.requestedPermissions = new String[0];

            final List<String> neededPermissions = new LinkedList<String>();
            for (String permission : packageInfo.requestedPermissions) {
                PermissionInfo permissionInfo = packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA);
                if (permissionInfo.protectionLevel != PermissionInfo.PROTECTION_DANGEROUS)
                    continue;
                if (checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
                    continue;
                neededPermissions.add(permission);
            }

            // No need to ask for any dangerous permissions
            if (neededPermissions.isEmpty()) {
                shouldBeOkayToStartTheApplicationNow();
                return;
            }

            final FragmentManager fragmentManager = getFragmentManager();
            final Fragment request = new Fragment() {

                @Override public void onStart()
                {
                    super.onStart();
                    if (Build.VERSION.SDK_INT >= 23) {
                        requestPermissions(neededPermissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
                    }
                }

                @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
                {
                    if (requestCode != PERMISSIONS_REQUEST_CODE)
                        return;

                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.remove(this);
                    fragmentTransaction.commit();

                    shouldBeOkayToStartTheApplicationNow();
                }
            };

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(0, request);
            fragmentTransaction.commit();
        }
        catch(Exception error)
        {
            Log.w("MyApplicationTag", String.format("Unable to query for permission: %s", error.getMessage()));
        }
    }
}
