package Utils;

public class libRaw {
    static{
        System.load("native-lib");
    }

    public native String testNative();
}
