package com.example.user.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyBasicCameraActivity extends AppCompatActivity {

    //intents codes
    private static final int CHOOSE_IMG_FROM_GALLERY_CODE = 1;

    private CameraListener mCameraListener = new CameraListener();
    private MyJavaCameraView mOpenCvCameraView;
    private static final String TAG = "HelloWorldOpenCV";
    private Button mSaveButton;
    private Button mLoadImageButton;

    //menu members
    private static final int SETTINGS_GROUP_ID = 1;
    private static final int RESOLUTION_GROUP_ID = 1;
    private static final int SWITCHCAM_ID = 2;
    //sub-menus
    private SubMenu mResolutionSubMenu;
    private SubMenu mCameraSubMenu;

    //flags
    private Boolean mSettingsMenuAvaialable =false;



    private String[] mCameraNames = {"Front", "Back"};
    private int[] mCameraIDarray = {CameraBridgeViewBase.CAMERA_ID_FRONT, CameraBridgeViewBase.CAMERA_ID_BACK};

    private BaseLoaderCallback mLoaderCallback = new
            BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.i(TAG, "OpenCV loaded successfully");
                            mOpenCvCameraView.enableView();
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_camera);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (MyJavaCameraView)
                findViewById(R.id.My_Java_Camera_View);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(mCameraListener);

        // Take care of the Save button's action:
        mSaveButton=(Button) findViewById(R.id.saveButton);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                SimpleDateFormat sdf = new
                        SimpleDateFormat("yy-MM-dd_HH-mm-ss");
                String currentDateandTime = sdf.format(new Date());
                String albumName = "RonisAppPics";
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), albumName);
                boolean is_mkdir = file.mkdir();
                if (!is_mkdir) {
                    Log.e(TAG, "Directory not created");
                }
                /*try {
                    file.createNewFile();
                } catch (IOException ex) {
                    Log.e(TAG, "Directory not created");
                }*/
                String fileName = file.getPath() +
                        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                                "/picture_of_myapp_" + currentDateandTime + ".jpg";
                mOpenCvCameraView.takePicture(fileName);
                addImageToGallery(fileName, MyBasicCameraActivity.this);

                Toast.makeText(MyBasicCameraActivity.this, fileName + " saved",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Take care of the Load image button:
        mLoadImageButton = (Button) findViewById(R.id.loadImageButton);
        mLoadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CHOOSE_IMG_FROM_GALLERY_CODE);
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
                mLoaderCallback); // TO DO we might need to not use async, so not to be dependant on the OpenCV app
        Log.i(TAG, "OpenCVLoader success");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CHOOSE_IMG_FROM_GALLERY_CODE) {
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA };
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));
                Log.w("path of image from gallery......******************.........", picturePath + "");
                // Allows viewing images from gallery, by a click of a button.
                ImageView imageView = (ImageView) findViewById(R.id.imageViewGallery);
                imageView.setImageBitmap(thumbnail);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "Options menu created");
        getMenuInflater().inflate(R.menu.menu_basic_camera, menu);
        super.onCreateOptionsMenu(menu);

        Menu settingsMenu = menu.addSubMenu("Settings");
        mResolutionSubMenu= settingsMenu.addSubMenu("Resolution");
        mCameraSubMenu = settingsMenu.addSubMenu("Camera");
        //we set up the settings menu in onPrepareOptionsMenu

        // *****These might be useful later on!!!!*****
        //menu.add(DEFAULT_GROUP_ID, CameraListener.VIEW_MODE_DEFAULT, Menu.NONE, "Default");

        //Menu colorMenu = menu.addSubMenu("Color");
        //colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_RGBA, Menu.NONE, "RGBA");
        //colorMenu.add(COLOR_GROUP_ID, CameraListener.VIEW_MODE_GRAYSCALE, Menu.NONE, "Grayscale");


        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mOpenCvCameraView.isCameraOpen() && !mSettingsMenuAvaialable){
            setResoltuionMenu(mResolutionSubMenu);
            setCameraMenu(mCameraSubMenu);
            mSettingsMenuAvaialable =true;
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        int groupId = item.getGroupId();
        int id = item.getItemId();
        switch (groupId) {
            case RESOLUTION_GROUP_ID:
                // we chose a new resolution
                Camera.Size res =
                        mOpenCvCameraView.getResolutionList().get(id);
                mOpenCvCameraView.setResolution(res);
                res = mOpenCvCameraView.getResolution();
                Toast.makeText(this, res.width + "x" + res.height,
                        Toast.LENGTH_SHORT).show();
                break;
            case SWITCHCAM_ID:
                mOpenCvCameraView.changeCameraIndex(mCameraIDarray[id]);
                String caption = mCameraNames[id] + " camera";
                Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
                setResoltuionMenu(mResolutionSubMenu);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        ImageView im = (ImageView)findViewById(R.id.imageViewGallery);
        if (im.getVisibility() != View.INVISIBLE) {
            im.setVisibility(View.INVISIBLE);
        } else {
            finish();
        }
    }

    private static void addImageToGallery(final String filePath, final Context
            context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN,
                System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        context.getContentResolver().
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public void setResoltuionMenu(SubMenu resMenu){
        int i=0;

        resMenu.clear();
        for (Camera.Size res : mOpenCvCameraView.getResolutionList()) {
            resMenu.add(RESOLUTION_GROUP_ID, i++, Menu.NONE, res.width + "x" + res.height);
        }
    }

    private void setCameraMenu(SubMenu camMenu){
        for (int i = 0; i < mOpenCvCameraView.getNumberOfCameras(); i++) {
            camMenu.add(SWITCHCAM_ID, i, Menu.NONE, mCameraNames[i]);
        }
    }


}
