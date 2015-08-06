#!/bin/bash

cd `dirname $0`
WD=`pwd`

CV_SUBMODULE_DIR="$WD/../ThirdParty/opencv"
CV_VERSION="2.4.11"

if [ -d "$CV_SUBMODULE_DIR" ] ; then
	cd $CV_SUBMODULE_DIR
	
	CV_VERSION=`git describe --always --dirty --tags`
	CV_SRC=`pwd`
	
	cd $WD
	
	rm -rf "*-dirty"
else
	unset CV_SUBMODULE_DIR
fi

CV_NAME="opencv-$CV_VERSION"
BUILD_DIR_ARM="$WD/build-$CV_VERSION-arm"
BUILD_DIR_X86="$WD/build-$CV_VERSION-x86"

ANDROID_CMAKE_FILE="android.toolchain.cmake"


if [ ! "$CV_SRC" ] ; then
	CV_SRC="$WD/$CV_NAME"
	
	CV_ZIP="$CV_NAME.zip"
	CV_URL="http://downloads.sourceforge.net/project/opencvlibrary/opencv-unix/$CV_VERSION/$CV_ZIP"

	# standard on MacOSX
	DOWNLOAD_DIR="$HOME/Downloads"

	if [ ! -d "$CV_SRC" ] ; then
	    if [ ! -f "$CV_ZIP" ] && [ -f "$DOWNLOAD_DIR/$CV_ZIP" ] ; then
	        echo "found $DOWNLOAD_DIR/$CV_ZIP"
	        CV_ZIP="$DOWNLOAD_DIR/$CV_ZIP"
	    fi
	    if [ ! -f "$CV_ZIP" ] ; then
	        echo "didn't find `basename $CV_ZIP` in $PWD or $DOWNLOAD_DIR"
	        echo "downloading OpenCV from $CV_URL"
        
	        wget $CV_URL || exit -1
	    fi
    
	    echo "unzipping $CV_ZIP"
	    unzip $CV_ZIP || exit -1
	fi
fi

ANDROID_CMAKE_URL="https://raw.githubusercontent.com/taka-no-me/android-cmake/master/android.toolchain.cmake"
wget -q $ANDROID_CMAKE_URL -O $ANDROID_CMAKE_FILE || exit -1

#building armeabi-v7a version
mkdir -p $BUILD_DIR_ARM
cd $BUILD_DIR_ARM

cmake -C "$WD/CMakeCache.android.initial.cmake" -DANDROID_ABI="armeabi-v7a" \
  -DCMAKE_TOOLCHAIN_FILE="$WD/$ANDROID_CMAKE_FILE" \
  $CV_SRC || exit -1

# we could specify which libs to make in the cmake args, or we could just build them manually.
make opencv_core -j16 || exit -1
make opencv_imgproc -j16 || exit -1

cp $BUILD_DIR_ARM/lib/armeabi-v7a/*.so $WD/../card.io/src/main/jni/lib/armeabi-v7a/

#building x86 version
mkdir -p $BUILD_DIR_X86
cd $BUILD_DIR_X86

cmake -C "$WD/CMakeCache.android.initial.cmake" -DANDROID_ABI="x86" \
  -DCMAKE_TOOLCHAIN_FILE="$WD/$ANDROID_CMAKE_FILE" \
  $CV_SRC || exit -1

# we could specify which libs to make in the cmake args, or we could just build them manually.
make opencv_core -j16 || exit -1
make opencv_imgproc -j16 || exit -1

cp $BUILD_DIR_X86/lib/x86/*.so $WD/../card.io/src/main/jni/lib/x86/