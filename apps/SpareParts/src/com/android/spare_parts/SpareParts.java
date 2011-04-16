/* //device/apps/Settings/src/com/android/settings/Keyguard.java
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.spare_parts;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceCategory;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.IWindowManager;

import java.util.List;
import java.io.File;

public class SpareParts extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "SpareParts";

    private static final String BATTERY_HISTORY_PREF = "battery_history_settings";
    private static final String BATTERY_INFORMATION_PREF = "battery_information_settings";
    private static final String USAGE_STATISTICS_PREF = "usage_statistics_settings";
    
    
    private static final String PERFORMANCE_SETTINGS_CATEGORY = "performance_settings_category";

    private static final String WINDOW_ANIMATIONS_PREF = "window_animations";
    private static final String TRANSITION_ANIMATIONS_PREF = "transition_animations";
    private static final String FANCY_IME_ANIMATIONS_PREF = "fancy_ime_animations";
    private static final String HAPTIC_FEEDBACK_PREF = "haptic_feedback";
    private static final String FONT_SIZE_PREF = "font_size";
    private static final String END_BUTTON_PREF = "end_button";
    private static final String KEY_COMPATIBILITY_MODE = "compatibility_mode";
    private static final String MEMCTL_STATE_PREF = "memctl_state";
    private static final String MEMCTL_SIZE_PREF = "memctl_size";
    private static final String MEMCTL_SWP_PREF = "memctl_swp";
    
    private static final String COMPCACHE_PREF = "persist.system.compcache";

    private static final String COMPCACHE_PERSIST_PROP = "persist.service.compcache";

    private static final String COMPCACHE_DEFAULT = SystemProperties.get("ro.compcache.default");

    private static final String SWAPPINESS_PREF = "persist.system.swappiness";

    private static final String SWAPPINESS_PERSIST_PROP = "persist.service.swappiness";

    private static final String SWAPPINESS_DEFAULT = SystemProperties.get("ro.compcache.swappiness");

    private static final String JIT_PREF = "pref_jit_mode";

    private static final String JIT_ENABLED = "int:jit";

    private static final String JIT_DISABLED = "int:fast";

    private static final String JIT_PERSIST_PROP = "persist.sys.jit-mode";

    private static final String JIT_PROP = "dalvik.vm.execution-mode";
  
    private static final String HEAPSIZE_PREF = "pref_heapsize";

    private static final String HEAPSIZE_PROP = "dalvik.vm.heapsize";

    private static final String HEAPSIZE_PERSIST_PROP = "persist.sys.vm.heapsize";

    private static final String HEAPSIZE_DEFAULT = "16m";
    
    private final Configuration mCurConfig = new Configuration();
    
    private ListPreference mWindowAnimationsPref;
    private ListPreference mTransitionAnimationsPref;
    private CheckBoxPreference mFancyImeAnimationsPref;
    private CheckBoxPreference mHapticFeedbackPref;
    private ListPreference mFontSizePref;
    private ListPreference mEndButtonPref;
    private CheckBoxPreference mCompatibilityMode;
    private ListPreference mCompcachePref;
    private ListPreference mSwappinessPref;
    private CheckBoxPreference mJitPref;
    private ListPreference mHeapsizePref;
    
    private IWindowManager mWindowManager;

    private int swapAvailable = -1;

    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {
        
        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }
        
        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {
                    
                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));
                    
                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);
        
        return true;
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.spare_parts);

        PreferenceScreen prefSet = getPreferenceScreen();
        PreferenceCategory pscCategory = (PreferenceCategory)prefSet.findPreference(PERFORMANCE_SETTINGS_CATEGORY);

        mWindowAnimationsPref = (ListPreference) prefSet.findPreference(WINDOW_ANIMATIONS_PREF);
        mWindowAnimationsPref.setOnPreferenceChangeListener(this);
        mTransitionAnimationsPref = (ListPreference) prefSet.findPreference(TRANSITION_ANIMATIONS_PREF);
        mTransitionAnimationsPref.setOnPreferenceChangeListener(this);
        mFancyImeAnimationsPref = (CheckBoxPreference) prefSet.findPreference(FANCY_IME_ANIMATIONS_PREF);
        mHapticFeedbackPref = (CheckBoxPreference) prefSet.findPreference(HAPTIC_FEEDBACK_PREF);
        mFontSizePref = (ListPreference) prefSet.findPreference(FONT_SIZE_PREF);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mEndButtonPref = (ListPreference) prefSet.findPreference(END_BUTTON_PREF);
        mEndButtonPref.setOnPreferenceChangeListener(this);
        mCompatibilityMode = (CheckBoxPreference) findPreference(KEY_COMPATIBILITY_MODE);
        mCompatibilityMode.setPersistent(false);
        mCompatibilityMode.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.COMPATIBILITY_MODE, 1) != 0);

        mCompcachePref = (ListPreference) prefSet.findPreference(COMPCACHE_PREF);
        if (isSwapAvailable()) {
        	System.out.println(COMPCACHE_PERSIST_PROP + SystemProperties.get(COMPCACHE_PERSIST_PROP));
        	if (SystemProperties.get(COMPCACHE_PERSIST_PROP) == "1")
                SystemProperties.set(COMPCACHE_PERSIST_PROP, COMPCACHE_DEFAULT);
            mCompcachePref.setValue(SystemProperties.get(COMPCACHE_PERSIST_PROP, COMPCACHE_DEFAULT));
        	System.out.println(COMPCACHE_PERSIST_PROP + SystemProperties.get(COMPCACHE_PERSIST_PROP, COMPCACHE_DEFAULT));
            mCompcachePref.setOnPreferenceChangeListener(this);
        } else {
            pscCategory.removePreference(mCompcachePref);
        }

        mSwappinessPref = (ListPreference) prefSet.findPreference(SWAPPINESS_PREF);
        if (isSwapAvailable()) {
        	System.out.println(SWAPPINESS_PERSIST_PROP + SystemProperties.get(SWAPPINESS_PERSIST_PROP));
        	if (SystemProperties.get(SWAPPINESS_PERSIST_PROP) == "1")
                SystemProperties.set(SWAPPINESS_PERSIST_PROP, SWAPPINESS_DEFAULT);
            mSwappinessPref.setValue(SystemProperties.get(SWAPPINESS_PERSIST_PROP, SWAPPINESS_DEFAULT));
        	System.out.println(SWAPPINESS_PERSIST_PROP + SystemProperties.get(SWAPPINESS_PERSIST_PROP, SWAPPINESS_DEFAULT));
            mSwappinessPref.setOnPreferenceChangeListener(this);
        } else {
            pscCategory.removePreference(mSwappinessPref);
        }

        mJitPref = (CheckBoxPreference) prefSet.findPreference(JIT_PREF);
        String jitMode = SystemProperties.get(JIT_PERSIST_PROP,
                SystemProperties.get(JIT_PROP, JIT_ENABLED));
        mJitPref.setChecked(JIT_ENABLED.equals(jitMode));

        mHeapsizePref = (ListPreference) prefSet.findPreference(HEAPSIZE_PREF);
        mHeapsizePref.setValue(SystemProperties.get(HEAPSIZE_PERSIST_PROP,
                SystemProperties.get(HEAPSIZE_PROP, HEAPSIZE_DEFAULT)));
        mHeapsizePref.setOnPreferenceChangeListener(this);
        
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        
        final PreferenceGroup parentPreference = getPreferenceScreen();
        updatePreferenceToSpecificActivityOrRemove(this, parentPreference,
                BATTERY_HISTORY_PREF, 0);
        updatePreferenceToSpecificActivityOrRemove(this, parentPreference,
                BATTERY_INFORMATION_PREF, 0);
        updatePreferenceToSpecificActivityOrRemove(this, parentPreference,
                USAGE_STATISTICS_PREF, 0);
        
        parentPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void updateToggles() {
        mFancyImeAnimationsPref.setChecked(Settings.System.getInt(
                getContentResolver(), 
                Settings.System.FANCY_IME_ANIMATIONS, 0) != 0);
        mHapticFeedbackPref.setChecked(Settings.System.getInt(
                getContentResolver(), 
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0);
    }
    
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mWindowAnimationsPref) {
            writeAnimationPreference(0, objValue);
        } else if (preference == mTransitionAnimationsPref) {
            writeAnimationPreference(1, objValue);
        } else if (preference == mFontSizePref) {
            writeFontSizePreference(objValue);
        } else if (preference == mEndButtonPref) {
            writeEndButtonPreference(objValue);
        } else if (preference == mCompcachePref) {
            if (objValue != null) {
                SystemProperties.set(COMPCACHE_PERSIST_PROP, (String)objValue);
            	System.out.println(COMPCACHE_PERSIST_PROP + (String)objValue);
                return true;
            }
        } else if (preference == mSwappinessPref) {
            if (objValue != null) {
                SystemProperties.set(SWAPPINESS_PERSIST_PROP, (String)objValue);
            	System.out.println(SWAPPINESS_PERSIST_PROP + (String)objValue);
                return true;
            }
        } else if (preference == mHeapsizePref) {
            if (objValue != null) {
                SystemProperties.set(HEAPSIZE_PERSIST_PROP, (String)objValue);
                return true;
            }
        }

        // always let the preference setting proceed.
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    	if (preference == mJitPref) {
            SystemProperties.set(JIT_PERSIST_PROP,
                    mJitPref.isChecked() ? JIT_ENABLED : JIT_DISABLED);
            return true;
        }

        if (preference == mCompatibilityMode) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.COMPATIBILITY_MODE,
                    mCompatibilityMode.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    public void writeAnimationPreference(int which, Object objValue) {
        try {
            float val = Float.parseFloat(objValue.toString());
            mWindowManager.setAnimationScale(which, val);
        } catch (NumberFormatException e) {
        } catch (RemoteException e) {
        }
    }
    
    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updateConfiguration(mCurConfig);
        } catch (RemoteException e) {
        }
    }
    
    public void writeEndButtonPreference(Object objValue) {
        try {
            int val = Integer.parseInt(objValue.toString());
            Settings.System.putInt(getContentResolver(),
                    Settings.System.END_BUTTON_BEHAVIOR, val);
        } catch (NumberFormatException e) {
        }
    }
    
    int floatToIndex(float val, int resid) {
        String[] indices = getResources().getStringArray(resid);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }
    
    public void readAnimationPreference(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            pref.setValueIndex(floatToIndex(scale,
                    R.array.entryvalues_animations));
        } catch (RemoteException e) {
        }
    }
    
    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(
                ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
        }
        pref.setValueIndex(floatToIndex(mCurConfig.fontScale,
                R.array.entryvalues_font_size));
    }
    
    public void readEndButtonPreference(ListPreference pref) {
        try {
            pref.setValueIndex(Settings.System.getInt(getContentResolver(),
                    Settings.System.END_BUTTON_BEHAVIOR));
        } catch (SettingNotFoundException e) {
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (FANCY_IME_ANIMATIONS_PREF.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.FANCY_IME_ANIMATIONS,
                    mFancyImeAnimationsPref.isChecked() ? 1 : 0);
        } else if (HAPTIC_FEEDBACK_PREF.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    mHapticFeedbackPref.isChecked() ? 1 : 0);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        readAnimationPreference(0, mWindowAnimationsPref);
        readAnimationPreference(1, mTransitionAnimationsPref);
        readFontSizePreference(mFontSizePref);
        readEndButtonPreference(mEndButtonPref);
        updateToggles();
    }
    
    /**
    * Check if swap support is available on the system
    */
        private boolean isSwapAvailable() {
            if (swapAvailable < 0) {
                swapAvailable = new File("/proc/swaps").exists() ? 1 : 0;
            }
            return swapAvailable > 0;
        }
}
