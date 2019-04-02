package com.example.monica.b_cycle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.monica.b_cycle.dialogs.LocationNotFoundAlertDialog;
import com.example.monica.b_cycle.dialogs.NoRouteFoundAlertDialog;
import com.example.monica.b_cycle.exceptions.LocationNotFoundException;
import com.example.monica.b_cycle.model.PendingRoute;
import com.example.monica.b_cycle.model.Route;
import com.example.monica.b_cycle.model.SimpleAddress;
import com.example.monica.b_cycle.model.TravelMode;
import com.example.monica.b_cycle.services.DatabaseService;
import com.example.monica.b_cycle.services.ElevationFinder;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, RouteBuilderListener {

    public static List<Route> bikeRoutes = new ArrayList<>();

    private final String TAG = "MapsActivity";
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private final float DEFAULT_ZOOM = 15f;
    private final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));
    private final DatabaseService db = new DatabaseService(this);

    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private Boolean editMode;
    private List<Marker> markers;
    private List<Route> allPartialRoutes;
    private List<List<Polyline>> allCustomRoutePolylines;
    private List<Polyline> bikeLanePolylines;
    private List<PendingRoute> pendingRoutes;

    private ImageView mGpsButton;
    private ImageView mSearchButton;
    private ImageView mExpandButton;
    private FloatingActionButton mShowBikeLanesButton;
    private FloatingActionButton mEditModeButton;
    private FloatingActionButton mTravelModeButton;
    private FloatingActionButton mSaveButton;
    private FloatingActionButton mUndoButton;
    private FloatingActionButton mLogoutButton;
    private FloatingActionMenu mMenu;

    private RelativeLayout mOriginLayout;
    private RelativeLayout mDestinationLayout;
    private AutoCompleteTextView mOrigin;
    private AutoCompleteTextView mDestination;
    private TextView mDistance;
    private TextView mDuration;
    private TravelMode mTravelMode;
    private Route mRoute;
    private RouteBuilder mRouteBuilder;

    private ProgressBar mSpinner;
    private View mSpinnerBackground;
    private SlidingUpPanelLayout mSlidingPanel;
    private FloatingActionButton mApproveRouteButton;
    private FloatingActionButton mDisproveRouteButton;
    private FloatingActionButton mSkipButton;
    private FloatingActionButton mCloseReviewButton;
    private int pendingRouteCount = 0;

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

        mOriginLayout = findViewById(R.id.origin_layout);
        mDestinationLayout = findViewById(R.id.destination_layout);
        mOrigin = findViewById(R.id.input_origin);
        mDestination = findViewById(R.id.input_destination);
        mGpsButton = findViewById(R.id.gps_button);
        mSearchButton = findViewById(R.id.search_button);
        mExpandButton = findViewById(R.id.expand_button);
        mShowBikeLanesButton = findViewById(R.id.bike_lanes_button);
        mEditModeButton = findViewById(R.id.custom_route_button);
        mTravelModeButton = findViewById(R.id.travel_mode_button);

        mGraph = findViewById(R.id.graph);
        mDistance = findViewById(R.id.distance);
        mDuration = findViewById(R.id.duration);
        mSpinner = findViewById(R.id.progressBar1);
        mSpinnerBackground = findViewById(R.id.progress_background);
        mTravelMode = TravelMode.DRIVING;
        mSaveButton = findViewById(R.id.save);
        mUndoButton = findViewById(R.id.undo);
        mLogoutButton = findViewById(R.id.logout_button);
        mApproveRouteButton = findViewById(R.id.like_button);
        mDisproveRouteButton = findViewById(R.id.dislike_button);
        mCloseReviewButton = findViewById(R.id.exit_button);
        mSkipButton = findViewById(R.id.skip_button);
        mSlidingPanel = findViewById(R.id.sliding_panel);

        mMenu = findViewById(R.id.menu);

        mGeoDataClient = Places.getGeoDataClient(this);
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        getLocationPermission();
        initMap();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * Applies style to map and initializes all UI elements.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        applyStyle();
        initOriginBar();
        initDestinationBar();
        initGpsButton();
        initSearchButton();
        initExpandButton();
        initBikeLaneButton();
        initTravelModeButton();
        initEditModeButton();
        initSaveButton();
        initUndoButton();
        initLogoutButton();
        initGraph();
        initMapAction();
        initReviewLayout();

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


        @SuppressLint("ResourceType") View locationButton = mapFragment.getView().findViewById(0x2);
        locationButton.setBackgroundColor(Color.rgb(215, 101, 63));
    }

    private void addCustomRoutePoint(LatLng latLng) {
        if (originLatLng == null) {
            originLatLng = latLng;
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        } else {
            if (destinationLatLng != null) {
                originLatLng = destinationLatLng;
            }
            destinationLatLng = latLng;
            findPartialDirection();
        }
        markers.add(mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))));
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
                addCustomRoutePoint(latLng);
            } else {
                originLatLng = currentLocationCoordinates;
                destinationLatLng = latLng;
                mDestination.setText(latLng.latitude + ", " + latLng.longitude);
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
        mOrigin.setOnClickListener(v -> {
            mOrigin.getText().clear();
        });
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
        mDestination.setOnClickListener(v -> {
            mDestination.getText().clear();
        });
        mDestination.setOnItemClickListener(mDestinationAutoCompleteClickListener);
        mDestinationAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGeoDataClient, LAT_LNG_BOUNDS, null);
        mDestination.setAdapter(mDestinationAutocompleteAdapter);
        mDestination.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                try {
                    if (editMode) {
                        addCustomRoutePoint(geoLocate());
                    } else {
                        destinationLatLng = geoLocate();
                        findDirection(mTravelMode);
                    }
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
                if (editMode) {
                    addCustomRoutePoint(destinationLatLng);
                }
                findDirection(mTravelMode);
                hideKeyboard();
            }
        });
    }

    /**
     * Sets on click listener for Expand Button
     */
    private void initExpandButton() {
        mExpandButton.setOnClickListener(v -> {
            if (mOriginLayout.getVisibility() == View.VISIBLE) {
                mOriginLayout.setVisibility(View.GONE);
                mExpandButton.setImageResource(R.drawable.ic_expand_down);
                setMargins(mDestinationLayout, 10, 10, 10, 0);
            } else {
                mOriginLayout.setVisibility(View.VISIBLE);
                mExpandButton.setImageResource(R.drawable.ic_expand_up);
                setMargins(mDestinationLayout, 10, 65, 10, 0);
            }
        });
    }

    /**
     * Sets on click listener for the Save button
     */
    private void initSaveButton() {
        mSaveButton.setOnClickListener(v -> {
            db.addToDatabase(mRoute);
        });
    }

    /**
     * Sets on click listener for the Undo button
     */
    private void initUndoButton() {
        mUndoButton.setOnClickListener(v -> {
            if (allPartialRoutes.size() > 0) {
                LatLng lastDestination = markers.get(markers.size() - 2).getPosition();
                Route routeToBeUndone = allPartialRoutes.get(allPartialRoutes.size() - 1);
                Log.d("no.points", String.valueOf(mRoute.getPointList().size()));
                Log.d("no.points", String.valueOf(routeToBeUndone.getPointList().size()));

                mRoute.getPointList().removeAll(routeToBeUndone.getPointList());
                Log.d("no.points", String.valueOf(mRoute.getPointList().size()));
                mRoute.setDestination(new SimpleAddress(null, lastDestination));
                mRoute.getDistance().setValue(mRoute.getDistance().getValue() - routeToBeUndone.getDistance().getValue());

                originLatLng = lastDestination;
                destinationLatLng = lastDestination;

                markers.get(markers.size() - 1).remove();
                markers.remove(markers.size() - 1);
                allCustomRoutePolylines.get(allCustomRoutePolylines.size() - 1)
                        .forEach(Polyline::remove);
                allCustomRoutePolylines.remove(allCustomRoutePolylines.size() - 1);
            }
        });
    }

    /**
     * Sets on click listener for the Bike Lanes button.
     */
    private void initBikeLaneButton() {
        bikeLanePolylines = new ArrayList<>();
        AtomicBoolean firstClick = new AtomicBoolean(true);
        mShowBikeLanesButton.setOnClickListener(v -> {
            if (firstClick.get()) {
                db.startReview();
                firstClick.set(false);
            }
            if (bikeLanePolylines.size() != 0) {
                bikeLanePolylines.forEach(Polyline::remove);
                bikeLanePolylines = new ArrayList<>();
            } else {
                for (Route route : bikeRoutes) {
                    PolylineOptions bikePoly = new PolylineOptions()
                            .geodesic(true)
                            .addAll(route.getPointList())
                            .color(Color.rgb(255, 195, 66))
                            .width(10);
                    bikeLanePolylines.add(mMap.addPolyline(bikePoly));
                }
            }
        });
    }

    /**
     * Set on click listener for the Edit Mode button.
     */
    private void initEditModeButton() {
        mEditModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add));
        mEditModeButton.setOnClickListener(v -> {
            if (editMode) {
                editMode = Boolean.FALSE;
                mExpandButton.setImageResource(R.drawable.ic_expand_down);
                mExpandButton.setClickable(true);
                if (markers.size() > 1) {
                    moveCamera(destinationLatLng, DEFAULT_ZOOM, true);
                    new ElevationFinder(mRoute, mRouteBuilder).findElevations();
                }

                markers.forEach(Marker::remove);
                mTravelModeButton.setVisibility(View.VISIBLE);
                mSaveButton.setVisibility(View.GONE);
                mUndoButton.setVisibility(View.GONE);
                mEditModeButton.setLabelText("Create custom route");
                mEditModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_add));

                Toast.makeText(MapsActivity.this, "Edit Mode Off", Toast.LENGTH_SHORT).show();
            } else {
                refreshAllVariables();
                mRoute = null;
                editMode = Boolean.TRUE;
                mExpandButton.setImageResource(R.drawable.ic_add_orange);
                mExpandButton.setClickable(false);
                mTravelModeButton.setVisibility(View.GONE);
                mSaveButton.setVisibility(View.VISIBLE);
                mUndoButton.setVisibility(View.VISIBLE);
                mEditModeButton.setLabelText("Done");
                mEditModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_done));
                Toast.makeText(MapsActivity.this, "Edit Mode On", Toast.LENGTH_SHORT).show();
            }
            mRoute = null;
            mMenu.close(false);
        });
    }

    /**
     * Set on click listener for the Travel Mode button.
     */
    private void initTravelModeButton() {
        mTravelModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_walk));
        mTravelModeButton.setOnClickListener(v -> {
            if (mTravelMode == TravelMode.WALKING) {
                mTravelMode = TravelMode.DRIVING;
                mTravelModeButton.setLabelText("Get sidewalk route");
                mTravelModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_walk));
            } else {
                mTravelMode = TravelMode.WALKING;
                mTravelModeButton.setLabelText("Get road route");
                mTravelModeButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_car));
            }
            if (originLatLng != null && destinationLatLng != null) {
                findDirection(mTravelMode);
            }
            mMenu.close(false);
        });
    }

    private void initLogoutButton() {
        mLogoutButton.setOnClickListener(v -> {
            LoginActivity.signOut();
            startActivity(new Intent(MapsActivity.this, LoginActivity.class));
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

    private void initReviewLayout() {
        mCloseReviewButton.setOnClickListener(v -> {
            mMap.clear();
            mCloseReviewButton.setVisibility(View.GONE);
            mApproveRouteButton.setVisibility(View.GONE);
            mDisproveRouteButton.setVisibility(View.GONE);
            mSkipButton.setVisibility(View.GONE);
            mSlidingPanel.setTouchEnabled(true);
            mDestinationLayout.setVisibility(View.VISIBLE);
            mMenu.setVisibility(View.VISIBLE);
        });
        mApproveRouteButton.setOnClickListener(v -> {
            PendingRoute currentRoute = pendingRoutes.get(pendingRouteCount);
            currentRoute.getEmails().add(LoginActivity.email);
            bikeRoutes.add(currentRoute.getRoute());
            db.updateRoute(currentRoute);
            pendingRouteCount++;
            if (pendingRouteCount >= pendingRoutes.size()) {
                mCloseReviewButton.performClick();
            } else {
                drawRoute(pendingRoutes.get(pendingRouteCount));
            }
        });
        mSkipButton.setOnClickListener(v -> {
            pendingRouteCount++;
            if (pendingRouteCount >= pendingRoutes.size()) {
                mCloseReviewButton.performClick();
            } else {
                drawRoute(pendingRoutes.get(pendingRouteCount));
            }
        });
        mDisproveRouteButton.setOnClickListener(v -> {
            PendingRoute currentRoute = pendingRoutes.get(pendingRouteCount);
            currentRoute.getFlags().add(LoginActivity.email);
            db.updateRoute(currentRoute);

            pendingRouteCount++;
            if (pendingRouteCount >= pendingRoutes.size()) {
                mCloseReviewButton.performClick();
            } else {
                drawRoute(pendingRoutes.get(pendingRouteCount));
            }
        });
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
        mRoute = null;
        mSpinner.setVisibility(View.VISIBLE);
        mSpinnerBackground.setVisibility(View.VISIBLE);

        new RouteBuilder(originLatLng, destinationLatLng, mMap, mGraph, mDistance, mDuration, this, travelMode, Boolean.FALSE);
        mMap.addMarker(new MarkerOptions()
                .position(originLatLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        moveCamera(destinationLatLng, DEFAULT_ZOOM, true);
    }

    /**
     * Creates a new RouteBuilder with customRoute parameter set to Boolean.FALSE in order to
     * call RouteBuilderListener's onPartialRoutFound method.
     * Builds route as per user request.
     */
    private void findPartialDirection() {
        mSpinner.setVisibility(View.VISIBLE);
        mSpinnerBackground.setVisibility(View.VISIBLE);

        mRouteBuilder = new RouteBuilder(originLatLng, destinationLatLng, mMap, mGraph, mDistance, mDuration, this, TravelMode.WALKING, Boolean.TRUE);
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
            if (editMode) {
                addCustomRoutePoint(places.get(0).getLatLng());
            } else {
                destinationLatLng = places.get(0).getLatLng();
                places.release();

                findDirection(mTravelMode);
            }
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
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
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
     * Clears the map and returns route-related variables to their default value.
     */
    private void refreshAllVariables() {
        mDuration.setText("0 km");
        mDistance.setText("0min");
        mDestination.getText().clear();
        mOrigin.getText().clear();
        markers = new ArrayList<>();
        mGraph.removeAllSeries();
        mRoute = null;
        allPartialRoutes = new ArrayList<>();
        allCustomRoutePolylines = new ArrayList<>();
        originLatLng = null;
        destinationLatLng = null;
        mMap.clear();
    }

    private void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(getSizeInDp(left), getSizeInDp(top), getSizeInDp(right), getSizeInDp(bottom));
            view.requestLayout();
        }
    }

    private int getSizeInDp(int size) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, size, getResources()
                        .getDisplayMetrics());
    }

    private void stopSpinner() {
        mSpinner.setVisibility(View.GONE);
        mSpinnerBackground.setVisibility(View.GONE);
    }

    public void startReview(List<PendingRoute> pendingRoutes) {
        mDestinationLayout.setVisibility(View.GONE);
        mMenu.setVisibility(View.GONE);
        mSlidingPanel.setTouchEnabled(false);
        mSkipButton.setVisibility(View.VISIBLE);
        mCloseReviewButton.setVisibility(View.VISIBLE);
        mApproveRouteButton.setVisibility(View.VISIBLE);
        mDisproveRouteButton.setVisibility(View.VISIBLE);
        this.pendingRoutes = pendingRoutes;
        drawRoute(pendingRoutes.get(0));
    }

    public void drawRoute(PendingRoute pendingRoute) {
        mMap.clear();
        mMap.addPolyline(new PolylineOptions()
                .addAll(pendingRoute.getRoute().getPointList())
                .geodesic(true)
                .color(Color.rgb(215, 101, 63))
                .width(20));
        moveCamera(pendingRoute.getRoute().getPointList().get(0), DEFAULT_ZOOM, false);
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
    public void onFinish(Route finalRoute) {
        stopSpinner();
        mRoute = finalRoute;
    }

    /**
     * Method Overridden from RouteBuilderListener interface
     */
    @Override
    public void onRouteNotFound() {
        NoRouteFoundAlertDialog dialog = new NoRouteFoundAlertDialog();
        dialog.show(getSupportFragmentManager(), "No route found.");
        stopSpinner();
    }

    /**
     * Method Overridden from RouteBuilderListener interface
     */
    @Override
    public void onPartialRouteFound(Route partialRoute, List<Polyline> polylines) {
        stopSpinner();
        if (mRoute == null) {
            allPartialRoutes.add(partialRoute);
            allCustomRoutePolylines.add(polylines);
            mRoute = new Route(partialRoute);
            Log.d("no.pointsss", String.valueOf(mRoute.getPointList().size()));

        } else {
            allPartialRoutes.add(partialRoute);
            allCustomRoutePolylines.add(polylines);
            mRoute.getPointList().addAll(partialRoute.getPointList());
            Log.d("no.pointss", String.valueOf(mRoute.getPointList().size()));

            mRoute.setDestination(partialRoute.getDestination());
            mRoute.getDistance().setValue(mRoute.getDistance().getValue() + partialRoute.getDistance().getValue());
        }
    }
}
