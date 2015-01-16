package io.card.development;

/* BuffaloApplication.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.app.Application;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

public class BuffaloApplication extends Application implements UncaughtExceptionHandler {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread arg0, Throwable arg1) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        arg1.printStackTrace(ps);
        String stackTrace = baos.toString();

        Log.i("Error Occurred:", stackTrace);
    }

}
