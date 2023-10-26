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
#include "unprocessed_raw.h"
#include <jni.h>
#include <android/log.h>
#include <jni.h>

extern "C"
JNIEXPORT jstring Java_Utils_libRaw_testNative(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

int my_progress_callback(void * unused_data, enum LibRaw_progress state, int iter, int expected){
    if(iter == 0)
        printf("CB : state=%x, expected %d iterations\n", state, expected);
    return 0;
}

char *customCameras[] = {
        (char *)"43704960,4080,5356, 0, 0, 0, 0,0,148,0,0, Dalsa, FTF4052C Full,0",
        (char *)"42837504,4008,5344, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF4052C 3:4",
        (char *)"32128128,4008,4008, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF4052C 1:1",
        (char *)"24096096,4008,3006, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF4052C 4:3",
        (char *)"18068064,4008,2254, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF4052C 16:9",
        (char *)"67686894,5049,6703, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF5066C Full",
        (char *)"66573312,4992,6668, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF5066C 3:4",
        (char *)"49840128,4992,4992, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF5066C 1:1",
        (char *)"37400064,4992,3746, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF5066C 4:3",
        (char *)"28035072,4992,2808, 0, 0, 0, 0,0,148,0,0,Dalsa, FTF5066C 16:9",
        NULL};

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_proj_1camera_RawActivity_simple_1dcraw(JNIEnv *env, jobject thiz,
                                                        jobjectArray jargv, jstring toPath) {
    // TODO: implement simple_dcraw()
    int argc = env->GetArrayLength(jargv);
    typedef char *pchar;
    pchar *argv = new pchar[argc];
    for(int i=0; i<argc; i++){
        jstring js = static_cast<jstring>(env->GetObjectArrayElement(jargv, i));
        const char *pjc = env->GetStringUTFChars(js, 0);
        size_t jslen = strlen(pjc);
        argv[i] = new char[jslen+1];
        strcpy(argv[i], pjc);
        env->ReleaseStringUTFChars(js, pjc);
    }

    int i, ret, verbose = 0, output_thumbs = 0, output_all_thumbs = 0;

    int resultCode = 0;

    // don't use fixed size buffers in real apps!
    char outfn[1024], thumbfn[1024];

    LibRaw* RawProcessor = new LibRaw;
//    RawProcessor->imgdata.rawparams.custom_camera_strings = customCameras;
    if (argc < 2)
    {
        __android_log_print(ANDROID_LOG_INFO,
                            "KSM",
                            "simple_dcraw - LibRaw %s sample. Emulates dcraw [-D] [-T] [-v] "
                            "[-e] [-E]\n"
                            " %d cameras supported\n"
                            "Usage: %s [-D] [-T] [-v] [-e] raw-files....\n"
                            "\t-4 - 16-bit mode\n"
                            "\t-L - list supported cameras and exit\n"
                            "\t-v - verbose output\n"
                            "\t-T - output TIFF files instead of .pgm/ppm\n"
                            "\t-e - extract thumbnails (same as dcraw -e in separate run)\n"
                            "\t-E - extract all thumbnails\n",
                            LibRaw::version(), LibRaw::cameraCount(), argv[0],
                            0);
        delete RawProcessor;
        return 0;
    }

    putenv((char *)"TZ=UTC"); // dcraw compatibility, affects TIFF datestamp field

#define P1 RawProcessor->imgdata.idata
#define S RawProcessor->imgdata.sizes
#define C RawProcessor->imgdata.color
#define T RawProcessor->imgdata.thumbnail
#define P2 RawProcessor->imgdata.other
#define OUT RawProcessor->imgdata.params

    //for Test argv, argc
    for(int i=0; i<argc; i++){
        __android_log_print(
                ANDROID_LOG_DEBUG,
                "KSM",
                "argv Test : av[%d]는 %s입니다.\n", i, argv[i]);
//        delete argv[i];
    }

    __android_log_print(
            ANDROID_LOG_DEBUG,
            "KSM",
            "argc Test : ac는 %d개 입니다.\n", argc);

//    delete [] argv;
    for (i = 0; i < argc; i++)
    {
        if (argv[i][0] == '-')
        {
            if (argv[i][1] == 'T' && argv[i][2] == 0){
                OUT.output_tiff = 1;
                __android_log_print(ANDROID_LOG_INFO,"KSM","tiff setted\n");
            }
            if (argv[i][1] == 'v' && argv[i][2] == 0){
                verbose++;
                __android_log_print(ANDROID_LOG_INFO,"KSM","verbose setted\n");
            }

            if (argv[i][1] == 'e' && argv[i][2] == 0){
                output_thumbs++;
                __android_log_print(ANDROID_LOG_INFO,"KSM","output_thumbs setted\n");
            }

            if (argv[i][1] == 'E' && argv[i][2] == 0)
            {
                output_thumbs++;
                output_all_thumbs++;
                __android_log_print(ANDROID_LOG_INFO,"KSM","output_thumbs setted\noutput_all_thumbs setted\n");
            }
            if (argv[i][1] == '4' && argv[i][2] == 0){
                OUT.output_bps = 16;
                __android_log_print(ANDROID_LOG_INFO,"KSM","output bps 16bit setted\n");
            }
//            if (argv[i][1] == 'C' && argv[i][2] == 0){
//                RawProcessor->set_progress_handler(my_progress_callback, NULL);
//                __android_log_print(ANDROID_LOG_INFO,"KSM","set_progress_handler setted\n");
//            }

            if (argv[i][1] == 'L' && argv[i][2] == 0)
            {
                const char **clist = LibRaw::cameraList();
                const char **cc = clist;
                while (*cc)
                {
                    __android_log_print(ANDROID_LOG_INFO,"KSM","%s\n", *cc);
                    cc++;
                }
                __android_log_print(ANDROID_LOG_INFO,"KSM","cameraList showed\n");
                delete RawProcessor;
                exit(0);
            }
            continue;
        }

        if (verbose)
            __android_log_print(ANDROID_LOG_INFO,
                                "KSM",
                                "Processing file %s\n", argv[i]);

        if ((ret = RawProcessor->open_file(argv[i])) != LIBRAW_SUCCESS)
        {
            __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot open_file %s: %s\n", argv[i], libraw_strerror(ret));
            resultCode = 1;
            continue; // no recycle b/c open file will recycle itself
        }

        if (!output_thumbs) // No unpack for thumb extraction
            if ((ret = RawProcessor->unpack()) != LIBRAW_SUCCESS)
            {
                __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot unpack %s: %s\n", argv[i], libraw_strerror(ret));
                resultCode = 1;
                continue;
            }

        // thumbnail unpacking and output in the middle of main
        // image processing - for test purposes!
        if(output_all_thumbs)
        {
            if (verbose)
                __android_log_print(ANDROID_LOG_INFO,"KSM","Extracting %d thumbnails\n", RawProcessor->imgdata.thumbs_list.thumbcount);
            for (int t = 0; t < RawProcessor->imgdata.thumbs_list.thumbcount; t++)
            {
                if ((ret = RawProcessor->unpack_thumb_ex(t)) != LIBRAW_SUCCESS) {
                    __android_log_print(ANDROID_LOG_ERROR, "KSM",
                                        "Cannot unpack_thumb #%d from %s: %s\n", t, argv[i],
                                        libraw_strerror(ret));
                    resultCode = 1;
                }

                if (LIBRAW_FATAL_ERROR(ret))
                    break; // skip to next file
                snprintf(thumbfn, sizeof(thumbfn), "%s.thumb.%d.%s", argv[i], t,
                         T.tformat == LIBRAW_THUMBNAIL_JPEG ? "jpg" : "ppm");
                if (verbose)
                    __android_log_print(ANDROID_LOG_INFO,"KSM","Writing thumbnail file %s\n", thumbfn);
                if (LIBRAW_SUCCESS != (ret = RawProcessor->dcraw_thumb_writer(thumbfn)))
                {
                    __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot write %s: %s\n", thumbfn, libraw_strerror(ret));
                    resultCode = 1;
                    if (LIBRAW_FATAL_ERROR(ret))
                        break;
                }
            }
            continue;
        }
        else if (output_thumbs)
        {
            if ((ret = RawProcessor->unpack_thumb()) != LIBRAW_SUCCESS)
            {
                __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot unpack_thumb %s: %s\n", argv[i],
                                    libraw_strerror(ret));
                resultCode = 1;
                if (LIBRAW_FATAL_ERROR(ret))
                    continue; // skip to next file
            }
            else
            {
                snprintf(thumbfn, sizeof(thumbfn), "%s.%s", argv[i],
                         T.tformat == LIBRAW_THUMBNAIL_JPEG ? "thumb.jpg"
                                                            : (T.tcolors == 1? "thumb.pgm" : "thumb.ppm"));
                if (verbose)
                    __android_log_print(ANDROID_LOG_INFO,"KSM","Writing thumbnail file %s\n", thumbfn);
                if (LIBRAW_SUCCESS != (ret = RawProcessor->dcraw_thumb_writer(thumbfn)))
                {
                    __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot write %s: %s\n", thumbfn, libraw_strerror(ret));
                    resultCode = 1;
                    if (LIBRAW_FATAL_ERROR(ret))
                        continue;
                }
            }
            continue;
        }

        ret = RawProcessor->dcraw_process();

        if (LIBRAW_SUCCESS != ret)
        {
            __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot do postprocessing on %s: %s\n", argv[i],
                                libraw_strerror(ret));
            resultCode = 1;
            if (LIBRAW_FATAL_ERROR(ret))
                continue;
        }

        const char *to_str = env->GetStringUTFChars(toPath, 0);

        snprintf(outfn, sizeof(outfn), "%s.%s", to_str,
                 OUT.output_tiff ? "tiff" : (P1.colors > 1 ? "ppm" : "pgm"));

        if (verbose)
            __android_log_print(ANDROID_LOG_INFO,"KSM","Writing file %s\n", outfn);

        if (LIBRAW_SUCCESS != (ret = RawProcessor->dcraw_ppm_tiff_writer(outfn))){
            __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot write %s: %s\n", outfn, libraw_strerror(ret));
            resultCode = 1;
        }


        delete argv[i];
        env->ReleaseStringUTFChars(toPath, to_str);
        RawProcessor->recycle(); // just for show this call
    }

    delete [] argv;
    delete RawProcessor;
    return resultCode;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_proj_1camera_RawActivity_dngToTiff(JNIEnv *env, jobject thiz, jstring from_path,
                                                    jstring to_path) {
    // TODO: implement dngToTiff()
    const char *from_str = env->GetStringUTFChars(from_path, 0);

    __android_log_print(
            ANDROID_LOG_DEBUG,
            "KSM",
            "In C++ From Path : %s\n", from_str,
            0);

//    LibRaw* RawProcessor = new LibRaw;
    LibRaw RawProcessor;

#define C RawProcessor.imgdata.color
#define S RawProcessor.imgdata.sizes
#define OUT RawProcessor.imgdata.params
#define OUTR RawProcessor.imgdata.rawparams

    int ret;
    char outfn[1024], thumbfn[1024]; //buffer
    OUT.output_tiff = 1;

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Opening File... \n",
            0);

    //open File
    if((ret = RawProcessor.open_file(from_str)) != LIBRAW_SUCCESS){
        __android_log_print(
                ANDROID_LOG_ERROR,
                "KSM",
                "Cannot Open File!!! %s : %s\n", from_str, libraw_strerror(ret),
                ret);
        return 1;
    }

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Image Size : %d*%d\nRaw Size : %d*%d\n", S.width, S.height, S.raw_width, S.raw_height);

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Margins : top=%d, left%d\n", S.top_margin, S.left_margin);

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Raw Unpacking... \n",
            0);

    //unpacking RawFile
    if((ret = RawProcessor.unpack()) != LIBRAW_SUCCESS){
        __android_log_print(
                ANDROID_LOG_ERROR,
                "KSM",
                "Unpacking Error!!! %s : %s\n", from_str, libraw_strerror(ret),
                ret);
        return 1;
    }

    //get raw profile
    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Raw image size %d*%d\n", RawProcessor.imgdata.sizes.raw_width, RawProcessor.imgdata.sizes.raw_height);

    if(!(RawProcessor.imgdata.idata.filters || RawProcessor.imgdata.idata.colors == 1)){
        __android_log_print(
                ANDROID_LOG_ERROR,
                "KSM",
                "Only Bayer-pattern RAW files supported\n",
                ret);
        return 1;
    }

