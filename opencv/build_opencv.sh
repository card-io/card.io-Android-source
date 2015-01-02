#!/bin/bash

cd `dirname $0`
WD=`pwd`

CV_SUBMODULE_DIR="../ThirdParty/opencv"
CV_VERSION="2.4.2"

if [ -d "$CV_SUBMODULE_DIR" ] ; then
	cd $CV_SUBMODULE_DIR
	
	CV_VERSION=`git describe --always --dirty --tags`
	CV_SRC=`pwd`
	
	cd $WD
	
	rm -rf "*-dirty"
else
	unset CV_SUBMODULE_DIR
fi

CV_NAME="OpenCV-$CV_VERSION"
BUILD_DIR="build-$CV_VERSION"

if [ ! "$CV_SRC" ] ; then
	CV_SRC="$WD/$CV_NAME"
	
	CV_ZIP="$CV_NAME.tar.bz2"
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
	    tar -xjf $CV_ZIP || exit -1
	fi

fi

mkdir -p $BUILD_DIR
cd $BUILD_DIR

cmake -C ../CMakeCache.android.initial.cmake -DANDROID_ABI="armeabi-v7a" \
  -DCMAKE_TOOLCHAIN_FILE=$CV_SRC/android/android.toolchain.cmake \
  -DLIBRARY_OUTPUT_PATH="$WD/../../card.io/src/main/jni/lib" \
  $CV_SRC || exit -1

# we could specify which libs to make in the cmake args, or we could just build them manually.
make opencv_core -j16 || exit -1
make opencv_imgproc -j16 || exit -1