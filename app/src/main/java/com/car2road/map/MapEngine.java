package com.car2road.map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Debug;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;

import com.car2road.client.SocketReader;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by qi11110 on 09.04.2015.
 */
public class MapEngine implements Observer, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int METER_TO_PIXEL = 4;

    private MapActivity mapActivity;

    private SparseArray<Voiture> voitureSparseArray;

    private LocationManager locationManager;

    private GoogleMap map = null;

    private boolean hasSatellite;

    private SensorManager sensorManager;

    private Sensor accelerometer;

    private Sensor magnetometer;

    private GoogleApiClient googleApiClient;

    private LocationRequest locationRequest = null;

    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean lastAccelerometerSet = false;
    private boolean lastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] orientation = new float[3];

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor == accelerometer) {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
                lastAccelerometerSet = true;
            } else if (event.sensor == magnetometer) {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
                lastMagnetometerSet = true;
            }

            if (lastAccelerometerSet && lastMagnetometerSet) {
                SensorManager.getRotationMatrix(mR, null, lastAccelerometer, lastMagnetometer);
                SensorManager.getOrientation(mR, orientation);
                //Log.i(getClass().getName(), String.format("Orientation: %f, %f, %f",orientation[0], orientation[1], orientation[2]));

                Location location = voitureSparseArray.get(0).getLocation();
                location.setBearing((float)(orientation[0]*180/Math.PI));
                //updateVehiclePosition(location);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(location.getProvider(), "Latitude : " + location.getLatitude() + ", longitude : " + location.getLongitude() + ", Bearing : " + location.getBearing() + ", Accurency : " + location.getAccuracy());

            if(LocationManager.GPS_PROVIDER.equals(location.getProvider()))
            {
                if (location.hasBearing())
                {
                    Log.i(getClass().getName(), "Bearing : " + String.valueOf(location.getBearing()));
                }
                updateVehiclePosition(location);
            }
            else if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider()) && !hasSatellite)
            {
                updateVehiclePosition(location);
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            if(LocationManager.GPS_PROVIDER.equals(provider))
            {
                createGpsDisabledAlert("Vous venez de désactiver le GPS. Veuillez le réactiver.");
            }
        }
    };

    private com.google.android.gms.location.LocationListener locationListener2 = new com.google.android.gms.location.LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(location.getProvider(), "Latitude : " + location.getLatitude() + ", longitude : " + location.getLongitude() + ", Bearing : " + location.getBearing() + ", Accurency : " + location.getAccuracy());
            updateVehiclePosition(location);
        }
    };

    private GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {

            if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS && locationManager!=null) {

                GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                Iterator<GpsSatellite> it = gpsStatus.getSatellites().iterator();

                boolean findSatellite = false;
                while (it.hasNext()) {
                    GpsSatellite gpsSatellite = it.next();
                    if (gpsSatellite.usedInFix()) {
                        findSatellite = true;
                        break;
                    }
                }
                hasSatellite = findSatellite;
            }
        }
    };

    private final Location testLocation = createTestLocation();


    MapEngine(MapActivity mapActivity)
    {
        this.mapActivity = mapActivity;
        locationManager = (LocationManager) mapActivity.getSystemService(Context.LOCATION_SERVICE);

        /*
        sensorManager = (SensorManager)mapActivity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        */

        buildGoogleApiClient();
        googleApiClient.connect();

        initialiserSocketClient();
    }

    MapEngine(MapActivity mapActivity, GoogleMap map)
    {
        this.mapActivity = mapActivity;
        this.map = map;
        locationManager = (LocationManager) mapActivity.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager)mapActivity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        buildGoogleApiClient();
        googleApiClient.connect();

        initialiserSocketClient();
    }

    public SparseArray<Voiture> buildVoitureList()
    {
        Display ecran = mapActivity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        ecran.getSize(point);

        voitureSparseArray = new SparseArray<>();

        Voiture voitureCenter = new Voiture(mapActivity, new Location(LocationManager.GPS_PROVIDER), point.x / 2, point.y /2, 0);
        voitureSparseArray.put(0, voitureCenter);

        if(isMockSettingOn(mapActivity))
        {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            Log.i(getClass().getName(), "HasBearing : " + location.hasBearing());
            location.setLatitude(48.188139);
            location.setLongitude(11.584568);
            location.setAccuracy(20);
            voitureSparseArray.put(1, new Voiture(mapActivity, location,2000,2000,0));

            location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(48.188039);
            location.setLongitude(11.585013);
            location.setAccuracy(30);
            voitureSparseArray.put(2, new Voiture(mapActivity, location,2000,2000,0));
        }


        return voitureSparseArray;
    }

    public void activateGPS()
    {
        if(Debug.isDebuggerConnected() && isMockSettingOn(mapActivity))
        {
            setMockLocation();
        }

        if(isGpsEnabled())
        {
            //setGpsRequestLocationUpdates();
        }
        else
        {
            createGpsDisabledAlert("Le GPS est inactif, veuillez l'activer ?");
        }
    }

    private boolean isGpsEnabled()
    {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void setGpsRequestLocationUpdates()
    {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
    }

    private void createGpsDisabledAlert(String message) {
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(mapActivity);
        localBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Activer GPS ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                showGpsOptions();
                            }
                        }
                );
        localBuilder.setNegativeButton("Ne pas l'activer ",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        paramDialogInterface.cancel();
                        mapActivity.finish();
                    }
                }
        );
        localBuilder.create().show();
    }

    private void showGpsOptions() {
        mapActivity.startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
    }

    public void updateVehiclePosition(Location locationCenter)
    {
        Log.i(this.getClass().getName(), "Position of vehicle will be updated");
        Voiture voitureCenter = voitureSparseArray.get(0);
        voitureCenter.setLocation(locationCenter);

        int size = voitureSparseArray.size();
        for(int i=0;i<size;i++)
        {
            if(i!=voitureSparseArray.indexOfValue(voitureCenter))
            {
                Voiture voiture = voitureSparseArray.valueAt(i);
                float distance = locationCenter.distanceTo(voiture.getLocation());
                double angle = locationCenter.getBearing() + locationCenter.bearingTo(voiture.getLocation()) * Math.PI / 180;

                voiture.setX(voitureCenter.getX() + (int)(distance * Math.sin(angle))*METER_TO_PIXEL);
                voiture.setY(voitureCenter.getY() - (int)(distance * Math.cos(angle))*METER_TO_PIXEL);
                voiture.setAngle(voiture.getLocation().getBearing()-locationCenter.getBearing());

                Location location = voiture.getLocation();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                Circle circle = voiture.getCircle();
                Circle accurencyCircle = voiture.getAccurencyCircle();
                if(accurencyCircle==null) {
                    CircleOptions accurencyCircleOptions = new CircleOptions();
                    accurencyCircleOptions.center(latLng);
                    accurencyCircleOptions.strokeColor(0xFF0000FF);
                    accurencyCircleOptions.fillColor(0x110000FF);
                    accurencyCircleOptions.strokeWidth(3);
                    accurencyCircleOptions.radius(location.getAccuracy());
                    voiture.setAccurencyCircle(map.addCircle(accurencyCircleOptions));
                } else {
                    accurencyCircle.setCenter(latLng);
                    accurencyCircle.setRadius(location.getAccuracy());
                }

                if(circle==null) {
                    CircleOptions circleOptions = new CircleOptions();
                    circleOptions.center(latLng);
                    circleOptions.fillColor(Color.RED);
                    circleOptions.radius(1);
                    voiture.setCircle(map.addCircle(circleOptions));
                } else {
                    circle.setCenter(latLng);
                }
            }
        }
    }



    private void initialiserSocketClient()
    {
        List<Observer> observerList = new ArrayList<>();
        observerList.add(this);
        //new Thread (new SocketClient(7171,"192.168.43.237", observerList, mapActivity)).start();
    }

    @Override
    public void update(Observable observable, Object data) {
        if(SocketReader.class.equals(observable.getClass()))
        {

        }
    }

    private void setMockLocation()
    {
        Log.i(getClass().getName(), "MockLocation will be set");
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false, false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    while (true) {
                        if (locationManager != null) {
                            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, testLocation);
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {

                        }
                    }
                } catch(IllegalArgumentException e) {
                    Log.e(getClass().getName(),"Erreur dans setMockLocation", e);
                }

            }
        }).start();

    }

    private static Location createTestLocation()
    {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setAccuracy(100);
        location.setLatitude(48.188035);
        location.setLongitude(11.584547);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(System.currentTimeMillis());

        return location;
    }

    private static boolean isMockSettingOn(Context context)
    {
        return !Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
    }

    public void stop()
    {
        locationManager.removeGpsStatusListener(gpsListener);

        //locationManager.removeUpdates(locationListener);

        if(isMockSettingOn(mapActivity) && Debug.isDebuggerConnected())
        {
            try{
                locationManager.clearTestProviderEnabled(LocationManager.GPS_PROVIDER);
                locationManager.clearTestProviderLocation(LocationManager.GPS_PROVIDER);
                locationManager.clearTestProviderStatus(LocationManager.GPS_PROVIDER);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch(IllegalArgumentException e) {
                Log.i(getClass().getName(), "LocationManager already cleared", e);
            }


        }

        /*
        sensorManager.unregisterListener(sensorEventListener, accelerometer);
        sensorManager.unregisterListener(sensorEventListener, magnetometer);
        */

        stopLocationUpdates();

    }

    public void resume()
    {
        locationManager.addGpsStatusListener(gpsListener);
        /*
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        */
        activateGPS();

        if(googleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(mapActivity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private synchronized  void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    public void onConnected(Bundle bundle) {
        if(locationRequest==null)
        {
            buildLocationRequest();
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener2);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener2);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public GoogleMap getMap() {
        return map;
    }

    public void setMap(GoogleMap map) {
        this.map = map;
    }
}
