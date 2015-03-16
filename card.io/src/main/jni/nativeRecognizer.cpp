
/* nativeRecognizer.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>

#include "opencv2/core/core_c.h"
#include "opencv2/imgproc/imgproc_c.h"

#include "dmz.h"
#include "processor_support.h"
#include "dmz_debug.h"
#include "cv/warp.h"

#include "scan/scan.h"

#define DEBUG_TAG "card.io native"

static dmz_context* dmz = NULL;
static int dmz_refcount = 0;

static ScannerState scannerState;
static bool detectOnly;
static bool flipped;
static bool lastFrameWasUsable;
static float minFocusScore;

static struct {
  jclass classRef;
  jfieldID top;
  jfieldID bottom;
  jfieldID left;
  jfieldID right;
} rectId;

static struct {
  jclass classRef;
  jfieldID complete;
  jfieldID topEdge;
  jfieldID bottomEdge;
  jfieldID leftEdge;
  jfieldID rightEdge;
  jfieldID focusScore;
  jfieldID prediction;
  jfieldID expiry_month;
  jfieldID expiry_year;
  jfieldID detectedCard;
} detectionInfoId;

static struct {
  jclass classRef;
  jfieldID flipped;
  jfieldID yoff;
  jfieldID xoff;
} creditCardId;

static struct {
  jclass classRef;
  jmethodID edgeUpdateCallback;
} cardScannerId;

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv* env;
  jint status = vm->GetEnv( (void**) &env, JNI_VERSION_1_6);
  if (status != JNI_OK)
    return -1;

  /* find class ref and field IDs.
   * class refs must be explicitly stated as global.
   * field IDs could change with a new classloader, but this method will get called in that case.
   * see http://www.milk.com/kodebase/dalvik-docs-mirror/docs/jni-tips.html
   */

  jclass myClass = env->FindClass("io/card/payment/CardScanner");
  if (!myClass) {
    dmz_error_log("Couldn't find CardScanner from JNI");
    return -1;
  }
  cardScannerId.classRef = (jclass)env->NewGlobalRef(myClass);
  cardScannerId.edgeUpdateCallback = env->GetMethodID(myClass, "onEdgeUpdate", "(Lio/card/payment/DetectionInfo;)V");
  if (!cardScannerId.edgeUpdateCallback) {
    dmz_error_log("Couldn't find edge update callback");
    return -1;
  }

  jclass rectClass = env->FindClass("android/graphics/Rect");
  if (!rectClass) {
    dmz_error_log("Couldn't find Rect class");
    return -1;
  }
  rectId.classRef = (jclass)env->NewGlobalRef(rectClass);
  rectId.top = env->GetFieldID(rectClass, "top", "I");
  rectId.bottom = env->GetFieldID(rectClass, "bottom", "I");
  rectId.left = env->GetFieldID(rectClass, "left", "I");
  rectId.right = env->GetFieldID(rectClass, "right", "I");

  if (!(rectId.top && rectId.bottom && rectId.left && rectId.right)) {
    dmz_error_log("Couldn't find square class");
    return -1;
  }

  jclass creditCardClass = (jclass)env->FindClass("io/card/payment/CreditCard");
  if (creditCardClass == NULL) {
    dmz_error_log("Couldn't find CreditCard class");
    return -1;
  }
  creditCardId.classRef = (jclass)env->NewGlobalRef(creditCardClass);
  creditCardId.flipped = env->GetFieldID(creditCardClass, "flipped", "Z");
  creditCardId.yoff = env->GetFieldID(creditCardClass, "yoff", "I");
  creditCardId.xoff = env->GetFieldID(creditCardClass, "xoff", "[I");
  if (!( creditCardId.flipped && creditCardId.yoff && creditCardId.xoff )) {
    dmz_error_log("at least one filed was not found for CreditCard");
    return -1;
  }

  jclass dInfoClass = env->FindClass("io/card/payment/DetectionInfo");
  if (dInfoClass == NULL) {
    dmz_error_log("Couldn't find DetectionInfo class");
    return -1;
  }
  detectionInfoId.classRef = (jclass)env->NewGlobalRef(dInfoClass);
  detectionInfoId.complete = env->GetFieldID(dInfoClass, "complete", "Z");
  detectionInfoId.topEdge = env->GetFieldID(dInfoClass, "topEdge", "Z");
  detectionInfoId.bottomEdge = env->GetFieldID(dInfoClass, "bottomEdge", "Z");
  detectionInfoId.leftEdge =  env->GetFieldID(dInfoClass, "leftEdge", "Z");
  detectionInfoId.rightEdge = env->GetFieldID(dInfoClass, "rightEdge", "Z");
  detectionInfoId.focusScore = env->GetFieldID(dInfoClass, "focusScore", "F");
  detectionInfoId.prediction = env->GetFieldID(dInfoClass, "prediction", "[I");
  detectionInfoId.expiry_month = env->GetFieldID(dInfoClass, "expiry_month", "I");
  detectionInfoId.expiry_year = env->GetFieldID(dInfoClass, "expiry_year", "I");
  detectionInfoId.detectedCard = env->GetFieldID(dInfoClass, "detectedCard", "Lio/card/payment/CreditCard;");

  if (!(detectionInfoId.complete && detectionInfoId.topEdge && detectionInfoId.bottomEdge
        && detectionInfoId.leftEdge && detectionInfoId.rightEdge
        && detectionInfoId.focusScore && detectionInfoId.prediction
        && detectionInfoId.expiry_month && detectionInfoId.expiry_year
        && detectionInfoId.detectedCard
       )) {
    dmz_error_log("at least one field was not found for DetectionInfo");
    return -1;
  }

  return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL Java_io_card_payment_CardScanner_nSetup(JNIEnv *env, jobject thiz,
    jboolean shouldOnlyDetectCard, jfloat jMinFocusScore) {
  dmz_debug_log("Java_io_card_payment_CardScanner_nSetup");
  dmz_trace_log("dmz trace enabled");


  detectOnly = shouldOnlyDetectCard;
  minFocusScore = jMinFocusScore;
  flipped = false;
  lastFrameWasUsable = false;

  if (dmz == NULL) {
    dmz = dmz_context_create();
    scanner_initialize(&scannerState);
  }
  else {
    scanner_reset(&scannerState);
  }
  dmz_refcount++;

  cvSetErrMode(CV_ErrModeParent);
}

