package io.thru.longitude;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LongitudeLoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private AutoCompleteTextView mPhoneView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_longitude_login);
        setupActionBar();
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPhoneView = (AutoCompleteTextView) findViewById(R.id.phonenumber);
        // TODO: Autocomplete phone field

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPhoneView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String phonenumber = mPhoneView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, phonenumber, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 6;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LongitudeLoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPhonenumber;
        private final String mPassword;

        private String mAuthCode;

        UserLoginTask(String email, String phonenumber, String password) {
            mEmail = email;
            mPhonenumber = phonenumber;
            mPassword = password;

            Log.i("UserLogin", "Email    : " + email);
            Log.i("UserLogin", "Phone    : " + phonenumber);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Attempt authentication against the network service.
            return checkRemoteApiLogin(mEmail, mPhonenumber, mPassword);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                updateProfile();
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        protected Boolean checkRemoteApiLogin(String email, String phonenumber, String password){
            URL url;
            try {
                url = new URL(LongitudeMapsActivity.baseUrl + "/login");

                HttpClient httpclient = new DefaultHttpClient();

                // Prepare a request object
                HttpPost httpPost = new HttpPost(url.toString());

                // Build the request object
                JSONObject loginRequest = new JSONObject();
                try{
                    loginRequest.put("email", email);
                    loginRequest.put("phone", phonenumber);
                    loginRequest.put("password", password);
                } catch (Exception ex) {

                }

                // Execute the request
                HttpResponse response;
                try {
                    String message = loginRequest.toString();
                    Log.i("LongitudeLogin", "Request JSON: " + message);
                    httpPost.setEntity(new StringEntity(message, "UTF8"));
                    httpPost.setHeader("Content-type", "application/json");
                    response = httpclient.execute(httpPost);
                    // Examine the response status
                    Log.i("LongitudeLogin","Response Code: " + response.getStatusLine().toString());

                    // Get hold of the response entity
                    HttpEntity entity = response.getEntity();
                    // If the response does not enclose an entity, there is no need
                    // to worry about connection release

                    if (entity != null) {

                        // A Simple JSON Response Read
                        InputStream instream = entity.getContent();
                        String result = convertStreamToString(instream);
                        Log.d("LongitudeLogin", "Response JSON: " + result);
                        // now you have the string representation of the JSON request
                        instream.close();

                        // Parse JSON
                        JSONObject locationUpdateResponse = new JSONObject(result);
                        String loginResponseStatus = locationUpdateResponse.getString("Status");
                        Log.d("LongitudeLogin", "Response JSON Status: " + loginResponseStatus);
                        JSONObject loginAuthCode = locationUpdateResponse.getJSONObject("AuthCode");
                        Log.d("LongitudeLogin", "Response JSON Auth Code: " + loginAuthCode.getString("auth_code"));
                        if(loginResponseStatus.toLowerCase().equals("okay")){
                            mAuthCode = loginAuthCode.getString("auth_code");
                            LongitudeMapsActivity.mAuthKey = mAuthCode;
                            Log.d("LongitudeLogin", "hooray, login response is good! Authcode: " + mAuthCode);
                            return true;
                        }else{
                            return false;
                        }
                    }

                } catch (Exception e) {
                    Log.d("LongitudeLogin", "Exception: " + e.toString());
                }
            }catch(MalformedURLException mue){

            }
            return false;
        }

        private String convertStreamToString(InputStream is) {
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

        public void updateProfile(){
            String email = "";
            String phoneNumber = updateProfileGetPhoneNumber();
            Log.i("UserProfileUpdate", "PhoneNumber: " + phoneNumber);
            String userName = updateProfileGetUsername();
            Log.i("UserProfileUpdate", "Username: " + userName);

            String fullName = updateProfileGetOwnerName();
            Log.i("UserProfileUpdate", "Fullname: " + fullName);

            UserProfileUpdate updateProfile = new UserProfileUpdate(email, phoneNumber, userName);
            updateProfile.execute((Void) null);
        }

        private String updateProfileGetOwnerName(){
            final String[] projection = new String[]
                    { ContactsContract.Profile.DISPLAY_NAME };
            String name = null;
            final Uri dataUri = Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            final ContentResolver contentResolver = getContentResolver();
            final Cursor c = contentResolver.query(dataUri, projection, null, null, null);

            try
            {
                if (c.moveToFirst())
                {
                    name = c.getString(c.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
                }
            }
            finally
            {
                c.close();
            }
            return name;
        }
        private String updateProfileGetPhoneNumber(){
            TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            return tMgr.getLine1Number();
        }

        private String updateProfileGetUsername(){
            AccountManager manager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
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
    }

    public class UserProfileUpdate extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPhonenumber;
        private final String mUserName;


        private String mAuthCode;

        UserProfileUpdate(String email, String phoneNumber, String userName) {
            mEmail = email;
            mPhonenumber = phoneNumber;
            mUserName = userName;


            Log.i("UserProfileUpdate", "Email    : " + mEmail);
            Log.i("UserProfileUpdate", "Phone    : " + mPhonenumber);
            Log.i("UserProfileUpdate", "Username : " + mUserName);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Attempt authentication against the network service.
            return checkRemoteApiLogin(mEmail, mPhonenumber, mUserName);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        protected Boolean checkRemoteApiLogin(String email, String phoneNumber, String userName){
            URL url;
            try {
                url = new URL(LongitudeMapsActivity.baseUrl + "/profile");

                HttpClient httpclient = new DefaultHttpClient();

                // Prepare a request object
                HttpPut httpPut = new HttpPut(url.toString());

                // Build the request object
                JSONObject loginRequest = new JSONObject();
                try{
                    loginRequest.put("authKey", LongitudeMapsActivity.mAuthKey);
                    loginRequest.put("email", email);
                    loginRequest.put("phoneNumber", phoneNumber);
                } catch (Exception ex) {

                }

                // Execute the request
                HttpResponse response;
                try {
                    String message = loginRequest.toString();
                    Log.i("UserProfileUpdate", "Request JSON: " + message);
                    httpPut.setEntity(new StringEntity(message, "UTF8"));
                    httpPut.setHeader("Content-type", "application/json");
                    response = httpclient.execute(httpPut);
                    // Examine the response status
                    Log.i("UserProfileUpdate","Response Code: " + response.getStatusLine().toString());

                    // Get hold of the response entity
                    HttpEntity entity = response.getEntity();
                    // If the response does not enclose an entity, there is no need
                    // to worry about connection release

                    if (entity != null) {

                        // A Simple JSON Response Read
                        InputStream instream = entity.getContent();
                        String result = convertStreamToString(instream);
                        Log.d("UserProfileUpdate", "Response JSON: " + result);
                        // now you have the string representation of the JSON request
                        instream.close();

                        // Parse JSON
                        JSONObject locationUpdateResponse = new JSONObject(result);
                        String loginResponseStatus = locationUpdateResponse.getString("Status");
                        Log.d("UserProfileUpdate", "Response JSON Status: " + loginResponseStatus);
                        if(loginResponseStatus.toLowerCase().equals("okay")){
                            Log.d("UserProfileUpdate", "hooray, profile update says okay!");
                            return true;
                        }else{
                            return false;
                        }
                    }

                } catch (Exception e) {
                    Log.d("UserProfileUpdate", "Exception: " + e.toString());
                }
            }catch(MalformedURLException mue){

            }
            return false;
        }

        private String convertStreamToString(InputStream is) {
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
}

