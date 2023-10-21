#
## For more information about using CMake with Android Studio, read the
## documentation: https://d.android.com/studio/projects/add-native-code.html.
## For more examples on how to use CMake, see https://github.com/android/ndk-samples.
#
## Sets the minimum CMake version required for this project.
#cmake_minimum_required(VERSION 3.22.1)
#
#set(CPP_FILES native-lib.cpp)
#
##set(pathPROJECT D:/studyProj_android/proj_camera_updated/proj_camera/proj_camera)
##set(pathLIBRAW ${pathPROJECT}/app/src/main/cpp)
#
## Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
## Since this is the top level CMakeLists.txt, the project name is also accessible
## with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
## build script scope).
#project("proj_camera")
#
## Creates and names a library, sets it as either STATIC
## or SHARED, and provides the relative paths to its source code.
## You can define multiple libraries, and CMake builds them for you.
## Gradle automatically packages shared libraries with your APK.
##
## In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
## the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
## is preferred for the same purpose.
##
## In order to load a library into your app from Java/Kotlin, you must call
## System.loadLibrary() and pass the name of the library defined here;
## for GameActivity/NativeActivity derived applications, the same library name must be
## used in the AndroidManifest.xml file.
#add_library(${CMAKE_PROJECT_NAME} SHARED
#
#        # List C/C++ source files with relative paths to this CMakeLists.txt.
#        native-lib.cpp)
#
##add_library(libraw SHARED IMPORTED)
##set_target_properties(
##        # Specifies the target library.
##        libraw
##
##        # Specifies the parameter you want to define.
##        PROPERTIES IMPORTED_LOCATION
##
##        # Provides the path to the library you want to import.
##        libraw-0.21.1/src/${ANDROID_ABI}/libraw.so )
#
##add_library(dngSdk_source SHARED dngSdk_source)
#
##target_include_directories(dngSdk_source PUBLIC ${pathDNGSDK})
#
#find_library(
#        # Defines the name of the path variable that stores the
#        # location of the NDK library.
#        log-lib
#
#        # Specifies the name of the NDK library that
#        # you want CMake to locate.
#        log
#)
#
## Specifies libraries CMake should link to your target library. You
## can link libraries from various origins, such as libraries defined in this
## build script, prebuilt third-party libraries, or Android system libraries.
#target_link_libraries(
#        ${CMAKE_PROJECT_NAME}
#    # List libraries link to the target library
#    android
#    log)