extern "C"
JNIEXPORT void JNICALL Java_io_card_payment_CardScanner_nResetAnalytics(JNIEnv *env, jobject thiz) {
  scanner_reset(&scannerState);
}

extern "C"
JNIEXPORT void JNICALL Java_io_card_payment_CardScanner_nCleanup(JNIEnv *env, jobject thiz) {
  dmz_debug_log("Java_io_card_payment_CardScanner_nCleanup");

  if (dmz_refcount == 1) {
    scanner_destroy(&scannerState);
    dmz_context_destroy(dmz);
    dmz = NULL;
  }
  dmz_refcount--;
}

extern "C"
JNIEXPORT void JNICALL Java_io_card_payment_CardScanner_nGetGuideFrame(JNIEnv *env, jobject thiz,
    jint orientation, jint width, jint height, jobject rect)
{
  dmz_trace_log("Java_io_card_payment_CardScanner_nGetGuideFrame");

  dmz_rect dr = dmz_guide_frame(orientation, width, height);

  env->SetIntField(rect, rectId.top, dr.y);
  env->SetIntField(rect, rectId.left, dr.x);
  env->SetIntField(rect, rectId.bottom, dr.y + dr.h);
  env->SetIntField(rect, rectId.right, dr.x + dr.w);
}

void updateEdgeDetectDisplay(JNIEnv* env, jobject thiz, jobject dinfo, dmz_edges found_edges) {
  env->SetBooleanField(dinfo, detectionInfoId.topEdge, found_edges.top.found);
  env->SetBooleanField(dinfo, detectionInfoId.bottomEdge, found_edges.bottom.found);
  env->SetBooleanField(dinfo, detectionInfoId.leftEdge, found_edges.left.found);
  env->SetBooleanField(dinfo, detectionInfoId.rightEdge, found_edges.right.found);

  env->CallVoidMethod(thiz, cardScannerId.edgeUpdateCallback, dinfo);
}

