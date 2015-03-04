
/* nativeDecider.cpp
 * See the file "LICENSE.md" for the full license governing this code.
 */

#include <jni.h>

#include "processor_support.h"

#define DEBUG_TAG "native"

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_6;
}

extern "C" jboolean JNICALL Java_io_card_payment_CardScanner_nUseNeon() {
  return (dmz_has_neon_runtime());
}

extern "C" jboolean JNICALL Java_io_card_payment_CardScanner_nUseTegra() {
  return (dmz_use_vfp3_16());
}