project("jni libraries")
cmake_minimum_required(VERSION 3.4.1)
#set(LOCAL_PATH "src/main/jni/libraw-0.21.1")
set(LOCAL_PATH "F:/Android_Project/proj_camera/proj_camera/proj_camera/app/src/main/jni/libraw-0.21.1")
set(CMAKE_CXX_FLAGS "-DANDROID_ABI=armeabi-v7a -DLIBRAW_NOTHREADS -D_LARGEFILE_SOURCE -fno-rtti -fexceptions -D_FILE_OFFSET_BITS=64 -D__USE_FILE_OFFSET64 -DLIBRAW_OWN_SWAB")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/src/main/libs/${ANDROID_ABI})
set(LOCAL_SRC_FILES ${LOCAL_PATH}/src/libraw_c_api.cpp ${LOCAL_PATH}/src/libraw_datastream.cpp ${LOCAL_PATH}/src/decoders/canon_600.cpp ${LOCAL_PATH}/src/decoders/crx.cpp
        ${LOCAL_PATH}/src/decoders/decoders_dcraw.cpp ${LOCAL_PATH}/src/decoders/decoders_libraw_dcrdefs.cpp ${LOCAL_PATH}/src/decoders/decoders_libraw.cpp ${LOCAL_PATH}/src/decoders/dng.cpp
        ${LOCAL_PATH}/src/decoders/fp_dng.cpp ${LOCAL_PATH}/src/decoders/fuji_compressed.cpp ${LOCAL_PATH}/src/decoders/generic.cpp ${LOCAL_PATH}/src/decoders/kodak_decoders.cpp
        ${LOCAL_PATH}/src/decoders/load_mfbacks.cpp ${LOCAL_PATH}/src/decoders/smal.cpp ${LOCAL_PATH}/src/decoders/unpack_thumb.cpp ${LOCAL_PATH}/src/decoders/unpack.cpp
        ${LOCAL_PATH}/src/demosaic/aahd_demosaic.cpp ${LOCAL_PATH}/src/demosaic/ahd_demosaic.cpp ${LOCAL_PATH}/src/demosaic/dcb_demosaic.cpp ${LOCAL_PATH}/src/demosaic/dht_demosaic.cpp
        ${LOCAL_PATH}/src/demosaic/misc_demosaic.cpp ${LOCAL_PATH}/src/demosaic/xtrans_demosaic.cpp ${LOCAL_PATH}/src/integration/dngsdk_glue.cpp ${LOCAL_PATH}/src/integration/rawspeed_glue.cpp
        ${LOCAL_PATH}/src/metadata/adobepano.cpp ${LOCAL_PATH}/src/metadata/canon.cpp ${LOCAL_PATH}/src/metadata/ciff.cpp ${LOCAL_PATH}/src/metadata/cr3_parser.cpp ${LOCAL_PATH}/src/metadata/epson.cpp
        ${LOCAL_PATH}/src/metadata/exif_gps.cpp ${LOCAL_PATH}/src/metadata/fuji.cpp ${LOCAL_PATH}/src/metadata/identify_tools.cpp ${LOCAL_PATH}/src/metadata/identify.cpp ${LOCAL_PATH}/src/metadata/kodak.cpp
        ${LOCAL_PATH}/src/metadata/leica.cpp ${LOCAL_PATH}/src/metadata/makernotes.cpp ${LOCAL_PATH}/src/metadata/mediumformat.cpp ${LOCAL_PATH}/src/metadata/minolta.cpp ${LOCAL_PATH}/src/metadata/misc_parsers.cpp
        ${LOCAL_PATH}/src/metadata/nikon.cpp ${LOCAL_PATH}/src/metadata/normalize_model.cpp ${LOCAL_PATH}/src/metadata/olympus.cpp ${LOCAL_PATH}/src/metadata/hasselblad_model.cpp ${LOCAL_PATH}/src/metadata/p1.cpp
        ${LOCAL_PATH}/src/metadata/pentax.cpp ${LOCAL_PATH}/src/metadata/samsung.cpp ${LOCAL_PATH}/src/metadata/sony.cpp ${LOCAL_PATH}/src/metadata/tiff.cpp ${LOCAL_PATH}/src/postprocessing/aspect_ratio.cpp
        ${LOCAL_PATH}/src/postprocessing/dcraw_process.cpp ${LOCAL_PATH}/src/postprocessing/mem_image.cpp ${LOCAL_PATH}/src/postprocessing/postprocessing_aux.cpp
        ${LOCAL_PATH}/src/postprocessing/postprocessing_utils_dcrdefs.cpp ${LOCAL_PATH}/src/postprocessing/postprocessing_utils.cpp ${LOCAL_PATH}/src/preprocessing/ext_preprocess.cpp
        ${LOCAL_PATH}/src/preprocessing/raw2image.cpp ${LOCAL_PATH}/src/preprocessing/subtract_black.cpp ${LOCAL_PATH}/src/tables/cameralist.cpp ${LOCAL_PATH}/src/tables/colorconst.cpp
        ${LOCAL_PATH}/src/tables/colordata.cpp ${LOCAL_PATH}/src/tables/wblists.cpp ${LOCAL_PATH}/src/utils/curves.cpp ${LOCAL_PATH}/src/utils/decoder_info.cpp ${LOCAL_PATH}/src/utils/init_close_utils.cpp
        ${LOCAL_PATH}/src/utils/open.cpp ${LOCAL_PATH}/src/utils/phaseone_processing.cpp ${LOCAL_PATH}/src/utils/read_utils.cpp ${LOCAL_PATH}/src/utils/thumb_utils.cpp ${LOCAL_PATH}/src/utils/utils_dcraw.cpp
        ${LOCAL_PATH}/src/utils/utils_libraw.cpp ${LOCAL_PATH}/src/write/apply_profile.cpp ${LOCAL_PATH}/src/write/file_write.cpp ${LOCAL_PATH}/src/write/tiff_writer.cpp ${LOCAL_PATH}/src/x3f/x3f_parse_process.cpp
        ${LOCAL_PATH}/src/x3f/x3f_utils_patched.cpp )

add_library(
        #sets the name of the library
        raw-0.21.1
        #sets the library as a shared library
        SHARED
        #provides a relative path to source files
        ${LOCAL_SRC_FILES}
)

#search for ap specified prebuilt library
#fine_library (
#sets the name of the library
#log-lib
#specifies the name of the ndk library
#log)
#target_link_libraries (
#specifes the target library
#links the target library
#${log-lib})
include_directories(${LOCAL_PATH})

