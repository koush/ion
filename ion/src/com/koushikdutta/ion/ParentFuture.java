package com.koushikdutta.ion;

import com.koushikdutta.async.future.*;

import java.util.ArrayList;

/**
 * Created by koush on 5/25/13.
 */
class ParentFuture<T> extends SimpleFuture<T> {
    FutureCallback<T> callbackFan = new FutureCallback<T>() {
        @Override
        public void onCompleted(Exception e, T result) {
            ArrayList<SimpleFuture<T>> futures;
            synchronized (ParentFuture.this) {
                futures = ParentFuture.this.callbacks;
                ParentFuture.this.callbacks = null;
            }
            if (futures == null)
                return;
            for (SimpleFuture<T> future: futures) {
                if (e != null)
                    future.setComplete(e);
                else
                    future.setComplete(result);
            }
        }
    };

    private <F extends SimpleFuture<T>> F setupChild(F future) {
        future.setParent(this);
        synchronized (this) {
            if (callbacks == null)
                callbacks = new ArrayList<SimpleFuture<T>>();
            callbacks.add(future);
        }
        // invoke the callbacks if necessary
        super.setCallback(callbackFan);

        return future;
    }

    ArrayList<SimpleFuture<T>> callbacks;
    public ParentFuture<T> addChildFuture(SimpleFuture<T> future) {
        setupChild(future);
        return this;
    }

    @Override
    public boolean cancel() {
        synchronized (this) {
            // dont allow cancelling if there are still dependents
            if (hasLiveFutures())
                return false;

            // cancel is possible, so remove the callbacks and call the underlying cancel implementation.
            callbacks = null;
            return super.cancel();
        }
    }

    // check the list of child callbacks to see if any exist.
    // remove cancelled ones.
    boolean hasLiveFutures() {
        if (callbacks == null)
            return false;
        ArrayList<SimpleFuture<T>> futures = this.callbacks;
        this.callbacks = null;
        for (SimpleFuture<T> future: futures) {
            if (!future.isCancelled()) {
                if (this.callbacks == null)
                    this.callbacks = new ArrayList<SimpleFuture<T>>();
                this.callbacks.add(future);
            }
        }
        return this.callbacks != null;
    }

    @Override
    public ParentFuture<T> setCallback(FutureCallback <T> callback) {
        throw new IllegalArgumentException("use getChildFuture");
    }
}
