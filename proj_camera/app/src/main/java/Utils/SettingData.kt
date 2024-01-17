package Utils

object SettingData {
    //StartRawTestActivity.kt onSensorChanged() variable setting - KSM_20231129
    const val TRACE_SET_DEFAULT_ACCXMIN = -1.0
    const val TRACE_SET_DEFAULT_ACCXMAX = 1.0
    const val TRACE_SET_DEFAULT_ACCYMIN = -1.0
    const val TRACE_SET_DEFAULT_ACCYMAX = 2.0
    const val TRACE_SET_DEFAULT_ACCZMIN = 9.0
    const val TRACE_SET_DEFAULT_ACCZMAX = 11.0

    //FindContour.java update(), fitImg() variable setting
    const val TRACE_SET_DEFAULT_THRESHOLDVALUE = 130
    const val TRACE_SET_DEFAULT_THRESHOLDVALUE2 = 110

    //FindContour.java update()
    const val TRACE_SET_DEFAULT_CROPIMGMINWIDTH = 50
    const val TRACE_SET_DEFAULT_CROPIMGMINHEIGHT = 1000
    const val TRACE_SET_DEFAULT_CROPIMGMINERRORWIDTH = 500

    //FindContour.java getSqr() variable setting
    const val TRACE_SET_DEFAULT_URINESTRIPSIZE = 80
    const val TRACE_SET_DEFAULT_URINESTRIPHEIGHT = 123.0
    const val TRACE_SET_DEFAULT_SQR_H_RATIO = 4.0
    const val TRACE_SET_DEFAULT_FBH_RATIO = 2.1
    const val TRACE_SET_DEFAULT_BH_RATIO = 3.55

    //FindContour.java checkCropImg() variable setting
    const val TRACE_SET_DEFAULT_CANNYTHRESHOLD = 10 //10
    const val TRACE_SET_DEFAULT_CANNYTHRESHOLD2 = 50 //30
    const val TRACE_SET_DEFAULT_HOUGHLINETHRESHOLD = 35 //prev value : 50
    const val TRACE_SET_DEFAULT_CHECKMINANGLE = 80 //prev value : 80
    const val TRACE_SET_DEFAULT_CHECKMAXANGLE = 100 //prev value : 100
    const val TRACE_SET_DEFAULT_CHECK_Y_POSITION = 5 //prev value : 5
}