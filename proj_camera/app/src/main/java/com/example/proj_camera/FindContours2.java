package com.example.proj_camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FindContours2 {
    private Mat src;

    private Mat enhancedSrc;
    private boolean isSimple;
    private static final int MAX_THRESHOLD = 255;
    /*
    meanable number
    19~30 : on white paper
    */
    private int threshold = 50;
    private Random rng = new Random(12345);

    // Define constants
    Scalar BLUE = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);
    Scalar RED = new Scalar(0, 0, 255);
    double FILTER_RATIO = 0.85;

    public FindContours2(String path, boolean isSimple){
        src = Imgcodecs.imread(path);
        if(src.empty()){
            Log.e("KSM", "Cannot Read image : "+path);
            System.exit(0);
        }
        this.isSimple = isSimple;

        enhancedSrc = img_contrast(src);
//        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.blur(srcGray, srcGray, new Size(3,3));
    }

    public Bitmap getBitmap(){
        Mat filterImg = new Mat();
        Core.inRange(enhancedSrc, new Scalar(0,0,0), new Scalar(255,150,255), filterImg);

        List<MatOfPoint> contoursSimple = getContours(filterImg, 50, true);
        List<MatOfPoint> contoursNone = getContours(filterImg, 50, false);

        String simpleText = "contours count : "+ contoursSimple.size();
        Mat simpleImg = putTextAndDrawContours(enhancedSrc.clone(), simpleText, contoursSimple, BLUE, GREEN, RED, 0.1, true);

        // Text for none contours
        String noneText = "contours count : " + contoursNone.size();
        Mat noneImg = putTextAndDrawContours(enhancedSrc.clone(), noneText, contoursNone, BLUE, GREEN, RED, 0.1, true);

        Bitmap bmp = null;

        if(isSimple){
            try{
                bmp = Bitmap.createBitmap(simpleImg.cols(), simpleImg.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(simpleImg, bmp);
            }catch(CvException e){
                Log.e("KSM", "Mat to bitmap Error!!", e);
            }
        }else{
            try{
                bmp = Bitmap.createBitmap(noneImg.cols(), noneImg.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(noneImg, bmp);
            }catch(CvException e){
                Log.e("KSM", "Mat to bitmap Error!!", e);
            }
        }

        return bmp;
    }

    private static List<MatOfPoint> getContours(Mat img, int minArea, boolean isSimple){
        List<MatOfPoint> result = new ArrayList<>();
        Mat hierarchy = new Mat();
        int method = isSimple ? Imgproc.CHAIN_APPROX_SIMPLE : Imgproc.CHAIN_APPROX_NONE;

        Imgproc.findContours(img, result, hierarchy, Imgproc.RETR_EXTERNAL, method);

        List<MatOfPoint> filteredContours = new ArrayList<>();
        for(MatOfPoint contour : result){
            if(Imgproc.contourArea(contour) > minArea){
                filteredContours.add(contour);
            }
        }

        return filteredContours;
    }

    private static Mat putTextAndDrawContours(Mat img, String text, List<MatOfPoint> contours,
                                              Scalar blue, Scalar green, Scalar red,
                                              double epsilon, boolean drawPoints){
        Imgproc.putText(img, text, new Point(0, 25), Imgproc.FONT_HERSHEY_DUPLEX, 1, red);

        for(MatOfPoint contour : contours){
            Imgproc.drawContours(img, contours, contours.indexOf(contour), blue, 2);

            if (isCircle(contour)) {
                drawPoints(img, contour, epsilon, red, drawPoints);
            } else {
                drawPoints(img, contour, epsilon, green, drawPoints);
            }
        }

        return img;
    }

    // Function to check if the contour is a circle
    private static boolean isCircle(MatOfPoint contour) {
        double cntLength = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
        double cntArea = Imgproc.contourArea(contour);
        double ratio = 4 * Math.PI * cntArea / (cntLength * cntLength);

        return ratio > 0.85;
    }

    // Function to draw points on the image
    private static void drawPoints(Mat img, MatOfPoint contour, double epsilon, Scalar color, boolean drawPoints) {
        double cntLength = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, epsilon * cntLength, true);

        if (drawPoints) {
            for (Point point : approx.toList()) {
                Imgproc.circle(img, point, 3, color, -1);
            }
        }
    }

    private Mat img_contrast(Mat img){
        Mat lab = new Mat();
        Imgproc.cvtColor(img, lab, Imgproc.COLOR_BGR2Lab);

        List<Mat> labCh = new ArrayList<>();
        Core.split(lab, labCh);
        Mat lChannel = labCh.get(0);

        CLAHE clahe = Imgproc.createCLAHE(3.0, new Size(8,8));
        Mat cl = new Mat();
        clahe.apply(lChannel, cl);

        labCh.set(0, cl);
        Mat limg = new Mat();
        Core.merge(labCh, limg);

        Mat enhancedImg = new Mat();
        Imgproc.cvtColor(limg, enhancedImg, Imgproc.COLOR_Lab2BGR);

        return enhancedImg;
    }
}
