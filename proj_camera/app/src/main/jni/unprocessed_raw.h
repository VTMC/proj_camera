//
// Created by owner on 2023-10-25.
//

#ifndef TRACEURINETEST_UNPROCESSED_RAW_H
#define TRACEURINETEST_UNPROCESSED_RAW_H


class unprocessed_raw {
    public:
        void write_tiff(int width, int height, unsigned short *bitmap,
                        const char *basename);
        void write_ppm(unsigned width, unsigned height, unsigned short *bitmap,
                       const char *fname);
};


#endif //TRACEURINETEST_UNPROCESSED_RAW_H
