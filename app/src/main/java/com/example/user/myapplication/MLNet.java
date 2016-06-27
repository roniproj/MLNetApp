package com.example.user.myapplication;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

/**
 * Created by user on 02/06/2016.
 */
public class MLNet {

    private static final String TAG = "MLNetClass";

    // MLNet parameters:
    // the NN:
    public NetworkParameters networkParameters;

    // The maximal image's signal.
    // This might be used to choose a network, if using different networks for different peak values.
    public int peakSignal;

    // The size of a single processed patch.
    public static int patchSize = 8;

    // The amount of shift between each patch.
    public static int shiftAmount = 1;

    // The parameter used in the "rho" function.
    public static int toEParameter = 4; // TODO: change to the max value of input image.

    // Useful functions:
    // The cost function's derivative:
    // "argument" is the parameter we derive with respect to, "samples" is the data (noisy image's patch).
    public static Mat CalculateCostsGradient(Mat argument, Mat samples) {
        Mat gradientMat = new Mat(argument.rows(), argument.cols(), CvType.CV_32FC1);
        Mat temp = new Mat(argument.rows(), argument.cols(), CvType.CV_32FC1);
        // Calculation based on "der_LogLikelihood"
        //Core.log(argument, temp);
        Core.divide(samples, argument, temp);
        //Core.multiply(samples, temp, temp);
        Core.subtract(Mat.ones(temp.size(), temp.type()), temp, gradientMat);

        temp.release();

        return gradientMat;
    }

    // Shrinkage function:
    public static Mat Shrink(Mat z, Mat threshold) {
        Mat z_shrunk = new Mat(z.rows(), z.cols(), CvType.CV_32FC1);
        Mat z_abs = new Mat(z.rows(), z.cols(), CvType.CV_32FC1);
        Mat z_diff = new Mat(z.rows(), z.cols(), CvType.CV_32FC1);

        Core.absdiff(z, Scalar.all(0.0), z_abs); // z_abs = |z|
        Core.subtract(z_abs, threshold, z_diff); // z_diff = |z|-t
        Core.max(z_diff, Scalar.all(0.0), z_shrunk); // z_shrunk = max{z_diff,0}
        setSignToDst(z, z_shrunk); // z_shrunk = sign(z) .*  max{z_diff,0}

        z_abs.release();
        z_diff.release();

        return z_shrunk;
    }

    public static void toECalculation(Mat argument, int toEParameter, Mat resultToE, Mat resultToEDerivative, boolean derive) {
        for (int row=0; row<argument.rows(); row++) {
            for (int col = 0; col < argument.cols(); col++) {
                double currVal = argument.get(row, col)[0];
                if (currVal <= 0) {
                    if (!derive) {
                        resultToE.put(row, col, (toEParameter * Math.exp(currVal) + Math.exp(-12)));
                    } else {
                        resultToEDerivative.put(row, col, (toEParameter * Math.exp(currVal) + Math.exp(-12)));
                    }
                } else {
                    if (!derive) {
                        resultToE.put(row, col, (toEParameter * (1+ currVal) ));
                    }
                    else {
                        resultToEDerivative.put(row, col, toEParameter);
                    }
                }
            }
        }
    }

