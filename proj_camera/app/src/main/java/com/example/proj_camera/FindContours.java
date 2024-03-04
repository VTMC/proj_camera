package com.example.proj_camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.RotatedRect;
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

public class FindContours {
    private Mat src = new Mat();
    private Mat srcGray = new Mat();
    private Mat enhancedSrc = new Mat();
    private Mat croppedSrc = new Mat();
    private Mat drawing = new Mat();
    private Mat cropOnlyUrineStripDrawing = new Mat();
    private Mat rotateDrawing = new Mat();
    public String[] cropImgFileList = new String[11]; //each sqr image path
//    public boolean[] suitabilityList = new boolean[11]; //each sqr suitability
    public boolean[] suitabilityList = new boolean[11]; //each sqr suitability
    public int[][] cropImgRGBList = new int[11][3]; //each sqr RGB
    int[] selectedSqr = new int[11]; //adopted drawable
    int croppedImg_x = 0;
    int croppedImg_y = 0;
    int croppedImg_w = 0;
    int croppedImg_h = 0;
    int croppedSqr_x = 0;
    int croppedSqr_y = 0;
    int croppedSqr_w = 0;
    int croppedSqr_h = 0;

    Mat[] croppedSqr = new Mat[11];
    Mat[] croppedSqrDrawing = new Mat[11];

    int[] test2 = {R.drawable.test2_1, R.drawable.test2_2, R.drawable.test2_3, R.drawable.test2_4, R.drawable.test2_5, R.drawable.test2_6, R.drawable.test2_7};
    int[] test3 = {R.drawable.test3_1, R.drawable.test3_2, R.drawable.test3_3, R.drawable.test3_4};
    int[] test4 = {R.drawable.test4_1, R.drawable.test4_2, R.drawable.test4_3, R.drawable.test4_4, R.drawable.test4_5, R.drawable.test4_6};
    int[] test5 = {R.drawable.test5_1, R.drawable.test5_2, R.drawable.test5_3, R.drawable.test5_4, R.drawable.test5_5};
    int[] test6 = {R.drawable.test6_1, R.drawable.test6_2, R.drawable.test6_3, R.drawable.test6_4, R.drawable.test6_5, R.drawable.test6_6};
    int[] test7 = {R.drawable.test7_1, R.drawable.test7_2};
    int[] test8 = {R.drawable.test8_1, R.drawable.test8_2, R.drawable.test8_3, R.drawable.test8_4, R.drawable.test8_5};
    int[] test9 = {R.drawable.test9_1, R.drawable.test9_2, R.drawable.test9_3, R.drawable.test9_4, R.drawable.test9_5, R.drawable.test9_6};
    int[] test10 = {R.drawable.test10_1, R.drawable.test10_2, R.drawable.test10_3, R.drawable.test10_4, R.drawable.test10_5, R.drawable.test10_6, R.drawable.test10_7};
    int[] test11 = {R.drawable.test11_1, R.drawable.test11_2, R.drawable.test11_3, R.drawable.test11_4, R.drawable.test11_5};

    int houghLineThresholdValue = 35; //50
    int cannyThreshold1 = 10; //10
    int cannyThreshold2 = 50; //50

    public double sqr_h_return = 0.0;
    public double fbh_return = 0.0;
    public double bh_return = 0.0;

    //return resultImages1 (FindContours Constants ~ update())
    //src, enhancedSrc, srcGray, thresholdOutput, croppedImg, drawing, croppedSrc
    List<Bitmap> resultImages1 = new ArrayList<Bitmap>();

    //return resultImages2 (getSqr())


    //ddeteed
    private Mat final_drawing = new Mat();
    //eedbdde

    public FindContours(String path){
        Log.i("KSM", "== FindContours contstructor START! ==");
        var timeStart = System.currentTimeMillis();

        src = Imgcodecs.imread(path);
        if(src.empty()){
            Log.e("KSM", "Cannot Read image : "+path);
            System.exit(0);
        }

        //existing method - get contrast
        enhancedSrc = img_contrast(src);

        Imgproc.cvtColor(enhancedSrc, srcGray, Imgproc.COLOR_BGR2GRAY);

//        Imgproc.blur(srcGray, srcGray, new Size(3,3));
        var timeEnd = System.currentTimeMillis();
        var takeTime = timeEnd - timeStart;
        Log.i("KSM", "== FindContours contstructor END! ==");
        Log.i("KSM", "== IT Takes "+takeTime+"ms ==");
    }

