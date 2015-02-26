# card.io Android NDK makefile
#
# See the file "LICENSE.md" for the full license governing this code.
#
# Builds three targets:
# - Static library for ARMv7 with NEON vector coprocessor (most devices)
# - Static library for ARMv7 without coprocessor. (NVidia Tegra2 processor, maybe others)
# - Very small static library to help figure out compatibility & work around some Android 4.0 bugs.

LOCAL_PATH := $(call my-dir)
LOCAL_DMZ_DIR := card.io-dmz

# --- declare opencv prebuilt static libs ---------------------------------
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_core
LOCAL_SRC_FILES := lib/libopencv_core.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_imgproc
LOCAL_SRC_FILES := lib/libopencv_imgproc.so
LOCAL_SHARED_LIBRARIES := opencv_core 
include $(PREBUILT_SHARED_LIBRARY)

endif

# --- libcardioRecocognizer.so -------------------------------------------------------- 
# (neon version)

ifeq (1,1)

include $(CLEAR_VARS)
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

LOCAL_MODULE  := cardioRecognizer
LOCAL_LDLIBS := -llog -L$(SYSROOT)/usr/lib -lz -ljnigraphics
LOCAL_SHARED_LIBRARIES := cpufeatures opencv_imgproc opencv_core 

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LOCAL_DMZ_DIR) $(LOCAL_PATH)/$(LOCAL_DMZ_DIR)/cv
LOCAL_SRC_FILES := $(LOCAL_DMZ_DIR)/dmz_all.cpp nativeRecognizer.cpp

LOCAL_CPPFLAGS := -DANDROID_HAS_NEON=1 -DSCAN_EXPIRY=1
LOCAL_ARM_NEON := true

include $(BUILD_SHARED_LIBRARY)
endif

endif

# build tegra compatible lib
# (no neon, limit to 16 VFP registers)

ifeq (1,1)

include $(CLEAR_VARS)
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)

LOCAL_MODULE  := cardioRecognizer_tegra2
LOCAL_LDLIBS := -llog -L$(SYSROOT)/usr/lib -lz -ljnigraphics
LOCAL_SHARED_LIBRARIES := cpufeatures opencv_imgproc opencv_core 

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LOCAL_DMZ_DIR) $(LOCAL_PATH)/$(LOCAL_DMZ_DIR)/cv
LOCAL_SRC_FILES := $(LOCAL_DMZ_DIR)/dmz_all.cpp nativeRecognizer.cpp

# Note: setting -mfloat-abi=hard will generate libs that cannot be linked with built in Android ones. So don't.
LOCAL_CPPFLAGS := -DANDROID_HAS_NEON=0 -mfpu=vfpv3-d16
LOCAL_ARM_NEON := false

include $(BUILD_SHARED_LIBRARY)

endif

endif

# --- libcardioDecider.so ------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := cardioDecider
LOCAL_LDLIBS := -llog 
LOCAL_SHARED_LIBRARIES := cpufeatures

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LOCAL_DMZ_DIR) 
LOCAL_SRC_FILES := $(LOCAL_DMZ_DIR)/processor_support.cpp nativeDecider.cpp

LOCAL_CFLAGS := -DANDROID_DMZ=1 -DANDROID_HAS_NEON=1

include $(BUILD_SHARED_LIBRARY)

# ------------

$(call import-module,android/cpufeatures)

