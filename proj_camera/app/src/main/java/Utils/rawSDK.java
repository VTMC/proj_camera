package Utils;

public class rawSDK {
    static{
        System.loadLibrary("proj_camera");
    }

    public native String testNative();

    private native byte[] dngFileInputStream(String path);

}
