package com.koushikdutta.ion.conscrypt;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;
import com.koushikdutta.async.AsyncSSLSocketWrapper;
import com.koushikdutta.async.future.Cancellable;
import com.koushikdutta.async.http.AsyncSSLSocketMiddleware;
import com.koushikdutta.async.http.SimpleMiddleware;

import java.security.Provider;
import java.security.Security;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by koush on 7/13/14.
 */
public class ConscryptMiddleware extends SimpleMiddleware {
    final static Object lock = new Object();
    static boolean initialized;
    static boolean success;
    boolean instanceInitialized;
    boolean enabled = true;

    private static final String LOGTAG = "IonConscrypt";

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

                // GMS Conscrypt is already initialized, from outside ion. Leave it alone.
                if (Security.getProvider(ProviderInstaller.PROVIDER_NAME) != null) {
                    success = true;
                    return;
                }

                SSLContext originalDefaultContext = SSLContext.getDefault();
                SSLSocketFactory originalDefaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

                ProviderInstaller.installIfNeeded(context);

                Provider[] providers = Security.getProviders();
                Provider provider = Security.getProvider(ProviderInstaller.PROVIDER_NAME);
                Security.removeProvider(ProviderInstaller.PROVIDER_NAME);
                Security.insertProviderAt(provider, providers.length);
                SSLContext.setDefault(originalDefaultContext);
                HttpsURLConnection.setDefaultSSLSocketFactory(originalDefaultSSLSocketFactory);
                success = true;
            }
        }
        catch (Throwable e) {
            Log.w(LOGTAG, "Conscrypt initialization failed.", e);
        }
    }

    public void initialize() {
        initialize(context);
        if (success && !instanceInitialized && enabled) {
            instanceInitialized = true;
            try {
                SSLContext sslContext = null;
                try {
                    sslContext = SSLContext.getInstance("TLS", ProviderInstaller.PROVIDER_NAME);
                }
                catch (Exception e) {
                }
                if (sslContext == null)
                    sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null);
                // only set the SSL context if it is the default SSL context
                if (middleware.getSSLContext() == AsyncSSLSocketWrapper.getDefaultSSLContext())
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