//    __android_log_print(
//            ANDROID_LOG_INFO,
//            "KSM",
//            "Image Processing... \n",
//            0);
//
//    ret = RawProcessor.dcraw_process();
//
//    if(ret != LIBRAW_SUCCESS){
//        __android_log_print(
//                ANDROID_LOG_ERROR,
//                "KSM",
//                "Cannot do postProcessing!!! %s : %s\n", from_str, libraw_strerror(ret),
//                ret);
//        return 1;
//    }

    env->ReleaseStringUTFChars(from_path, from_str);

    const char *to_str = env->GetStringUTFChars(to_path, 0);

    __android_log_print(
            ANDROID_LOG_DEBUG,
            "KSM",
            "In C++ To Path : %s\n", to_str,
            0);

    snprintf(outfn, sizeof(outfn), "%s.%s", to_str,
             OUT.output_tiff ? "tiff" : "ppm"/*(P1.colors > 1 ? "ppm" : "pgm")*/);


    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Saving File... \n",
            0);

//    if((ret = RawProcessor.dcraw_ppm_tiff_writer(outfn)) != LIBRAW_SUCCESS){
//        __android_log_print(
//                ANDROID_LOG_ERROR,
//                "KSM",
//                "Saving Error!!! %s : %s\n", to_str, libraw_strerror(ret),
//                ret);
//        return 1;
//    }

    unprocessed_raw* ur = new unprocessed_raw();

    if(OUT.output_tiff){
        ur->write_tiff(S.raw_width, S.raw_height, RawProcessor.imgdata.rawdata.raw_image, outfn);
    }else{
        ur->write_ppm(S.raw_width, S.raw_height, RawProcessor.imgdata.rawdata.raw_image, outfn);
    }

    env->ReleaseStringUTFChars(to_path, to_str);
    RawProcessor.recycle();
