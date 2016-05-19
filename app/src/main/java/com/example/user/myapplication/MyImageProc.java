package com.example.user.myapplication;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Vector;

/**
 * Created by user on 17/04/2016.
 */
public class MyImageProc extends CameraListener {

    private static final String TAG = "MyImageProc";

    Mat processImage(Mat inputImage) {
        // HERE we shall add calling to the processing methods.
        return inputImage;
    }

    public static Mat getPatch(Mat inputImage, int size, int xTopLeftPos, int yTopLeftPos, Vector<Rect> rects, int rectIndex) {
        Rect rect = new Rect(xTopLeftPos,yTopLeftPos,size,size);
        rects.add(rectIndex, rect);
        Mat patch = new Mat(inputImage, rect);
        return patch;
    }

    public static Vector<Mat> splitToPatches(Mat inputImage, int patchSize, int shiftAmount, Vector<Rect> rects) {
        int amountOfPatches = ((int) Math.floor((inputImage.rows() - patchSize)/shiftAmount) + 1) * ((int) Math.floor((inputImage.cols() - patchSize)/shiftAmount) + 1);
        Vector<Mat> patchesVec = new Vector<>(amountOfPatches);
        int patchCounter = 0;
        for (int row=0; row < (inputImage.rows() - patchSize + 1); row+=shiftAmount) {
            for (int col=0; col< (inputImage.cols() - patchSize + 1); col+=shiftAmount) {
                if (patchCounter >= amountOfPatches) {
                    Log.e(TAG, "Exceeded expected amount of patches");
                }
                Mat patch = getPatch(inputImage,patchSize,col,row,rects,patchCounter);
                patchesVec.add(patchCounter, patch);
                patchCounter++;
                //patch.release();
            }
        }
        return patchesVec;
    }

    // Currently supports only the case where there is no averaging of overlaps.
    public static Mat mergePatches(Vector<Mat> patchesVec, int rowsAmount, int colsAmount, Vector<Rect> rects) {
        Mat recontructedImage = new Mat(rowsAmount, colsAmount, CvType.CV_8UC1);
        Log.i(TAG, "Created reconst image");
        int amountOfPatches = patchesVec.size();
        for (int patchIndex = 0; patchIndex < amountOfPatches; patchIndex++){
            //Log.i(TAG, "Entered loop " + Integer.toString(patchIndex) + "/" + Integer.toString(amountOfPatches));
            Mat currentPatch = patchesVec.get(patchIndex);
            Rect roi = rects.get(patchIndex);
            currentPatch.copyTo(recontructedImage.submat(roi));
            //Log.i(TAG, "Ended loop " + Integer.toString(patchIndex) + "/" + Integer.toString(amountOfPatches));
        }
        Log.i(TAG, "Leaving merge function");
        return recontructedImage;
    }

    public static Mat myConvertBitmapToMat(Bitmap bmpImage) {
        Mat matImage = new Mat(bmpImage.getHeight(), bmpImage.getWidth(), CvType.CV_64F);
        bmpImage = bmpImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmpImage, matImage);
        return matImage;
    }
}
