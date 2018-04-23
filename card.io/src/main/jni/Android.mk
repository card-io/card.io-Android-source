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
ifneq (,$(filter $(TARGET_ARCH_ABI),armeabi-v7a x86 arm64-v8a x86_64))

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_core
LOCAL_SRC_FILES := lib/$(TARGET_ARCH_ABI)/libopencv_core.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_imgproc
LOCAL_SRC_FILES := lib/$(TARGET_ARCH_ABI)/libopencv_imgproc.so
LOCAL_SHARED_LIBRARIES := opencv_core 
include $(PREBUILT_SHARED_LIBRARY)

endif

# --- libcardioRecognizer.so --------------------------------------------------------
# (neon version)

ifeq (1,1)

include $(CLEAR_VARS)
ifneq (,$(filter $(TARGET_ARCH_ABI),armeabi-v7a x86 arm64-v8a x86_64))

LOCAL_MODULE := cardioRecognizer
LOCAL_LDLIBS := -llog -L$(SYSROOT)/usr/lib -lz -ljnigraphics
LOCAL_SHARED_LIBRARIES := cpufeatures opencv_imgproc opencv_core 

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LOCAL_DMZ_DIR) $(LOCAL_PATH)/$(LOCAL_DMZ_DIR)/cv
LOCAL_SRC_FILES := $(LOCAL_DMZ_DIR)/dmz_all.cpp nativeRecognizer.cpp

LOCAL_CPPFLAGS := -DSCAN_EXPIRY=1

ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
LOCAL_CPPFLAGS += -DANDROID_HAS_NEON=1
LOCAL_ARM_NEON := true
endif

ifneq (,$(filter $(TARGET_ARCH_ABI), arm64-v8a x86_64))
LOCAL_CFLAGS += -DANDROID_HAS_NEON=0 ## 64-bit changed register names - requires asm fixes
endif

include $(BUILD_SHARED_LIBRARY)
endif

endif

# build tegra compatible lib
# (no neon, limit to 16 VFP registers)

ifeq (1,1)

include $(CLEAR_VARS)
ifneq (,$(filter $(TARGET_ARCH_ABI),armeabi-v7a x86 arm64-v8a x86_64))

LOCAL_MODULE := cardioRecognizer_tegra2
LOCAL_LDLIBS := -llog -L$(SYSROOT)/usr/lib -lz -ljnigraphics
LOCAL_SHARED_LIBRARIES := cpufeatures opencv_imgproc opencv_core 

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LOCAL_DMZ_DIR) $(LOCAL_PATH)/$(LOCAL_DMZ_DIR)/cv
LOCAL_SRC_FILES := $(LOCAL_DMZ_DIR)/dmz_all.cpp nativeRecognizer.cpp

ifeq ($(TARGET_ARCH_ABI), x86) #we're generating an empty libcardioRecognizer_tegra2.so for x86 devices, so the list of .so files is the same for armeabi-v7a and x86 folders. This is to avoid any fallback to arm versions.
LOCAL_C_INCLUDES :=
LOCAL_SRC_FILES :=
endif

# Note: setting -mfloat-abi=hard will generate libs that cannot be linked with built in Android ones. So don't.
LOCAL_CPPFLAGS := -DANDROID_HAS_NEON=0 -mfpu=vfpv3-d16
LOCAL_ARM_NEON := false

include $(BUILD_SHARED_LIBRARY)

endif

endif

# --- libcardioDecider.so ------------------------------------------------------------

ifneq (,$(filter $(TARGET_ARCH_ABI), armeabi armeabi-v7a mips x86 arm64-v8a x86_64))

include $(CLEAR_VARS)

LOCAL_MODULE := cardioDecider
LOCAL_LDLIBS := -llog 
LOCAL_SHARED_LIBRARIES := cpufeatures

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(LOCAL_DMZ_DIR) 
LOCAL_SRC_FILES := $(LOCAL_DMZ_DIR)/processor_support.cpp nativeDecider.cpp

LOCAL_CFLAGS := -DANDROID_DMZ=1 -DANDROID_HAS_NEON=1

include $(BUILD_SHARED_LIBRARY)
endif

# ------------

$(call import-module,android/cpufeatures)

