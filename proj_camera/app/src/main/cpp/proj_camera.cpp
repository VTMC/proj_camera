// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("proj_camera");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("proj_camera")
//      }
//    }

#include <jni.h>
#include <string>
#include "dngSdk_source/dng_read_image.h"


extern "C"
JNIEXPORT jstring JNICALL
Java_Utils_nativeTest_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_Utils_dngSDK_testNative(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_Utils_dngSDK_DngReadImage(JNIEnv *env, jobject thiz){
    dng_read_image()
}