package com.car2road.map;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.SparseArray;
import android.view.Window;

import com.car2road.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapActivity extends Activity implements OnMapReadyCallback{

    private MapSurfaceView mapSurfaceView = null;
    private MapEngine mapEngine = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_map);

        mapEngine = new MapEngine(this);

        /*
        mapSurfaceView = new MapSurfaceView(this);
        setContentView(mapSurfaceView);


        mapSurfaceView.setVoitureList(voitures);
        */

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);
        mapEngine.setMap(googleMap);
        SparseArray<Voiture> voitures = mapEngine.buildVoitureList();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapEngine.resume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapEngine.stop();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapEngine.stop();
    }
}
