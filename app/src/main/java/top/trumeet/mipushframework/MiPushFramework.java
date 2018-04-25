package top.trumeet.mipushframework;

import android.app.Application;
import android.content.SharedPreferences;

import com.crossbowffs.remotepreferences.RemotePreferenceAccessException;

import top.trumeet.common.utils.PreferencesUtils;
import top.trumeet.mipush.BuildConfig;
import top.trumeet.mipushframework.utils.BaseAppsBinder;

/**
 * Created by Trumeet on 2017/12/23.
 */

public class MiPushFramework extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        {
            //debugIcon init
            try {
                SharedPreferences prefs = PreferencesUtils.getPreferences(this);
                BaseAppsBinder.debugIcon  = prefs.getBoolean(PreferencesUtils.KeyDebugIcon, false);
            } catch (RemotePreferenceAccessException e) {
            }
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