    // Propagate forward in the network, according to its parameters saved in "networkParameters".
    public static Mat PropagateForward(Mat z0, Mat inputPatch) {

        // here will be A*z0, which will be the argument for the toE calculation, no derivative.
        Mat toEArgument_A = new Mat(NetworkParameters.A.rows(), z0.cols(), CvType.CV_32FC1);
        // here will be the result of the toE calculation of the above.
        Mat toE_A = new Mat(toEArgument_A.size(), CvType.CV_32FC1);


        // The same for Q*z0, only this time we will derive.
        Mat toEDerArgument_Q = new Mat(NetworkParameters.Q.rows(), z0.cols(), CvType.CV_32FC1);
        // here will be the derivative.
        Mat toEDerivative_Q = new Mat(toEDerArgument_Q.size(), CvType.CV_32FC1);

        // CalculateA*z0, and then calculate toE(A*z0)
        Core.gemm(NetworkParameters.A, z0, 1.0, Mat.zeros(toEArgument_A.size(), toEArgument_A.type()), 0.0, toEArgument_A);
        boolean derive = false;
        toECalculation(toEArgument_A,toEParameter,toE_A,null, derive);
        // Calculate Q*z0, and then calculate toE'(Q*z0)
        Core.gemm(NetworkParameters.Q, z0, 1.0, Mat.zeros(toEDerArgument_Q.size(), toEDerArgument_Q.type()), 0.0, toEDerArgument_Q);
        derive = true;
        toECalculation(toEDerArgument_Q, toEParameter, null, toEDerivative_Q, derive);

        Mat gradient = CalculateCostsGradient(toE_A, inputPatch);

        Mat zTemp = new Mat(z0.rows(), z0.cols(), z0.type());
        Mat zShrunk;
        Mat toSubstract = new Mat(z0.rows(), z0.cols(), z0.type());

        Mat blankMat = Mat.zeros(NetworkParameters.W.rows(), toEDerivative_Q.cols(), CvType.CV_32FC1);
        Core.multiply(toEDerivative_Q, gradient, toEDerivative_Q);
        Core.gemm(NetworkParameters.W, toEDerivative_Q, 1.0, blankMat, 0.0, toSubstract);

        Core.subtract(z0, toSubstract, zTemp);

        for (int i=0; i<NetworkParameters.T - 2; i++) {
            zShrunk = Shrink(zTemp, NetworkParameters.t);
            // DEBUG:
            Log.i(TAG, "Value of zShrunk(0), (8): " + String.valueOf(zShrunk.get(0,0)[0] + ", " + String.valueOf(zShrunk.get(8,0)[0])));

            // Calculate toE(A*z)
            Core.gemm(NetworkParameters.A, zShrunk, 1.0, Mat.zeros(toEArgument_A.size(), toEArgument_A.type()), 0.0, toEArgument_A);
            derive = false;
            toECalculation(toEArgument_A,toEParameter,toE_A,null, derive);
            // Calculate toE'(Q*z)
            Core.gemm(NetworkParameters.Q, zShrunk, 1.0, Mat.zeros(toEDerArgument_Q.size(), toEDerArgument_Q.type()), 0.0, toEDerArgument_Q);
            derive = true;
            toECalculation(toEDerArgument_Q, toEParameter, null, toEDerivative_Q, derive);

            gradient = CalculateCostsGradient(toE_A, inputPatch);

            Core.multiply(toEDerivative_Q, gradient, toEDerivative_Q);
            Core.gemm(NetworkParameters.W, toEDerivative_Q, 1.0, blankMat, 0.0, toSubstract);

            Core.subtract(zShrunk, toSubstract, zTemp);
        }
        zShrunk = Shrink(zTemp, NetworkParameters.t);

        // Calculate the final patch (toE(D*z))
        Mat toEArgument_D = new Mat(NetworkParameters.D.rows(), zShrunk.cols(), CvType.CV_32FC1);
        // here will be the result of the toE calculation of the above.
        Mat toE_D = new Mat(toEArgument_D.size(), CvType.CV_32FC1);
        Core.gemm(NetworkParameters.D, zShrunk, 1.0, Mat.zeros(toEArgument_D.size(), toEArgument_D.type()), 0.0, toEArgument_D);
        derive = false;
        toECalculation(toEArgument_D, toEParameter, toE_D, null, derive);
        // DEBUG:
        Log.i(TAG, "Value of toE_D(4), (16): " + String.valueOf(toE_D.get(4, 0)[0] + ", " + String.valueOf(toE_D.get(16, 0)[0])));

        // release all allocated aids:
        toE_A.release();
        toEArgument_A.release();
        toEDerivative_Q.release();
        toEDerArgument_Q.release();
        toEArgument_D.release();
        zShrunk.release();

        return toE_D;
    }

    // Aid function for the shrinkage function - multiply each element in dst with the sign of the
    // corresponding element in sgn
    private static void setSignToDst(Mat sgn, Mat dst) {
        for (int row=0; row<dst.rows(); row++) {
            for (int col=0; col<dst.cols(); col++) {
                if (sgn.get(row,col)[0] < 0) {
                    double val = dst.get(row,col)[0];
                    dst.put(row, col, (-1)*val);
                }
            }
        }
    }


}
