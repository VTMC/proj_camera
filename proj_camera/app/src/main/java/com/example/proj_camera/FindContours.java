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
    private int threshold = 60;
    private Random rng = new Random(12345);

    public FindContours(String path){
        src = Imgcodecs.imread(path);
        if(src.empty()){
            Log.e("KSM", "Cannot Read image : "+path);
            System.exit(0);
        }

        enhancedSrc = img_contrast(src);

        Imgproc.cvtColor(enhancedSrc, srcGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(srcGray, srcGray, new Size(3,3));
    }

    public Bitmap update(){
        Bitmap bmp = null;

        Mat cannyOutput = new Mat();
        Imgproc.Canny(srcGray, cannyOutput, threshold, threshold*2);

        List<MatOfPoint> contoursSimple = new ArrayList<>();
        List<MatOfPoint> contoursNone = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contoursSimple, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.findContours(cannyOutput, contoursNone, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

        drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple.size(); i++){
            Scalar color = new Scalar(255, 0, 0);
            Imgproc.drawContours(drawing, contoursSimple, i, color, 5, Imgproc.LINE_8, hierarchy, 5, new Point());
        }

//        for(int i = 0; i < contoursNone.size(); i++){
//            Scalar color = new Scalar(0, 0, 255);
//            Imgproc.drawContours(drawing, contoursNone, i, color, 2, Imgproc., hierarchy, 2, new Point());
//        }

        resultMat_2 = Mat.zeros(drawing.size(), CvType.CV_8UC3);
        for(int i = 0; i < contoursSimple.size(); i++){
            Rect boundingRect = Imgproc.boundingRect(contoursSimple.get(i));
            int x = boundingRect.x;
            int y = boundingRect.y;
            int w = boundingRect.width;
            int h = boundingRect.height;
            Log.d("KSM", "CONTOUR INFO : x : "+x+", y : "+y+", width : "+w+", height : "+h);

            if(w>=30 && h>=30){
                Rect rectCrop = new Rect(x,y,w,h);
                Mat croppedImg = new Mat(src, rectCrop);

                Imgproc.rectangle(resultMat_2, rectCrop, new Scalar(255, 255, 0), 2);

                long timeStampMillis = System.currentTimeMillis();
                String imgName = "/storage/emulated/0/Pictures/CameraProj-Image Raw/CROP_"+timeStampMillis+"_"+i+".bmp";
                boolean saveRes = Imgcodecs.imwrite(imgName, croppedImg);
                if(saveRes){
                    Log.d("KSM", "SAVE SUCCESSED!");
                }else{
                    Log.d("KSM", "SAVE ERROR!!");
                }
            }
        }


        Mat result_mat = new Mat();
        Core.add(drawing, enhancedSrc, result_mat);

        try{
            bmp = Bitmap.createBitmap(result_mat.cols(), result_mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(result_mat, bmp);
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
