package com.example.monica.b_cycle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.monica.b_cycle.exceptions.LocationNotFoundAlertDialog;
import com.example.monica.b_cycle.exceptions.LocationNotFoundException;
import com.example.monica.b_cycle.exceptions.NoRouteFoundAlertDialog;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.TravelMode;
import com.example.monica.b_cycle.services.DatabaseService;
import com.example.monica.b_cycle.services.PlaceAutocompleteAdapter;
import com.example.monica.b_cycle.services.RouteBuilder;
import com.example.monica.b_cycle.services.RouteBuilderListener;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, RouteBuilderListener {

    public static List<Route> bikeRoutes = new ArrayList<>();

    private final String TAG = "MapsActivity";
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private final float DEFAULT_ZOOM = 15f;
    private final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));

    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private Boolean editMode;
    private List<Marker> markers;

    private ImageView mGpsButton;
    private ImageView mSearchButton;
    private FloatingActionButton mShowBikeLanesButton;
    private FloatingActionButton mEditModeButton;
    private FloatingActionButton mTravelModeButton;
    private FloatingActionButton mSaveButton;
    private FloatingActionButton mUndoButton;
    private FloatingActionMenu mMenu;

    private AutoCompleteTextView mOrigin;
    private AutoCompleteTextView mDestination;
    private TextView mDistance;
    private TextView mDuration;
    private TravelMode mTravelMode;
    private Route mCustomRoute;

    private ProgressBar mSpinner;
    private View mSpinnerBackground;

    private PlaceAutocompleteAdapter mDestinationAutocompleteAdapter;
    private PlaceAutocompleteAdapter mOriginPlaceAutocompleteAdapter;
    private GraphView mGraph;
    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;
    private GoogleApiClient mGoogleApiClient;
    private LatLng currentLocationCoordinates;

    private LatLng originLatLng;
    private LatLng destinationLatLng;

    /**
     * Initializes all elements of map and calls method for permission request.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        editMode = Boolean.FALSE;

        mOrigin = findViewById(R.id.input_origin);
        mDestination = findViewById(R.id.input_destination);
        mGpsButton = findViewById(R.id.ic_gps);
        mSearchButton = findViewById(R.id.ic_magnify);
        mShowBikeLanesButton = findViewById(R.id.ic_bike_lanes);
        mEditModeButton = findViewById(R.id.ic_edit_mode);
        mGraph = findViewById(R.id.graph);
        mDistance = findViewById(R.id.distance);
        mDuration = findViewById(R.id.duration);
        mSpinner = findViewById(R.id.progressBar1);
        mSpinnerBackground = findViewById(R.id.progress_background);
        mTravelMode = TravelMode.DRIVING;
        mTravelModeButton = findViewById(R.id.ic_change_travel_mode);
        mSaveButton = findViewById(R.id.save);
        mUndoButton = findViewById(R.id.undo);
        mMenu = findViewById(R.id.menu);

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
        initBikeLaneButton();
        initTravelModeButton();
        initEditModeButton();
        initGraph();
        initMapAction();

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
    }

    /*----------------------------------INITIALISATIONS----------------------------------*/

    /**
     * Applies style from res/raw/style_json.json to map and disables standard GPS button.
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
     * Initialises onMapLongClickListener
     */
    private void initMapAction() {
        mMap.setOnMapClickListener(latLng -> {
            mMenu.close(true);
        });
        mMap.setOnMapLongClickListener(latLng -> {
            if (editMode) {
                if (originLatLng == null) {
                    originLatLng = latLng;
                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                } else {
                    if (destinationLatLng != null) {
                        originLatLng = destinationLatLng;
                    }
                    destinationLatLng = latLng;
                    findPartialDirection();
                    markers.add(mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
                }
            } else {
                originLatLng = currentLocationCoordinates;
                destinationLatLng = latLng;
                findDirection(TravelMode.DRIVING);
            }
        });
    }

    /**
     * Sets the autocomplete click listener as the originLatLng search bar on click listener.
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
                try {
                    originLatLng = geoLocate();
                } catch (LocationNotFoundException e) {
                    Log.d(TAG, "Could not find the inputed originLatLng address.");
                }
            }
            return false;
        });
    }

    /**
     * Sets the autocomplete click listener as the destinationLatLng search bar on click listener.
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
                try {
                    destinationLatLng = geoLocate();
                    findDirection(mTravelMode);
                } catch (LocationNotFoundException e) {
                    Log.d(TAG, "Could not find the inputed destinationLatLng address.");
                }
            }
            return false;
        });
    }

    /**
     * Sets on click listeners for the GPS button.
     */
    private void initGpsButton() {
        mGpsButton.setOnClickListener(v -> getDeviceLocation());
        mGpsButton.setOnLongClickListener(v -> {
            getDeviceLocation();
            originLatLng = currentLocationCoordinates;
            mOrigin.setText("My Location");
            return true;
        });
    }

    /**
     * Sets on click listener for the Search button.
     */
    private void initSearchButton() {
        mSearchButton.setOnClickListener(v -> {
            if (originLatLng == null) {
                try {
                    originLatLng = geoLocate();
                } catch (LocationNotFoundException e) {
                    Log.d(TAG, "Could not find the inputed origin address.");
                }
            }
            if (destinationLatLng == null) {
                try {
                    destinationLatLng = geoLocate();
                } catch (LocationNotFoundException e) {
                    Log.d(TAG, "Could not find the inputed destination address.");
                }
            }
            if (originLatLng != null && destinationLatLng != null) {
                findDirection(mTravelMode);
                hideKeyboard();
            }
        });
    }

    /**
     * Sets on click listener for the Bike Lanes button.
     */
    private void initBikeLaneButton() {
        final boolean[] clicked = {false};
        mShowBikeLanesButton.setOnClickListener(v -> {
            if (clicked[0]) {
                mMap.clear();
                clicked[0] = false;
            } else {
                showAllBikeLanes();
                clicked[0] = true;
            }
        });
    }

    /**
     * Set on click listener for the Edit Mode button.
     */
    private void initEditModeButton() {
        mEditModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_30dp));
        mEditModeButton.setOnClickListener(v -> {
            if (editMode) {
                editMode = Boolean.FALSE;
                markers.forEach(Marker::remove);

                if (destinationLatLng != null) {
                    moveCamera(destinationLatLng, DEFAULT_ZOOM, true);
                    new RouteBuilder(originLatLng, destinationLatLng, mMap, mGraph, mDistance, mDuration, this, TravelMode.WALKING, Boolean.FALSE)
                            .onRouteFinderSuccess(new ArrayList() {{
                                add(mCustomRoute);
                            }});
                }
                mTravelModeButton.setVisibility(View.VISIBLE);
                mSaveButton.setVisibility(View.GONE);
                mUndoButton.setVisibility(View.GONE);
                mEditModeButton.setLabelText("Create custom route");
                mEditModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add_30dp));

                Toast.makeText(MapsActivity.this, "Edit Mode Off", Toast.LENGTH_SHORT).show();
            } else {
                markers = new ArrayList<>();
                originLatLng = null;
                destinationLatLng = null;
                mMap.clear();
                editMode = Boolean.TRUE;
                mTravelModeButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.VISIBLE);
                mUndoButton.setVisibility(View.VISIBLE);
                mEditModeButton.setLabelText("Done");
                mEditModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done_24dp));
                Toast.makeText(MapsActivity.this, "Edit Mode On", Toast.LENGTH_SHORT).show();
            }
            mMenu.close(false);
        });
    }

    /**
     * Set on click listener for the Travel Mode button.
     */
    private void initTravelModeButton() {
        mTravelModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_walk_24dp));
        mTravelModeButton.setOnClickListener(v -> {
            if (mTravelMode == TravelMode.WALKING) {
                mTravelMode = TravelMode.DRIVING;
                mTravelModeButton.setLabelText("Get sidewalk route");
                mTravelModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_walk_24dp));
                Toast.makeText(MapsActivity.this, "Travel Mode: Roads & bike lanes", Toast.LENGTH_SHORT).show();
            } else {
                mTravelMode = TravelMode.WALKING;
                mTravelModeButton.setLabelText("Get road route");
                mTravelModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_car_24dp));
                Toast.makeText(MapsActivity.this, "Travel Mode: Sidewalk & bike lanes", Toast.LENGTH_SHORT).show();
            }
            if (originLatLng != null && destinationLatLng != null) {
                findDirection(mTravelMode);
            }
        });
    }

    /**
     * Initialises the Elevation graph
     */
    private void initGraph() {
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(15);
        mGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        mGraph.getGridLabelRenderer().setGridColor(Color.rgb(115, 143, 167));
        mGraph.getGridLabelRenderer().setVerticalLabelsColor(Color.rgb(115, 143, 167));
        mGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);// remove horizontal x labels and line
    }


    /*----------------------------------PERMISSION----------------------------------*/

    /**
     * Checks if FINE_LOCATION and COARSE_LOCATION permissions are granted.
     * Sends permission request if not.
     */
    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);

        }
    }


    /*----------------------------------LOCATION----------------------------------*/

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
                        if (originLatLng == null) {
                            originLatLng = currentLocationCoordinates;
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

        LocationNotFoundAlertDialog locationNotFoundAlertDialog = new LocationNotFoundAlertDialog();
        locationNotFoundAlertDialog.show(getSupportFragmentManager(), "Location not found.");
        throw new LocationNotFoundException();
    }

    /**
     * Creates a new RouteBuilder to call upon the Google Directions request URL
     * to query about different routes from originLatLng to destinationLatLng.
     * Builds route as per user request.
     */
    private void findDirection(TravelMode travelMode) {
        mMap.clear();
        mSpinner.setVisibility(View.VISIBLE);
        mSpinnerBackground.setVisibility(View.VISIBLE);

        new RouteBuilder(originLatLng, destinationLatLng, mMap, mGraph, mDistance, mDuration, this, travelMode, Boolean.FALSE);
        mMap.addMarker(new MarkerOptions()
                .position(originLatLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        moveCamera(destinationLatLng, DEFAULT_ZOOM, true);
    }

    private void findPartialDirection() {
        mSpinner.setVisibility(View.VISIBLE);
        mSpinnerBackground.setVisibility(View.VISIBLE);

        new RouteBuilder(originLatLng, destinationLatLng, mMap, mGraph, mDistance, mDuration, this, TravelMode.WALKING, Boolean.TRUE);
        moveCamera(destinationLatLng, DEFAULT_ZOOM, false);
    }
    /*----------------------------------LOCATION AUTOCOMPLETE----------------------------------*/

    /**
     * Listener for the autocomplete suggestions given to the user for the originLatLng
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
     * Listener for the autocomplete suggestions given to the user for the destinationLatLng
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
     * Callback for the originLatLng AutoCompleteTextView
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsForOriginCallback = places -> {
        hideKeyboard();

        if (!places.getStatus().isSuccess()) {
            Log.d(TAG, "onResult: " + places.getStatus().toString());
            places.release();
        } else {
            originLatLng = places.get(0).getLatLng();
            places.release();
        }
    };

    /**
     * Callback for when we receive the Place object corespondent to the autocomplete selected by the user
     * Callback for the destinationLatLng AutoCompleteTextView
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsForDestinationCallback = places -> {
        hideKeyboard();

        if (!places.getStatus().isSuccess()) {
            Log.d(TAG, "onResult: " + places.getStatus().toString());
            places.release();
        } else {
            destinationLatLng = places.get(0).getLatLng();
            places.release();
            findDirection(mTravelMode);
        }
    };


    /*----------------------------------OTHER FUNCTIONS----------------------------------*/

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
     * Creates a Toast with the message given as argument
     *
     * @param message
     */
    public void notifyUser(String message) {
        Toast.makeText(MapsActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Creates polylines representative of all bicycle lanes.
     */
    private void showAllBikeLanes() {
        for (Route route : bikeRoutes) {
            PolylineOptions bikePoly = new PolylineOptions()
                    .geodesic(true)
                    .addAll(route.getPointList())
                    .color(Color.rgb(167, 121, 233))
                    .width(10);
            mMap.addPolyline(bikePoly);
            Toast.makeText(MapsActivity.this, "Showing all bike lanes", Toast.LENGTH_SHORT).show();
        }
    }

    void dummydatabase() {
        DatabaseService db = new DatabaseService(this);
//        db.addToDatabase(new LatLng(47.170095, 27.576226), new LatLng(47.190355, 27.559079), TravelMode.DRIVING);
//        db.addToDatabase(new LatLng(47.173751, 27.539233), new LatLng(47.173378, 27.560231), TravelMode.DRIVING);
//        db.addToDatabase(new LatLng(47.169090, 27.577570), new LatLng(47.162010, 27.594817), TravelMode.DRIVING);
//        db.addToDatabase(new LatLng(47.158793, 27.601097), new LatLng(47.156457, 27.603424), TravelMode.DRIVING);
//        db.addToDatabase(new LatLng(47.150937, 27.586846), new LatLng(47.134892, 27.573074), TravelMode.DRIVING);
//        db.addToDatabase(new LatLng(47.154486, 27.604114), new LatLng(47.152010, 27.588313), TravelMode.WALKING);
//        db.addToDatabase(new LatLng(47.165670, 27.579843), new LatLng(47.158991, 27.585694), TravelMode.WALKING);
//        db.addToDatabase(new LatLng(), new LatLng());
    }


    /*----------------------------------OVERRIDDEN INTERFACE METHODS----------------------------------*/

    /**
     * Method overridden from FragmentActivity
     * Checks if the request code corresponds to the LOCATION_PERMISSION_REQUEST_CODE and checks if permission for
     * said code has been granted. If so, it calls getDeviceLocation()
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                getDeviceLocation();
            }
        }
    }

    /**
     * Method overrun from GoogleApiClient
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed: " + connectionResult.getErrorMessage());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    /**
     * Method Overridden from RouteBuilderListener interface
     */
    @Override
    public void onFinish() {
        mSpinner.setVisibility(View.GONE);
        mSpinnerBackground.setVisibility(View.GONE);
    }

    /**
     * Method Overridden from RouteBuilderListener interface
     */
    @Override
    public void onRouteNotFound() {
        NoRouteFoundAlertDialog dialog = new NoRouteFoundAlertDialog();
        dialog.show(getSupportFragmentManager(), "No route found.");
        this.onFinish();
    }

    @Override
    public void onPartialRouteFound(Route partialRoute) {
        this.onFinish();
        if (mCustomRoute == null) {
            mCustomRoute = partialRoute;
        } else {
            mCustomRoute.getPointList().addAll(partialRoute.getPointList());
            mCustomRoute.setDestination(partialRoute.getDestination());
            mCustomRoute.getDistance().setValue(partialRoute.getDistance().getValue() + partialRoute.getDistance().getValue());
        }
    }
}
