package com.example.proj_camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.Random;

public class GoodFeaturesToTrack {


    private Mat src = new Mat();
    private Mat srcGray = new Mat();
    private static final int MAX_THRESHOLD = 100;
    private int maxCorners = 23;
    private Random rng = new Random(12345);

    public GoodFeaturesToTrack(String path){
        src = Imgcodecs.imread(path);
        if(src.empty()){
//            System.err.println("Cannot Read image : "+path);
            Log.e("KSM", "Cannot Read image : "+path);
            System.exit(0);
        }

        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
    }

    public Bitmap update(){
        Bitmap bmp = null;

        maxCorners = Math.max(maxCorners, 1);
        MatOfPoint corners = new MatOfPoint();
        double qualityLevel = 0.01;
        double minDistance = 10;
        int blockSize = 3, gradientSize = 3;
        boolean useHarrisDetector = false;
        double k = 0.04;

        Mat copy = src.clone();

        Imgproc.goodFeaturesToTrack(srcGray, corners, maxCorners, qualityLevel, minDistance, new Mat(),
                blockSize, gradientSize, useHarrisDetector, k);

        Log.d("KSM", "OPENCV : Number of corners detected : "+corners.rows());
        int[] cornersData = new int[(int) (corners.total() * corners.channels())];
        corners.get(0, 0, cornersData);
        int radius = 4;
        for(int i = 0; i < corners.rows(); i++){
            Imgproc.circle(copy, new Point(cornersData[i*2], cornersData[i*2+1]), radius,
                    new Scalar(255,0,0), Core.FILLED);
        }

        try{
            bmp = Bitmap.createBitmap(copy.cols(), copy.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(copy, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }

        return bmp;
    }
}
