package com.koushikdutta.ion;

import android.graphics.Point;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.bitmap.BitmapInfo;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

abstract class BitmapCallback {
    String key;
    Ion ion;

    protected BitmapCallback(Ion ion, String key, boolean put) {
        this.key = key;
        this.put = put;
        this.ion = ion;

        ion.bitmapsPending.tag(key, this);
    }

    boolean put;

    boolean put() {
        return put;
    }

    protected void onReported() {
        ion.processDeferred();
    }

    protected void report(final Exception e, final BitmapInfo info) {
        AsyncServer.post(Ion.mainHandler, new Runnable() {
            @Override
            public void run() {
                BitmapInfo result = info;
                if (result == null) {
                    // cache errors, unless they were cancellation exceptions
                    result = new BitmapInfo(key, null, null, new Point());
                    result.exception = e;
                    if (!(e instanceof CancellationException))
                        ion.getBitmapCache().put(result);
                } else if (put()) {
                    ion.getBitmapCache().put(result);
                }

                final ArrayList<FutureCallback<BitmapInfo>> callbacks = ion.bitmapsPending.remove(key);
                if (callbacks == null || callbacks.size() == 0) {
                    onReported();
                    return;
                }

                for (FutureCallback<BitmapInfo> callback : callbacks) {
                    callback.onCompleted(e, result);
                }
                onReported();
            }
        });
    }
}
