# Native layer

The core computer vision code is in the DMZ, and is shared across iOS and Android.

## Architectures

Android has the unique feature of offering multiple processors, each with potentially different configurations. 

For the ARM architecture we support ARMv7a processors, both with NEON and VFPv3-d16 configurations. NEON SIMD configurations are a superset of VFPv3. NEON configurations have 32 VFP registers, while other non-NEON (notably those based on nVidia Tegra2) have only 16 VFP registers. Since we want the performance provided by NEON (if available), we ship two versions of our native layer. Our libs use OpenCV, but only lightly, so both link to shared VFPv3-d16 OpenCV libraries.

## OpenCV

Compiled OpenCV libs (libopencv_core.so and libopencv_imgproc.so) are kept in git, so rebuilding OpenCV is not strictly necessary. However, should you wish to do so, simply run 'card.io-Android-source/opencv/build_opencv.sh'. The script will, if necessary, download, build, install the libs into their place in 'card.io-Android-source/card.io/src/main/jni'.

## Building

### Prerequisites

- Android NDK tools installed.
- Clang toolchain

### Compile

If everything is set up properly, the NDK build will be invoked by gradle when card.io is built. But for debugging, you can use `./gradlew buildNative` to kick off just this portion of the build.