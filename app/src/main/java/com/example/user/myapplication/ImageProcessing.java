package com.example.user.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageProcessing extends AppCompatActivity {

    private static final String TAG = "ImageProcessingActivity";

    // Buttons:
    Button mLoadImageButton;
    Button mConvert2GrayButton;

    //intents codes
    private static final int CHOOSE_IMG_FROM_GALLERY_CODE = 1;
    private static final int CONVERT_IMG_TO_GRAYSCALE = 2;

    // Here we will hold the loaded from memory image and a grayscale version, on which we'll perform the processing.
    // This instance will be saved upon clicking the load image button and its related method.
    private Mat mLoadedImage;
    private Mat mGrayImageToProcess;


    private BaseLoaderCallback mLoaderCallback = new
            BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.i(TAG, "OpenCV loaded successfully");
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
        setContentView(R.layout.activity_image_processing);

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

        // Take care of convert to grayscale button"
        //final Context context = this;
        mConvert2GrayButton = (Button) findViewById(R.id.cnvrt2Gray);
        mConvert2GrayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                convertToGrayscaleAction();
            }
        });
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
                Bitmap fetchedImage = (BitmapFactory.decodeFile(picturePath));
                mLoadedImage = MyImageProc.myConvertBitmapToMat(fetchedImage);
                Log.w("path of image from gallery: ", picturePath + "");
                // Allows viewing images from gallery, by a click of a button.
                ImageView imageView = (ImageView) findViewById(R.id.imageViewGallery);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(fetchedImage);
            }
        }
    }

    protected void convertToGrayscaleAction() {
        Mat grayImg = new Mat();
        Imgproc.cvtColor(mLoadedImage, grayImg, Imgproc.COLOR_BGR2GRAY);
        mGrayImageToProcess = grayImg;
        ImageView imageView = (ImageView) findViewById(R.id.imageViewGallery);
        imageView.setVisibility(View.VISIBLE);
        Bitmap grayBmp = Bitmap.createBitmap(grayImg.cols(), grayImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayImg, grayBmp);
        imageView.setImageBitmap(grayBmp);
        grayImg.release();
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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
