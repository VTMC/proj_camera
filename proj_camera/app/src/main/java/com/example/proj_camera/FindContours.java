package com.example.proj_camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FindContours {
    private Mat src = new Mat();
    private Mat srcGray = new Mat();
    private Mat enhancedSrc = new Mat();
    private Mat croppedSrc = new Mat();
    private Mat drawing = new Mat();
    private Mat final_drawing = new Mat();
    private Mat resultMat_2 = new Mat();
    private static final int MAX_THRESHOLD = 255;
    /*
    meanable number
    19~30 : on white paper
    */
    private int threshold;
    private int threshold2 = threshold;
    private Random rng = new Random(12345);

    public String[] cropImgFileList;

    int croppedImg_x = 0;
    int croppedImg_y = 0;
    int croppedImg_w = 0;
    int croppedImg_h = 0;

    public FindContours(String path){
        src = Imgcodecs.imread(path);
        if(src.empty()){
            Log.e("KSM", "Cannot Read image : "+path);
            System.exit(0);
        }

        //existing method - get contrast
        enhancedSrc = img_contrast(src);

        Imgproc.cvtColor(enhancedSrc, srcGray, Imgproc.COLOR_BGR2GRAY);

//        Imgproc.blur(srcGray, srcGray, new Size(3,3));
    }

    public Bitmap update(){
        Bitmap bmp = null;
        long startTimeMillis = System.currentTimeMillis();

        //new method - threshold
        Mat thresholdOutput = new Mat();
        //if background color is black
        Imgproc.threshold(srcGray, thresholdOutput, 130, 255, Imgproc.THRESH_BINARY);
        /*180 = 검은색 천(반사율이 적은 검은색 바탕) / 140 = 검은색 종이 (반사율이 어느 정도 있는 검은색 바탕)
         * 190 = 검은색 종이, 안의 사각형이 찍힘.(1,3,4,5,6,7,11) *//*
        //if background color is wthie
//        Imgproc.threshold(srcGray, thresholdOutput, 180, 255, Imgproc.THRESH_BINARY_INV);
        *//* 190 = 검은색 종이, 안의 사각형 찍힘(1,3,4,5,6,7,11), 정면에서는 아얘 찍히지도 않음(비스듬히 찍어야함)
         * */
        List<MatOfPoint> contoursSimple = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdOutput, contoursSimple, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);


        //new method - threshold
        drawing = Mat.zeros(thresholdOutput.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple.size(); i++){
            Scalar color = new Scalar(255, 0, 0);
            Imgproc.drawContours(drawing, contoursSimple, i, color, 5, Imgproc.LINE_8, hierarchy, 2, new Point());
        }

        Mat croppedImg = cropImg(src, contoursSimple, 70, 1400);


        if(croppedImg.width() > 500){
            return bmp;
        }

        //Crop src like croppedImg
        Rect roi = new Rect(croppedImg_x, croppedImg_y, croppedImg_w, croppedImg_h);
        croppedSrc = new Mat(src, roi);

        /*List<MatOfPoint> contoursSimple2 = new ArrayList<>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(thresOutput, contoursSimple2, hierarchy2, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        final_drawing = Mat.zeros(thresOutput.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple2.size(); i++){
            Scalar color = new Scalar(255, 0, 0);
            Imgproc.drawContours(final_drawing, contoursSimple2, i, color, 1, Imgproc.LINE_8, hierarchy2, 2, new Point());
        }

        cropImgFileList = finalCropImg(croppedSrc, contoursSimple2, 80, 80);*/

        long endTimeMillis = System.currentTimeMillis();
        long progressedTime = endTimeMillis - startTimeMillis;
        Log.d("KSM", "==========[progressedTime]==========\n" +
                "progressedTime : "+progressedTime+"\n" +
                "====================================\n");

        Mat result_mat = new Mat();
        Core.add(drawing, enhancedSrc, result_mat);

        try{
            Imgproc.cvtColor(croppedSrc, croppedSrc, Imgproc.COLOR_BGR2RGB);
            bmp = Bitmap.createBitmap(croppedSrc.cols(), croppedSrc.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedSrc, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }

        Log.d("KSM", "bmp width : "+bmp.getWidth()+" / height : "+bmp.getHeight());

        return bmp;
    }

    public Bitmap update2(){
        Bitmap bmp = null;

        Mat fittedImg = new Mat();
        fittedImg = fitImg(croppedSrc, src.width(), src.height());

        try{
            bmp = Bitmap.createBitmap(fittedImg.cols(), fittedImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(fittedImg, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }
        return bmp;
    }

    private Mat img_contrast(Mat img){
        Mat lab = new Mat();
        Imgproc.cvtColor(img, lab, Imgproc.COLOR_BGR2Lab);

        List<Mat> labCh = new ArrayList<>();
        Core.split(lab, labCh);
        Mat lChannel = labCh.get(0);

        CLAHE clahe = Imgproc.createCLAHE(3.5, new Size(8,8));
        Mat cl = new Mat();
        clahe.apply(lChannel, cl);

        labCh.set(0, cl);
        Mat limg = new Mat();
        Core.merge(labCh, limg);

        Mat enhancedImg = new Mat();
        Imgproc.cvtColor(limg, enhancedImg, Imgproc.COLOR_Lab2BGR);

        return enhancedImg;
    }

    private Mat mosaic(Mat img, int rate){
        Mat mosaic = new Mat();
        Imgproc.resize(img, mosaic, new Size(img.width() / rate, img.height() / rate));
        Imgproc.resize(mosaic, img, img.size(), 0, 0, Imgproc.INTER_AREA);

        return img;
    }

    private Mat fitImg(Mat img, int width, int height){
        Mat result = new Mat();

        Mat imgGray = new Mat();
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);

        try{
            Mat thresholdOutput = new Mat();
            Imgproc.threshold(imgGray, thresholdOutput, 130, 255, Imgproc.THRESH_BINARY_INV);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(thresholdOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint maxContour = null;
            double maxContourArea = 0;
            for(MatOfPoint contour : contours){
                double area = Imgproc.contourArea(contour);
                if(area > maxContourArea){
                    maxContour = contour;
                    maxContourArea = area;
                }
            }

            double epsilon = 0.02 * Imgproc.arcLength(new MatOfPoint2f(maxContour.toArray()), true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(maxContour.toArray()), approx, epsilon, true);

            Point[] sortedPoints = sortPointsByYThenX(approx.toArray());
            Point bottomLeft = sortedPoints[0];
            Point topLeft = sortedPoints[1];
            Point bottomRight = sortedPoints[2];
            Point topRight = sortedPoints[3];

            MatOfPoint2f imgRect = new MatOfPoint2f(bottomLeft, topRight, topLeft, bottomRight);

            MatOfPoint2f targetRect = new MatOfPoint2f(
                    new Point(0,0),
                    new Point(width, height),
                    new Point(width, 0),
                    new Point(0, height)
            );

            Mat matrix = Imgproc.getPerspectiveTransform(imgRect, targetRect);

            Imgproc.warpPerspective(img, result, matrix, new Size(width, height));
        }catch(Exception e){
            Log.e("KSM", "FitImg ERROR!!", e);
        }

        return result;
    }

    private Point[] sortPointsByYThenX(Point[] points){
        Arrays.sort(points, (pt1, pt2) -> {
            if(pt1.y != pt2.y){
                return Double.compare(pt1.y, pt2.y);
            }else{
                return Double.compare(pt1.x, pt2.x);
            }
        });
        return points;
    }

    private Mat cropImg(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        resultMat_2 = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x-10;
            int y = boundingRect.y;
            int w = boundingRect.width+20;
            int h = boundingRect.height;

            Rect rectCrop = new Rect(x,y,w,h);
            Imgproc.rectangle(resultMat_2, rectCrop, new Scalar(255, 255, 255), 1);

            if(w>=width && h>=height){
//                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(img, rectCrop);
                Log.d("KSM", "CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
                croppedImg_x = x;
                croppedImg_y = y;
                croppedImg_w = w;
                croppedImg_h = h;

                Imgproc.rectangle(resultMat_2, rectCrop, new Scalar(255, 255, 0), 2);
                Log.d("KSM", "successed!");

//                long timeStampMillis = System.currentTimeMillis();
//                String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/CROP_"+timeStampMillis+"_"+i+".bmp";
//                boolean saveRes = Imgcodecs.imwrite(imgName, croppedImg);
//                if(saveRes){
//                    Log.d("KSM", "SAVE SUCCESSED!\n"+imgName);
//                }else{
//                    Log.d("KSM", "SAVE ERROR!!");
//                }
            }
        }

        return croppedImg;
    }

    private String[] finalCropImg(Mat img, List<MatOfPoint> contours, int width, int height){
        String[] croppedImgList = new String[0];
        Mat croppedImg = new Mat();
        resultMat_2 = Mat.zeros(final_drawing.size(), CvType.CV_8UC3);
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;

//            if(w>=width && h>=height && h < 150 && w < 150){
//                Rect rectCrop = new Rect(x,y,w,h);
//                croppedImg = new Mat(img, rectCrop);
//                Log.d("KSM", "CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
//                Imgproc.rectangle(final_drawing, rectCrop, new Scalar(255, 255, 0), 3);
//
//
//                String FILENAME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
//                SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA);
//                String timeStamp = sdf.format(System.currentTimeMillis());
//                String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/CROP_"+timeStamp+"_"+i+".bmp";
//
//                croppedImgList = Add(croppedImgList, imgName);
//                boolean saveRes = Imgcodecs.imwrite(imgName, croppedImg);
//                if(saveRes){
//                    Log.d("KSM", "SAVE SUCCESSED!\n"+imgName);
//                }else{
//                    Log.d("KSM", "SAVE ERROR!!");
//                }
//            }
        }
        return croppedImgList;
    }

    private static String[] Add(String[] originArray, String val){
        String[] newArray = Arrays.copyOf(originArray, originArray.length + 1);
        newArray[originArray.length] = val;
        return newArray;
    }
}
