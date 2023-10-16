#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java__dngSdk_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}