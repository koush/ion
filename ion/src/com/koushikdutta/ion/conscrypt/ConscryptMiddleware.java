package com.koushikdutta.ion.conscrypt;

import android.content.Context;

import com.koushikdutta.async.future.Cancellable;
import com.koushikdutta.async.http.AsyncSSLSocketMiddleware;
import com.koushikdutta.async.http.SimpleMiddleware;

import java.security.Provider;
import java.security.Security;

import javax.net.ssl.SSLContext;

/**
 * Created by koush on 7/13/14.
 */
public class ConscryptMiddleware extends SimpleMiddleware {
    final static Object lock = new Object();
    static boolean initialized;
    static boolean success;
    boolean instanceInitialized;
    boolean enabled = true;

    public void enable(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            instanceInitialized = false;
            middleware.setSSLContext(null);
        }
    }

    public static void initialize(Context context) {
        try {
            synchronized (lock) {
                if (initialized)
                    return;
                initialized = true;
                Context gms = context.createPackageContext("com.google.android.gms", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                gms
                .getClassLoader()
                .loadClass("com.google.android.gms.common.security.ProviderInstallerImpl")
                .getMethod("insertProvider", Context.class)
                .invoke(null, context);
                success = true;
            }
        }
        catch (Exception e) {
        }
    }

    private void initialize() {
        initialize(context);
        if (success && !instanceInitialized && enabled) {
            instanceInitialized = true;
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
                middleware.setSSLContext(sslContext);
            }
            catch (Exception e) {
            }
        }
    }

    AsyncSSLSocketMiddleware middleware;
    Context context;
    public ConscryptMiddleware(Context context, AsyncSSLSocketMiddleware middleware) {
        this.middleware = middleware;
        this.context = context.getApplicationContext();
    }

    @Override
    public Cancellable getSocket(GetSocketData data) {
        // initialize here vs the constructor, or this will potentially block the ui thread.
        initialize();
        return super.getSocket(data);
    }
}
