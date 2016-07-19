package com.example.user.myapplication;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Vector;



/**
 * Created by user on 17/04/2016.
 */
public class MyImageProc extends CameraListener {

    private static final String TAG = "MyImageProc";


    public static Mat getPatch(Mat inputImage, int size, int xTopLeftPos, int yTopLeftPos, Vector<Rect> rects, int rectIndex) {
        Rect rect = new Rect(xTopLeftPos,yTopLeftPos,size,size);
        rects.add(rectIndex, rect);
        Mat patch = new Mat(inputImage, rect);
        return patch;
    }


    // Split the inputImage to patches, according to the supplied shift amount and the patch size, and return a vector of the patches.
    // The function will also save the "rects" the were used in the splitting process, so we'll have matching patch<->rect pairs.
    public static Vector<Mat> splitToPatches(Mat inputImage, int patchSize, int shiftAmount, Vector<Rect> rects) {
        int amountOfPatches = ((int) Math.floor((inputImage.rows() - patchSize)/shiftAmount) + 1) * ((int) Math.floor((inputImage.cols() - patchSize)/shiftAmount) + 1);
        Vector<Mat> patchesVec = new Vector<>(amountOfPatches);
        int patchCounter = 0;
        Mat patch;
        for (int row=0; row < (inputImage.rows() - patchSize + 1); row+=shiftAmount) {
            for (int col=0; col< (inputImage.cols() - patchSize + 1); col+=shiftAmount) {
                if (patchCounter >= amountOfPatches) {
                    Log.e(TAG, "Exceeded expected amount of patches");
                }
                patch = getPatch(inputImage,patchSize,col,row,rects,patchCounter);
                patchesVec.add(patchCounter, patch);
                patchCounter++;
            }
        }
        return patchesVec;
    }

    // Copy the content of a double array to a Mat object.
    public static void doubleArrayToMat(double[][] doubleArray, Mat matArray) {
        if (doubleArray == null) {
            Log.e(TAG, "Double[][] is null");
            return;
        }
        if (matArray == null) {
            Log.e(TAG, "Mat is null");
            return;
        }
        int d1 = doubleArray.length;
        int d2 = doubleArray[0].length;
        for (int i=0; i<d1; i++) {
            for (int j=0; j<d2; j++) {
                matArray.put(i,j,doubleArray[i][j]);
            }
        }
    }

    // Merge the patches in "patchesVec" to a complete image, and return it.
    // The merging process does an average of the overlapping parts.
    public static Mat mergePatches(Vector<Mat> patchesVec, int rowsAmount, int colsAmount, Vector<Rect> rects) {

        Mat recontructedImage = Mat.zeros(rowsAmount, colsAmount, patchesVec.get(0).type());
        Mat aidRecontPatch = new Mat(patchesVec.get(0).size(), recontructedImage.type());

        // This matrix will hold counters of the amount of patches covering each pixel.
        // At the end - we will normalize the result of the addition of patches with these factors, to receive the average.
        Mat normalizingFactorMat = Mat.zeros(recontructedImage.size(), recontructedImage.type());

        Mat onesPatch = Mat.ones(patchesVec.get(0).size(), recontructedImage.type());
        Mat aidPatch = new Mat(patchesVec.get(0).size(), recontructedImage.type());

        int amountOfPatches = patchesVec.size();
        for (int patchIndex = 0; patchIndex < amountOfPatches; patchIndex++) {
            Mat currentPatch = patchesVec.get(patchIndex);
            Rect roi = rects.get(patchIndex);
            // Accumulate the values of the pixels:
            Core.add(currentPatch, recontructedImage.submat(roi), aidRecontPatch);
            aidRecontPatch.copyTo(recontructedImage.submat(roi));

            // Update the "counters" for the final normalization:
            Core.add(normalizingFactorMat.submat(roi), onesPatch, aidPatch);
            aidPatch.copyTo(normalizingFactorMat.submat(roi));
        }
        // Normalize to get the average per pixel:
        Core.divide(recontructedImage, normalizingFactorMat, recontructedImage);

        Log.i(TAG, "Leaving merge function");

        aidPatch.release();
        aidRecontPatch.release();
        normalizingFactorMat.release();
        onesPatch.release();

        return recontructedImage;
    }

    public static Mat myConvertBitmapToMat(Bitmap bmpImage) {
        Mat matImage = new Mat(bmpImage.getHeight(), bmpImage.getWidth(), CvType.CV_64FC1);
        bmpImage = bmpImage.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmpImage, matImage);
        return matImage;
    }

    // Reshape a matrix into a column (column-stack):
    public static Mat matrixToColumn(Mat srcMat, boolean keepSrcMat) {
        int rows = srcMat.rows() * srcMat.cols();
        int cols = 1;
        Mat dstMat = new Mat(rows, cols, CvType.CV_32FC1); // Must be of 32F type in order to be able to use gemm function.

        int dstMatCounter = 0; // The index for the destination column Mat
        for (int col=0; col < srcMat.cols(); col++) {
            for (int row=0; row < srcMat.rows(); row++) {
                dstMat.put(dstMatCounter, 0, srcMat.get(row,col)[0]);
                dstMatCounter++;
            }
        }
        if (!keepSrcMat) {
            srcMat.release();
        }
        return dstMat;
    }

    // Reshape a column into a matrix of patchSize x patchSize:
    public static Mat columnToMatrix(Mat srcColumnMat, boolean keepSrcMat ) {
        int patchSize = (int) Math.sqrt(srcColumnMat.rows() * srcColumnMat.cols());
        Mat dstMat = new Mat(patchSize,patchSize,CvType.CV_32FC1);

        int srcMatCounter = 0;
        for (int col=0; col < dstMat.cols(); col++) {
            for (int row=0; row < dstMat.rows(); row++) {
                dstMat.put(row, col, srcColumnMat.get(srcMatCounter, 0)[0]);
                srcMatCounter++;
            }
        }
        if (!keepSrcMat) {
            srcColumnMat.release();
        }
        return dstMat;

    }


    // Scale down an image from 0-255 to 0-1
    public static void scaleImageBy255(Mat inputImage) {
        inputImage.convertTo(inputImage, CvType.CV_32FC1);
        Core.scaleAdd(inputImage, (1.0 / 255.0), Mat.zeros(inputImage.size(), inputImage.type()), inputImage);
    }

    // Scale up an image from 0-1 to 0-255, and save as an 8bit uints:
    public static void scaleImageUpBy255(Mat inputImage) {
        // TODO this is commented for debug
        Core.scaleAdd(inputImage, (255.0), Mat.zeros(inputImage.size(), inputImage.type()), inputImage);
        inputImage.convertTo(inputImage, CvType.CV_8UC1);
    }


}
