package com.winlator.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月07日 17:34
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class PreferenceManager {

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
                getDefaultSharedPreferencesMode());
    }

    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    private static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

}
