package com.koushikdutta.ion;

import android.content.Context;

import java.util.concurrent.CancellationException;

/**
 * Created by koush on 7/2/17.
 */

public interface IonContext {
    String isAlive();
    Context getContext();

    default void ensureAlive() {
        String alive = isAlive();
        if (alive != null)
            throw new CancellationException(alive);
    }
}
