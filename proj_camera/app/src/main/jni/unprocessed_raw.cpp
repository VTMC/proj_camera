//
// Created by owner on 2023-10-25.
//

#include "unprocessed_raw.h"

/* -*- C++ -*-
 * File: unprocessed_raw.cpp
 * Copyright 2009-2021 LibRaw LLC (info@libraw.org)
 * Created: Fri Jan 02, 2009
 *
 * LibRaw sample
 * Generates unprocessed raw image: with masked pixels and without black
subtraction
 *

LibRaw is free software; you can redistribute it and/or modify
it under the terms of the one of two licenses as you choose:

1. GNU LESSER GENERAL PUBLIC LICENSE version 2.1
   (See file LICENSE.LGPL provided in LibRaw distribution archive for details).

2. COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0
   (See file LICENSE.CDDL provided in LibRaw distribution archive for details).

 */
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <time.h>

#include "libraw-0.21.1/libraw/libraw.h"

#include <netinet/in.h>
#include <jni.h>
#include <android/log.h>
#include <jni.h>

void gamma_curve(unsigned short curve[]);
void write_ppm(unsigned width, unsigned height, unsigned short *bitmap,
               const char *basename);
void write_tiff(int width, int height, unsigned short *bitmap,
                const char *basename);

void write_ppm(unsigned width, unsigned height, unsigned short *bitmap,
               const char *fname)
{
    if (!bitmap)
        return;

    FILE *f = fopen(fname, "wb");
    if (!f)
        return;
    int bits = 16;
    fprintf(f, "P5\n%d %d\n%d\n", width, height, (1 << bits) - 1);
    unsigned char *data = (unsigned char *)bitmap;
    unsigned data_size = width * height * 2;
#define SWAP(a, b)                                                             \
  {                                                                            \
    a ^= b;                                                                    \
    a ^= (b ^= a);                                                             \
  }
    for (unsigned i = 0; i < data_size; i += 2)
    SWAP(data[i], data[i + 1]);
#undef SWAP
    fwrite(data, data_size, 1, f);
    fclose(f);
}

void unprocessed_raw::write_ppm(unsigned int width, unsigned int height, unsigned short *bitmap,
                                const char *fname) {
    write_ppm(width, height, bitmap, fname);
}

void unprocessed_raw::write_tiff(int width, int height, unsigned short *bitmap,
                                 const char *basename) {
    write_tiff(width, height, bitmap, basename);
}

/*  == gamma curve and tiff writer - simplified cut'n'paste from dcraw.c */

#define SQR(x) ((x) * (x))

void gamma_curve(unsigned short *curve)
{

    double pwr = 1.0 / 2.2;
    double ts = 0.0;
    int imax = 0xffff;
    int mode = 2;
    int i;
    double g[6], bnd[2] = {0, 0}, r;

    g[0] = pwr;
    g[1] = ts;
    g[2] = g[3] = g[4] = 0;
    bnd[g[1] >= 1] = 1;
    if (g[1] && (g[1] - 1) * (g[0] - 1) <= 0)
    {
        for (i = 0; i < 48; i++)
        {
            g[2] = (bnd[0] + bnd[1]) / 2;
            if (g[0])
                bnd[(pow(g[2] / g[1], -g[0]) - 1) / g[0] - 1 / g[2] > -1] = g[2];
            else
                bnd[g[2] / exp(1 - 1 / g[2]) < g[1]] = g[2];
        }
        g[3] = g[2] / g[1];
        if (g[0])
            g[4] = g[2] * (1 / g[0] - 1);
    }
    if (g[0])
        g[5] = 1 / (g[1] * SQR(g[3]) / 2 - g[4] * (1 - g[3]) +
                    (1 - pow(g[3], 1 + g[0])) * (1 + g[4]) / (1 + g[0])) -
               1;
    else
        g[5] = 1 / (g[1] * SQR(g[3]) / 2 + 1 - g[2] - g[3] -
                    g[2] * g[3] * (log(g[3]) - 1)) -
               1;
    for (i = 0; i < 0x10000; i++)
    {
        curve[i] = 0xffff;
        if ((r = (double)i / imax) < 1)
            curve[i] =
                    0x10000 *
                    (mode ? (r < g[3] ? r * g[1]
                                      : (g[0] ? pow(r, g[0]) * (1 + g[4]) - g[4]
                                              : log(r) * g[2] + 1))
                          : (r < g[2] ? r / g[1]
                                      : (g[0] ? pow((r + g[4]) / (1 + g[4]), 1 / g[0])
                                              : exp((r - 1) / g[2]))));
    }
}

