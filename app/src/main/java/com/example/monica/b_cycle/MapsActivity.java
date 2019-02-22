package com.example.monica.b_cycle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.monica.b_cycle.exceptions.LocationNotFoundException;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.services.DatabaseService;
import com.example.monica.b_cycle.services.PlaceAutocompleteAdapter;
import com.example.monica.b_cycle.services.RouteBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    public static List<Route> bikeRoutes = new ArrayList<>();

    private final String TAG = "MapsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private AutoCompleteTextView mOrigin;
    private AutoCompleteTextView mDestination;
    private ImageView mGpsButton;
    private ImageView mSearchButton;
    private PlaceAutocompleteAdapter mDestinationAutocompleteAdapter;
    private PlaceAutocompleteAdapter mOriginPlaceAutocompleteAdapter;

    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;
    private GoogleApiClient mGoogleApiClient;
    private LatLng currentLocationCoordinates;
    private List<PatternItem> polylinePattern = Arrays.asList(new Dot(), new Gap(20));

    private LatLng origin;
    private LatLng destination;

    /**
     * Initializes all elements of map and calls method for permission request.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mOrigin = findViewById(R.id.input_origin);
        mDestination = findViewById(R.id.input_destination);
        mGpsButton = findViewById(R.id.ic_gps);
        mSearchButton = findViewById(R.id.ic_magnify);

        mGeoDataClient = Places.getGeoDataClient(this);
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        dummydatabase();
        getLocationPermission();
        initMap();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * Applies style to map and initializes search bar.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        applyStyle();
        initOriginBar();
        initDestinationBar();
        initGpsButton();
        initSearchButton();

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

    }


    /**
     * Applies style found in res/raw/style_json.json to map and disables standard GPS button.
     */
    private void applyStyle() {
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    /**
     * Geolocates and moves camera to the first address found
     * that matches the text in the search bar.
     */
    private LatLng geoLocate() {
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocationName(mDestination.getText().toString(), 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            return new LatLng(address.getLatitude(), address.getLongitude());
        }
        throw new LocationNotFoundException();
    }

    /**
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Sets the autocomplete click listener as the origin search bar on click listener.
     * Sets the autocomplete adapter as the search bar's adapter.
     * Sets on editor listener for "done", "search", "down" or "enter".
     */
    private void initOriginBar() {
        mOrigin.setOnItemClickListener(mOriginAutoCompleteClickListener);
        mOriginPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGeoDataClient, LAT_LNG_BOUNDS, null);
        mOrigin.setAdapter(mOriginPlaceAutocompleteAdapter);
        mOrigin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                origin = geoLocate();
            }
            return false;
        });
    }

    /**
     * Sets the autocomplete click listener as the destination search bar on click listener.
     * Sets the autocomplete adapter as the search bar's adapter.
     * Sets on editor listener for "done", "search", "down" or "enter".
     */
    private void initDestinationBar() {
        mDestination.setOnItemClickListener(mDestinationAutoCompleteClickListener);
        mDestinationAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGeoDataClient, LAT_LNG_BOUNDS, null);
        mDestination.setAdapter(mDestinationAutocompleteAdapter);
        mDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                destination = geoLocate();
                findDirection();
            }
            return false;
        });
    }

    /**
     * Sets on click listener for the GPS button.
     */
    private void initGpsButton() {
        mGpsButton.setOnClickListener(v -> getDeviceLocation());
        mGpsButton.setOnLongClickListener(v-> {
            getDeviceLocation();
            origin = currentLocationCoordinates;
            mOrigin.setText("My Location");
            return true;
        });
    }

    private void initSearchButton() {
        mSearchButton.setOnClickListener(v -> {
            if (origin != null && destination != null) {
                findDirection();
            }
    });
}

    /**
     * If the user granted permission to his location, then it zooms in on his location.
     * If task was unsuccessful or permission was not granted, it notifies the user through a Toast.
     */
    public void getDeviceLocation() {
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mMap.setMyLocationEnabled(true);
                        Location currentLocation = (Location) task.getResult();
                        currentLocationCoordinates = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        if (origin == null) {
                            origin = currentLocationCoordinates;
                        }
                        moveCamera(currentLocationCoordinates, DEFAULT_ZOOM, false);
                    } else {
                        notifyUser("Unable to get current location");
                    }
                });
            } else {
                notifyUser("Permission not granted for device location");
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: " + e.getMessage());
        }
    }

    /**
     * Checks if FINE_LOCATION and COARSE_LOCATION permissions are granted.
     * Sends permission request if not.
     */
    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);

        }
    }

    /**
     * Creates a new RouteBuilder to call upon the Google Directions request URL
     * to query about different routes from origin to destination.
     * Builds route as per user request.
     */
    private void findDirection() {
        mMap.clear();
        new RouteBuilder(origin, destination, mMap);
        mMap.addMarker(new MarkerOptions()
                    .position(origin)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        moveCamera(destination, DEFAULT_ZOOM, true);
    }

    /**
     * Moves camera to the specified location, with a given zoom.
     * Optionally adds a marker.
     *
     * @param latLng       the location to which the camera will move
     * @param zoom         the zoom used on the location
     * @param markerWanted if true a marker will be added. If false, then the camera
     *                     will move to the selected location without adding a marker
     */
    public void moveCamera(LatLng latLng, float zoom, boolean markerWanted) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (markerWanted) {
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
    }

    /**
     * Hides soft input, implicitly the soft keyboard, when an element gets focused.
     */
    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Listener for the autocomplete suggestions given to the user for the origin
     */
    private AdapterView.OnItemClickListener mOriginAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final AutocompletePrediction item = mOriginPlaceAutocompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeBufferPendingResult.setResultCallback(mUpdatePlaceDetailsForOriginCallback);
        }
    };

    /**
     * Listener for the autocomplete suggestions given to the user for the destination
     */
    private AdapterView.OnItemClickListener mDestinationAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final AutocompletePrediction item = mDestinationAutocompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeBufferPendingResult.setResultCallback(mUpdatePlaceDetailsForDestinationCallback);
        }
    };

    /**
     * Callback for when we receive the Place object corespondent to the autocomplete selected by the user
     * Callback for the origin AutoCompleteTextView
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsForOriginCallback = places -> {
        hideKeyboard();

        if (!places.getStatus().isSuccess()) {
            Log.d(TAG, "onResult: " + places.getStatus().toString());
            places.release();
        } else {
            origin = places.get(0).getLatLng();
            places.release();
        }
    };

    /**
     * Callback for when we receive the Place object corespondent to the autocomplete selected by the user
     * Callback for the destination AutoCompleteTextView
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsForDestinationCallback = places -> {
        hideKeyboard();

        if (!places.getStatus().isSuccess()) {
            Log.d(TAG, "onResult: " + places.getStatus().toString());
            places.release();
        } else {
            destination = places.get(0).getLatLng();
            places.release();
            findDirection();
        }
    };


    /**
     * Method overrun from GoogleApiClient
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed: " + connectionResult.getErrorMessage());
    }

    void dummydatabase() {
        DatabaseService db = new DatabaseService(this);
//        db.addToDatabase(new LatLng(47.170095, 27.576226), new LatLng(47.190355, 27.559079));
//        db.addToDatabase(new LatLng(47.173751, 27.539233), new LatLng(47.173378, 27.560231));
//        db.addToDatabase(new LatLng(47.169090, 27.577570), new LatLng(47.162010, 27.594817));
//        db.addToDatabase(new LatLng(), new LatLng());
//        db.addToDatabase(new LatLng(), new LatLng());
//        db.addToDatabase(new LatLng(), new LatLng());
//        db.addToDatabase(new LatLng(), new LatLng());

    }

    public void notifyUser(String message) {
        Toast.makeText(MapsActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                getDeviceLocation();
            }
        }
    }


}
