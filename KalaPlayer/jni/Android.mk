LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ffmpeg_call
LOCAL_SRC_FILES := ffmpeg_call.c cmdutils.c ffmpeg.c ffmpeg_filter.c ffmpeg_opt.c
LOCAL_LDLIBS := -llog -ljnigraphics -lz 
LOCAL_SHARED_LIBRARIES := libavformat libavcodec libswscale libavutil  libwsresample  libavfilter

include $(BUILD_SHARED_LIBRARY)
$(call import-module,ffmpeg-2.0.5/android/arm)
