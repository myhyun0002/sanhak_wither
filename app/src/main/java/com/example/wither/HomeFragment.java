package com.example.wither;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;

import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;

import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;

import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;

import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;


public class HomeFragment extends Fragment implements OnMapReadyCallback {
//,Overlay.OnClickListener
    FragmentActivity mcontext;
    private Context context;

    // ????????? ????????? ?????? ??????
    private SharedViewModel sharedViewModel;
    private SharedViewModel sharedViewModel_to_InfoWindowClickFragment;

    private LocationManager locationManager;
    private GpsTracker gpsTracker;
    private FusedLocationProviderClient mFusedLocationClient;
    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;
    private double latitude, longitude;
    Location location;
    private Marker marker = new Marker();

    private HomeFloatingFragment homeFlaotingActionFragment;
    private FragmentActivity myContext;
    private InfoWindowClickFragment infoWindowClickFragment;

    String username;

    int checking_floating = 1;
    int checking_floating_bundle = 0;
     //mongodb????????? ?????? ????????????
    ArrayList<MakeDatabase> GET_database_set = new ArrayList<MakeDatabase>();

    // ??????
    ArrayList<Marker> markers = new ArrayList<Marker>();
    ArrayList<InfoWindow> infoWindows = new ArrayList<InfoWindow>();
    ArrayList<MakeDatabase> databases = new ArrayList<MakeDatabase>();

    private InfoWindow infoWindow = new InfoWindow();

    // ?????? thread?????? main thread??? ???????????? ?????? ??????
    Handler mainHandler = new Handler(Looper.getMainLooper());
    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Bundle bundle = getArguments();
        if(bundle!=null){
            username = bundle.getString("username");
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        // ????????? ?????? ????????? ?????? ????????? ????????? ?????? ?????? ??????
        ImageButton ShowLocationButton = (ImageButton)view.findViewById(R.id.gpsbtn);
        ShowLocationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                gpsTracker = new GpsTracker(getActivity().getApplicationContext());

                setLatitude(gpsTracker.getLatitude());
                setLongitude(gpsTracker.getLongitude());

                // ????????? ?????? ??????
                setUserMarker(getLatitude(),getLongitude());

                // ???????????? ?????? ??????
                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(getLatitude(), getLongitude()));
                mNaverMap.moveCamera(cameraUpdate);
            }
        });

        // ?????? ????????? ?????? ?????? ?????? ???????????? ?????? ????????? ????????? ?????? ??????
        MapView mapView;
        mapView = (MapView) view.findViewById(R.id.map_fragment);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this); // ???????????? ???????????? ?????? ??? ??????

        // ????????? floatingbutton ????????? HomeFloatingFragment ?????????
        FragmentManager fm = getParentFragmentManager();
        FloatingActionButton fab = (FloatingActionButton)view.findViewById(R.id.floatingActionButton);
        homeFlaotingActionFragment = new HomeFloatingFragment();


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    Bundle bundle = new Bundle(3); // ????????? ?????? ??? ??????
                    bundle.putDouble("latitude",getLatitude());//????????? ?????? ??? ??????
                    bundle.putDouble("longitude",getLongitude());//????????? ?????? ??? ??????
                    bundle.putBundle("long_latitudeBundle",bundle);

                    HomeFloatingFragment homeFloatingFragment = new HomeFloatingFragment();//???????????????2 ??????
                    homeFloatingFragment.setArguments(bundle);//????????? ???????????????2??? ?????? ??????

                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

                    transaction.add(R.id.homeFragmentFrame, homeFloatingFragment,"homeFloatingFragment");
                    transaction.commit();

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // homeFloatingFragment?????? ?????? ?????? ????????? ????????? ????????? ????????? ????????? ?????? ??????
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        sharedViewModel.getLiveData().observe(getViewLifecycleOwner(), new Observer<MakeDatabase>() {
            @Override
            public void onChanged(MakeDatabase database) {
                setCategoryMarker(database.getMarker(), database);
                databases.add(database);
                markers.add(database.getMarker());
                infoWindows.add(database.getInfoWindow());
            }
        });

        // ????????? ?????? ?????????
        infoWindow.setAdapter(new InfoWindow.ViewAdapter() {
            @NonNull
            @Override
            public View getView(@NonNull InfoWindow infoWindow) {
                ViewGroup rootView = (ViewGroup)view.findViewById(R.id.homeFragmentFrame);
                MarkerPointAdapter adapter = new MarkerPointAdapter(getActivity(), rootView);

                // ????????? ????????? ?????? for??? (infoWindow.getMarker()??? ????????? ?????? marker???)
                // for????????? database??? ?????? ????????? ?????? ??? ????????? ????????? ????????????????????? ????????? ????????? ??????.
                for(int i = 0 ; i < databases.size();i++){
                    if(databases.get(i).getMarker() == infoWindow.getMarker()){
                        adapter.setDatabase(databases.get(i));
                        break;
                    }
                }
                return (View)adapter.getContentView(infoWindow);
            }
        });
    }

    // ?????? ?????? ????????? ?????? ?????????
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        // NaverMap ?????? ????????? NaverMap ????????? ?????? ?????? ??????
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        Bundle bundle = getArguments();

        // ?????? ??? ?????? ?????? ???????????? ????????? ????????????
        if(bundle != null){
            setLatitude(bundle.getDouble("latitude"));
            setLongitude(bundle.getDouble("longitude"));
            setUserMarker(getLatitude(),getLongitude());

            LatLng first_lating = new LatLng(getLatitude(), getLongitude());
            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(first_lating,15.5);
            mNaverMap.moveCamera(cameraUpdate);
        }

        // ????????? ?????? , zoom?????? ??????
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(false);
        uiSettings.setZoomControlEnabled(false);

