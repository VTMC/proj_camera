# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := raw-0.21.1
LOCAL_SRC_FILES := src/libraw_c_api.cpp \
                   	src/libraw_datastream.cpp src/decoders/canon_600.cpp \
                   	src/decoders/crx.cpp src/decoders/decoders_dcraw.cpp \
                   	src/decoders/decoders_libraw_dcrdefs.cpp \
                   	src/decoders/decoders_libraw.cpp src/decoders/dng.cpp \
                   	src/decoders/fp_dng.cpp src/decoders/fuji_compressed.cpp \
                   	src/decoders/generic.cpp src/decoders/kodak_decoders.cpp \
                   	src/decoders/load_mfbacks.cpp src/decoders/smal.cpp \
                   	src/decoders/unpack_thumb.cpp src/decoders/unpack.cpp \
                   	src/demosaic/aahd_demosaic.cpp src/demosaic/ahd_demosaic.cpp \
                   	src/demosaic/dcb_demosaic.cpp src/demosaic/dht_demosaic.cpp \
                   	src/demosaic/misc_demosaic.cpp src/demosaic/xtrans_demosaic.cpp \
                   	src/integration/dngsdk_glue.cpp src/integration/rawspeed_glue.cpp\
                   	src/metadata/adobepano.cpp src/metadata/canon.cpp \
                   	src/metadata/ciff.cpp src/metadata/cr3_parser.cpp \
                   	src/metadata/epson.cpp src/metadata/exif_gps.cpp \
                   	src/metadata/fuji.cpp src/metadata/identify_tools.cpp \
                   	src/metadata/identify.cpp src/metadata/kodak.cpp \
                   	src/metadata/leica.cpp src/metadata/makernotes.cpp \
                   	src/metadata/mediumformat.cpp src/metadata/minolta.cpp \
                   	src/metadata/misc_parsers.cpp src/metadata/nikon.cpp \
                   	src/metadata/normalize_model.cpp src/metadata/olympus.cpp \
                   	src/metadata/hasselblad_model.cpp \
                   	src/metadata/p1.cpp src/metadata/pentax.cpp src/metadata/samsung.cpp \
                   	src/metadata/sony.cpp src/metadata/tiff.cpp \
                   	src/postprocessing/aspect_ratio.cpp \
                   	src/postprocessing/dcraw_process.cpp src/postprocessing/mem_image.cpp \
                   	src/postprocessing/postprocessing_aux.cpp \
                   	src/postprocessing/postprocessing_utils_dcrdefs.cpp \
                   	src/postprocessing/postprocessing_utils.cpp \
                   	src/preprocessing/ext_preprocess.cpp src/preprocessing/raw2image.cpp \
                   	src/preprocessing/subtract_black.cpp src/tables/cameralist.cpp \
                   	src/tables/colorconst.cpp src/tables/colordata.cpp \
                   	src/tables/wblists.cpp src/utils/curves.cpp \
                   	src/utils/decoder_info.cpp src/utils/init_close_utils.cpp \
                   	src/utils/open.cpp src/utils/phaseone_processing.cpp \
                   	src/utils/read_utils.cpp src/utils/thumb_utils.cpp \
                   	src/utils/utils_dcraw.cpp src/utils/utils_libraw.cpp \
                   	src/write/apply_profile.cpp src/write/file_write.cpp \
                   	src/write/tiff_writer.cpp src/x3f/x3f_parse_process.cpp \
                   	src/x3f/x3f_utils_patched.cpp

LOCAL_LDLIBS    := -llog -ljnigraphics
#LOCAL_CFLAGS += -DLIBRAW_NOTHREADS -D_LARGEFILE_SOURCE -fno-rtti -fexceptions -D_FILE_OFFSET_BITS=64
LOCAL_CXXFLAGS += -DLIBRAW_NOTHREADS -D_LARGEFILE_SOURCE -fno-rtti -fexceptions -D_FILE_OFFSET_BITS=64 -D__USE_FILE_OFFSET64 -D__ANDROID_API__=24
LOCAL_C_INCLUDES += $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)
