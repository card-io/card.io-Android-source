
/* glesExperimental.cpp
 * See the file "LICENSE.md" for the full license governing this code.
 */

#include <jni.h>
#include <android/log.h>
#include "dmz/dmz.h"
#include "dmz/dmz_debug.h"
#include "dmz/processor_support.h"
#include "dmz/cv/warp.h"
#include "opencv2/core/core_c.h" // needed for IplImage

#define DEBUG_TAG "card.io native experimental"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL Java_io_card_development_NativeGLESWarp_nSetup(JNIEnv *env, jobject thiz, jint width, jint height) {
  llcv_gles_setup(kCreditCardTargetWidth, kCreditCardTargetHeight);
}

extern "C"
JNIEXPORT jint JNICALL Java_io_card_development_NativeGLESWarp_nCardWidth(JNIEnv *env, jobject thiz) {
  return kCreditCardTargetWidth;
}

extern "C"
JNIEXPORT jint JNICALL Java_io_card_development_NativeGLESWarp_nCardHeight(JNIEnv *env, jobject thiz) {
  return kCreditCardTargetHeight;
}

extern "C"
JNIEXPORT jboolean JNICALL Java_io_card_development_NativeGLESWarp_nWarp(JNIEnv *env, jobject thiz,
    jbyteArray jbImage, jint width, jint height, jintArray jCorners, jobject dinfo, jstring dInfoClassName)
{
  jint *jCornerBytes = env->GetIntArrayElements(jCorners, 0);

  IplImage *image = cvCreateImageHeader(cvSize(width, height), IPL_DEPTH_8U, 4);

  jbyte *jBytes = env->GetByteArrayElements(jbImage, 0);
  image->imageData = (char *)jBytes;

  IplImage *card = cvCreateImage( cvSize(kCreditCardTargetWidth, kCreditCardTargetHeight), IPL_DEPTH_8U, 4 );

  CvPoint2D32f corners[4];
  for (int i = 0; i < 4; i++) {
    corners[i].x = (float)jCornerBytes[2 * i + 0];
    corners[i].y = (float)jCornerBytes[2 * i + 1];
  }

  llcv_warp_perspective(image, corners, NULL, card);

  int size = card->width * card->height * card->nChannels;

  dmz_debug_log("need array sized %i x %i x %i  (%i)", card->width, card->height, card->nChannels, size);

  jclass clazz;
  jfieldID fid;

  const char *nativeDInfoClassName = env->GetStringUTFChars(dInfoClassName, 0);
  clazz = env->FindClass(nativeDInfoClassName);
  env->ReleaseStringUTFChars(dInfoClassName, nativeDInfoClassName);

  if (clazz) {
    fid = env->GetFieldID(clazz, "cardImageWidth", "I");
    if (fid) {
      env->SetIntField(dinfo, fid, card->width);
    }

    fid = env->GetFieldID(clazz, "cardImageHeight", "I");
    if (fid) {
      env->SetIntField(dinfo, fid, card->height);
    }

    fid = env->GetFieldID(clazz, "cardImageRGB", "[B");
    if (fid) {
      dmz_debug_log("- card->nSize: %d", card->nSize);
      dmz_debug_log("- card->width: %d", card->width);
      dmz_debug_log("- card->height: %d", card->height);
      dmz_debug_log("- card->nChannels: %d", card->nChannels);
      dmz_debug_log("- card->depth: %d", card->depth);

      jbyteArray jb = env->NewByteArray(size);
      env->SetByteArrayRegion(jb, 0, size, (jbyte *)card->imageData);
      env->SetObjectField(dinfo, fid, jb);
    }

  }
  cvReleaseImageHeader(&image);
  cvReleaseImage(&card);

  return (jboolean) true;
}

extern "C"
JNIEXPORT void JNICALL Java_io_card_development_NativeGLESWarp_nCleanup(JNIEnv *env, jobject thiz) {
  llcv_gles_teardown();
}
