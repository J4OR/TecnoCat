package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreMapActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST = 410;
    private static final String STORE_ADDRESS = "Carrera 18 #6a-15, Aguachica, Cesar, Colombia";
    private static final double STORE_LATITUDE = 8.307764;
    private static final double STORE_LONGITUDE = -73.616104;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private WebView webMap;
    private TextView distanceLabel;
    private TextView walkSummary;
    private TextView driveSummary;
    private Button navigationButton;
    private LinearLayout walkCard;
    private LinearLayout driveCard;
    private LinearLayout speedPanel;
    private TextView currentSpeedLabel;
    private Location currentLocation;
    private Location previousSpeedLocation;
    private String pendingMapScript;
    private boolean mapReady;
    private boolean navigationActive;
    private boolean startNavigationAfterPermission;
    private Location lastRouteOrigin;
    private long lastRouteRequestTime;
    private LocationManager locationManager;
    private RouteInfo walkingRoute;
    private RouteInfo drivingRoute;
    private boolean walkingSelected;
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLocation = location;
            updateCurrentLocationMarker(location);
            if (navigationActive) updateCurrentSpeed(location);
            if (drivingRoute == null && walkingRoute == null) findStoreAndRoute();
            if (navigationActive) {
                maybeRefreshNavigationRoute(location);
            } else if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.store_map);

        webMap = findViewById(R.id.web_map);
        distanceLabel = findViewById(R.id.tv_store_distance);
        walkSummary = findViewById(R.id.tv_walk_summary);
        driveSummary = findViewById(R.id.tv_drive_summary);
        walkCard = findViewById(R.id.card_walk);
        driveCard = findViewById(R.id.card_drive);
        speedPanel = findViewById(R.id.speed_panel);
        currentSpeedLabel = findViewById(R.id.tv_current_speed);
        navigationButton = findViewById(R.id.btn_navigation);
        walkCard.setOnClickListener(v -> selectTravelMode(true));
        driveCard.setOnClickListener(v -> selectTravelMode(false));
        navigationButton.setOnClickListener(v -> {
            if (navigationActive) stopNavigation();
            else startNavigation();
        });
        selectTravelMode(false);
        WebSettings settings = webMap.getSettings();
        enableLeafletJavaScript(settings);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString("TechnoCatAndroid/1.0 (com.example.myapplication)");
        webMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mapReady = true;
                webMap.evaluateJavascript("setNavigationActive(" + navigationActive + ");", null);
                if (currentLocation != null) updateCurrentLocationMarker(currentLocation);
                showPendingRoute();
            }
        });
        webMap.loadDataWithBaseURL("https://www.openstreetmap.org/", mapHtml(), "text/html", "UTF-8", null);

        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.MAP);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestUserLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.MAP);
    }

    private void requestUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST);
            distanceLabel.setText(R.string.store_map_location_needed);
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location best = null;
            boolean hasFineLocation = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            for (String provider : locationManager.getProviders(true)) {
                if (LocationManager.GPS_PROVIDER.equals(provider) && !hasFineLocation) continue;
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
            }
            if (best != null) {
                currentLocation = best;
                updateCurrentLocationMarker(best);
                findStoreAndRoute();
            } else {
                distanceLabel.setText(R.string.store_map_waiting_location);
                boolean listening = false;
                for (String provider : locationManager.getProviders(true)) {
                    if (LocationManager.GPS_PROVIDER.equals(provider) && !hasFineLocation) continue;
                    locationManager.requestLocationUpdates(provider, 1000, 0, locationListener);
                    listening = true;
                }
                if (!listening) distanceLabel.setText(R.string.store_map_enable_location);
            }
        } catch (SecurityException | IllegalArgumentException e) {
            distanceLabel.setText(R.string.store_map_location_failed);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) granted |= result == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                requestUserLocation();
                if (startNavigationAfterPermission) {
                    startNavigationAfterPermission = false;
                    startNavigation();
                }
            } else {
                startNavigationAfterPermission = false;
                distanceLabel.setText(R.string.store_map_grant_permission);
            }
        }
    }

    private void findStoreAndRoute() {
        if (currentLocation == null) return;
        final double startLat = currentLocation.getLatitude();
        final double startLon = currentLocation.getLongitude();
        lastRouteOrigin = new Location(currentLocation);
        lastRouteRequestTime = System.currentTimeMillis();
        runOnUiThread(() -> distanceLabel.setText(R.string.store_map_searching_store));

        networkExecutor.execute(() -> {
            try {
                final double storeLat = STORE_LATITUDE;
                final double storeLon = STORE_LONGITUDE;
                runOnUiThread(() -> distanceLabel.setText(
                        getString(R.string.store_map_store_label, STORE_ADDRESS)));

                try {
                    RouteInfo route = requestRoute(startLat, startLon, storeLat, storeLon, false);
                    drivingRoute = route;
                    runOnUiThread(() -> {
                        driveSummary.setText(route.summary());
                        if (!walkingSelected) showSelectedRoute();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> driveSummary.setText(R.string.store_map_route_unavailable));
                }

                Thread.sleep(1100L);
                try {
                    RouteInfo route = requestRoute(startLat, startLon, storeLat, storeLon, true);
                    walkingRoute = route;
                    runOnUiThread(() -> {
                        walkSummary.setText(route.summary());
                        if (walkingSelected) showSelectedRoute();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> walkSummary.setText(R.string.store_map_route_unavailable));
                }
            } catch (Exception e) {
                runOnUiThread(() -> distanceLabel.setText(R.string.store_map_route_error));
            }
        });
    }

    private RouteInfo requestRoute(double startLat, double startLon, double destLat,
                                   double destLon, boolean walking) throws Exception {
        String server = walking ? "routed-foot" : "routed-car";
        String routeUrl = String.format(Locale.US,
                "https://routing.openstreetmap.de/%s/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                server, startLon, startLat, destLon, destLat);
        JSONObject routeRoot = new JSONObject(httpGet(routeUrl));
        JSONArray routes = routeRoot.optJSONArray("routes");
        if (routes == null || routes.length() == 0) throw new Exception("No se encontró la ruta.");
        JSONObject route = routes.getJSONObject(0);
        return new RouteInfo(route.getDouble("distance"), route.getDouble("duration"),
                route.getJSONObject("geometry").getJSONArray("coordinates"), startLat, startLon, destLat, destLon);
    }

    private void selectTravelMode(boolean walking) {
        walkingSelected = walking;
        styleModeCard(walkCard, walking);
        styleModeCard(driveCard, !walking);
        showSelectedRoute();
        if (navigationActive && currentLocation != null) {
            lastRouteOrigin = null;
            lastRouteRequestTime = 0L;
            maybeRefreshNavigationRoute(currentLocation);
        }
    }

    private void styleModeCard(LinearLayout card, boolean selected) {
        card.setBackgroundResource(selected ? R.drawable.bg_add_button : R.drawable.bg_category_card);
        int color = ContextCompat.getColor(this, selected ? R.color.white : R.color.text_black);
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) ((TextView) child).setTextColor(color);
        }
    }

    private void showSelectedRoute() {
        RouteInfo route = walkingSelected ? walkingRoute : drivingRoute;
        if (route == null) return;
        pendingMapScript = String.format(Locale.US,
                "showRoute(%f,%f,%f,%f,%s);", route.startLat, route.startLon,
                route.destLat, route.destLon, route.coordinates.toString());
        showPendingRoute();
    }

    private void startNavigation() {
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasLocationPermission) {
            startNavigationAfterPermission = true;
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST);
            distanceLabel.setText(R.string.store_map_navigation_permission);
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            distanceLabel.setText(R.string.store_map_navigation_location_unavailable);
            return;
        }

        try {
            locationManager.removeUpdates(locationListener);
            boolean hasFineLocation = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean listening = false;
            for (String provider : locationManager.getProviders(true)) {
                if (LocationManager.GPS_PROVIDER.equals(provider) && !hasFineLocation) continue;
                locationManager.requestLocationUpdates(provider, 500, 0, locationListener);
                listening = true;
            }
            if (!listening) {
                distanceLabel.setText(R.string.store_map_enable_location);
                return;
            }

            navigationActive = true;
            navigationButton.setText(R.string.store_map_stop_navigation);
            updateNavigationButtonColor(true);
            speedPanel.setVisibility(View.VISIBLE);
            currentSpeedLabel.setText(R.string.store_map_speed_waiting);
            previousSpeedLocation = null;
            distanceLabel.setText(R.string.store_map_navigation_active);
            if (mapReady) webMap.evaluateJavascript("setNavigationActive(true);", null);
            if (currentLocation != null) {
                updateCurrentLocationMarker(currentLocation);
                maybeRefreshNavigationRoute(currentLocation);
            }
        } catch (SecurityException | IllegalArgumentException e) {
            navigationActive = false;
            locationManager.removeUpdates(locationListener);
            navigationButton.setText(R.string.store_map_start_navigation);
            updateNavigationButtonColor(false);
            speedPanel.setVisibility(View.GONE);
            distanceLabel.setText(R.string.store_map_navigation_location_unavailable);
        }
    }

    private void stopNavigation() {
        navigationActive = false;
        startNavigationAfterPermission = false;
        previousSpeedLocation = null;
        if (locationManager != null) locationManager.removeUpdates(locationListener);
        if (navigationButton != null) {
            navigationButton.setText(R.string.store_map_start_navigation);
            updateNavigationButtonColor(false);
        }
        if (speedPanel != null) speedPanel.setVisibility(View.GONE);
        if (mapReady) webMap.evaluateJavascript("setNavigationActive(false);", null);
        distanceLabel.setText(getString(R.string.store_map_store_label, STORE_ADDRESS));
    }

    private void updateNavigationButtonColor(boolean navigating) {
        if (navigationButton == null) return;
        int color = getResources().getColor(
                navigating ? R.color.logout_red : R.color.brand_blue,
                getTheme()
        );
        navigationButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void updateCurrentSpeed(Location location) {
        if (currentSpeedLabel == null || location == null) return;
        float speedMetersPerSecond = -1f;
        if (location.hasSpeed()) {
            speedMetersPerSecond = location.getSpeed();
        } else if (previousSpeedLocation != null) {
            long elapsedNanos = location.getElapsedRealtimeNanos()
                    - previousSpeedLocation.getElapsedRealtimeNanos();
            float elapsedSeconds = elapsedNanos / 1_000_000_000f;
            if (elapsedSeconds >= 0.25f && elapsedSeconds <= 10f) {
                float accuracyMeters = Math.max(
                        location.hasAccuracy() ? location.getAccuracy() : 0f,
                        previousSpeedLocation.hasAccuracy() ? previousSpeedLocation.getAccuracy() : 0f);
                float movedMeters = previousSpeedLocation.distanceTo(location);
                float movementThreshold = Math.max(1.5f, accuracyMeters * 0.35f);
                speedMetersPerSecond = movedMeters > movementThreshold
                        ? movedMeters / elapsedSeconds : 0f;
            }
        }

        previousSpeedLocation = new Location(location);
        if (speedMetersPerSecond < 0f) {
            currentSpeedLabel.setText(R.string.store_map_speed_waiting);
            return;
        }
        int speedKmh = Math.max(0, Math.round(speedMetersPerSecond * 3.6f));
        currentSpeedLabel.setText(getString(R.string.store_map_speed_value, speedKmh));
    }

    private void updateCurrentLocationMarker(Location location) {
        if (webMap == null || !mapReady || location == null) return;
        String script = String.format(Locale.US, "updateUserLocation(%.6f,%.6f);",
                location.getLatitude(), location.getLongitude());
        webMap.evaluateJavascript(script, null);
    }

    private void maybeRefreshNavigationRoute(Location location) {
        if (!navigationActive || location == null) return;
        long now = System.currentTimeMillis();
        if (lastRouteOrigin != null && location.distanceTo(lastRouteOrigin) < 50f) return;
        if (now - lastRouteRequestTime < 20000L) return;

        Location routeOrigin = new Location(location);
        lastRouteOrigin = routeOrigin;
        lastRouteRequestTime = now;
        boolean walking = walkingSelected;
        networkExecutor.execute(() -> {
            try {
                RouteInfo route = requestRoute(routeOrigin.getLatitude(), routeOrigin.getLongitude(),
                        STORE_LATITUDE, STORE_LONGITUDE, walking);
                if (walking) walkingRoute = route;
                else drivingRoute = route;
                runOnUiThread(() -> {
                    if (!navigationActive || isFinishing()) return;
                    if (walking) walkSummary.setText(route.summary());
                    else driveSummary.setText(route.summary());
                    showSelectedRoute();
                    if (currentLocation != null) updateCurrentLocationMarker(currentLocation);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (navigationActive && !isFinishing()) {
                        distanceLabel.setText(R.string.store_map_route_error);
                    }
                });
            }
        });
    }

    private static class RouteInfo {
        final double distanceMeters;
        final double durationSeconds;
        final JSONArray coordinates;
        final double startLat, startLon, destLat, destLon;

        RouteInfo(double distanceMeters, double durationSeconds, JSONArray coordinates,
                  double startLat, double startLon, double destLat, double destLon) {
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
            this.coordinates = coordinates;
            this.startLat = startLat;
            this.startLon = startLon;
            this.destLat = destLat;
            this.destLon = destLon;
        }

        String summary() {
            long minutes = Math.max(1, Math.round(durationSeconds / 60.0));
            String time = minutes >= 60
                    ? String.format(Locale.getDefault(), "%dh %02d min", minutes / 60, minutes % 60)
                    : String.format(Locale.getDefault(), "%d min", minutes);
            return String.format(Locale.getDefault(), "%s · %.1f km", time, distanceMeters / 1000.0);
        }
    }

    private String httpGet(String urlString) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "TechnoCatAndroid/1.0 (store map)");
        try (InputStream stream = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        } finally {
            connection.disconnect();
        }
    }

    private void showPendingRoute() {
        if (mapReady && pendingMapScript != null) webMap.evaluateJavascript(pendingMapScript, null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void enableLeafletJavaScript(WebSettings settings) {
        // Leaflet needs JavaScript for the map; the WebView exposes no JavaScript bridge.
        settings.setJavaScriptEnabled(true);
    }

    private String mapHtml() {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<style>html,body,#map{height:100%;margin:0}.map-pin{width:42px;height:42px;border-radius:50% 50% 50% 8px;"
                + "transform:rotate(-45deg);display:flex;align-items:center;justify-content:center;border:3px solid #fff;"
                + "box-shadow:0 3px 10px #0006}.map-pin span{transform:rotate(45deg);font-size:22px;line-height:1}"
                + ".user-pin{background:#075be8}.store-pin{background:#08a9d6}</style></head><body><div id='map'></div><script>"
                + "var map=L.map('map').setView([8.3,-73.6],13);"
                + "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',"
                + "{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);"
                + "var routeLayer=null,startMarker=null,storeMarker=null,navigationActive=false;"
                + "var userIcon=L.divIcon({className:'',html:'<div class=\"map-pin user-pin\"><span>&#128100;</span></div>',"
                + "iconSize:[48,48],iconAnchor:[24,44],popupAnchor:[0,-40]});"
                + "var storeIcon=L.divIcon({className:'',html:'<div class=\"map-pin store-pin\"><span>&#127978;</span></div>',"
                + "iconSize:[48,48],iconAnchor:[24,44],popupAnchor:[0,-40]});"
                + "function updateUserLocation(lat,lon){var p=[lat,lon];if(!startMarker){startMarker=L.marker(p,{icon:userIcon})"
                + ".addTo(map).bindTooltip('Tu ubicación',{permanent:true,direction:'top',offset:[0,-36]})"
                + ".bindPopup('Tu ubicación');}else{startMarker.setLatLng(p);}"
                + "if(navigationActive)map.panTo(p,{animate:true,duration:0.5});}"
                + "function setNavigationActive(active){navigationActive=active;}"
                + "function showRoute(slat,slon,dlat,dlon,coords){"
                + "if(routeLayer)map.removeLayer(routeLayer);"
                + "routeLayer=L.polyline(coords.map(function(c){return [c[1],c[0]];}),{color:'#0052FF',weight:6}).addTo(map);"
                + "updateUserLocation(slat,slon);if(!storeMarker){storeMarker=L.marker([dlat,dlon],{icon:storeIcon})"
                + ".addTo(map).bindTooltip('Tienda TechnoCat',{permanent:true,direction:'top',offset:[0,-36]})"
                + ".bindPopup('Tienda TechnoCat<br>Carrera 18 #6A-15, Aguachica');}else{storeMarker.setLatLng([dlat,dlon]);}"
                + "if(navigationActive)map.setView([slat,slon],Math.max(map.getZoom(),16),{animate:true});"
                + "else map.fitBounds(routeLayer.getBounds(),{padding:[30,30]});}</script></body></html>";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) locationManager.removeUpdates(locationListener);
        previousSpeedLocation = null;
        networkExecutor.shutdownNow();
        if (webMap != null) webMap.destroy();
    }
}
