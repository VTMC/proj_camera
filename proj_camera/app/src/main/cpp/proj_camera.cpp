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
#include <iostream>
#include "dngSdk_source/dng_read_image.h"
#include "dngSdk_source/dng_sdk_limits.h"


extern "C"{
    JNIEXPORT jstring Java_Utils_dngSDK_testNative(JNIEnv *env, jobject thiz) {
        // TODO: implement stringFromJNI()
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_Utils_dngSDK_processDNG(JNIEnv *env, jobject thiz, jstring dng_file_path) {
    // TODO: implement processDNG()
    const char *path = env->GetStringUTFChars(dng_file_path, 0);
    dng_read_image
}