    public List<Bitmap> update(){
        Log.i("KSM", "== FindContours.update() START! ==");
        var timeStart = System.currentTimeMillis();

        Bitmap bmp = null;

        //new method - threshold
        Mat thresholdOutput = new Mat();
        //if background color is black
        Imgproc.threshold(srcGray, thresholdOutput, 130, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contoursSimple = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdOutput, contoursSimple, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);


        //new method - threshold
        drawing = Mat.zeros(thresholdOutput.size(), CvType.CV_8UC3);
//        for(int i = 0; i < contoursSimple.size(); i++){
//            Scalar color = new Scalar(255, 0, 0);
//            Imgproc.drawContours(drawing, contoursSimple, i, color, 5, Imgproc.LINE_8, hierarchy, 2, new Point());
//        }
        Core.add(drawing, src, drawing);

        Mat croppedImg = cropImg(src, contoursSimple, 70, 1200);

        if(croppedImg.width() > 500){
            Log.d("KSM", "DDDDDD~~!!~!");
            resultImages1.add(bmp);
            return resultImages1;
        }

        //Crop src like croppedImg
        Rect roi = new Rect(croppedImg_x, croppedImg_y, croppedImg_w, croppedImg_h);
        croppedSrc = new Mat(src, roi);

        try{
//            Imgproc.cvtColor(drawing, drawing, Imgproc.COLOR_BGR2RGB);
            //FindContours constants
            bmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, bmp);
            resultImages1.add(bmp);
            bmp = Bitmap.createBitmap(enhancedSrc.cols(), enhancedSrc.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(enhancedSrc, bmp);
            resultImages1.add(bmp);
            bmp = Bitmap.createBitmap(srcGray.cols(), srcGray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(srcGray, bmp);
            resultImages1.add(bmp);

            //FindContours updates()
            bmp = Bitmap.createBitmap(thresholdOutput.cols(), thresholdOutput.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(thresholdOutput, bmp);
            resultImages1.add(bmp);
//            bmp = Bitmap.createBitmap(croppedImg.cols(), croppedImg.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(croppedImg, bmp);
//            resultImages1.add(bmp);
            bmp = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(drawing, bmp);
            resultImages1.add(bmp);
            bmp = Bitmap.createBitmap(croppedSrc.cols(), croppedSrc.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedSrc, bmp);
            resultImages1.add(bmp);

        }catch(CvException e){
            Log.e("KSM", "OPENCV - update : Mat to bitmap Error!!", e);
        }

        Log.d("KSM", "bmp width : "+bmp.getWidth()+" / height : "+bmp.getHeight());

        var timeEnd = System.currentTimeMillis();
        var takeTime = timeEnd - timeStart;
        Log.i("KSM", "== FindContours.update() END! ==");
        Log.i("KSM", "== IT Takes "+takeTime+"ms ==");

        return resultImages1;
    }

    public Bitmap getSqr() {
        Log.i("KSM", "== FindContours.getSqr() START! ==");
        var timeStart = System.currentTimeMillis();

        Bitmap bmp = null;

        if(croppedSrc.width() == 0 || croppedSrc.height() == 0){
            Log.e("KSM", "getSqr CroppedSrc Error!!!");

            var timeEnd = System.currentTimeMillis();
            var takeTime = timeEnd - timeStart;
            Log.i("KSM", "== FindContours.getSqr() END! ==");
            Log.i("KSM", "== IT Takes "+takeTime+"ms ==");

            return bmp;
        }

        Mat cropOnlyUrineStrip = fitImg(croppedSrc, 80, (int)(80*(24.6)));
        Log.d("KSM", "rotatedImg : " +
                "\nw : "+cropOnlyUrineStrip.width()+"/ h : "+cropOnlyUrineStrip.height());

        //getSqr existing method
        //crop UrineStrip
        int w = cropOnlyUrineStrip.width(); //x = 10
        int h = cropOnlyUrineStrip.height();
        double sqr_h = (4.0 / 123.0) * h; //origin : almost 0.04
//        double fbh = (2.1 / 123.0) * h; //first blank height | origin : almost 0.02
//        double bh = (3.5 / 123.0) * h; //blank height | origin : almost 0.021

        //fbh 2.0~3.0 / bh 3.0~4.0 반복
        Rect[] sqrArray = new Rect[11];
        boolean suitability = true;
        boolean allSuitability = false;

        for(double i = 2.0; i < 3.1; i+=0.1) {
            for (double j = 3.0; j < 4.1; j+=0.1) {
                double fbh = (i / 123.0) * h; //first blank height | origin : almost 0.02
                double bh = (j / 123.0) * h; //blank height | origin : almost 0.021

                Log.d("KSM", "setting.... \nw : " + cropOnlyUrineStrip.width() + "\nh : " + cropOnlyUrineStrip.height()
                        + "\nsqr_h : " + sqr_h + "\nfbh : " + fbh + "("+i+")\nbh : " + bh+"("+j+")");

                sqrArray[0] = new Rect(0, (int) fbh, w, (int) sqr_h); //0
                croppedSqr[0] = new Mat(cropOnlyUrineStrip, sqrArray[0]);
                croppedSqrDrawing[0] = croppedSqr[0].clone();

                sqr_h_return = 4.0;
                fbh_return = i;
                bh_return = j;

                for (int k = 1; k < 11; k++) { //1~10
                    int y = (int) (fbh + (sqr_h * k) + (bh * k));

                    Rect sqr = new Rect(0, y, w, (int) sqr_h);
                    sqrArray[k] = sqr;

                    Log.d("KSM", "Rect [" + k + "] : " + sqrArray[k]);

                    croppedSqr[k] = new Mat(cropOnlyUrineStrip, sqrArray[k]);
                    croppedSqrDrawing[k] = croppedSqr[k].clone();
                }

                for (int k = 0; k < croppedSqr.length; k++) {
                    //sharpen
//                    Mat kernel = new Mat(3, 3, CvType.CV_32F);
//                    kernel.put(0,0,0,-1,0,-1,5,-1,0,-1,0);
//                    Mat sharpenCroppedSqr = new Mat();
//                    Imgproc.filter2D(croppedSqr[k], sharpenCroppedSqr, -1, kernel);
//                    croppedSqrDrawing[k] = sharpenCroppedSqr;

                    Mat blurredSqr = new Mat();
                    Imgproc.blur(croppedSqr[k], blurredSqr, new Size(3,3));

                    Mat croppedSqrGray = new Mat();
                    Imgproc.cvtColor(blurredSqr, croppedSqrGray, Imgproc.COLOR_BGR2GRAY);

                    Mat cannyOutput = new Mat();
                    Imgproc.Canny(croppedSqrGray, cannyOutput, cannyThreshold1, cannyThreshold2);

                    if(!cannyOutput.empty()){
                        croppedSqrDrawing[k] = cannyOutput;
                    }

                    Mat lines = new Mat();
                    Imgproc.HoughLines(cannyOutput, lines, 1.0, Math.PI / 180, houghLineThresholdValue);

                    Log.d("KSM", "Square ["+k+"]");
                    Log.i("KSM", "lines.rows() : "+lines.rows());
                    for (int x = 0; x < lines.rows(); x++) {
                        double rho = lines.get(x, 0)[0];
                        double theta = lines.get(x, 0)[1];
                        double angle = theta * (180 / Math.PI);
                        double a = Math.cos(theta);
                        double b = Math.sin(theta);
                        double x0 = a * rho;
                        double y0 = b * rho;
                        Point pt1 = new Point(Math.round(x0 + croppedSqr[k].rows() * (-b)), Math.round(y0 + croppedSqr[k].rows() * (a)));
                        Point pt2 = new Point(Math.round(x0 - croppedSqr[k].cols() * (-b)), Math.round(y0 - croppedSqr[k].cols() * (a)));
                        Log.d("KSM", "Angle : "+angle);
                        Log.d("KSM", "Point 1 : "+pt1+", 2 : "+pt2);
                        Imgproc.line(croppedSqrDrawing[k], pt1, pt2, new Scalar(255, 0, 0), 2);

                        //USUALLY CASE
                        if (angle > 80 && angle < 100) {
                            if ((pt1.y > 5 && pt1.y < croppedSqr[k].height() - 5) || (pt2.y > 5 && pt2.y < croppedSqr[k].height() - 5)) {
                                suitability = false;
                            }
                        }

                        //BAD CASE
                        /*if(k == croppedSqr.length - 1){
                            suitability = false;
                        }else{
                            suitability = true;
                        }*/
                    }

                    Log.d("KSM", "suitabilityList[" + k + "] = " + suitability);

                    if (suitability == false) {
                        Log.i("KSM", "-- suitabilityList have false BREAK!!!!");
                        suitabilityList[k] = suitability;

                        suitability = true;
                        break;
                    }else{
                        suitabilityList[k] = suitability;
                    }

                    if(k == (croppedSqr.length - 1)){
                        for (int l = 0; l < suitabilityList.length; l++) {
                            if (suitabilityList[l] == false) {
                                allSuitability = false;
                                break;
                            } else {
                                allSuitability = true;
                            }
                        }
                    }

                    if (allSuitability == true) {
                        Log.i("KSM", "-- suitabilityList all true cropSqr BREAK!!!!");
                        break;
                    }
                }

                if (allSuitability == true) {
                    Log.i("KSM", "-- suitabilityList all true fbh BREAK!!!!");
                    break;
                }
            }

            if (allSuitability == true) {
                Log.i("KSM", "-- suitabilityList all true fbh BREAK!!!!");
                break;
            }
        }

        drawing = Mat.zeros(cropOnlyUrineStrip.size(), CvType.CV_8UC3);
        Core.add(drawing, cropOnlyUrineStrip, drawing);

        //save cropped Img
        for (int i = 0; i < 11; i++) {
            Imgproc.rectangle(drawing, sqrArray[i], new Scalar(255, 0, 0), 1);

            String FILENAME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
            SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA);
            String timeStamp = sdf.format(System.currentTimeMillis());
            String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/CROP_"+timeStamp+"_"+i+".bmp";
            cropImgFileList[i] = imgName;
            boolean saveRes = Imgcodecs.imwrite(imgName, croppedSqr[i]);
            if(saveRes){
                Log.d("KSM", "SAVE SUCCESSED!\n"+imgName);
            }else{
                Log.d("KSM", "SAVE ERROR!!");
            }
        }

        Mat cropOnlyUrinStripGray = new Mat();
        Imgproc.cvtColor(cropOnlyUrineStrip, cropOnlyUrinStripGray, Imgproc.COLOR_BGR2GRAY);

        String FILENAME_FORMAT = "yyyy-MM-dd_HH_mm_ss";
        SimpleDateFormat sdf = new SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA);
        String timeStamp = sdf.format(System.currentTimeMillis());
//        String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/DRAWING_"+timeStamp+".bmp";
        String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/URINESTRIP_" + timeStamp + ".bmp";
//        boolean saveRes = Imgcodecs.imwrite(imgName, drawing);
        boolean saveRes = Imgcodecs.imwrite(imgName, cropOnlyUrinStripGray);
        if (saveRes) {
            Log.d("KSM", "SAVE SUCCESSED!\n" + imgName);
        } else {
            Log.d("KSM", "SAVE ERROR!!");
        }

        try{
            Imgproc.cvtColor(drawing, drawing, Imgproc.COLOR_BGR2RGB);
//            bmp = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(drawing, bmp);
            bmp = Bitmap.createBitmap(drawing.cols(), drawing.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(drawing, bmp);
        }catch(CvException e){
            Log.e("KSM", "Mat to bitmap Error!!", e);
        }

        var timeEnd = System.currentTimeMillis();
        var takeTime = timeEnd - timeStart;
        Log.i("KSM", "== FindContours.getSqr() END! ==");
        Log.i("KSM", "== IT Takes "+takeTime+"ms ==");
        return bmp;
    }

    public Bitmap[] checkCropImg(){
        Log.i("KSM", "== FindContours.ckeckCropImg() START! ==");
        var timeStart = System.currentTimeMillis();

        Bitmap[] result = new Bitmap[cropImgFileList.length];
//        boolean suitability = true;

        for(int i = 0; i < croppedSqr.length; i++){
//            Mat croppedSqr = Imgcodecs.imread(cropImgFileList[i]);
//            if(croppedSqr.empty()){
//                Log.e("KSM", "Cannot Read image : "+cropImgFileList[i]);
//                System.exit(0);
//            }

//            Mat kernel = new Mat(3, 3, CvType.CV_32F);
//            kernel.put(0,0,0,-1,0,-1,5,-1,0,-1,0);
//            Mat sharpenCroppedSqr = new Mat();
//            Imgproc.filter2D(croppedSqr, sharpenCroppedSqr, -1, kernel);

//            Mat croppedSqrDrawing = new Mat();
//            croppedSqr.copyTo(croppedSqrDrawing);
//
//            Mat croppedSqrGray = new Mat();
//            Imgproc.cvtColor(croppedSqr, croppedSqrGray, Imgproc.COLOR_BGR2GRAY);
//
//            Mat cannyOutput = new Mat();
//            Imgproc.Canny(croppedSqrGray, cannyOutput, cannyThreshold1, cannyThreshold2);
//
//            Mat lines = new Mat();
//            Imgproc.HoughLines(cannyOutput, lines, 1.0, Math.PI / 180, houghLineThresholdValue);

//            Log.d("KSM", "Square ["+i+"]");
//            for (int x = 0; x < lines.rows(); x++) {
//                double rho = lines.get(x, 0)[0];
//                double theta = lines.get(x, 0)[1];
//                double angle = theta * (180/Math.PI);
//                double a = Math.cos(theta);
//                double b = Math.sin(theta);
//                double x0 = a * rho;
//                double y0 = b * rho;
//                Point pt1 = new Point(Math.round(x0 + croppedSqr.rows() * (-b)), Math.round(y0 + croppedSqr.rows() * (a)));
//                Point pt2 = new Point(Math.round(x0 - croppedSqr.cols() * (-b)), Math.round(y0 - croppedSqr.cols() * (a)));
////                Log.d("KSM", "Angle : "+angle);
//                Log.d("KSM", "Point 1 : "+pt1+", 2 : "+pt2);
//                Imgproc.line(croppedSqrDrawing, pt1, pt2, new Scalar(255, 0, 0), 2);

//                if(angle > 80 && angle < 100){
//                    if((pt1.y > 5 && pt1.y < croppedSqr.height()-5) && (pt2.y > 5 && pt2.y < croppedSqr.height()-5)){
//                        suitability = false;
//                    }
//                }
//            }
//
//            suitabilityList[i] = suitability;
//            suitability = true;

            //getRGB value from croppedSqr
            Imgproc.cvtColor(croppedSqr[i], croppedSqr[i], Imgproc.COLOR_BGR2RGB);
            double totalRed = 0.0;
            double totalGreen = 0.0;
            double totalBlue = 0.0;
            int totalPxCn = croppedSqr[i].width() * croppedSqr[i].height();
            for(int k = 0; k < croppedSqr[i].width(); k++){
                for(int l = 0; l < croppedSqr[i].height(); l++){
                    double[] rgb = croppedSqr[i].get(l, k);
                    double r = rgb[0];
                    double g = rgb[1];
                    double b = rgb[2];

                    totalRed += r;
                    totalGreen += g;
                    totalBlue += b;
                }
            }
            cropImgRGBList[i][0] = (int)(totalRed/totalPxCn); //avgRed
            cropImgRGBList[i][1] = (int)(totalGreen/totalPxCn); //avgGreen
            cropImgRGBList[i][2] = (int)(totalBlue/totalPxCn); //avgBlue

            Log.i("KSM", "croppedSqrDrawaing.length ["+i+"] = "+croppedSqrDrawing.length);
            try{
                Imgproc.cvtColor(croppedSqrDrawing[i], croppedSqrDrawing[i], Imgproc.COLOR_BGR2RGB);
                Bitmap bmp = Bitmap.createBitmap(croppedSqrDrawing[i].cols(), croppedSqrDrawing[i].rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(croppedSqrDrawing[i], bmp);

                result[i] = bmp;
            }catch(CvException e){
                Log.e("KSM", "Mat to bitmap Error!!", e);
            }
        }

        for(int i=0; i < cropImgRGBList.length; i++){
            Log.d("KSM", "CheckCropImg - Sqr["+i+"] AVG R : "+cropImgRGBList[i][0]+" / G : "+cropImgRGBList[i][1]+" / B : "+cropImgRGBList[i][2]);
        }

        var timeEnd = System.currentTimeMillis();
        var takeTime = timeEnd - timeStart;
        Log.i("KSM", "== FindContours.checkCropImg() END! ==");
        Log.i("KSM", "== IT Takes "+takeTime+"ms ==");

        return result;
    }

    public boolean[] getSuitabilityList(){
        return suitabilityList;
    }

    public String[] getCropImgFileList(){
        return cropImgFileList;
    }

    public int[][] getCropImgRGBList(){
        return cropImgRGBList;
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

    //mosaic
    private Mat mosaic(Mat img, int rate){
        Mat mosaic = new Mat();
        Imgproc.resize(img, mosaic, new Size(img.width() / rate, img.height() / rate));
        Imgproc.resize(mosaic, img, img.size(), 0, 0, Imgproc.INTER_AREA);

        return img;
    }

    //rotateToVerticalityImg() - existing Method
//    private Mat rotateToVerticalityImg(Mat img){
//        Mat result = new Mat();
//
//        Mat imgGray = new Mat();
//        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);
//
//        try{
//            Mat thresholdOutput = new Mat();
//            Imgproc.threshold(imgGray, thresholdOutput, 130, 255, Imgproc.THRESH_BINARY);
//
//            List<MatOfPoint> contours = new ArrayList<>();
//            Mat hierarchy = new Mat();
//            Imgproc.findContours(thresholdOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//            rotateDrawing = Mat.zeros(thresholdOutput.size(), CvType.CV_8UC3);
//            for(int i = 0; i < contours.size(); i++){
//                Scalar color = new Scalar(255, 0, 0);
//                Imgproc.drawContours(rotateDrawing, contours, i, color, 1, Imgproc.LINE_8, hierarchy, 3, new Point());
//            }
//
//            //RotatedRect Part (Not Use)
//            /*MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contours.size()-1).toArray());
//            RotatedRect rotatedRect = Imgproc.minAreaRect(contour2f);
//
//            Point[] boxPoints = new Point[4];
//            rotatedRect.points(boxPoints);
//
//            MatOfPoint boxContour = new MatOfPoint(boxPoints);
//            Imgproc.drawContours(rotateDrawing, Arrays.asList(boxContour), 0, new Scalar(255, 255, 0), 2);*/
//
//            /* ref :
//            https://www.charlezz.com/?p=45831
//            https://docs.opencv.org/4.x/dd/d49/tutorial_py_contour_features.html
//             */
//            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(contours.size()-1).toArray());
//            Mat line = new Mat();
//            //주어진 점들에 대해 적합한 선분 정보 구하기
//            Imgproc.fitLine(contour2f, line, Imgproc.DIST_L2, 0.0, 0.01, 0.01);
//
//            //단위 벡터
//            double vx = line.get(0,0)[0];
//            double vy = line.get(1,0)[0];
//            //직선위의 점
//            double x = line.get(2,0)[0];
//            double y = line.get(3,0)[0];
//
//            //이미지 경계부근까지 연장한 후 노란색으로 그림
//            //vy/vx = 기울기, x,y는 Contour된 UrineStrip의 중심점
//            double lefty = Math.round((-x * vy / vx + y));
//            double righty = Math.round(((thresholdOutput.cols() - x) * vy / vx) + y);
//
//            //직사각형 가운데 선을 지나가는 점을 구하기
//            Point point1 = new Point((double)(thresholdOutput.cols() - 1), righty);
//            Point point2 = new Point(0, lefty);
//            Point contourCenterPoint = new Point(x,y);
//
//            Log.d("KSM", "point1 x : "+ point1.x + " / y : "+point1.y);
//            Log.d("KSM", "point2 x : "+ point2.x + " / y : "+point2.y);
//            Log.d("KSM", "contour center Point x : "+contourCenterPoint.x+
//                    "/ y : "+contourCenterPoint.y);
//            Log.d("KSM", "contour line slope grade : "+(vy / vx));
//
//            Imgproc.circle(rotateDrawing, point1, 3, new Scalar(0, 255, 0), 3);
//            Imgproc.circle(rotateDrawing, point2, 3, new Scalar(0, 255, 0), 3);
//            Imgproc.circle(rotateDrawing, contourCenterPoint, 3, new Scalar(0, 255, 0), 3);
//
//            //UrineStrip크기 만큼 자른 사각형의 너비 중앙값을 기준으로 한 선을 긋는다
//            Point widthCenterPoint1 = new Point((thresholdOutput.cols()/2), 0);
//            Point widthCenterPoint2 = new Point((thresholdOutput.cols()/2), thresholdOutput.rows());
//
//            Imgproc.circle(rotateDrawing, widthCenterPoint1, 3, new Scalar(0, 255, 0), 3);
//            Imgproc.circle(rotateDrawing, widthCenterPoint2, 3, new Scalar(0, 255, 0), 3);
//
//            Point centerPoint = new Point((thresholdOutput.cols()/2), (thresholdOutput.rows()/2));
//            Imgproc.circle(rotateDrawing, centerPoint, 1, new Scalar(0, 255, 0), 3);
//
//            Log.d("KSM", "widthCenterPoint1 x : "+ widthCenterPoint1.x + " / y : "+widthCenterPoint1.y);
//            Log.d("KSM", "widthCenterPoint2 x : "+ widthCenterPoint2.x + " / y : "+widthCenterPoint2.y);
//
//            //각각의 위치에 대한 라인을 그려넣기.
//            Imgproc.line(rotateDrawing, widthCenterPoint1, widthCenterPoint2, new Scalar(255, 255, 255));
//            Imgproc.line(rotateDrawing, point1, point2, new Scalar(255, 255, 0));
//
//            //각도 구하기 ref : https://cording-cossk3.tistory.com/32
//            double pointAngle = getAngle(point1, point2);
//            if(pointAngle > 180){
//                pointAngle -= 180; //무조건 180도 안의 값을 가지도록 진행
//            }
//            double widthCenterAngle = getAngle(widthCenterPoint1, widthCenterPoint2);
//            if(widthCenterAngle > 180){
//                widthCenterAngle -= 180;
//            }
//            movedAngle = pointAngle - widthCenterAngle;
//
//            //rotatedRect Part (Not Use)
//            /*double pointAngle = rotatedRect.angle;
//            Point centerPoint = new Point(img.width()/2, img.height()/2);*/
//
//            Log.d("KSM", "pointAngle : "+pointAngle);
//            Log.d("KSM", "widthCenterAngle : "+widthCenterAngle);
//            Log.d("KSM", "pointAngle - 90˚ : "+movedAngle);
//            Log.d("KSM", "rotate angle˚ : "+movedAngle*(-1.0));
//
//
//            Mat rotatedImg = new Mat();
//            //90도로 다시 맞추기 위해서는 이동된 방향과 반대방향으로 회전해야한다.
//
//            //RotatedRect Part (Not Use)
//            /*Mat matrix = Imgproc.getRotationMatrix2D(centerPoint, pointAngle, 1.0);*/
//            Mat matrix = Imgproc.getRotationMatrix2D(contourCenterPoint, movedAngle * (-1.0), 1.0);
//            Imgproc.warpAffine(img, rotatedImg, matrix, new Size(img.width(), img.height()));
//
////            if(Math.abs(movedAngle) > 1){
////                Mat matrix = Imgproc.getRotationMatrix2D(contourCenterPoint, movedAngle * (-1.0), 1.0);
////                Imgproc.warpAffine(img, rotatedImg, matrix, new Size(img.width(), img.height()));
////                Log.d("KSM", "ROTATED!!!");
////            }else{
////                noRotate = true;
////                rotatedImg = img;
////                Log.d("KSM", "NOT ROTATED!!!");
////            }
//
//            result = rotatedImg;
//
//            //existing method
////            MatOfPoint maxContour = null;
////            double maxContourArea = 0;
////            for(MatOfPoint contour : contours){
////                double area = Imgproc.contourArea(contour);
////                if(area > maxContourArea){
////                    maxContour = contour;
////                    maxContourArea = area;
////                    Log.d("KSM", "maxContour : "+maxContour+"\nmaxContourArea : "+maxContourArea);
////                }
////            }
////
////            double epsilon = 0.01 * Imgproc.arcLength(new MatOfPoint2f(maxContour.toArray()), true);
////            MatOfPoint2f approx = new MatOfPoint2f();
////            Imgproc.approxPolyDP(new MatOfPoint2f(maxContour.toArray()), approx, epsilon, true);
////
////
////            Point[] sortedPoints = sortPointsByYThenX(approx.toArray());
////            for(Point point : sortedPoints){
////                Log.d("KSM", "Point - "+point);
////                Imgproc.circle(drawing, point, 2, new Scalar(255, 255, 0), 3);
////            }
//
////            Point bottomLeft = sortedPoints[0];
////            Point topLeft = sortedPoints[1];
////            Point bottomRight = sortedPoints[2];
////            Point topRight = sortedPoints[3];
////
////            MatOfPoint2f imgRect = new MatOfPoint2f(bottomLeft, topRight, topLeft, bottomRight);
//
////            MatOfPoint2f targetRect = new MatOfPoint2f(
////                    new Point(0,0),
////                    new Point(width, height),
////                    new Point(width, 0),
////                    new Point(0, height)
////            );
//
////            Mat matrix = Imgproc.getPerspectiveTransform(imgRect, targetRect);
////
////            Imgproc.warpPerspective(img, result, matrix, new Size(width, height));
//        }catch(Exception e){
//            Log.e("KSM", "FitImg ERROR!!", e);
//        }
//
//        return result;
//    }

//    private Point[] sortPointsByYThenX(Point[] points){
//        Arrays.sort(points, (pt1, pt2) -> {
//            if(pt1.y != pt2.y){
//                return Double.compare(pt1.y, pt2.y);
//            }else{
//                return Double.compare(pt1.x, pt2.x);
//            }
//        });
//        return points;
//    }

    //reference : https://cording-cossk3.tistory.com/32
//    private double getAngle(Point p1, Point p2){
//        double deltaY = p1.y-p2.y;
//        double deltaX = p2.x-p1.x;
//        double result = Math.toDegrees(Math.atan2(deltaY, deltaX));
//
//        Log.d("KSM", "getAngle - Degrees : "+result);
//
//        return (result < 0) ? (360d + result) : result;
//    }


    private Mat fitImg(Mat img, int width, int height){
        Mat resMat = new Mat();

        Mat imgGray = new Mat();
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);

        Mat thresholdImg = new Mat();
        Imgproc.threshold(imgGray, thresholdImg, 130, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

//        Mat sourceContourImg =  Mat.zeros(thresholdImg.size(), CvType.CV_8UC3);
//        for(int i = 0; i < contours.size(); i++){
//            Imgproc.drawContours(sourceContourImg, contours, i, new Scalar(255, 255, 0), 1, Imgproc.LINE_8, hierarchy, 2, new Point());
//        }

        MatOfPoint sourceContour = findMaxContour(contours);
        Point[] sourceRect = findRect(sourceContour);
        Point[] targetRect = {
                new Point(0, height),
                new Point(width, 0),
                new Point(0, 0),
                new Point(width, height)
        };

        MatOfPoint2f sourceMat = new MatOfPoint2f(sourceRect);
        MatOfPoint2f targetMat = new MatOfPoint2f(targetRect);

        RotatedRect minAreaRect = Imgproc.minAreaRect(sourceMat);
        double urineStripAngle = minAreaRect.angle;
        if(urineStripAngle > 45){
            urineStripAngle = 90 - urineStripAngle;
            Log.d("KSM", "fitImg() - UrineStrip Angle : -"+urineStripAngle);
        }else{
            Log.d("KSM", "fitImg() - UrineStrip Angle : "+urineStripAngle);
        }


        Mat matrix = Imgproc.getPerspectiveTransform(sourceMat, targetMat);
        Imgproc.warpPerspective(img, resMat, matrix, new Size(width, height));

        return resMat;
    }

    private MatOfPoint findMaxContour(List<MatOfPoint> contours){
        MatOfPoint maxContour = null;

        double maxArea = 0;

        for(MatOfPoint contour : contours){
            double area = Imgproc.contourArea(contour);

            if(maxArea < area){
                maxContour = contour;
                maxArea = area;
            }
        }

        return maxContour;
    }

    private Point[] findRect(MatOfPoint contour){
        Point[] contourPoints = contour.toArray();
        Point[] rotatedContour = rotateContour(contour, 45);

        int index1 = 0; //x최솟값
        int index2 = 0; //x최댓값
        int index3 = 0; //y최솟값
        int index4 = 0; //y최댓값

        for(int i = 1; i < rotatedContour.length; i++){
            if(rotatedContour[i].x < rotatedContour[index1].x)
                index1 = i;
            if(rotatedContour[i].x > rotatedContour[index2].x)
                index2 = i;
            if(rotatedContour[i].y < rotatedContour[index3].y)
                index3 = i;
            if(rotatedContour[i].y > rotatedContour[index4].y)
                index4 = i;
        }

        Point[] resPoints = {contourPoints[index1], contourPoints[index2],
                contourPoints[index3], contourPoints[index4]};
        return resPoints;
    }

    private Point[] rotateContour(MatOfPoint contour, int angle){
        Point[] contourPoints = contour.toArray();
        Point[] rotatedPoints = new Point[contourPoints.length];

        Rect boundcontourRect = Imgproc.boundingRect(contour);

        double centerX = (boundcontourRect.x+ boundcontourRect.width)/2;
        double centerY = (boundcontourRect.y+ boundcontourRect.height)/2;

        double radian = angle * Math.PI / 180;

        for(int i = 0; i < contourPoints.length; i++){
            int rotatedX = (int)((contourPoints[i].x - centerX) * Math.cos(radian) - (contourPoints[i].y - centerY) * Math.sin(radian));
            int rotatedY = (int)((contourPoints[i].x - centerX) * Math.sin(radian) + (contourPoints[i].y - centerY) * Math.cos(radian));

            rotatedPoints[i] = new Point(rotatedX, rotatedY);
        }

        return rotatedPoints;
    }

    private Mat cropImg(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = new Mat();
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
//            Log.d("KSM", "contour : "+contours.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;
            Rect rectCrop = new Rect(x,y,w,h);

            if(w>=width && h>=height && w<img.width() && h<img.height()){
                Imgproc.rectangle(drawing, rectCrop, new Scalar(255, 255, 0));

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
            }else{
                Imgproc.rectangle(drawing, rectCrop, new Scalar(255, 0, 0));
            }
        }

        return croppedImg;
    }

    private Mat cropUrineStrip(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = new Mat();
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;

            if(w>=width && h>=height){
                x+=10;
                w-=20;

                cropOnlyUrineStripDrawing = Mat.zeros(img.size(), CvType.CV_8UC3);
                Core.add(img, cropOnlyUrineStripDrawing, cropOnlyUrineStripDrawing);

                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(img, rectCrop);
                Log.d("KSM", "TRACEURINE - CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
                croppedImg_x = x;
                croppedImg_y = y;
                croppedImg_w = w;
                croppedImg_h = h;

                Imgproc.rectangle(cropOnlyUrineStripDrawing, rectCrop, new Scalar(255, 0, 0), 2);
                Log.d("KSM", "successed!");
            }
        }

        return croppedImg;
    }

    private Mat cropSqr(Mat img, List<MatOfPoint> contours, int width, int height){
        Mat croppedImg = new Mat();
        for(int i = 0; i < contours.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;

            if(w>=width && h>=height && height < 100){
//                cropOnlyUrineStripDrawing = Mat.zeros(img.size(), CvType.CV_8UC3);
//                Core.add(img, cropOnlyUrineStripDrawing, cropOnlyUrineStripDrawing);

                Rect rectCrop = new Rect(x,y,w,h);
                croppedImg = new Mat(img, rectCrop);
                Log.d("KSM", "CROPSQR - CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);
                croppedSqr_x = x;
                croppedSqr_y = y;
                croppedSqr_w = w;
                croppedSqr_h = h;

//                Imgproc.rectangle(cropOnlyUrineStripDrawing, rectCrop, new Scalar(255, 0, 0), 2);
                Log.d("KSM", "successed!");
            }
        }

        return croppedImg;
    }

    //startH, endH -> double형태로 8비트(0~255)
    private Mat colorRange(Mat img, Scalar minHSV, Scalar maxHSV){
        Mat resMat = new Mat();
        Mat imgHSV = new Mat();
        img.copyTo(imgHSV);
        Imgproc.cvtColor(imgHSV, imgHSV, Imgproc.COLOR_BGR2HSV);

//        Scalar minHSV = new Scalar(startH, 110, 110);
//        Scalar maxHSV = new Scalar(endH, 255, 255); //(S,V 적정 값) 225, 225 / 230, 230 / 235, 235 / 240, 240 / 245, 245

        Log.d("KSM", "colorRangeCut : minHSV : "+minHSV);
        Log.d("KSM", "colorRangeCut : maxHSV : "+maxHSV);

        Mat imgMask = new Mat();
        Core.inRange(imgHSV, minHSV, maxHSV, imgMask);
        Core.bitwise_and(img, img, resMat, imgMask);

        return resMat;
    }

    private static String[] Add(String[] originArray, String val){
        String[] newArray = Arrays.copyOf(originArray, originArray.length + 1);
        newArray[originArray.length] = val;
        return newArray;
    }

    private Bitmap decodeResource(Context context, int resourceId){
        return BitmapFactory.decodeResource(context.getResources(), resourceId);
    }
}
