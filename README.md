[![card.io logo](https://raw.githubusercontent.com/card-io/press-kit/master/card_io_logo_200.png "card.io")](https://www.card.io)

Credit card scanning for Android apps
=====================================

This repository contains everything needed to build the [**card.io**](https://card.io) library for Android.

What it does not yet contain is much in the way of documentation. :crying_cat_face: So please feel free to ask any questions by creating github issues -- we'll gradually build our documentation based on the discussions there.

Note that this is actual production code, which has been iterated upon by multiple developers over several years. If you see something that could benefit from being tidied up, rewritten, or otherwise improved, your Pull Requests will be welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

Brought to you by  
[![PayPal logo](https://raw.githubusercontent.com/card-io/card.io-iOS-source/master/Resources/pp_h_rgb.png)](https://paypal.com/ "PayPal")


Using card.io
-------------

If you merely wish to incorporate **card.io** within your Android app, simply download the latest official release from https://github.com/card-io/card.io-Android-SDK. That repository includes complete integration instructions and sample code.

Dev Setup
---------

### Prerequisites

- Current version of the Android SDK. (obviously)
- Android NDK. We've tested with r10d. At minimum, the Clang toolchain is required.

### First build

There are a few bugs in the build process, so these steps are required for the first build:

1. `$ cd card.io-Android-source/card.io`
2. `$ android update project -p .`
3. Assuming you've defined `$ANDROID_NDK` correctly, `$ echo "ndk.dir=$ANDROID_NDK" >>local.properties`
4. `$ ./gradelw build` 

#### Hints & tricks.
- Get ant going before Eclipse.
- Make sure that you do not have Eclipse auto-building while an Ant build is running. (You can also close the project within Eclipse to avoid this.)
- See [card.io/jni](card.io/jni/) for native layer (NDK) discussion.

### Testing

#### Setup
Requires a recording of a capture session. 

1. Connect an Android 4.0 (or better) device. (You can also use a device back to Gingerbread, but some autotests won't work).
2. Load a card recording by running:
	`$ adb push test-data/recording_320455133.550273.zip /storage/sdcard0/card_recordings/recording_320455133.550273.zip`

#### Running

1. `$ cd card.io-Android-source/card.io-test`
2. `$ ant debug && ant installd test`

That's it! You should see the app open and run through some tests.

### Un-official Release

`$ ant dist` Cleans and builds a zip for distribution

The [official release process](official-release-process.md) is described separately.

Contributors
------------

**card.io** was created by [Josh Bleecher Snyder](https://github.com/josharian/).

Subsequent help has come from [Brent Fitzgerald](https://github.com/burnto/), [Tom Whipple](https://github.com/tomwhipple), [Dave Goldman](https://github.com/dgoldman-ebay), [Jeff Brateman](https://github.com/braebot), [Roman Punskyy](https://github.com/romk1n), and [Matt Jacunski](https://github.com/mattjacunski).

And from **you**! Pull requests and new issues are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.



