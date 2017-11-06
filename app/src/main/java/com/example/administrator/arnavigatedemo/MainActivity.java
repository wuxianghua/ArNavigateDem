package com.example.administrator.arnavigatedemo;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.palmaplus.nagrand.core.Types;
import com.palmaplus.nagrand.data.DataSource;
import com.palmaplus.nagrand.data.Feature;
import com.palmaplus.nagrand.data.FeatureCollection;
import com.palmaplus.nagrand.data.LocationList;
import com.palmaplus.nagrand.data.LocationModel;
import com.palmaplus.nagrand.data.LocationPagingList;
import com.palmaplus.nagrand.data.Param;
import com.palmaplus.nagrand.data.PlanarGraph;
import com.palmaplus.nagrand.geos.Coordinate;
import com.palmaplus.nagrand.navigate.NavigateManager;
import com.palmaplus.nagrand.position.ble.BeaconPositioningManager;
import com.palmaplus.nagrand.position.ble.RTLSBeaconLocationManager;
import com.palmaplus.nagrand.rtls.pdr.LocationPair;
import com.palmaplus.nagrand.rtls.pdr.PDR;
import com.palmaplus.nagrand.view.MapView;
import com.palmaplus.nagrand.view.gestures.OnSingleTapListener;
import com.palmaplus.nagrand.view.layer.FeatureLayer;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    protected MapView mapView;
    protected ViewGroup container;
    protected RTLSBeaconLocationManager rtlsBeaconLocationManager;
    private LocationMark locationMark;
    private NavigateManager navigateManager;
    private FeatureLayer navigateLayer;
    double locationX;
    double locationY;
    double startX = 0;
    double startY = 0;
    long startId = 0;
    double toX = 0;
    double toY = 0;
    long toId = 0;
    private DataSource dataSource;
    public List<Mark> markList = new ArrayList<Mark>();
    private boolean ishasEndPoint = false;
    private PDR pdr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.getMap().startWithMapID(Constants.mapId);
        container = (ViewGroup) findViewById(R.id.overlay_container);
        mapView.setOverlayContainer(container);
        locationMark = new LocationMark(mapView.getContext(), mapView);
        dataSource = new DataSource("http://api.ipalmap.com/");
        navigateManager = new NavigateManager();
        rtlsBeaconLocationManager = new RTLSBeaconLocationManager(this,
                Constants.mapId,
                Constants.AppKey);
        rtlsBeaconLocationManager.setOnLocationChangeListener(new RTLSBeaconLocationManager.OnLocationChangeListener() {
            @Override
            public void onLocationChange(RTLSBeaconLocationManager.LocationStatus locationStatus, com.palmaplus.nagrand.position.ble.utils.Location location, com.palmaplus.nagrand.position.ble.utils.Location location1) {
                Log.e(TAG,"LOCATIONsTATUS"+locationStatus);
                switch (locationStatus) {
                    case MOVE:
                        initPDR(location1.getX(),location1.getY());
                        pdr.start();
                        double[] dr = pdr.dr(new double[]{location1.getX(), location1.getY()});
                        Coordinate coordinate = new Coordinate(dr[0], dr[1]);
                        TextView textView = new TextView(MainActivity.this);
                        coordinate.setZ(location1.getFloorId());
                        refreshLocation(coordinate);
                }
            }
        });
        pdr = new PDR(this);
        rtlsBeaconLocationManager.start();
        mapView.setOnChangePlanarGraph(new MapView.OnChangePlanarGraph() {
            @Override
            public void onChangePlanarGraph(PlanarGraph planarGraph, PlanarGraph planarGraph1, long l, long l1) {
                navigateLayer = new FeatureLayer("navigate");
                mapView.addLayer(navigateLayer);
                mapView.setLayerOffset(navigateLayer);
            }
        });
        mapView.setOnLoadStatusListener(new MapView.OnLoadStatusListener() {
            @Override
            public void onLoadStatus(final MapView mapView, int i, MapView.LoadStatus loadStatus) {
                if (loadStatus == MapView.LoadStatus.LOAD_EDN) {
                    mapView.getMap().dataSource().requestPOIChildren(mapView.getMap().getFloorId(), new DataSource.OnRequestDataEventListener<LocationList>() {
                        @Override
                        public void onRequestDataEvent(DataSource.ResourceState resourceState, LocationList locationList) {
                            //poi数据
                            for (int i = 0; i < locationList.getSize(); i++) {
                                LocationModel poi = locationList.getPOI(i);
                                long poiId = MapParam.getId(poi);

                                Feature feature = mapView.selectFeature(poiId);
                                /*if (feature != null) {
                                    feature.getGeometry().getCoordinate().getX();
                                    feature.getGeometry().getCoordinate().getY();
                                }*/
                                String poiName = MapParam.getName(poi);
                            }
                        }
                    });
                }
            }
        });
        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            @Override
            public void onSingleTap(MapView mapView, float x, float y) {
                Feature feature = mapView.selectFeature(x, y);
                Mark mark = new Mark(getApplicationContext());
                if (feature == null) {
                    return;
                }
                Types.Point point = mapView.converToWorldCoordinate(x, y);

                startX = locationX;
                startY = locationY;
                startId = 1915490;
                toId = 1915490;
                toX = point.x;
                toY = point.y;
                if (ishasEndPoint) {
                    for (int i = 0; i < markList.size(); i++) {
                        mapView.removeOverlay(markList.get(i));
                    }
                }
                ishasEndPoint = true;
                mark.setMark(R.mipmap.icon_end2x);
                // TODO 请求导航线路
                navigateManager.navigation(1.35087681243E7, 3661332.9333, startId, toX, toY, toId, startId);
                mark.setFloorId(toId);
                mark.init(new double[]{point.x, point.y});
                //将这个覆盖物添加到MapView中
                mapView.addOverlay(mark);
                markList.add(mark);
            }
        });
        navigateManager.setOnNavigateComplete(new NavigateManager.OnNavigateComplete() {
            @Override
            public void onNavigateComplete(NavigateManager.NavigateState navigateState, FeatureCollection featureCollection) {
                Log.e(TAG, navigateManager.getLength() + "");
                //导航路网的数据
                navigateLayer.clearFeatures();
                for (int i = 0; i < navigateManager.getLength(); i++) {
                    Coordinate coordinateByFeatureCollection = navigateManager.getCoordinateByFeatureCollection(featureCollection, i);
                    coordinateByFeatureCollection.getX();
                    coordinateByFeatureCollection.getY();
                }

                navigateLayer.addFeatures(featureCollection); //获取导航线
            }
        });
    }

    private void initPDR(double x, double y) {
            pdr.initLocation(new LocationPair(x, y,
                    System.currentTimeMillis()));
    }

    //定位数据获取
    public void refreshLocation(Coordinate coordinate) {
        locationX = coordinate.getX();
        locationY = coordinate.getY();
        locationMark.init(new double[]{locationX, locationY});
        locationMark.setFloorId((long) coordinate.getZ());
        startId = (long) coordinate.getZ();
        toId = (long) coordinate.getZ();
        mapView.addOverlay(locationMark);
        mapView.getOverlayController().refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.drop();
        rtlsBeaconLocationManager.stop();
        rtlsBeaconLocationManager.close();
    }

    @Override
    public void onClick(View view) {
    }
}
