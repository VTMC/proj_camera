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
#include <vector>

#include "libraw-0.21.1/libraw/libraw.h"
#include "libraw-0.21.1/libraw/libraw_alloc.h"
#include "libraw-0.21.1/libraw/libraw_const.h"
#include "libraw-0.21.1/libraw/libraw_datastream.h"
#include "libraw-0.21.1/libraw/libraw_internal.h"
#include "libraw-0.21.1/libraw/libraw_types.h"
#include "libraw-0.21.1/libraw/libraw_version.h"
#include <jni.h>

extern "C"
JNIEXPORT jstring Java_Utils_libRaw_testNative(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT jstring JNICALL
Java_Utils_rawSDK_dngLoad(JNIEnv *env, jobject thiz) {
    // TODO: implement dngLoad()

    LibRaw libraw;

}


extern "C"
JNIEXPORT jstring