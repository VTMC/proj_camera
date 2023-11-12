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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FindContours {
    private Mat src = new Mat();
    private Mat srcGray = new Mat();
    private Mat enhancedSrc = new Mat();
    private Mat croppedSrc = new Mat();
    private Mat drawing = new Mat();
    public String[] cropImgFileList;
    int croppedImg_x = 0;
    int croppedImg_y = 0;
    int croppedImg_w = 0;
    int croppedImg_h = 0;
    double movedAngle = 0;

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
            Log.d("KSM", "DDDDDD~~!!~!");
            return bmp;
        }

        //Crop src like croppedImg
        Rect roi = new Rect(croppedImg_x, croppedImg_y, croppedImg_w, croppedImg_h);
        croppedSrc = new Mat(src, roi);

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

    public Bitmap getSqr(){
        Bitmap bmp = null;

        Mat rotatedImg = rotateToVerticalityImg(croppedSrc);

        //rotatedImg make gray
        Mat rotatedImgGray = new Mat();
        Imgproc.cvtColor(rotatedImg, rotatedImgGray, Imgproc.COLOR_BGR2GRAY);

        //threshold it
        Mat thresholdOutput = new Mat();
        Imgproc.threshold(rotatedImgGray, thresholdOutput, 130, 255 , Imgproc.THRESH_BINARY);

        //contour it
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //crop to Only UrineStick
        Mat cropOnlyUrineStick = cropUrineStick(rotatedImg, contours, 70, 1400);

        //do croppedSrc rotate and crop
        Mat croppedUrineStripSrc = new Mat();
        Mat matrix = Imgproc.getRotationMatrix2D(new Point(croppedSrc.cols()/2, croppedSrc.rows()/2), movedAngle*(-1), 1.0);
        Imgproc.warpAffine(croppedSrc, croppedUrineStripSrc, matrix, new Size(croppedSrc.width(), croppedSrc.height()));
        Rect roi = new Rect(croppedImg_x, croppedImg_y, croppedImg_w, croppedImg_h);
        croppedUrineStripSrc = new Mat(croppedUrineStripSrc, roi);
        Imgproc.cvtColor(croppedUrineStripSrc, croppedUrineStripSrc, Imgproc.COLOR_BGR2RGB);

        Log.d("KSM", "cropOnlyUrineStick w : "+cropOnlyUrineStick.width()+", h : "+cropOnlyUrineStick.height());

        //crop UrineStick
        int w = cropOnlyUrineStick.width();
        int h = cropOnlyUrineStick.height();
        double sqr_h = 0.04*h;
        double fbh = 0.02 * h; //first blank height
        double bh = 0.021 * h; //blank height

        Log.d("KSM", "setting.... \nw : "+cropOnlyUrineStick.width()+"\nh : "+cropOnlyUrineStick.height()
                +"\nsqr_h : "+sqr_h+"\nfbh : "+fbh+"\nbh : "+bh);

        Rect[] sqrArray = new Rect[11];

        sqrArray[0] = new Rect(0,(int)fbh, w, (int)sqr_h);
        for(int i = 1; i < 11; i++){
            Rect sqr = new Rect(0, (int)(fbh+(sqr_h*i)+(bh*i)), w, (int)sqr_h);
            sqrArray[i] = sqr;
        }

        for(int i = 0; i < 11; i++){
            Log.d("KSM", "Rect ["+(i+1)+"] : "+sqrArray[i]);
        }

        //croppedImg = new Mat(img, rectCrop);
        Mat[] croppedSqr = new Mat[11];
        for(int i = 0; i < 11; i++){
            croppedSqr[i] = new Mat(croppedUrineStripSrc, sqrArray[i]);

            String FILENAME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
            SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA);
            String timeStamp = sdf.format(System.currentTimeMillis());
            String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/CROP_"+timeStamp+"_"+i+".bmp";
            boolean saveRes = Imgcodecs.imwrite(imgName, croppedSqr[i]);
            if(saveRes){
                Log.d("KSM", "SAVE SUCCESSED!\n"+imgName);
            }else{
                Log.d("KSM", "SAVE ERROR!!");
            }
        }

        try{
//            Imgproc.cvtColor(croppedSrc, croppedSrc, Imgproc.COLOR_BGR2RGB);
            bmp = Bitmap.createBitmap(croppedUrineStripSrc.cols(), croppedUrineStripSrc.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedUrineStripSrc, bmp);
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

    private Mat rotateToVerticalityImg(Mat img){
        Mat result = new Mat();

        Mat imgGray = new Mat();
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);

        try{
            Mat thresholdOutput = new Mat();
            Imgproc.threshold(imgGray, thresholdOutput, 130, 255, Imgproc.THRESH_BINARY);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(thresholdOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Mat drawing = Mat.zeros(thresholdOutput.size(), CvType.CV_8UC3);
            for(int i = 0; i < contours.size(); i++){
                Scalar color = new Scalar(255, 0, 0);
                Imgproc.drawContours(drawing, contours, i, color, 1, Imgproc.LINE_8, hierarchy, 2, new Point());
            }

            /* ref :
            https://www.charlezz.com/?p=45831
            https://docs.opencv.org/4.x/dd/d49/tutorial_py_contour_features.html
             */
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contours.size()-1).toArray());
            Mat line = new Mat();
            //주어진 점들에 대해 적합한 선분 정보 구하기
            Imgproc.fitLine(contour2f, line, Imgproc.DIST_L2, 0.0, 0.01, 0.01);

            //단위 벡터
            double vx = line.get(0,0)[0];
            double vy = line.get(1,0)[0];
            //직선위의 점
            double x = line.get(2,0)[0];
            double y = line.get(3,0)[0];

            //이미지 경계부근까지 연장한 후 노란색으로 그림
            double lefty = (-x * vy / vx) + y;
            double righty = ((thresholdOutput.cols() - x) * vy / vx) + y;

            //직사각형 가운데 선을 지나가는 점을 구하기
            Point point1 = new Point(thresholdOutput.cols() - 1, righty);
            Point point2 = new Point(0, lefty);

            Log.d("KSM", "point1 x : "+ point1.x + " / y : "+point1.y);
            Log.d("KSM", "point2 x : "+ point2.x + " / y : "+point2.y);

            Imgproc.circle(drawing, point1, 3, new Scalar(0, 255, 0), 3);
            Imgproc.circle(drawing, point2, 3, new Scalar(0, 255, 0), 3);

            //UrineStrip크기 만큼 자른 사각형의 너비 중앙값을 기준으로 한 선을 긋는다
            Point widthCenterPoint1 = new Point((thresholdOutput.cols()/2), 0);
            Point widthCenterPoint2 = new Point((thresholdOutput.cols()/2), thresholdOutput.rows());

            Imgproc.circle(drawing, widthCenterPoint1, 3, new Scalar(0, 255, 0), 3);
            Imgproc.circle(drawing, widthCenterPoint2, 3, new Scalar(0, 255, 0), 3);

            Point centerPoint = new Point((thresholdOutput.cols()/2), (thresholdOutput.rows()/2));
            Imgproc.circle(drawing, centerPoint, 1, new Scalar(0, 255, 0), 3);

            Log.d("KSM", "widthCenterPoint1 x : "+ widthCenterPoint1.x + " / y : "+widthCenterPoint1.y);
            Log.d("KSM", "widthCenterPoint2 x : "+ widthCenterPoint2.x + " / y : "+widthCenterPoint2.y);

            //각각의 위치에 대한 라인을 그려넣기.
            Imgproc.line(drawing, widthCenterPoint1, widthCenterPoint2, new Scalar(255, 255, 255));
            Imgproc.line(drawing, point1, point2, new Scalar(255, 255, 0));

            //각도 구하기 ref : https://cording-cossk3.tistory.com/32
            double pointAngle = getAngle(point1, point2);
            if(pointAngle > 180){
                pointAngle -= 180; //무조건 180도 안의 값을 가지도록 진행
            }
            double widthCenterAngle = getAngle(widthCenterPoint1, widthCenterPoint2);
            movedAngle = pointAngle - 90;

            Log.d("KSM", "pointAngle : "+pointAngle);
            Log.d("KSM", "widthCenterAngle : "+widthCenterAngle);
            Log.d("KSM", "pointAngle - 90˚ : "+movedAngle);



            //90도로 다시 맞추기 위해서는 이동된 방향과 반대방향으로 회전해야한다.
            Mat rotatedImg = new Mat();
            Mat matrix = Imgproc.getRotationMatrix2D(centerPoint, movedAngle*(-1), 1.0);
            Imgproc.warpAffine(img, rotatedImg, matrix, new Size(img.width(), img.height()));

            result = rotatedImg;

            //existing method
//            MatOfPoint maxContour = null;
//            double maxContourArea = 0;
//            for(MatOfPoint contour : contours){
//                double area = Imgproc.contourArea(contour);
//                if(area > maxContourArea){
//                    maxContour = contour;
//                    maxContourArea = area;
//                    Log.d("KSM", "maxContour : "+maxContour+"\nmaxContourArea : "+maxContourArea);
//                }
//            }
//
//            double epsilon = 0.01 * Imgproc.arcLength(new MatOfPoint2f(maxContour.toArray()), true);
//            MatOfPoint2f approx = new MatOfPoint2f();
//            Imgproc.approxPolyDP(new MatOfPoint2f(maxContour.toArray()), approx, epsilon, true);
//
//
//            Point[] sortedPoints = sortPointsByYThenX(approx.toArray());
//            for(Point point : sortedPoints){
//                Log.d("KSM", "Point - "+point);
//                Imgproc.circle(drawing, point, 2, new Scalar(255, 255, 0), 3);
//            }

//            Point bottomLeft = sortedPoints[0];
//            Point topLeft = sortedPoints[1];
//            Point bottomRight = sortedPoints[2];
//            Point topRight = sortedPoints[3];
//
//            MatOfPoint2f imgRect = new MatOfPoint2f(bottomLeft, topRight, topLeft, bottomRight);

//            MatOfPoint2f targetRect = new MatOfPoint2f(
//                    new Point(0,0),
//                    new Point(width, height),
//                    new Point(width, 0),
//                    new Point(0, height)
//            );

//            Mat matrix = Imgproc.getPerspectiveTransform(imgRect, targetRect);
//
//            Imgproc.warpPerspective(img, result, matrix, new Size(width, height));
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

    //reference : https://cording-cossk3.tistory.com/32
    private double getAngle(Point p1, Point p2){
        double deltaY = p1.y-p2.y;
        double deltaX = p2.x-p1.x;
        double result = Math.toDegrees(Math.atan2(deltaY, deltaX));

        return (result < 0) ? (360d + result) : result;
    }

    private Mat cropImg(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x-10;
            int y = boundingRect.y-10;
            int w = boundingRect.width+20;
            int h = boundingRect.height+20;

            Rect rectCrop = new Rect(x,y,w,h);

            if(w>=width && h>=height){
//                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(img, rectCrop);
                Log.d("KSM", "CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
                croppedImg_x = x;
                croppedImg_y = y;
                croppedImg_w = w;
                croppedImg_h = h;

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

    private Mat cropUrineStick(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = new Mat();
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;

            if(w>=width && h>=height){
                if(Math.abs(movedAngle) > 1.0){
                    x += 10;
                    y += 4;
                    w -= 15;
                    h -= 4;
                }

                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(img, rectCrop);
                Log.d("KSM", "TRACEURINE - CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
                croppedImg_x = x;
                croppedImg_y = y;
                croppedImg_w = w;
                croppedImg_h = h;

//                Imgproc.rectangle(drawing, rectCrop, new Scalar(255, 255, 0), 2);
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

    private static String[] Add(String[] originArray, String val){
        String[] newArray = Arrays.copyOf(originArray, originArray.length + 1);
        newArray[originArray.length] = val;
        return newArray;
    }
}
