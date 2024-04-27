package com.example.cs306;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.example.cs306.databinding.ActivityMainBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    MapView mMapView = null;
    SeekBar positionBar;
    MediaPlayer mp;
    int totaltime;
    AMap aMap = null;
    List<LatLng> latLngs = new ArrayList<LatLng>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapsInitializer.updatePrivacyShow(this,true,true);
        MapsInitializer.updatePrivacyAgree(this,true);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        MyLocationStyle myLocationStyle;
        myLocationStyle= new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        myLocationStyle.interval(2000);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        mp=MediaPlayer.create(this,R.raw.a1);
        mp.setLooping(true);
        mp.seekTo(0);
        mp.setVolume(0.5f,0.5f);
        totaltime=mp.getDuration();

        positionBar=(SeekBar) findViewById(R.id.positionBar);
        positionBar.setMax(totaltime);
        positionBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener(){
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int process, boolean fromUser){
                        if (fromUser){
                            mp.seekTo(process);
                            positionBar.setProgress(process);
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar){

                    }
                    public void onStopTrackingTouch(SeekBar seekBar){

                    }
                }
        );
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mp!= null){
                    try{
                        Message msg=new Message();
                        msg.what=mp.getCurrentPosition();
                        handler.sendMessage(msg);
                        Thread.sleep(1000);
                    }catch (InterruptedException e){

                    }
                }
            }
        }).start();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    static final int REQUEST_IMAGE_CAPTURE=1;
    private void dispatchTakepictureIntent(){
        Intent takePictureIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivity(takePictureIntent);
    }
    private Handler handler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            int currentPosition=msg.what;
            positionBar.setProgress(currentPosition);
        }
    };
    public void PlayButton(View view){
        if (!mp.isPlaying()){
            mp.start();
        }else{
            mp.pause();
        }
    }
    public void timecount(View view) {

        Thread thread = new Thread(null, doBackgroundThreadProcessing,"Background");
        thread.start();
    }

    //定义子线程
    private Runnable doBackgroundThreadProcessing = new Runnable() {
        int t=5;
        public void run() {
            int tt=t;
            while(tt>0){
                System.out.println(tt);
                tt-=1;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            handler1.sendEmptyMessage(0);
        }
    };

    private Handler handler1 = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Context context = getApplicationContext();
                    String msgtext = "Time is out!";
                    int duration = Toast.LENGTH_SHORT;
                    Toast.makeText(context, msgtext, duration).show();
                    MediaPlayer mp1;
                    mp1=MediaPlayer.create(getApplicationContext(),R.raw.a1);
                    mp1.setLooping(true);
                    mp1.seekTo(0);
                    mp1.setVolume(0.5f,0.5f);
                    mp1.start();
                    break;
            }
        }
    };
}