//         ????????? ?????? ?????? ??????
        uiSettings.setLogoGravity(getView().getTop());
        uiSettings.setLogoMargin(940,30,30,0);

        //????????? ?????? ?????? ????????????
        uiSettings.setTiltGesturesEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setScaleBarEnabled(false);
        // ????????? thread?????? mongodb????????? GET??? ??? ???????????? ????????? ???????????? ???????????? ?????? ?????? ???????????????.
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                    GET_database_set = MakeDatabase.json_to_database_set(MakeDatabase.GET());
                    MakeDatabase.setGET_dabase(GET_database_set);
                int databases_size = databases.size();
                try{
                    for(int i = 0 ; i < GET_database_set.size();i++){
                        MakeDatabase GET_database = GET_database_set.get(i);
                        String category_String = GET_database.getMeeting_category();
                        int resourceID = GET_database.getMarkerIcon_int(category_String);
                        databases.add(GET_database);
                        markers.add(GET_database.getMarker());
                        infoWindows.add(GET_database.getInfoWindow());
                    }
                    // main Thread?????? ????????? ??????????????? ??? ???????????? main thread??? ????????? ??? ????????????.
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0 ; i < databases.size();i++){
                                setCategoryMarker(databases.get(i).getMarker(),databases.get(i));
                            }
                        } // This is your code
                    };
                    mainHandler.post(myRunnable);



                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        });
        th.start();

        // ?????? ?????? ????????? ???????????? ??????(?????????)- onMapReady?????? ??????

//        naverMap.addOnCameraChangeListener(new NaverMap.OnCameraChangeListener() {
//            @Override
//            public void onCameraChange(int reason, boolean animated) {
//                freeActiveMarkers();
//                // ????????? ?????????????????? ???????????? ????????????????????? ?????? ??????
//                LatLng currentPosition = getCurrentPosition(naverMap);
//                for (LatLng markerPosition: markersPosition) {
//                    if (!withinSightMarker(currentPosition, markerPosition))
//                        continue;
//                    Marker marker = new Marker();
//                    marker.setPosition(markerPosition);
//                    marker.setMap(naverMap);
//                    activeMarkers.add(marker);
//                }
//            }
//        });
    }

    // ?????? ?????? ????????? ???????????? ?????????

