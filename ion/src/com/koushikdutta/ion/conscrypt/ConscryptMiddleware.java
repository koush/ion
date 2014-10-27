package com.koushikdutta.ion.conscrypt;

import android.content.Context;

import com.koushikdutta.async.AsyncSSLSocketWrapper;
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

    static void initialize(Context context) {
        try {
            synchronized (lock) {
                if (initialized)
                    return;
                initialized = true;
                SSLContext originalDefault = SSLContext.getDefault();
                Context gms = context.createPackageContext("com.google.android.gms", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                gms
                .getClassLoader()
                .loadClass("com.google.android.gms.common.security.ProviderInstallerImpl")
                .getMethod("insertProvider", Context.class)
                .invoke(null, context);

                Provider[] providers = Security.getProviders();
                for (Provider provider: providers) {
                    if (GMS_PROVIDER.equals(provider.getName())) {
                        Security.removeProvider(GMS_PROVIDER);
                        Security.insertProviderAt(provider, providers.length);
                        SSLContext.setDefault(originalDefault);
                        break;
                    }
                }
                success = true;
            }
        }
        catch (Exception e) {
        }
    }

    private static final String GMS_PROVIDER = "GmsCore_OpenSSL";

    public void initialize() {
        initialize(context);
        if (success && !instanceInitialized && enabled) {
            instanceInitialized = true;
            try {
                SSLContext sslContext = null;
                try {
                    sslContext = SSLContext.getInstance("TLS", GMS_PROVIDER);
                }
                catch (Exception e) {
                }
                if (sslContext == null)
                    sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
                if (middleware.getSSLContext() != AsyncSSLSocketWrapper.getDefaultSSLContext())
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
        if (!enabled) {
            return null;
        }
        // initialize here vs the constructor, or this will potentially block the ui thread.
        initialize();
        return super.getSocket(data);
    }
}
