package com.example.mappingmemoriesapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mappingmemoriesapp.Models.PageLocation;
import com.example.mappingmemoriesapp.Models.User;
import com.example.mappingmemoriesapp.Models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MainActivity";

    public static final int ERROR_DIALOG_REQUEST = 9001;
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    //widgets
    private ProgressBar progressBar;

    //vars
    private FirebaseFirestore firebaseFirestore;
    private boolean mLocationPermissionGranted = false;
    private MapView mMapView;
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap googleMap;
    private LatLngBounds mapBoundary;
    private UserLocation userLocation;
    private PageLocation pageLocation;

    private boolean markerClicked = false;
    private Marker lastClickedMarker = null;
    private List<MarkerOptions> markerList = new ArrayList<MarkerOptions>();

    private GeofencingClient geofencingClient;
    private GeofenceBroadcastReceiver geofenceBroadcastReceiver;
    private static final String GEOFENCE_ACTION = "com.example.ACTION_GEOFENCE";
    PendingIntent geofencePendingIntent;

    double userLatitude;
    double userLongitude;

    private ActivityResultLauncher<Intent> enableGpsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        mMapView = findViewById(R.id.user_map);

        //Botón para crear una nueva página de diario para guardar la ubicación
        findViewById(R.id.fab_create_poi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Elige una opción");
                builder.setMessage("Puedes guardar la ubicación para más tarde o crear una página ahora");
                builder.setPositiveButton("Más tarde", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "onClick: positive button clicked: save location for later");

                        saveLocationForLater();

                        MarkerOptions markerOptions = new MarkerOptions();
                        LatLng latLng = new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude());
                        markerOptions.position(latLng);
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        googleMap.addMarker(markerOptions);
                    }
                });
                builder.setNegativeButton("Ahora", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "onClick: negative button clicked: save location and info");

                        getLastKnownLocation();
                        Intent intent= new Intent(MainActivity.this, DiaryPage.class);
                        intent.putExtra("geoPoint_lat", userLocation.getGeo_point().getLatitude());
                        intent.putExtra("geoPoint_lon", userLocation.getGeo_point().getLongitude());
                        intent.putExtra("isNewPage", true);
                        startActivity(intent);
                    }
                });
                builder.setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: neutral button clicked");
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        //Boton para leer las páginas de diario guardadas
        findViewById(R.id.fab_read).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "onClick: read diary clicked");

                Intent intent= new Intent(MainActivity.this, PagesList.class);
                startActivity(intent);
            }
        });

        enableGpsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "enableGpsLauncher: result OK");
                        getLastKnownLocation();
                    } else {
                        Log.d(TAG, "enableGpsLauncher: GPS not enabled");
                        Toast.makeText(this, "El GPS no está habilitado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        mMapView.getMapAsync(this);

        //Instanciar Firebase
        firebaseFirestore = FirebaseFirestore.getInstance();

        //Iniciar mapa
        initGoogleMap(savedInstanceState);

        //Localizacion
        userLocation = new UserLocation();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Geofences
        geofencingClient = LocationServices.getGeofencingClient(this);

        geofenceBroadcastReceiver = new GeofenceBroadcastReceiver();
        Intent geofenceIntent = new Intent(this, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Crear un IntentFilter para filtrar las transmisiones
        IntentFilter intentFilter = new IntentFilter(GEOFENCE_ACTION);

        // Registrar el receptor de transmisión
        registerReceiver(geofenceBroadcastReceiver, intentFilter);

    }

    //Guardar la ubicación para completar la información después
    private void saveLocationForLater() {
        showDialog();

        if(userLocation != null){

            pageLocation= new PageLocation();
            pageLocation.setGeo_point(userLocation.getGeo_point());
            pageLocation.setTimestamp(userLocation.getTimestamp());
            pageLocation.setUser_id(userLocation.getUser().getUser_id());
            pageLocation.setTitle("Titulo");
            pageLocation.setText("Texto");
            pageLocation.setImage("0");

            firebaseFirestore.collection("PageLocations").add(pageLocation).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                @Override
                public void onComplete(@NonNull Task<DocumentReference> task) {
                    hideDialog();
                    if(task.isSuccessful()){
                        Log.d(TAG, "savePageLocation: \ninserted page location into database." +
                                "\n latitude: " + userLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + userLocation.getGeo_point().getLongitude() +
                                "\n pageLocation: " + pageLocation.toString());
                    }
                }
            });

        }
    }

    //Método para inicializar el mapa de google
    private void initGoogleMap(Bundle savedInstanceState){
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);
    }

    //Método para guardar el estado actual del mapa de Google
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    //Método para verificar si los servicios de Google Play y ubicación están habilitados
    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    //Verifica la disponibilidad y versión de los servicios Google Play
    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "No puedes hacer solicitudes de mapas", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    //Verifica si la ubicación está habilitada
    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    //Alerta al usuario cuando la función de ubicación no está habilitada
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Esta aplicación requiere GPS para funcionar correctamente, ¿quieres habilitarlo?")
                .setCancelable(false)
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        enableGpsLauncher.launch(enableGpsIntent);                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //Método para manejar la respuesta del usuario cuando se le soliciten los permisos necesarios
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    getLastKnownLocation();
                }
            }
        }
    }

    //Comprobar si se ha concedido el permiso de ubicación y solicitarlo en caso contrario
    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getLastKnownLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        unregisterReceiver(geofenceBroadcastReceiver);
        geofencingClient.removeGeofences(geofencePendingIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        if(checkMapServices()){
            if(mLocationPermissionGranted){
                getLastKnownLocation();
            }
            else{
                getLocationPermission();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
        geofencingClient.removeGeofences(geofencePendingIntent);
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    //Cerrar sesión
    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_sign_out:{
                signOut();
                return true;
            }
            default:{
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //Método para cuando el google map puede usarse
    @Override
    public void onMapReady(GoogleMap map) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        googleMap = map;
        googleMap.setOnMarkerClickListener(this);
    }

    //Función para mostrar los markers guardados por el usuario en el mapa
    private void showMarkers() {
        firebaseFirestore.collection("PageLocations").whereEqualTo("user_id", FirebaseAuth.getInstance().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            List<DocumentSnapshot> lista = task.getResult().getDocuments();
                            if(!lista.isEmpty()){
                                for(int i = 0; i < lista.size(); i++){
                                    Log.d(TAG, lista.get(i).getId() + " => " + lista.get(i).getData());
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    GeoPoint geoPoint = (GeoPoint) lista.get(i).getData().get("geo_point");
                                    LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                    markerOptions.position(latLng);
                                    markerOptions.title((String) lista.get(i).getData().get("title"));
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                    Timestamp timestamp= (Timestamp) lista.get(i).getData().get("timestamp");
                                    markerOptions.snippet(timestamp.toDate().toString());
                                    googleMap.addMarker(markerOptions);
                                    markerList.add(markerOptions);

                                    Log.d(TAG, "savePageLocation: \n marker loaded." +
                                            "\n latitude: " + geoPoint.getLatitude() +
                                            "\n longitude: " + geoPoint.getLongitude() +
                                            "\n title: " + (String) lista.get(i).getData().get("title"));
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                        distanceWithMarkers();

                    }
                });
    }

    //Función cuando se hace click en un marcador
    @Override
    public boolean onMarkerClick(Marker marker) {
        String markerTitle = marker.getTitle();
        String markerSnippet = marker.getSnippet();

        marker.showInfoWindow();

        if (marker.equals(lastClickedMarker)) {
            // Se hizo clic en el mismo marcador por segunda vez
            String markerId = marker.getId();
            Double lat = marker.getPosition().latitude;
            Double lon = marker.getPosition().longitude;

            Intent intent = new Intent(MainActivity.this, DiaryPage.class);
            intent.putExtra("markerId", markerId);
            intent.putExtra("title", markerTitle);
            intent.putExtra("snippet", markerSnippet);
            intent.putExtra("markerLat", lat);
            intent.putExtra("markerLon", lon);
            intent.putExtra("isNewPage", false);
            startActivity(intent);

            markerClicked = false;
            lastClickedMarker = null;
        } else {
            // Se hizo clic en un marcador diferente o por primera vez en este marcador
            markerClicked = true;
            lastClickedMarker = marker;
        }

        return true;
    }

    //Establece la vista de la cámara del mapa
    private void setCameraView(){
        double bottomBoundary = userLocation.getGeo_point().getLatitude()- .1;
        double leftBoundary = userLocation.getGeo_point().getLongitude()- .1;
        double topBoundary = userLocation.getGeo_point().getLatitude()+ .1;
        double rightBoundary = userLocation.getGeo_point().getLongitude()+ .1;
        mapBoundary = new LatLngBounds(new LatLng(bottomBoundary, leftBoundary),
                new LatLng(topBoundary,rightBoundary));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBoundary, 0));
    }

    //Solicita la ultima ubicación del usuario
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called.");
        showDialog();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "onComplete: latitude: " + geoPoint.getLatitude());
                        Log.d(TAG, "onComplete: longitude: " + geoPoint.getLongitude());
                        userLocation.setGeo_point(geoPoint);
                        userLocation.setTimestamp(null);

                        userLatitude = location.getLatitude();
                        userLongitude = location.getLongitude();

                        DocumentReference userRef = firebaseFirestore.collection("Users")
                                .document(FirebaseAuth.getInstance().getUid());

                        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                hideDialog();
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "onComplete: successfully set the user client.");
                                    User user = task.getResult().toObject(User.class);
                                    userLocation.setUser(user);

                                    Log.d(TAG, "onComplete: userLocation: " + userLocation.getGeo_point());
                                    setCameraView();
                                    showMarkers();
                                }
                                distanceWithMarkers();
                            }
                        });
                    }
                }
            }
        });
    }

    //Establece las geofences y calcula la distancia con el usuario
    private void distanceWithMarkers() {

        for(int i= 0; i< markerList.size(); i++){
            double positionLat= markerList.get(i).getPosition().latitude;
            double positionLon= markerList.get(i).getPosition().longitude;
            float radius = 100;
            String geofenceId = markerList.get(i).getTitle();

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(geofenceId)
                    .setCircularRegion(positionLat, positionLon, radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build();

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .addGeofence(geofence)
                    .build();
            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent());

            double distancia = calculateDistance(positionLat, positionLon);
            if(distancia < geofence.getRadius()){
                geofenceBroadcastReceiver.showNotification(this);
            }
        }
    }

    //Calcula la distancia entre el marcador y el usuario
    private Double calculateDistance(double positionLat, double positionLon) {
        double radio_tierra = 6371;

        double positionLat_radianes = Math.toRadians(positionLat);
        double positionLon_radianes = Math.toRadians(positionLon);
        double usuarioLat_radianes = Math.toRadians(userLatitude);
        double usuarioLon_radianes = Math.toRadians(userLongitude);

        double diff_lat = usuarioLat_radianes - positionLat_radianes;
        double diff_lon = usuarioLon_radianes - positionLon_radianes;

        double a = Math.pow(Math.sin(diff_lat / 2), 2) + Math.cos(positionLat_radianes) * Math.cos(usuarioLat_radianes)
                * Math.pow(Math.sin(diff_lon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distancia = radio_tierra * c *1000; //metros

        return distancia;
    }


    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void showDialog(){
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        progressBar.setVisibility(View.GONE);
    }


}