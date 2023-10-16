package Utils;

public class dngSDK {
    static{
        System.loadLibrary("proj_camera");
    }

    public native String testNative();

    private native void processDNG(String dngFilePath);

    public void readDNG(String dngFilePath){
        processDNG(dngFilePath);
    }

}
