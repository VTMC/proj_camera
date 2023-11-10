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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FindContours2 {
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

    public FindContours2(String path){
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

//        Object[] contoursSimpleArray = contoursSimple.toArray();
//        for(int i = 0; i < contoursSimpleArray.length; i++){
//            Log.d("KSM", "BIG Rectangle MatOfPoint contoursSimple["+i+"] : "+contoursSimpleArray[i]);
//        }


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

        Mat croppedImg = cropImg(src, contoursSimple, 70, 1400);


        if(croppedImg.width() > 500){
            return bmp;
        }

        //Crop src like croppedImg
        Rect roi = new Rect(croppedImg_x, croppedImg_y, croppedImg_w, croppedImg_h);
        croppedSrc = new Mat(src, roi);

//        //get Gray ContourRange
//        Mat contouredImg = new Mat();
//        contouredImg = contourRange(croppedImg);

//        Mat mosaicImg = mosaic(croppedImg, 2);

        //make Gray for adaptiveThreshold
        Mat croppedImgGray = new Mat();
        Imgproc.cvtColor(croppedImg, croppedImgGray, Imgproc.COLOR_BGR2GRAY);

//        Mat blurredImg = new Mat();
//        Imgproc.GaussianBlur(croppedImgGray, blurredImg, new Size(5,5), 0);
//
//        Mat sharpenKernel = new Mat(3, 3, CvType.CV_32F, new Scalar(-1));
//        sharpenKernel.put(1,1,9);
//
//        Mat sharpenImg = new Mat();
//        Imgproc.filter2D(blurredImg, sharpenImg, -1, sharpenKernel);
////
//        //threshold
////        Mat thresOutput =  Mat.zeros(croppedImgGray.size(), CvType.CV_8UC1);
////        Mat output = new Mat();
////        for(int i = 180; i < 220; i++){
////            Log.d("KSM", "Threshold doing...");
////            Imgproc.threshold(sharpenImg, output, i, 255, Imgproc.THRESH_BINARY_INV);
////            Core.add(thresOutput, output, thresOutput);
////        }
//        Mat thresOutput = new Mat();
//        Imgproc.threshold(sharpenImg, thresOutput, 180, 255, Imgproc.THRESH_BINARY);

        Mat adaptThresOutput = new Mat();
//        int blockSize = 3; //THRESH_BINARY Imgproc.ADAPTIVE_THRESH_MEAN_C
//        int C = 0; //THRESH_BINARY Imgproc.ADAPTIVE_THRESH_MEAN_C
        int blockSize = 11; //THRESH_BINARY Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C
        int C = 0; //THRESH_BINARY Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C
//        Imgproc.adaptiveThreshold(croppedImgGray, adaptThresOutput, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, blockSize, C);
        Imgproc.adaptiveThreshold(croppedImgGray, adaptThresOutput, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, C);

//        //Morphological closing
//        Mat closeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
//        Mat closedImg = new Mat();
//        Imgproc.morphologyEx(adaptThresOutput, closedImg, Imgproc.MORPH_CLOSE, closeKernel, new Point(-1, -1), 2);

        List<MatOfPoint> contoursSimple2 = new ArrayList<>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(adaptThresOutput, contoursSimple2, hierarchy2, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        final_drawing = Mat.zeros(adaptThresOutput.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple2.size(); i++){
            Scalar color = new Scalar(255, 0, 0);
            Imgproc.drawContours(final_drawing, contoursSimple2, i, color, 1, Imgproc.LINE_8, hierarchy2, 2, new Point());
        }

        cropImgFileList = finalCropImg(croppedSrc, contoursSimple2, 70, 70);

        long endTimeMillis = System.currentTimeMillis();
        long progressedTime = endTimeMillis - startTimeMillis;
        Log.d("KSM", "==========[progressedTime]==========\n" +
                              "progressedTime : "+progressedTime+"\n" +
                              "====================================\n");

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
            Imgproc.cvtColor(croppedImg, croppedImg, Imgproc.COLOR_BGR2RGB);
            bmp = Bitmap.createBitmap(adaptThresOutput.cols(), adaptThresOutput.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(adaptThresOutput, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }

        Log.d("KSM", "bmp width : "+bmp.getWidth()+" / height : "+bmp.getHeight());

        return bmp;
    }

    public Bitmap update2(){
        Bitmap bmp = null;

//        Mat result_mat = new Mat();
//        Core.add(resultMat_2, drawing, result_mat);

        try{
            bmp = Bitmap.createBitmap(final_drawing.cols(), final_drawing.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(final_drawing, bmp);
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

    private Mat contourRange(Mat img){
        Mat resMat = new Mat();
        Mat imgHSV = new Mat();
        img.copyTo(imgHSV);
        Imgproc.cvtColor(imgHSV, imgHSV, Imgproc.COLOR_BGR2HSV);

        //get subtract1 (white)
        Scalar minHSV = new Scalar(85, 18, 0); //100, 39, 220
        Scalar maxHSV = new Scalar(125, 53, 255); //110, 48, 255

        Log.d("KSM", "minHSV : "+minHSV);
        Log.d("KSM", "maxHSV : "+maxHSV);

        Mat imgMask = new Mat();
        Core.inRange(imgHSV, minHSV, maxHSV, imgMask);

        Mat subtractMat1 = new Mat();
        Core.bitwise_and(img, img, subtractMat1, imgMask);

        //get subtract2 (black)
        minHSV = new Scalar(95, 17, 0); //103, 27, 36
        maxHSV = new Scalar(150, 111, 100); //133, 101, 70
        Core.inRange(imgHSV, minHSV, maxHSV, imgMask);
        Mat subtractMat2 = new Mat();
        Core.bitwise_and(img, img, subtractMat2, imgMask);

        //two subtract add
        Mat subtractRes = new Mat();
        Core.add(subtractMat1, subtractMat2, subtractRes);

        //add some square
        minHSV = new Scalar(90, 3, 176);
        maxHSV = new Scalar(150, 25, 225);
        Core.inRange(imgHSV, minHSV, maxHSV, imgMask);
        Mat addSquare1 = new Mat();
        Core.bitwise_and(img, img, addSquare1, imgMask);

        //add some square2
//        minHSV = new Scalar(70, 25, 213);
//        maxHSV = new Scalar(96, 35, 255);
//        Core.inRange(imgHSV, minHSV, maxHSV, imgMask);
//        Mat addSquare2 = new Mat();
//        Core.bitwise_and(img, img, addSquare2, imgMask);

        //add all squares
//        Mat allSquares = new Mat();
//        Core.add(addSquare1, addSquare2, allSquares);

        //and subtract
        Core.subtract(img, subtractRes, resMat);
        Core.add(resMat, addSquare1, resMat);


//        for(int i = 0; i < 2; i++){
//            resMat = img_contrast(resMat);
//        }

        Imgproc.blur(resMat, resMat, new Size(6,6));

//        int rate = 3;
//        Mat mosaic = new Mat();
//        Imgproc.resize(resMat, mosaic, new Size(resMat.width() / rate, resMat.height() / rate));
//        Imgproc.resize(mosaic, resMat, resMat.size(), 0, 0, Imgproc.INTER_AREA);

        return resMat;
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

            if(w>=width && h>=height && h < 150 && w < 150){
                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(img, rectCrop);
                Log.d("KSM", "CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
                Imgproc.rectangle(final_drawing, rectCrop, new Scalar(255, 255, 0), 3);


                String FILENAME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
                SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA);
                String timeStamp = sdf.format(System.currentTimeMillis());
                String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/CROP_"+timeStamp+"_"+i+".bmp";

                croppedImgList = Add(croppedImgList, imgName);
                boolean saveRes = Imgcodecs.imwrite(imgName, croppedImg);
                if(saveRes){
                    Log.d("KSM", "SAVE SUCCESSED!\n"+imgName);
                }else{
                    Log.d("KSM", "SAVE ERROR!!");
                }
            }
        }
        return croppedImgList;
    }

    private static String[] Add(String[] originArray, String val){
        String[] newArray = Arrays.copyOf(originArray, originArray.length + 1);
        newArray[originArray.length] = val;
        return newArray;
    }
}
