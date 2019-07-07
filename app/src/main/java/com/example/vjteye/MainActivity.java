package com.example.vjteye;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private CameraManager cameraManager;
    private int cameraFacing;
    private String cameraId;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private CameraCaptureSession cameraCaptureSession;
    private Size previewSize;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private Button capture_button;
    private File galleryFolder;
    private File file;
    private String path;
    private File outputDir;
    public  LinkedList<Department>departments;
    private ViewAdapter viewAdapter;
    private RecyclerView recyclerView;
    private LinkedList<Site> sites;
    private static int siteCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        createImageGallery();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        sites = new LinkedList<Site>();
        capture_button = (Button) findViewById(R.id.button2);
        departments = new LinkedList<Department>();
        try {
            setDepData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSiteData();

        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                setUpCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
                MainActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                cameraDevice.close();
                ;
                MainActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {
                cameraDevice.close();
                ;
                MainActivity.this.cameraDevice = null;
            }
        };
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                onTakePhoneButtonClicked();

            }
        });
        setUpRecyclerView();
//        removeInfo();


    }

    @Override
    protected void onResume() {
        super.onResume();

        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

    }


    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null) {
                        return;
                    }
                    try {
                        captureRequest = captureRequestBuilder.build();
                        MainActivity.this.cameraCaptureSession = cameraCaptureSession;
                        MainActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest, null, backgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private void setUpCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                    this.cameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

    }

    private void createImageGallery() {
        Toast.makeText(this, Environment.DIRECTORY_PICTURES.toString(), Toast.LENGTH_LONG).show();
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath().toString() + file.separator + "VJTEYE";
        Toast.makeText(this, path, Toast.LENGTH_LONG).show();

        outputDir = new File(path);
        if(!outputDir.exists()){
            boolean wasCreated = outputDir.mkdirs();
            if(!wasCreated)
            Toast.makeText(this, "Directory created", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(this, "Makedirs failed", Toast.LENGTH_LONG).show();


        }
        else{
            Toast.makeText(this, "Directory not created", Toast.LENGTH_LONG).show();

        }
//        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
//        if (!galleryFolder.exists()) {
//            boolean wasCreated = galleryFolder.mkdirs();
//        if (!wasCreated) {
//                Log.e("CapturedImages", "Failed to create directory");
//            Toast.makeText(this, "Directory created", Toast.LENGTH_LONG).show();
//
//        } else{
//            Toast.makeText(this, "Directory not created", Toast.LENGTH_LONG).show();
//
//        }
//        }else{
//            Log.v("CapturedImages", "Failed to create directory");
//            Toast.makeText(this, "Directory not created", Toast.LENGTH_LONG).show();
//
//        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }

    public void onTakePhoneButtonClicked() {
//        lock();
        FileOutputStream outputPhoto = null;
        try {
            outputPhoto = new FileOutputStream(createImageFile(outputDir));
            textureView.getBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
            Toast.makeText(this, "Captured", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setDepData() throws IOException {
        InputStream isDep = getResources().openRawResource(R.raw.departments);
        BufferedReader reader = new BufferedReader(new InputStreamReader(isDep, Charset.forName("UTF-8")));
        String line;
        reader.readLine();
        while((line = reader.readLine()) != null){
            String[] tokens = line.split(",");
            Department department = new Department();
            department.setName(tokens[0]);
            department.setDep_id(Integer.parseInt(tokens[1]));
            departments.add(department);


        }
    }

    private void setSiteData(){
        //Go through departments and addSite for each one of them
        //Alt 1) go through each line in sites.csv and then set it to a local site which will then be set to

        InputStream isSite = getResources().openRawResource(R.raw.sites);
        BufferedReader reader = new BufferedReader(new InputStreamReader(isSite, Charset.forName("UTF-8")));
        String line;
        try{
        reader.readLine();
        while((line = reader.readLine()) != null) {
            Site site = new Site();
            String[] tokens = line.split(",");
            site.setName(tokens[0]);
            site.setId(Integer.parseInt(tokens[2]));
            site.setUnique_id(Integer.parseInt(tokens[3]));
            site.setInfo(tokens[4]);
            departments.get(Integer.parseInt(tokens[1]) - 1).addSite(site);
        } }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(this, "IO error occured while setting Sites", Toast.LENGTH_LONG).show();

        }
    }
    private void setList(int departmentId, int id){

        sites.add(departments.get(departmentId - 1).landmarks.get(id - 1));


    }
    private void update(ViewAdapter viewAdapter){
        viewAdapter.notifyItemInserted(siteCount);
        siteCount += 1;
    }
    private void setUpRecyclerView() {
        setList(4, 1);
        setList(4, 2);
        recyclerView = findViewById(R.id.detected_info);
        viewAdapter = new ViewAdapter(sites);

        recyclerView.setAdapter(viewAdapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));




        Toast.makeText(this, "RecyclerView made", Toast.LENGTH_LONG).show();

    }

//    private void removeInfo(){
//        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP | ItemTouchHelper.DOWN) {
//            @Override
//            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//                return false;
//            }
//
//            @Override
//            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
//               wordList.remove(viewHolder.getAdapterPosition());
//               viewAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
//
//            }
//        });
//        itemTouchHelper.attachToRecyclerView(recyclerView);
//    }

}