void tiff_set(ushort *ntag, ushort tag, ushort type, int count, int val)
{
    struct libraw_tiff_tag *tt;
    int c;

    tt = (struct libraw_tiff_tag *)(ntag + 1) + (*ntag)++;
    tt->tag = tag;
    tt->type = type;
    tt->count = count;
    if ((type < LIBRAW_EXIFTAG_TYPE_SHORT) && (count <= 4))
        for (c = 0; c < 4; c++)
            tt->val.c[c] = val >> (c << 3);
    else if (tagtypeIs(LIBRAW_EXIFTAG_TYPE_SHORT) && (count <= 2))
        for (c = 0; c < 2; c++)
            tt->val.s[c] = val >> (c << 4);
    else
        tt->val.i = val;
}
#define TOFF(ptr) ((char *)(&(ptr)) - (char *)th)

void tiff_head(int width, int height, struct tiff_hdr *th)
{
    int c;
    time_t timestamp = time(NULL);
    struct tm *t;

    memset(th, 0, sizeof *th);
    th->t_order = htonl(0x4d4d4949) >> 16;
    th->magic = 42;
    th->ifd = 10;
    tiff_set(&th->ntag, 254, 4, 1, 0);
    tiff_set(&th->ntag, 256, 4, 1, width);
    tiff_set(&th->ntag, 257, 4, 1, height);
    tiff_set(&th->ntag, 258, 3, 1, 16);
    for (c = 0; c < 4; c++)
        th->bps[c] = 16;
    tiff_set(&th->ntag, 259, 3, 1, 1);
    tiff_set(&th->ntag, 262, 3, 1, 1);
    tiff_set(&th->ntag, 273, 4, 1, sizeof *th);
    tiff_set(&th->ntag, 277, 3, 1, 1);
    tiff_set(&th->ntag, 278, 4, 1, height);
    tiff_set(&th->ntag, 279, 4, 1, height * width * 2);
    tiff_set(&th->ntag, 282, 5, 1, TOFF(th->rat[0]));
    tiff_set(&th->ntag, 283, 5, 1, TOFF(th->rat[2]));
    tiff_set(&th->ntag, 284, 3, 1, 1);
    tiff_set(&th->ntag, 296, 3, 1, 2);
    tiff_set(&th->ntag, 306, 2, 20, TOFF(th->date));
    th->rat[0] = th->rat[2] = 300;
    th->rat[1] = th->rat[3] = 1;
    t = localtime(&timestamp);
    if (t)
        sprintf(th->date, "%04d:%02d:%02d %02d:%02d:%02d", t->tm_year + 1900,
                t->tm_mon + 1, t->tm_mday, t->tm_hour, t->tm_min, t->tm_sec);
}

