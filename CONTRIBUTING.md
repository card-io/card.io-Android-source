# Contribute to the card.io Android SDK

### *Pull requests are welcome!*


General Guidelines
------------------

* **Code style.** Please follow local code style. Ask if you're unsure. 
* **No warnings.** All generated code must compile without warnings. 
* **Android version support.** The library should support versions of Android that have more than 5% distribution, as measured by [Google's dashboard](https://developer.android.com/about/dashboards/index.html). No versions should be dropped without good reason.
* **Architecture support.** Scanning should support ARMv7a, with and without NEON vector instruction support. Other architectures may be added.
* **Testing.** Test both with the `buffalo` demo app, and with the included `SampleApp`. Test at least on one physical device and report the device model & OS tested on.
