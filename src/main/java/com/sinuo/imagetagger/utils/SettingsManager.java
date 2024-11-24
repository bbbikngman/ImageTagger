package com.sinuo.imagetagger.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREFS_NAME = "ImageTaggerSettings";

    /**
     * Save a setting to SharedPreferences
     * @param context Application context
     * @param key Setting key
     * @param value Setting value
     */
    public static void saveSetting(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Retrieve a setting from SharedPreferences
     * @param context Application context
     * @param key Setting key
     * @param defaultValue Default value if setting doesn't exist
     * @return The stored setting value or defaultValue if not found
     */
    public static String getSetting(Context context, String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, defaultValue);
    }

    /**
     * Clear all settings
     * @param context Application context
     */
    public static void clearAllSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Remove a specific setting
     * @param context Application context
     * @param key Setting key to remove
     */
    public static void removeSetting(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }
}