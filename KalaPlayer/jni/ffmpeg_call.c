// tutorial01.c
//
// This tutorial was based on the code written by Stephen Dranger (dranger@gmail.com).
//
// The code is modified so that it can be compiled to a shared library and run on Android
//
// The code dumps first 5 fives of an input video file to /sdcard/android-ffmpeg-tutorial01
// folder of the external storage of your Android device
//
// Feipeng Liu (http://www.roman10.net/)
// Aug 2013

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>

#include <stdio.h>
#include <wchar.h>

#include <jni.h>

/*for android logs*/
#include <android/log.h>

#define LOG_TAG "ffmpeg_jni"
#define LOGI(...) __android_log_print(4, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(6, LOG_TAG, __VA_ARGS__);

jint naMain(JNIEnv *pEnv, jobject pObj, jobject pMainAct, jstring pFileName, jstring pDstName) {
	AVFormatContext *pFormatCtx = NULL;
	int             i, videoStream;
	AVCodecContext  *pCodecCtx = NULL;
	AVCodec         *pCodec = NULL;
	AVFrame         *pFrame = NULL;
	AVFrame         *pFrameRGBA = NULL;
	AVPacket        packet;
	int             frameFinished;
	jobject			bitmap;
	void* 			buffer;
	char path[100];

	AVDictionary    *optionsDict = NULL;
	struct SwsContext      *sws_ctx = NULL;
	char *videoFileName;
	char *dstPath;

	// Register all formats and codecs
	av_register_all();

	//get C string from JNI jstring
	videoFileName = (char *)(*pEnv)->GetStringUTFChars(pEnv, pFileName, NULL);
	dstPath = (char *)(*pEnv)->GetStringUTFChars(pEnv, pDstName, NULL);
	strcpy(path,videoFileName);

	// Open video file
	if(avformat_open_input(&pFormatCtx, videoFileName, NULL, NULL)!=0)
		return -1; // Couldn't open file

	AVDictionaryEntry *entry=av_dict_get(pFormatCtx->metadata,"major_brand",0,0);
	if ((strcmp(entry->value,"isom")!=0) && (strcmp(entry->value,"avc1")!=0)){
		//Let's try to do convert here
		char * argv[]={
				"",//should be the execute file path
				"-i",
				path,
				"-acodec",
				"copy",
				"-vcodec",
				"copy",
				dstPath
		};
		LOGI("get file %s with format %s should be convert to %s",path,entry->value,dstPath);
		main_work(8,argv);
	}

	// Close the video file
	avformat_close_input(&pFormatCtx);


	return 0;
}

jint JNI_OnLoad(JavaVM* pVm, void* reserved) {
	JNIEnv* env;
	if ((*pVm)->GetEnv(pVm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
		 return -1;
	}
	JNINativeMethod nm[1];
	nm[0].name = "naMain";
	nm[0].signature = "(Lnet/wesley/kalaplayer/MainActivity;Ljava/lang/String;Ljava/lang/String;)I";
	nm[0].fnPtr = (void*)naMain;
	jclass cls = (*env)->FindClass(env, "net/wesley/kalaplayer/MainActivity");
	//Register methods with env->RegisterNatives.
	(*env)->RegisterNatives(env, cls, nm, 1);
	return JNI_VERSION_1_6;
}
