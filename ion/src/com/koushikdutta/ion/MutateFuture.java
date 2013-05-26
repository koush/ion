package com.koushikdutta.ion;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.SimpleFuture;

/**
 * Created by koush on 5/25/13.
 */
abstract class MutateFuture<T, F> extends ParentFuture<T> implements FutureCallback<F> {
    public void setMutateFuture(Future<F> future) {
        future.setCallback(this);
        setParent(future);
    }
    public SimpleFuture<F> createMutateFuture() {
        SimpleFuture<F> ret = new SimpleFuture<F>();
        setMutateFuture(ret);
        return ret;
    }
}