void setScanCardNumberResult(JNIEnv* env, jobject dinfo, ScannerResult* scanResult) {

  jint numbers[16];
  jint offsets[16];
  for (int i = 0; i < scanResult->n_numbers; i++) {
    numbers[i] = scanResult->predictions(i);
    dmz_debug_log("prediction[%i]= %i", i, scanResult->predictions(i));
    offsets[i] = scanResult->hseg.offsets[i];
    dmz_debug_log("offsets[%i]= %i", i, scanResult->hseg.offsets[i]);
  }

  jobject digitArray = env->GetObjectField(dinfo, detectionInfoId.prediction);
  dmz_debug_log("setting prediction array region");
  env->SetIntArrayRegion((jintArray)digitArray, 0, scanResult->n_numbers, numbers);

  jobject cardObj = env->GetObjectField(dinfo, detectionInfoId.detectedCard);
  dmz_debug_log("got cardObj: %x", cardObj);
  env->SetIntField(cardObj, creditCardId.yoff, scanResult->vseg.y_offset);

  jobject xoffArray = env->GetObjectField(cardObj, creditCardId.xoff);
  dmz_debug_log("setting xoffset array region: %x", xoffArray);
  env->SetIntArrayRegion((jintArray)xoffArray, 0, scanResult->n_numbers, offsets);

  dmz_debug_log("setting expiry to %i/%i", scanResult->expiry_month, scanResult->expiry_year);
  env->SetIntField(dinfo, detectionInfoId.expiry_month, scanResult->expiry_month);
  env->SetIntField(dinfo, detectionInfoId.expiry_year, scanResult->expiry_year);

  dmz_debug_log("setting detectionInfoId.complete=true");
  env->SetBooleanField(dinfo, detectionInfoId.complete, true);

  dmz_debug_log("done in setScanCardNumberResult()");
}

void logDinfo(JNIEnv* env, jobject dinfo) {
  dmz_debug_log("dinfo: complete=%i", env->GetBooleanField(dinfo, detectionInfoId.complete));

  jintArray digitArray = (jintArray) env->GetObjectField(dinfo, detectionInfoId.prediction);
  dmz_debug_log("dinfo: prediction[0-3]=%i%i%i%i...",
                env->GetIntArrayElements(digitArray, NULL)[0],
                env->GetIntArrayElements(digitArray, NULL)[1],
                env->GetIntArrayElements(digitArray, NULL)[2],
                env->GetIntArrayElements(digitArray, NULL)[3]);
}

void setDetectedCardImage(JNIEnv* env, jobject jCardResultBitmap, IplImage* cardY, IplImage* cb, IplImage* cr,
                          dmz_corner_points corner_points, int orientation) {

  char* pixels = NULL;

  AndroidBitmapInfo  bmInfo;
  int bmRes = AndroidBitmap_getInfo(env, jCardResultBitmap, &bmInfo);
  // Yes, it really is defined as _RESUT_ ... figures. <sigh>
  bool validCardInfo = (bmRes == ANDROID_BITMAP_RESUT_SUCCESS);
  if (!validCardInfo) {
    dmz_error_log("AndroidBitmap_getInfo() failed! error=%i", bmRes);
  }
  if (validCardInfo && bmInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    dmz_error_log("the dmz was given a bitmap that is not RGBA_8888");
    validCardInfo = false;
  }

  bmRes = AndroidBitmap_lockPixels(env, jCardResultBitmap, (void**) &pixels );
  if (bmRes != ANDROID_BITMAP_RESUT_SUCCESS) {
    dmz_error_log("couldn't lock bitmap:%i", bmRes);
  }
  else {
    IplImage* bigCb = NULL;
    dmz_transform_card(NULL, cb, corner_points, orientation, true, &bigCb);

    IplImage* bigCr = NULL;
    dmz_transform_card(NULL, cr, corner_points, orientation, true, &bigCr);

    IplImage* cardResult = cvCreateImageHeader(cvSize(bmInfo.width, bmInfo.height), IPL_DEPTH_8U, 4);
    cvSetData(cardResult, pixels, bmInfo.stride);

    dmz_YCbCr_to_RGB(cardY, bigCb, bigCr, &cardResult);
    AndroidBitmap_unlockPixels(env, jCardResultBitmap);

    cvReleaseImageHeader(&cardResult);
    cvReleaseImage(&bigCb);
    cvReleaseImage(&bigCr);
  }
}

