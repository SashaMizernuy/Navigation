package ua.genesis.sasha.navigation;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.optimization.v1.MapboxOptimization;
import com.mapbox.api.optimization.v1.models.OptimizationResponse;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;

import android.location.Location;
import android.widget.Toast;

import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.View;
import android.widget.Button;

import com.mapbox.services.android.navigation.ui.v5.MapboxNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;



import static com.mapbox.core.constants.Constants.PRECISION_6;

public class MainActivity extends AppCompatActivity implements PermissionsListener, LocationEngineListener {
    private MapView mapView;
    Context context;

    private MapboxMap mapboxMap;
    private LatLng originCoord;
    private LatLng destinationCoord;
    private DirectionsRoute currentRoute;
    private DirectionsRoute route;

    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    private MapboxOptimization optimizedClient;
    private DirectionsRoute optimizedRoute;
    private DirectionsRoute optiCurRoute;
    List<Point> allPoint;

    private static final String FIRST = "first";
    private static final String ANY = "any";
    private static final String TEAL_COLOR = "#23D2BE";
    private static final int POLYLINE_WIDTH = 5;

    private Polyline optimizedPolyline;
    Point origin;
    private Point dest;
    Point point;
    List<Point> coord;
    private Marker destinationMarker;
    private Point originPosition;
    private Button button;
    private Button buttonStart;

    private NavigationMapRoute navigationMapRoute;






    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    class OnMapClickListenner implements View.OnClickListener, MapboxMap.OnMapClickListener {

        @Override
        public void onClick(View view) {
            Log.i("Script","OK");
        }

        @Override
        public void onMapClick(@NonNull LatLng point) {

            addDestinationMarker(point);
            originPosition = Point.fromLngLat(originCoord.getLongitude(), originCoord.getLatitude());
            allPoint.add(Point.fromLngLat(point.getLongitude(), point.getLatitude()));


            button.setEnabled(true);
            button.setBackgroundResource(R.color.mapboxBlue);

            buttonStart.setEnabled(true);
            buttonStart.setBackgroundResource(R.color.mapboxBlue);

        }
    }

    class OnButtonClickListenner implements View.OnClickListener{

        @Override
        public void onClick(View v) {

            boolean simulateRoute = true;
            NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(simulateRoute)
                    .build();
            // Call this method with Context from within an Activity
            NavigationLauncher.startNavigation(MainActivity.this, options);

        }
    }


    class OnButtonStartClickListenner implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            getRoute(originPosition,allPoint);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1Ijoic2FzaGFtaXplcm51eSIsImEiOiJjam10bDVzMDcwbWx0M3BudXZxbGVneXRuIn0.Vu3k42Vj2yiT_GFwBQ-dYQ");

        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        button = findViewById(R.id.startButton);
        buttonStart = findViewById(R.id.startButtonRoute);
        button.setOnClickListener(new OnButtonClickListenner());
        buttonStart.setOnClickListener(new OnButtonStartClickListenner());

        context=this;
        allPoint = new ArrayList<>();

        mapView.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(MapboxMap mapboxxMap) {
                mapboxMap = mapboxxMap;

                enableLocationPlugin();

                originCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
                origin = Point.fromLngLat(originCoord.getLongitude(), originCoord.getLatitude());
                mapboxMap.addOnMapClickListener(new OnMapClickListenner());
            }
        });

    }

    private void addDestinationMarker(LatLng point) {
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(point.getLatitude(), point.getLongitude()))
                .title("destenation"));
    }


    private void getRoute(Point origin,List<Point> points) {

        NavigationRoute.Builder builder = NavigationRoute.builder(context)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .profile(DirectionsCriteria.PROFILE_CYCLING);
        for (Point waypoint : points)
            builder.addWaypoint(waypoint);
        builder.build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.i("Script", "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.i("Script", "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.i("Script", "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);
                        Log.i("Script", "Response code: " + currentRoute);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.i("Script", "Error: " + throwable.getMessage());
                    }
                });
    }



    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional
            // parameter
            LocationLayerPlugin locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap);

            // Set the plugin's camera mode
            locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
            getLifecycle().addObserver(locationLayerPlugin);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onLocationChanged(Location location) {

    }
}