//    delete RawProcessor;
    return 0;
}

extern "C"
JNIEXPORT jintArray JNICALL Java_com_example_proj_1camera_RawActivity_getRGB(JNIEnv * env, jobject thiz,
                                                                           jint x, jint y, jstring path) {
    // TODO: implement getRGB()

    const char *path_str = env->GetStringUTFChars(path, 0);

    __android_log_print(
            ANDROID_LOG_DEBUG,
    "KSM",
    "In C++ From Path : %s\n", path_str,
    0);

    LibRaw RawProcessor;

    int ret;

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Try to Open File... \n");

    //file opening
    if((ret = RawProcessor.open_file(path_str)) != LIBRAW_SUCCESS){
        __android_log_print(
                ANDROID_LOG_ERROR,
                "KSM",
                "File Open Error %s : %s\n", path_str, libraw_strerror(ret));
        return NULL;
    }

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Try to Unpack File... \n");

    if((ret = RawProcessor.unpack()) != LIBRAW_SUCCESS){
        __android_log_print(
                ANDROID_LOG_ERROR,
                "KSM",
                "File Unpack Error : %s\n", libraw_strerror(ret));
        return NULL;
    }

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Try to Process File... \n");

    if((ret = RawProcessor.dcraw_process()) != LIBRAW_SUCCESS){
        __android_log_print(
                ANDROID_LOG_ERROR,
                "KSM",
                "File Process Error : %s\n", libraw_strerror(ret));
        return NULL;
    }

    //get middle point
//    int ctrX = RawProcessor.imgdata.sizes.iwidth / 2;
//    int ctrY = RawProcessor.imgdata.sizes.iheight / 2;

    //swap X,Y
    int ctrX = RawProcessor.imgdata.sizes.iheight / 2;
    int ctrY = RawProcessor.imgdata.sizes.iwidth / 2;

    //get Middle Point Pixel RGB by Libraw
    int r = RawProcessor.imgdata.image[ctrX * RawProcessor.imgdata.sizes.iwidth + ctrY][0];
    int g = RawProcessor.imgdata.image[ctrX * RawProcessor.imgdata.sizes.iwidth + ctrY][1];
    int b = RawProcessor.imgdata.image[ctrX * RawProcessor.imgdata.sizes.iwidth + ctrY][2];

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Path : %s\nPos[%d,%d] R : %d, G : %d, B : %d\n", path_str, ctrX, ctrY, r, g, b);

    //change to 8Bit
    int b8r = (r*255)/65535;
    int b8g = (g*255)/65535;
    int b8b = (b*255)/65535;

    __android_log_print(
            ANDROID_LOG_INFO,
            "KSM",
            "Path : %s\nPos[%d,%d] 8bit R : %d, G : %d, B : %d\n", path_str, ctrX, ctrY, b8r, b8g, b8b);

    //make rgb values to int Array
    jintArray rgbArray = env->NewIntArray(3);
    int rgb[] = {b8r,b8g,b8b};
    env->SetIntArrayRegion(rgbArray, 0, 3, rgb);


    env->ReleaseStringUTFChars(path, path_str);
    RawProcessor.recycle();
    return rgbArray;
}