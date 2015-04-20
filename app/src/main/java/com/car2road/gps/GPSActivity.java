package com.car2road.gps;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.car2road.R;
import com.car2road.client.SocketClient;

import org.apache.http.util.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class GPSActivity extends Activity implements Observer{


    static private final String PROVIDER = "gps";

    private LocationManager locationManager;

    private TextView locationTextView = null;

    private SocketClient socketClient;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        locationTextView = (TextView) findViewById(R.id.locationTextView);

        initialiserStartClientButton();

        initialiserGPS();



        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setSpeedRequired(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setBearingRequired(true);
        criteria.setCostAllowed(false);



        LocationProvider locationProvider = locationManager.getProvider("GPS");

        Button getLastLocationButton = (Button) findViewById(R.id.getLastLocationButton);
        getLastLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = locationManager.getLastKnownLocation(PROVIDER);
                if(location!=null) {
                    Toast.makeText(GPSActivity.this, "GPS : Latitude : " + location.getLatitude() + ", longitude : " + location.getLongitude(), Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(GPSActivity.this, "Impossible d'obtenir la position !", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void update(Observable observable, Object data) {
        Log.i("Update",data.toString());
    }

    private void initialiserStartClientButton()
    {
        Collection<Observer> observerList = new ArrayList<Observer>();
        observerList.add(this);

        socketClient = new SocketClient(7171,"192.168.43.237", observerList, GPSActivity.this);

        Button startClientButton = (Button) findViewById(R.id.startClientButton);
        startClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(socketClient).start();
            }
        });
    }

    private void initialiserGPS()
    {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        List<String> providerList = locationManager.getProviders(false);
        Log.d("GPS", providerList.toString());

        locationManager.requestLocationUpdates("gps", 1000, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("GPS", "Latitude : " + location.getLatitude() + ", longitude : " + location.getLongitude());
                locationTextView.setText("Latitude : " + location.getLatitude() + ", longitude : " + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }
}
