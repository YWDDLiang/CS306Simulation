package com.example.cs306;
import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private final int CAMERA_PERMISSION_CODE=1;
    ActivityMainBinding mainBinding;
    ActivityResultLauncher<Uri> takePictureLauncher;
    Uri imageUri;
    private double total_distance = 0;
    Button camera;
    MapView mMapView = null;
    private long StartTime = 0;
    private long FinishTime = 0;
    private double speed = 0;
    SeekBar positionBar;
    MediaPlayer mp;
    int totaltime;
    private double Total_run_time = 0;
    AMap aMap = null;
    List<LatLng> latLngs = new ArrayList<LatLng>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainBinding =ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        imageUri=createUri();
        registerPictureLauncher();
        mainBinding.takePhoto.setOnClickListener(view -> {
            checkCameraPermissionAndopenCamera();
        });
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
        mp=which_music();
        mp.setLooping(false);
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
    private Uri createUri(){
        File imageFile=new File(getApplicationContext().getFilesDir(),"camera_photo.jpg");
        return FileProvider.getUriForFile(getApplicationContext(),
                "com.example.cs306.fileProvider",imageFile);
    }
    private Handler mainHandler = new Handler (Looper.getMainLooper());
    private Thread Location_Muti_Thread;
    private volatile boolean running = false;
    private Location PreviousLocation;
    public void drawLines(Location Plo, Location Clo){
        mainHandler.post(new Runnable(){
            @Override
            public void run(){
                options.add(new LatLng(Plo.getLatitude(), Plo.getLongitude()));
                options.add(new LatLng(Clo.getLatitude(), Clo.getLongitude()));
                options.width(15).geodesic(true).color(Color.RED);
                aMap.addPolyline(options);
            }
        });
    }

    private double getSpeed(double Distance, double time){
        double speed = Math.round(Distance/time*100)/100.0;
        return speed;
    }
    private void StartWalk(){
        if (Location_Muti_Thread == null || !Location_Muti_Thread.isAlive()){
            running = true;
            StartTime = SystemClock.elapsedRealtime();
            Location_Muti_Thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    while (running){
                        Location NewLocation = aMap.getMyLocation();
                        if (PreviousLocation != null && NewLocation != null){
                            drawLines(PreviousLocation, NewLocation);
                            total_distance+=getDistances(PreviousLocation,NewLocation);
                        }
                        PreviousLocation=NewLocation;
                        try{
                            Thread.sleep(100);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            });
            Location_Muti_Thread.start();
        }else{
            FinishTime= SystemClock.elapsedRealtime();
            double run_time=(FinishTime-StartTime)*0.001;
            Total_run_time+=run_time;
            speed = getSpeed(total_distance, Total_run_time);
            String text0="Total distance is: "+total_distance+" m";
            Total_run_time=Math.round(Total_run_time);
            String text1="Your total running time is: "+Total_run_time+" s";
            String text2="Your average speed is: "+speed+ " m/s";
            String text3=to_text(speed);
            showNormalDialog(text0,text1,text2,text3);
            Total_run_time=0;
            Location_Muti_Thread.interrupt();

        }

    }
    private void stopLocationThread(){
        running = false;
    }
    private void registerPictureLauncher(){
        takePictureLauncher=registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean result) {
                        try{
                            if(result){
                                mainBinding.ivUser.setImageURI(null);
                                mainBinding.ivUser.setImageURI(imageUri);
                            }
                        }catch (Exception exception){
                            exception.getStackTrace();
                        }
                    }
                }
        );
    }

    private void checkCameraPermissionAndopenCamera(){
        if (ActivityCompat.checkSelfPermission(
                MainActivity.this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION_CODE);
        }else{
            takePictureLauncher.launch(imageUri);
        }
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if (requestCode==CAMERA_PERMISSION_CODE){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                takePictureLauncher.launch(imageUri);
            }else{
                Toast.makeText(this,
                        "Camera Permission denied, please allow permission",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showNormalDialog(String text0, String text1, String text2, String text3){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle("Movement Report");
        String text_final=text0+"\n"+text1+"\n"+text2+"\n"+text3;
        normalDialog.setMessage(text_final);
        normalDialog.setPositiveButton("Download the Report",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveFileToMediaStore(getApplicationContext(),"Movement_Report.txt",text_final);
                    }
                });
        normalDialog.setNegativeButton("Okay",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });
        // 显示
        normalDialog.show();
    }
    private double getDistances(Location l1, Location l2){
        double distance = Math.round(l1.distanceTo(l2)*100)/100.0;
        if (distance>10000){
            distance=0;
        }
        return distance;
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
    public String to_text(double speed){
        String text = "";
        if (speed>3){
            text="Your average speed is pretty fast. Keep going!";
        } else if (speed<1) {
            text="Your average speed is pretty slow. Please speed up.";
        }
        else{
            text="Ah, you are just walking. Have a nice day!";
        }
        return text;
    }
    public void PlayButton(View view){
        if (!mp.isPlaying()){
            mp.start();
        }else{
            mp.pause();
        }
    }
    public void timecount(View view) {
        StartWalk();
        StartTime = SystemClock.elapsedRealtime();
//        Thread thread = new Thread(null, doBackgroundThreadProcessing,"Background");
//        thread.start();
    }


//    private Runnable doBackgroundThreadProcessing = new Runnable() {
//        int t=5;
//        public void run() {
//            int tt=t;
//            while(tt>0){
//                System.out.println(tt);
//                tt-=1;
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            handler1.sendEmptyMessage(0);
//        }
//    };
private MediaPlayer which_music(){
    double a=Math.random();
    MediaPlayer mp1;
    if (a>0.5){
        mp1=MediaPlayer.create(this,R.raw.a1);
    }else{
        mp1=MediaPlayer.create(this,R.raw.a2);
    };
    return mp1;
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
                    if (!mp1.isPlaying()){
                        mp1.start();
                    }else{
                        mp1.pause();
                    }
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestAllFilesAccess() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.getMessage();
        }
}


    public static Uri saveFileToMediaStore(Context context, String fileName, String fileContents) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri collection = MediaStore.Files.getContentUri("external");
        Uri itemUri = context.getContentResolver().insert(collection, values);

        try (FileOutputStream fos = (FileOutputStream) context.getContentResolver().openOutputStream(itemUri)) {
            fos.write(fileContents.getBytes());
            Toast.makeText(context.getApplicationContext(), "Save successfully",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            System.out.println(e);
        }
        return itemUri;
    }
}