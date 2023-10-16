package Utils;

public class dngSDK {
    static{
        System.loadLibrary("proj_camera");
    }

    public native String testNative();

    public native Object DngReadImage();

}
