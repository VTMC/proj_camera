@echo off
"C:\\Users\\Owner\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\studyProj_android\\proj_camera_updated\\proj_camera\\proj_camera\\opencv\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=C:\\Users\\Owner\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\Owner\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\Owner\\AppData\\Local\\Android\\Sdk\\ndk\\26.1.10909125\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\Owner\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\studyProj_android\\proj_camera_updated\\proj_camera\\proj_camera\\opencv\\build\\intermediates\\cxx\\Debug\\56412sf5\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\studyProj_android\\proj_camera_updated\\proj_camera\\proj_camera\\opencv\\build\\intermediates\\cxx\\Debug\\56412sf5\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BD:\\studyProj_android\\proj_camera_updated\\proj_camera\\proj_camera\\opencv\\.cxx\\Debug\\56412sf5\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
