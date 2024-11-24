package com.sinuo.imagetagger.utils;

import android.content.Context;

public class ContextUtils {
    private static Context appContext;

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Context getMyApplicationContext() {
        if (appContext == null) {
            throw new IllegalStateException("ContextUtil has not been initialized. Call initialize() first.");
        }
        return appContext;
    }
}
