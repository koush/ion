package com.koushikdutta.ion;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by koush on 4/15/14.
 */
public abstract class ContextReference<T> extends WeakReference<T> implements IonContext{
    public ContextReference(T context) {
        super(context);
    }

    abstract static class NormalContextReference<T extends Context> extends ContextReference<T> {
        NormalContextReference(T context) {
            super(context);
        }

        static String isAlive(Context context) {
            if (context instanceof Service)
                return ServiceContextReference.isAlive((Service) context);
            if (context instanceof Activity)
                return ActivityContextReference.isAlive((Activity) context);
            return null;
        }

        @Override
        public Context getContext() {
            return get();
        }
    }

    public static class ServiceContextReference extends com.koushikdutta.ion.ContextReference.NormalContextReference<Service> {
        public ServiceContextReference(Service service) {
            super(service);
        }

        static String isAlive(Service candidate) {
            if (candidate == null)
                return "Service reference null";
            ActivityManager manager = (ActivityManager)candidate.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
            if (services == null)
                return "Could not retrieve services from service manager";
            for (ActivityManager.RunningServiceInfo service: services) {
                if (candidate.getClass().getName().equals(service.service.getClassName())) {
                    return null;
                }
            }
            return "Service stopped";
        }

        @Override
        public String isAlive() {
            return isAlive(get());
        }
    }

    public static class ActivityContextReference extends com.koushikdutta.ion.ContextReference.NormalContextReference<Activity> {
        public ActivityContextReference(Activity activity) {
            super(activity);
        }

        static String isAlive(Activity a) {
            if (a == null)
                return "Activity reference null";
            if (a.isFinishing())
                return "Activity finished";
            return null;
        }

        @Override
        public String isAlive() {
            return isAlive(get());
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public static class FragmentContextReference extends ContextReference<Fragment> {
        public FragmentContextReference(Fragment fragment) {
            super(fragment);
        }

        @Override
        public Context getContext() {
            Fragment fragment = get();
            if (fragment == null)
                return null;
            return fragment.getActivity();
        }

        @Override
        public String isAlive() {
            Fragment fragment = get();
            if (fragment == null)
                return "Fragment reference null";
            String ret = ActivityContextReference.isAlive(fragment.getActivity());
            if (ret != null)
                return ret;
            if (fragment.isDetached())
                return "Fragment detached";
            return null;
        }
    }

    public static class SupportFragmentContextReference extends ContextReference<androidx.fragment.app.Fragment> {
        public SupportFragmentContextReference(androidx.fragment.app.Fragment fragment) {
            super(fragment);
        }

        @Override
        public Context getContext() {
            androidx.fragment.app.Fragment fragment = get();
            if (fragment == null)
                return null;
            return fragment.getActivity();
        }

        @Override
        public String isAlive() {
            androidx.fragment.app.Fragment fragment = get();
            if (fragment == null)
                return "Fragment reference null";
            String ret = ActivityContextReference.isAlive(fragment.getActivity());
            if (ret != null)
                return ret;
            if (fragment.isDetached())
                return "Fragment detached";
            return null;
        }
    }

    public static class ImageViewContextReference extends ContextReference<ImageView> {
        public ImageViewContextReference(ImageView imageView) {
            super(imageView);
        }

        @Override
        public String isAlive() {
            ImageView iv = get();
            if (iv == null)
                return "ImageView reference null";
            return NormalContextReference.isAlive(iv.getContext());
        }

        @Override
        public Context getContext() {
            ImageView iv = get();
            if (iv == null)
                return null;
            return iv.getContext();
        }
    }

    public static ContextReference fromContext(Context context) {
        if (context instanceof Service)
            return new com.koushikdutta.ion.ContextReference.ServiceContextReference((Service)context);
        if (context instanceof Activity)
            return new com.koushikdutta.ion.ContextReference.ActivityContextReference((Activity)context);

        return new com.koushikdutta.ion.ContextReference.NormalContextReference<Context>(context) {
            @Override
            public String isAlive() {
                Context context = get();
                if (context == null)
                    return "Context reference null";
                return null;
            }
        };
    }
}
