package com.example.proj_camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FindContours {
    private Mat srcGray = new Mat();
    private static final int MAX_THRESHOLD = 255;
    private int threshold = 100;
    private Random rng = new Random(12345);

    public FindContours(String path){
        Mat src = Imgcodecs.imread(path);
        if(src.empty()){
            Log.e("KSM", "Cannot Read image : "+path);
            System.exit(0);
        }

        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.blur(srcGray, srcGray, new Size(3,3));
    }

    public Bitmap update(){
        Bitmap bmp = null;

        Mat cannyOutput = new Mat();
        Imgproc.Canny(srcGray, cannyOutput, threshold, threshold*2);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);
        for(int i = 0; i < contours.size(); i++){
            Scalar color = new Scalar(255, 0, 0);
            Imgproc.drawContours(drawing, contours, i, color, 2, Imgproc.LINE_8, hierarchy, 0, new Point());
        }

        try{
            bmp = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(drawing, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }

        return bmp;
    }
}