void write_tiff(int width, int height, unsigned short *bitmap, const char *fn)
{
    struct tiff_hdr th;

    FILE *ofp = fopen(fn, "wb");
    if (!ofp)
        return;
    tiff_head(width, height, &th);
    fwrite(&th, sizeof th, 1, ofp);
    fwrite(bitmap, 2, width * height, ofp);
    fclose(ofp);
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_proj_1camera_RawActivity_unprocessed_1raw(JNIEnv *env, jobject thiz,
                                                           jobjectArray jargv, jstring to_path) {
    // TODO: implement unprocessed_raw()
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

    int i, ret;
    int verbose = 1, autoscale = 0, use_gamma = 0, out_tiff = 0;
    char outfn[1024];

    LibRaw RawProcessor;
    if (argc < 2)
    {
        usage:
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
                            LibRaw::version(), LibRaw::cameraCount(), argv[0]);
        return 0;
    }

#define S RawProcessor.imgdata.sizes
#define OUT RawProcessor.imgdata.params
#define OUTR RawProcessor.imgdata.rawparams

    for (i = 0; i < argc; i++)
    {
        if (argv[i][0] == '-')
        {
            if (argv[i][1] == 'q' && argv[i][2] == 0){
                verbose = 0;
                __android_log_print(ANDROID_LOG_INFO,"KSM","verbose quitted\n");
            }
            else if (argv[i][1] == 'A' && argv[i][2] == 0){
                autoscale = 1;
                __android_log_print(ANDROID_LOG_INFO,"KSM","autoscale setted\n");
            }
            else if (argv[i][1] == 'g' && argv[i][2] == 0){
                use_gamma = 1;
                __android_log_print(ANDROID_LOG_INFO,"KSM","use_gamma setted\n");
            }
            else if (argv[i][1] == 'T' && argv[i][2] == 0){
                out_tiff = 1;
                __android_log_print(ANDROID_LOG_INFO,"KSM","tiff setted\n");
            }
            else if (argv[i][1] == 's' && argv[i][2] == 0)
            {
                i++;
                OUTR.shot_select = argv[i] ? atoi(argv[i]) : 0;
                __android_log_print(ANDROID_LOG_INFO,"KSM","shot_select setted\n");
            }
            else
                goto usage;
            continue;
        }

        if (verbose)
            __android_log_print(ANDROID_LOG_INFO,"KSM","Processing file %s\n", argv[i]);
        if ((ret = RawProcessor.open_file(argv[i])) != LIBRAW_SUCCESS)
        {
            __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot open %s: %s\n", argv[i], libraw_strerror(ret));
            continue; // no recycle b/c open file will recycle itself
        }
        if (verbose)
        {
            __android_log_print(ANDROID_LOG_INFO,"KSM","Image size: %dx%d\nRaw size: %dx%d\n", S.width, S.height,
                                S.raw_width, S.raw_height);
            __android_log_print(ANDROID_LOG_INFO,"KSM","Margins: top=%d, left=%d\n", S.top_margin, S.left_margin);
        }

        if ((ret = RawProcessor.unpack()) != LIBRAW_SUCCESS)
        {
            __android_log_print(ANDROID_LOG_ERROR,"KSM","Cannot unpack %s: %s\n", argv[i], libraw_strerror(ret));
            continue;
        }

        if (verbose)
            printf("Unpacked....\n");

        if (!(RawProcessor.imgdata.idata.filters ||
              RawProcessor.imgdata.idata.colors == 1))
        {
            __android_log_print(ANDROID_LOG_ERROR,"KSM","Only Bayer-pattern RAW files supported, sorry....\n");
            continue;
        }

        if (autoscale)
        {
            unsigned max = 0, scale;
            for (int j = 0; j < S.raw_height * S.raw_width; j++)
                if (max < RawProcessor.imgdata.rawdata.raw_image[j])
                    max = RawProcessor.imgdata.rawdata.raw_image[j];
            if (max > 0 && max < 1 << 15)
            {
                scale = (1 << 16) / max;
                if (verbose)
                    __android_log_print(ANDROID_LOG_INFO,"KSM","Scaling with multiplier=%d (max=%d)\n", scale, max);

                for (int j = 0; j < S.raw_height * S.raw_width; j++)
                    RawProcessor.imgdata.rawdata.raw_image[j] *= scale;
            }
        }
        if (use_gamma)
        {
            unsigned short curve[0x10000];
            gamma_curve(curve);
            for (int j = 0; j < S.raw_height * S.raw_width; j++)
                RawProcessor.imgdata.rawdata.raw_image[j] =
                        curve[RawProcessor.imgdata.rawdata.raw_image[j]];
            if (verbose)
                __android_log_print(ANDROID_LOG_INFO,"KSM","Gamma-corrected....\n");
        }

        const char *to_str = env->GetStringUTFChars(to_path, 0);

        if (OUTR.shot_select)
            snprintf(outfn, sizeof(outfn), "%s-%d.%s", to_str, OUTR.shot_select,
                     out_tiff ? "tiff" : "pgm");
        else
            snprintf(outfn, sizeof(outfn), "%s.%s", to_str, out_tiff ? "tiff" : "pgm");

        if (out_tiff)
            write_tiff(S.raw_width, S.raw_height,
                       RawProcessor.imgdata.rawdata.raw_image, outfn);
        else
            write_ppm(S.raw_width, S.raw_height,
                      RawProcessor.imgdata.rawdata.raw_image, outfn);

        if (verbose)
            __android_log_print(ANDROID_LOG_INFO,"KSM","Stored to file %s\n", outfn);

        env->ReleaseStringUTFChars(to_path, to_str);
    }
    return 0;
}