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
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Vector;

import static com.example.user.myapplication.MyImageProc.doubleArrayToMat;
import static org.opencv.imgproc.Imgproc.resize;

public class ImageProcessing extends AppCompatActivity {

    private static final String TAG = "ImageProcessingActivity";

    // Buttons:
    Button mLoadImageButton;
    Button mConvert2GrayButton;
    Button mSplitToPatchesButton;
    Button mMergePatchesButton;
    Button mDenoiseImageButton;

    // Menu IDs:
    private static final int BASIC_PROC_ID = 0;
    private static final int ADV_PROC_ID = 1;
    // Sub-Menus IDs:
    private static final int CONV_TO_GRAYSCALE = 0;
    private static final int SPLIT_TO_PATCHES = 0;
    private static final int MERGE_PATCHES = 1;

    // Menus:
    private SubMenu mBasicProcMenu;
    private SubMenu mAdvancedProcMenu;

    boolean mMenuIsAvailable = false;

    //intents codes
    private static final int CHOOSE_IMG_FROM_GALLERY_CODE = 1;
    private static final int CONVERT_IMG_TO_GRAYSCALE = 2;

    // Here we will hold the loaded from memory image and a grayscale version, on which we'll perform the processing.
    // This instance will be saved upon clicking the load image button and its related method.
    private Mat mLoadedImage;
    private Mat mGrayImageToProcess;
    private Mat mDisplayedPatch;

    // Split to patches data:
    Vector<Mat> mPatches;
    Vector<Rect> mPatchesRects;
    Vector<Mat> mDenoisedPatches;
    Mat mReconstImage;
    int mShiftForSplitting;
    int mPatchSize;

    // This is the netparams in use for the advanced image processing:
    String mNetparamsFilename = "java_netparams_poiss_ours_temp_train2000_test1000_k_1_T_16_dr_4.mat";
    MLNet mNetparams;
    NetworkParameters mNetworkParameters;


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

