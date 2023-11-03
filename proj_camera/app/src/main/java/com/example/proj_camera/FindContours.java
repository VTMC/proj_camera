package com.example.proj_camera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FindContours {
    private Mat src = new Mat();
    private Mat srcGray = new Mat();
    private Mat enhancedSrc = new Mat();
    private Mat drawing = new Mat();
    private Mat resultMat_2 = new Mat();
    private static final int MAX_THRESHOLD = 255;
    /*
    meanable number
    19~30 : on white paper
    */
    private int threshold = 100;
    private int threshold2 = (int) (threshold*1.2);
    private Random rng = new Random(12345);

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

        //existing method - Canny
        /*Mat cannyOutput = new Mat();
        Imgproc.Canny(enhancedSrc, cannyOutput, threshold, threshold2);

        List<MatOfPoint> contoursSimple = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contoursSimple, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);*/

        //new method - inRange
        /*int lowerInt = 180;
        int upperInt = 235;

        Scalar bgrLower = new Scalar(lowerInt,lowerInt,lowerInt);
        Scalar bgrUpper = new Scalar(upperInt,upperInt,upperInt);

        Mat mask = new Mat();
        Core.inRange(srcGray, bgrLower, bgrUpper, mask);
        List<MatOfPoint> contoursSimple = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contoursSimple, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);*/

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


        //existing method
//        drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);
//        for(int i = 0; i < contoursSimple.size(); i++){
//            Scalar color = new Scalar(255, 0, 0);
//            Imgproc.drawContours(drawing, contoursSimple, i, color, 5, Imgproc.LINE_8, hierarchy, 2, new Point());
//        }

        //new method - inRange
        /*drawing = Mat.zeros(mask.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple.size(); i++){
            Scalar color = new Scalar(255,0,0);
            Imgproc.drawContours(drawing, contoursSimple, i, color, 5, Imgproc.LINE_8, hierarchy, 2, new Point());
        }*/

        //new method - threshold
        drawing = Mat.zeros(thresholdOutput.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple.size(); i++){
            Scalar color = new Scalar(255, 0, 0);
            Imgproc.drawContours(drawing, contoursSimple, i, color, 5, Imgproc.LINE_8, hierarchy, 2, new Point());
        }

        Mat croppedImg = cropImg(src, contoursSimple, 70, 200);

//        Mat cannyOutput = new Mat();
//        Imgproc.Canny(croppedImg, cannyOutput, threshold, threshold2);

        Mat croppedImgGray = new Mat();
        Imgproc.cvtColor(croppedImg, croppedImgGray, Imgproc.COLOR_BGR2GRAY);

        Mat adaptThresOutput = new Mat();
        int blockSize = 15;
        int C = 3;
        Imgproc.adaptiveThreshold(croppedImgGray, adaptThresOutput, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, C);

//        List<MatOfPoint> contoursSimple2 = new ArrayList<>();
//        Mat hierarchy2 = new Mat();
//        Imgproc.findContours(thresholdOutput2, contoursSimple2, hierarchy2, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        Mat drawing2 = Mat.zeros(thresholdOutput2.size(), CvType.CV_8UC3);
//        for(int i = 0; i < contoursSimple2.size(); i++){
//            Scalar color = new Scalar(255, 0, 0);
//            Imgproc.drawContours(drawing2, contoursSimple2, i, color, 5, Imgproc.LINE_8, hierarchy2, 2, new Point());
//            Rect boundingRect = Imgproc.boundingRect(contoursSimple2.get(i));
//            if(boundingRect.width >= 70 && boundingRect.height >= 70){
//                Rect rectCrop = new Rect(boundingRect.x,boundingRect.y,boundingRect.width,boundingRect.height);
//                Imgproc.rectangle(drawing2, rectCrop, new Scalar(255, 255, 0), 2);
//            }
//        }

//        Mat croppedImg2 = cropImg(src, contoursSimple2, 50, 50);

        Mat result_mat = new Mat();
        Core.add(drawing, enhancedSrc, result_mat);

        try{
            //existing method
            /*bmp = Bitmap.createBitmap(result_mat.cols(), result_mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(result_mat, bmp);*/
            //watch cannyOutput
//            bmp = Bitmap.createBitmap(cannyOutput.cols(), cannyOutput.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(cannyOutput, bmp);
            //watch enhancedSrc
            bmp = Bitmap.createBitmap(adaptThresOutput.cols(), adaptThresOutput.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(adaptThresOutput, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }

        return bmp;
    }

    public Bitmap update2(){
        Bitmap bmp = null;

        Mat result_mat = new Mat();
        Core.add(resultMat_2, drawing, result_mat);

        try{
            bmp = Bitmap.createBitmap(result_mat.cols(), result_mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(result_mat, bmp);
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

    private Mat cropImg(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        resultMat_2 = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;
            Log.d("KSM", "CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);

            Rect rectCrop = new Rect(x,y,w,h);
            Imgproc.rectangle(resultMat_2, rectCrop, new Scalar(255, 255, 255), 1);

            if(w>=width && h>=height){
//                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(src, rectCrop);

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
}