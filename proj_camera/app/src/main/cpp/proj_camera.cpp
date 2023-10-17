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
#include "dngSdk_source/dng_file_stream.h"
#include "dngSdk_source/dng_read_image.h"

#include "dngSdk_source/dng_host.h"
//#include "dngSdk_source/dng_render.h"
//#include "dngSdk_source/dng_negative.h"
//#include "dngSdk_source/dng_image_writer.h"
//#include "dngSdk_source/dng_file_stream.h"

#include "dngSdk_source/dng_classes.h"

extern "C"
JNIEXPORT jstring Java_Utils_rawSDK_testNative(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_Utils_rawSDK_dngFileInputStream(JNIEnv *env, jobject thiz, jstring path) {
    // TODO: implement dngFileStream()
    //make dng sdk host
//    dng_host host;

    unsigned int smallBufSize =	8 * 1024;

    try{
        const char *pathFromJ = env->GetStringUTFChars(path, 0);

        //open fileStream DNG file
        dng_file_stream fileStream(pathFromJ, false, smallBufSize);

        //get file size
        uint64 fileSize = fileStream.Length();

        if(fileSize == 0){
            return nullptr;
        }

        //Read the file content into a C++ vector
        std::vector<uint8_t> fileData(fileSize);
        fileStream.Get(&fileData[0], static_cast<uint32>(fileSize), 0);

        //Create a jbyteArray and copy the C++ vector data to it.
        jbyteArray result = env->NewByteArray(fileData.size());
        env->SetByteArrayRegion(result, 0, fileData.size(), reinterpret_cast<jbyte*>(&fileData[0]));

        return result;
    }catch(dng_exception e){
        printf("An Error occured");
    }

    //Return null if an error occurs.
    return nullptr;

    //DNG 파일의 네거티브 생성
    //스트림 네거티브 읽기
    //네거티브에서 렌더링 파라미터 생성
    //랜더링 파라미터를 사용하여 최종 이미지 생성
    //JPEG로 저장할 파일 스트림 생성
    //dng_image_writer를 사용하여 이미지를 JPEG파일로 작성

}