        // Take care of convert to grayscale button:
        mConvert2GrayButton = (Button) findViewById(R.id.cnvrt2Gray);
        mConvert2GrayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                convertToGrayscaleAction();
            }
        });

        // Take care of split-to-patches button:
        mSplitToPatchesButton = (Button) findViewById(R.id.split2Patches);
        mSplitToPatchesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                splitToPatches();
            }
        });

        // Take care of merge patches button:
        mMergePatchesButton = (Button) findViewById(R.id.mergePatches);
        mMergePatchesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                mergePatches(mPatches);
            }
        });

        mDenoiseImageButton = (Button) findViewById(R.id.denoiseImage);
        mDenoiseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick event");
                denoiseImage();
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

    // Convert the loaded-from-memory image to grayscale, and save in the designated private member.
    protected void convertToGrayscaleAction() {
        Mat grayImg = new Mat();
        Imgproc.cvtColor(mLoadedImage, grayImg, Imgproc.COLOR_BGR2GRAY);
        mGrayImageToProcess = grayImg;
        ImageView imageView = (ImageView) findViewById(R.id.imageViewGallery);
        imageView.setVisibility(View.VISIBLE);
        Bitmap grayBmp = Bitmap.createBitmap(grayImg.cols(), grayImg.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(grayImg, grayBmp);
        imageView.setImageBitmap(grayBmp);
        MyImageProc.scaleImageBy255(mGrayImageToProcess);
        // TEMPORARY UNTIL SPEED-UP IS DONE:
        Size imSize = new Size(100,100);
        resize(mGrayImageToProcess, mGrayImageToProcess, imSize);
    }

    // Split to patches and display the 100th patch.
    protected void splitToPatches() {
        int maxAmountOfPatches = mGrayImageToProcess.rows() * mGrayImageToProcess.cols();
        mPatchesRects = new Vector<>(maxAmountOfPatches);
        mShiftForSplitting = mNetparams.shiftAmount;
        mPatchSize = mNetparams.patchSize;
        mPatches = MyImageProc.splitToPatches(mGrayImageToProcess, mPatchSize, mShiftForSplitting, mPatchesRects);
        mDisplayedPatch = mPatches.get(100);
        Bitmap patchBmp = Bitmap.createBitmap(mDisplayedPatch.cols(), mDisplayedPatch.rows(), Bitmap.Config.ARGB_8888);
        MyImageProc.scaleImageUpBy255(mDisplayedPatch);
        Utils.matToBitmap(mDisplayedPatch, patchBmp);
        ImageView imageView = (ImageView) findViewById(R.id.imageViewGallery);
        imageView.setImageBitmap(patchBmp);
    }

    // Merge patches and display merged result.
    protected void mergePatches(Vector<Mat> patchesVector) {
        mReconstImage = MyImageProc.mergePatches(patchesVector, mGrayImageToProcess.rows(), mGrayImageToProcess.cols(), mPatchesRects);
        Log.i(TAG, "Successfully merged patches");
        //mDisplayedPatch = reconstructedImage;
        Bitmap patchBmp = Bitmap.createBitmap(mReconstImage.cols(), mReconstImage.rows(), Bitmap.Config.ARGB_8888);
        MyImageProc.scaleImageUpBy255(mReconstImage);
        Utils.matToBitmap(mReconstImage, patchBmp);
        ImageView imageView = (ImageView) findViewById(R.id.imageViewGallery);
        imageView.setImageBitmap(patchBmp);
    }

    // Denoise image based on the loaded netparams.
    protected void denoiseImage() {
        // First - load network parameters:
        try {
            InputStream netparamsStream = getResources().getAssets().open(mNetparamsFilename);
            File netparamsFile = new File(getFilesDir() + "tempNetparams");
            copyInputStreamToFile(netparamsStream, netparamsFile);
            Log.i(TAG, "File exists? " + String.valueOf(netparamsFile.exists()));
            readNetwork(netparamsFile);
            //mNetparams.networkParameters = mNetworkParameters;
            netparamsFile.delete();
            Log.i(TAG, "Loaded netparams successfully");
        } catch (IOException ex) {
            Log.e(TAG, "Couldn't read netparams file");
            return;
        }
        // an initial check for a simple multiplication... on a single patch
        boolean keepSrcMatrices = true;
        Mat patchColumn = new Mat();
        //Log.i(TAG, "Successfully created patchColumn");
        mDenoisedPatches = new Vector<>(mPatches.size());
        for (int patchIdx=0; patchIdx < mPatches.size(); patchIdx++) {

            Mat processedPatchColumn = MyImageProc.matrixToColumn(mPatches.elementAt(patchIdx),keepSrcMatrices);
            Mat z0 = Mat.zeros(NetworkParameters.D.cols(), processedPatchColumn.cols(), processedPatchColumn.type());
            MLNet.PropagateForward(z0, processedPatchColumn);
            //Core.gemm(dict, patchColumn, 1, new Mat(), 0, processedPatchColumn);
            Mat processedPatch = MyImageProc.columnToMatrix(processedPatchColumn, !keepSrcMatrices);
            mDenoisedPatches.add(patchIdx, processedPatch);
            z0.release();
            if (patchIdx % 10 == 0) {
                Log.i(TAG, "Processing patch " + String.valueOf(patchIdx) + "/" + String.valueOf(mPatches.size()));
            }
        }

        mergePatches(mDenoisedPatches);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "Options menu created");
        getMenuInflater().inflate(R.menu.menu_image_processing, menu);
        super.onCreateOptionsMenu(menu);

        mBasicProcMenu = menu.addSubMenu("Basic Image Processing");
        mAdvancedProcMenu = menu.addSubMenu("Advanced Image Processing");

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (!mMenuIsAvailable) {
            mBasicProcMenu.add(BASIC_PROC_ID, CONV_TO_GRAYSCALE, Menu.NONE, "Convert to Grayscale");
            mAdvancedProcMenu.add(ADV_PROC_ID, SPLIT_TO_PATCHES, Menu.NONE, "Split to Patches");
            mAdvancedProcMenu.add(ADV_PROC_ID, MERGE_PATCHES, Menu.NONE, "Merge Patches");
            mMenuIsAvailable = true;
            Log.i(TAG, "Options menu prepared");
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        int groupId = item.getGroupId();
        int id = item.getItemId();
        switch (groupId) {
            case BASIC_PROC_ID:
                switch (id) {
                    case CONV_TO_GRAYSCALE:
                        convertToGrayscaleAction();
                        break;
                }
                break;
            case ADV_PROC_ID:
                switch (id) {
                    case SPLIT_TO_PATCHES:
                        splitToPatches();
                        break;
                    case MERGE_PATCHES:
                        mergePatches(mPatches);
                        break;
                }
                break;
        }
        return true;
    }


    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
                mLoaderCallback);
        Log.i(TAG, "OpenCVLoader success");
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPatches != null) {
            mPatches.clear();
        }
        if (mPatchesRects != null) {
            mPatchesRects.clear();
        }
        if (mDenoisedPatches != null) {
            mDenoisedPatches.clear();
        }
    }

    // Aid function to convert an input stream into a file.
    private void copyInputStreamToFile( InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024 * 250]; //250kB
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This function reads a mat file (maybe) that contains java arrays,
    // and convert them to Mat files.
    public void readNetwork(File netparamsInputFile) {
        Log.i(TAG, "Entered readNetwork");
        MatFileReader netparamsFile = null;
        try {
            netparamsFile = new MatFileReader(netparamsInputFile);
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read netparams file");
            return; // Ori
        }
        Log.i(TAG, "Tried, didn't catch");
        if (netparamsFile != null) {
            Map<String,MLArray> netparamsMap = netparamsFile.getContent();
            if (netparamsMap != null) {
                MLArray DArr = netparamsMap.get("javaD");
                Log.i(TAG, "Succeeded in getting javaD MLArray");
                MLDouble DArrDouble = (MLDouble) DArr;
                Log.i(TAG, "Succeeded in casting javaD MLArray to MLDouble");
                double[][] D = ((MLDouble) netparamsMap.get("javaD")).getArray();
                NetworkParameters.D = new Mat(D.length, D[0].length, CvType.CV_32FC1);
                MyImageProc.doubleArrayToMat(D, mNetworkParameters.D);
                Log.i(TAG, "Loaded D successfully");
                double[][] W = ((MLDouble) netparamsMap.get("javaW")).getArray();
                NetworkParameters.W = new Mat(W.length, W[0].length, CvType.CV_32FC1);
                MyImageProc.doubleArrayToMat(W, mNetworkParameters.W);
                Log.i(TAG, "Loaded W successfully");
                double[][] A = ((MLDouble) netparamsMap.get("javaA")).getArray();
                NetworkParameters.A = new Mat(A.length, A[0].length, CvType.CV_32FC1);
                doubleArrayToMat(A, mNetworkParameters.A);
                Log.i(TAG, "Loaded A successfully");
                double[][] Q = ((MLDouble) netparamsMap.get("javaQ")).getArray();
                NetworkParameters.Q = new Mat(Q.length, Q[0].length, CvType.CV_32FC1);
                doubleArrayToMat(Q, mNetworkParameters.Q);
                Log.i(TAG, "Loaded Q successfully");
                double[][] t = ((MLDouble) netparamsMap.get("javat")).getArray();
                NetworkParameters.t = new Mat(t.length, t[0].length, CvType.CV_32FC1);
                doubleArrayToMat(t, mNetworkParameters.t);
                Log.i(TAG, "Loaded t successfully");
                mNetworkParameters.T = (int) (((MLDouble) netparamsMap.get("javaT")).getArray())[0][0];
                Log.i(TAG, "Loaded T successfully");

                Log.i(TAG, "Value of T: " + String.valueOf(mNetworkParameters.T));
            } else {
                Log.e(TAG, "Unable to create content map from netparams file");
            }
            //netparamsMap.clear();
        }

    }

}