//    // ?????? ???????????? ???????????? ??????
//    public LatLng getCurrentPosition(NaverMap naverMap) {
//        CameraPosition cameraPosition = naverMap.getCameraPosition();
//        return new LatLng(cameraPosition.target.latitude, cameraPosition.target.longitude);
//    }
//
//    // ?????? ?????? ???????????? ????????? ??????
//    private Vector<LatLng> markersPosition;
//    private Vector<Marker> activeMarkers;
//
//    // 3km ???????????? ?????? ????????? ????????? ??????
//    public final static double REFERANCE_LAT = 1 / 109.958489129649955;
//    public final static double REFERANCE_LNG = 1 / 88.74;
//    public final static double REFERANCE_LAT_X3 = 3 / 109.958489129649955;
//    public final static double REFERANCE_LNG_X3 = 3 / 88.74;
//    public boolean withinSightMarker(LatLng currentPosition, LatLng markerPosition) {
//        boolean withinSightMarkerLat = Math.abs(currentPosition.latitude - markerPosition.latitude) <= REFERANCE_LAT_X3;
//        boolean withinSightMarkerLng = Math.abs(currentPosition.longitude - markerPosition.longitude) <= REFERANCE_LNG_X3;
//        return withinSightMarkerLat && withinSightMarkerLng;
//    }
//
//    private void freeActiveMarkers() {
//        if (activeMarkers == null) {
//            activeMarkers = new Vector<Marker>();
//            return;
//        }
//        for (Marker activeMarker: activeMarkers) {
//            activeMarker.setMap(null);
//        }
//        activeMarkers = new Vector<Marker>();
//    }

    // ????????? ?????? ?????? ??????
    private void setUserMarker(double lat, double lng){
        LocationOverlay locationOverlay = mNaverMap.getLocationOverlay();
        locationOverlay.setIconHeight(70);
        locationOverlay.setIconWidth(70);
        locationOverlay.setCircleRadius(50);
//        locationOverlay.setCircleOutlineWidth(1);
        locationOverlay.setCircleOutlineColor(Color.rgb(12,144,255));
        locationOverlay.setPosition(new LatLng(lat, lng));
        locationOverlay.setVisible(true);
    }

    // ?????? ??? ?????? ??????
    private void setCategoryMarker(Marker marker,MakeDatabase makeDatabase)
    {
        Marker method_marker = marker;
        MakeDatabase method_database = makeDatabase;

        // ?????? ?????? ??? ????????? ????????? ??????

        ViewGroup rootView = (ViewGroup)getView().findViewById(R.id.homeFragmentFrame);
        MarkerPointAdapter adapter = new MarkerPointAdapter(getActivity(), rootView);

//        marker.set
        //????????? ??????
        method_marker.setIcon(OverlayImage.fromResource(method_database.getResourceID()));
        //?????? ??????
        method_marker.setPosition(new LatLng(method_database.getLatitude(), method_database.getLongitude()));
        //?????? ??????
        method_marker.setWidth(50);
        method_marker.setHeight(50);

        method_marker.setAnchor(new PointF(0, 0));
        method_marker.setHideCollidedCaptions(true);
        method_marker.setMap(mNaverMap);

        sharedViewModel_to_InfoWindowClickFragment = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // ????????? ????????? ?????? ??? ?????? ????????? ??????
        // ????????? ????????? ???????????? ????????? ??????
        method_marker.setOnClickListener(overlay -> {

            infoWindow.setZIndex(5);
                //????????? ??????
            infoWindow.setAlpha(0.9f);
            infoWindow.open(method_marker);
            // ????????? ?????? ?????????
            infoWindow.setOnClickListener(new Overlay.OnClickListener()
            {
                @Override
                public boolean onClick(@NonNull Overlay overlay)
                {
                    sharedViewModel_to_InfoWindowClickFragment.setLiveData(method_database);
                    infoWindowClickFragment = new InfoWindowClickFragment();

                    Bundle bundle = new Bundle(1);
                    bundle.putString("username",username);
                    infoWindowClickFragment.setArguments(bundle);

                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    transaction.add(R.id.homeFragmentFrame, infoWindowClickFragment);
                    transaction.commit();
                    return false;
                }
            });
            return true;
        });
    }

    public double getLatitude(){
        return latitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public void GET_Json_to_database(ArrayList<MakeDatabase> GET_database_set){
        this.GET_database_set = GET_database_set;
    }
}