/* This method forms the core of card.io scanning. All others (nCardDetected & nGetFocusScore) */
extern "C"
JNIEXPORT void JNICALL Java_io_card_payment_CardScanner_nScanFrame(JNIEnv *env, jobject thiz,
    jbyteArray jb, jint width, jint height, jint orientation, jobject dinfo,
    jobject jCardResultBitmap, jboolean jScanExpiry) {
  dmz_trace_log("Java_io_card_payment_CardScanner_nScanFrame ... width:%i height:%i orientation:%i", width, height, orientation);

  if (orientation == 0) {
    dmz_error_log("orientation is 0. Nothing good can come from this.");
    return;
  }

  if (flipped) {
    orientation = dmz_opposite_orientation(orientation);
  }

  FrameScanResult result;

  IplImage *image = cvCreateImageHeader(cvSize(width, height), IPL_DEPTH_8U, 1);
  jbyte *jBytes = env->GetByteArrayElements(jb, 0);
  image->imageData = (char *)jBytes;

  float focusScore = dmz_focus_score(image, false);
  env->SetFloatField(dinfo, detectionInfoId.focusScore, focusScore);
  dmz_trace_log("focus score: %f", focusScore);
  if (focusScore >= minFocusScore) {

    IplImage *cbcr = cvCreateImageHeader(cvSize(width / 2, height / 2), IPL_DEPTH_8U, 2);
    cbcr->imageData = ((char *)jBytes) + width * height;
    IplImage *cb, *cr;

    // Note: cr and cb are reversed here because Android uses android.graphics.ImageFormat.NV21. This is actually YCrCb rather than YCbCr!
    dmz_deinterleave_uint8_c2(cbcr, &cr, &cb);

    cvReleaseImageHeader(&cbcr);

    dmz_edges found_edges;
    dmz_corner_points corner_points;
    bool cardDetected = dmz_detect_edges(image, cb, cr,
                                         orientation,
                                         &found_edges, &corner_points
                                        );

    updateEdgeDetectDisplay(env, thiz, dinfo, found_edges);

    if (cardDetected) {
      IplImage *cardY = NULL;
      dmz_transform_card(NULL, image, corner_points, orientation, false, &cardY);

      if (!detectOnly) {
        result.focus_score = focusScore;
        result.flipped = flipped;
        scanner_add_frame_with_expiry(&scannerState, cardY, jScanExpiry, &result);
        if (result.usable) {
          ScannerResult scanResult;
          scanner_result(&scannerState, &scanResult);

          if (scanResult.complete) {
            setScanCardNumberResult(env, dinfo, &scanResult);
            logDinfo(env, dinfo);
          }
        }
        else if (result.upside_down) {
          flipped = !flipped;
        }
      }

      setDetectedCardImage(env, jCardResultBitmap, cardY, cb, cr, corner_points, orientation);
      cvReleaseImage(&cardY);
    }

    cvReleaseImage(&cb);
    cvReleaseImage(&cr);
  }

  cvReleaseImageHeader(&image);
  env->ReleaseByteArrayElements(jb, jBytes, 0);
}

extern "C"
JNIEXPORT jint JNICALL Java_io_card_payment_CardScanner_nGetNumFramesScanned(JNIEnv *env, jobject thiz) {
  return scannerState.session_analytics.num_frames_scanned;
}




