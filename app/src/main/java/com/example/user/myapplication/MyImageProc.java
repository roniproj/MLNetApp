package com.example.user.myapplication;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by user on 17/04/2016.
 */
public class MyImageProc extends CameraListener {

    Mat processImage(Mat inputImage) {
        // HERE we shall add calling to the processing methods.
        return inputImage;
    }

    public static Mat myConvertBitmapToMat(Bitmap bmpImage) {
        Mat matImage = new Mat(bmpImage.getHeight(), bmpImage.getWidth(), CvType.CV_64F);
        bmpImage = bmpImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmpImage, matImage);
        return matImage;
    }
}
