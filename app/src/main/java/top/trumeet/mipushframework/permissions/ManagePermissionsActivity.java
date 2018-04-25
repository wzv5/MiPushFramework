package top.trumeet.mipushframework.permissions;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.widget.EntityHeaderController;

import moe.shizuku.preference.Preference;
import moe.shizuku.preference.PreferenceCategory;
import moe.shizuku.preference.PreferenceFragment;
import moe.shizuku.preference.PreferenceScreen;
import moe.shizuku.preference.SimpleMenuPreference;
import moe.shizuku.preference.SwitchPreferenceCompat;
import top.trumeet.common.Constants;
import top.trumeet.common.db.RegisteredApplicationDb;
import top.trumeet.common.register.RegisteredApplication;
import top.trumeet.common.utils.PermissionUtils;
import top.trumeet.common.utils.Utils;
import top.trumeet.mipush.R;
import top.trumeet.mipushframework.event.RecentActivityActivity;

import static android.os.Build.VERSION_CODES.O;
import static android.provider.Settings.EXTRA_APP_PACKAGE;
import static android.provider.Settings.EXTRA_CHANNEL_ID;
import static top.trumeet.common.utils.NotificationUtils.getChannelIdByPkg;

/**
 * Created by Trumeet on 2017/8/27.
 * @author Trumeet
 */

public class ManagePermissionsActivity extends AppCompatActivity implements PermissionUtils.PermissionGrantListener {
    public static final String EXTRA_PACKAGE_NAME =
            ManagePermissionsActivity.class.getName()
            + ".EXTRA_PACKAGE_NAME";

    private LoadTask mTask;
    private String mPkg;

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null &&
                getIntent().hasExtra(EXTRA_PACKAGE_NAME)) {
            mPkg = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
            PermissionUtils.requestPermissionsIfNeeded(this,
                    new String[]{Constants.permissions.WRITE_SETTINGS});
        }
        getSupportActionBar()
                .setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy () {
        if (mTask != null && !mTask.isCancelled()) {
            mTask.cancel(true);
            mTask = null;
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged (Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }

    private class LoadTask extends AsyncTask<Void, Void, RegisteredApplication> {
        private String pkg;
        private CancellationSignal mSignal;

        LoadTask (String pkg) {
            this.pkg = pkg;
        }

        @Override
        protected RegisteredApplication doInBackground(Void... voids) {
            mSignal = new CancellationSignal();
            return RegisteredApplicationDb.registerApplication(pkg /* Package */
                    , false /* Auto Create */,
                    ManagePermissionsActivity.this /* Context */,
                    mSignal);
        }

        @Override
        protected void onPostExecute (RegisteredApplication application) {
            if (application != null) {
                ManagePermissionsFragment fragment = new ManagePermissionsFragment();
                fragment.setApplicationItem(application);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content,
                                fragment)
                        .commitAllowingStateLoss();
            }
        }

        @Override
        protected void onCancelled () {
            if (mSignal != null) {
                if (!mSignal.isCanceled())
                    mSignal.cancel();
                mSignal = null;
            }
        }
    }

    public static class ManagePermissionsFragment extends PreferenceFragment {
        private RegisteredApplication mApplicationItem;
        private SaveTask mSaveTask;

        /**
         * Not using {@link android.os.Parcelable}, too bad
         * @param applicationItem item
         */
        public void setApplicationItem (RegisteredApplication applicationItem) {
            this.mApplicationItem = applicationItem;
        }

        @Override
        public void onCreate (Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
            MenuItem itemOk = menu.add(0, 0, 0, R.string.apply);
            Drawable iconOk = ContextCompat.getDrawable(getActivity(),
                    R.drawable.ic_check_black_24dp);
            DrawableCompat.setTint(iconOk, Utils.getColorAttr(getContext(), R.attr.colorAccent));
            itemOk.setIcon(iconOk);
            itemOk.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        @Override
        public boolean onOptionsItemSelected (MenuItem item) {
            if (item.getItemId() == 0) {
                if (mSaveTask != null && !mSaveTask.isCancelled()) {
                    return true;
                }
                mSaveTask = new SaveTask();
                mSaveTask.execute();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onDetach () {
            if (mSaveTask != null && !mSaveTask.isCancelled()) {
                mSaveTask.cancel(true);
                mSaveTask = null;
            }
            super.onDetach();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            PreferenceScreen screen = getPreferenceManager()
                    .createPreferenceScreen(getActivity());

            Preference appPreferenceOreo = EntityHeaderController.newInstance((AppCompatActivity)getActivity(),
                    this, null)
                    .setRecyclerView(getListView())
                    .setIcon(mApplicationItem.getIcon(getContext()))
                    .setLabel(mApplicationItem.getLabel(getContext()))
                    .setSummary(mApplicationItem.getPackageName())
                    .setPackageName(mApplicationItem.getPackageName())
                    .setButtonActions(EntityHeaderController.ActionType.ACTION_APP_INFO
                            , EntityHeaderController.ActionType.ACTION_NONE)
                    .done((AppCompatActivity)getActivity(), getContext());
            screen.addPreference(appPreferenceOreo);

            if (Build.VERSION.SDK_INT >= O) {
                Preference manageNotificationPreference = new Preference(getActivity());
                manageNotificationPreference.setTitle(R.string.settings_manage_app_notifications);
                manageNotificationPreference.setSummary(R.string.settings_manage_app_notifications_summary);
                manageNotificationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    @TargetApi(O)
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                .putExtra(EXTRA_APP_PACKAGE, Constants.SERVICE_APP_NAME)
                                .putExtra(EXTRA_CHANNEL_ID, getChannelIdByPkg(mApplicationItem.getPackageName())));
                        return true;
                    }
                });
                screen.addPreference(manageNotificationPreference);
            }

            final SimpleMenuPreference preferenceRegisterMode =
                    new SimpleMenuPreference(getActivity(),
                            null, moe.shizuku.preference.simplemenu.R.attr.simpleMenuPreferenceStyle,
                            R.style.SimpleMenuPreference);
            preferenceRegisterMode.setEntries(R.array.register_types);
            preferenceRegisterMode.setEntryValues(R.array.register_entries);
            preferenceRegisterMode.setTitle(R.string.permission_register_type);
            preferenceRegisterMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mApplicationItem.setType(Integer.parseInt(String.valueOf(newValue)));
                    updateRegisterType(mApplicationItem.getType(),
                            preferenceRegisterMode);
                    return true;
                }
            });
            updateRegisterType(mApplicationItem.getType(),
                    preferenceRegisterMode);

            Preference viewRecentActivityPreference = new Preference(getActivity());
            viewRecentActivityPreference.setTitle(R.string.recent_activity_view);
            viewRecentActivityPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(),
                            RecentActivityActivity.class)
                    .setData(Uri.parse(mApplicationItem.getPackageName())));
                    return true;
                }
            });

            screen.addPreference(preferenceRegisterMode);
            screen.addPreference(viewRecentActivityPreference);

            PreferenceCategory category = new PreferenceCategory(getActivity(), null, moe.shizuku.preference.R.attr.preferenceCategoryStyle,
                    R.style.Preference_Category_Material);
            category.setTitle(R.string.permissions);
            screen.addPreference(category);

            addItem(mApplicationItem.getAllowReceivePush(),
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            mApplicationItem.setAllowReceivePush(((Boolean)newValue));
                            return true;
                        }
                    },
            getString(R.string.permission_allow_receive),
                    category);

            addItem(mApplicationItem.isAllowReceiveCommand(),
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            mApplicationItem.setAllowReceiveCommand(((Boolean)newValue));
                            return true;
                        }
                    },
                    getString(R.string.permission_allow_receive_command),
                    category);

            addItem(mApplicationItem.getAllowReceiveRegisterResult(),
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            mApplicationItem.setAllowReceiveRegisterResult(((Boolean)newValue));
                            return true;
                        }
                    },
                    getString(R.string.permission_allow_receive_register_result),
                    category);

            setPreferenceScreen(screen);
        }

        private void updateRegisterType (@RegisteredApplication.Type int type,
                                         SimpleMenuPreference preference) {
            int index = 0;
            switch (type) {
                case RegisteredApplication.Type.ALLOW:
                    index = 1;
                    break;
                case RegisteredApplication.Type.ASK:
                    index = 0;
                    break;
                case RegisteredApplication.Type.DENY:
                    index = 2;
                    break;
            }
            preference.setValueIndex(index);
            preference.setSummary(getResources().getStringArray(R.array.register_types)
            [index]);
        }

        private void addItem (boolean value, Preference.OnPreferenceChangeListener listener
                , CharSequence title, PreferenceCategory parent) {
            SwitchPreferenceCompat preference = new SwitchPreferenceCompat(getActivity(),
                    null, moe.shizuku.preference.R.attr.switchPreferenceStyle,
                    R.style.Preference_SwitchPreferenceCompat);
            preference.setOnPreferenceChangeListener(listener);
            preference.setTitle(title);
            preference.setChecked(value);
            parent.addPreference(preference);
        }

        private class SaveTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                RegisteredApplicationDb.update(mApplicationItem,
                        getActivity());
                return null;
            }

            @Override
            protected void onPostExecute (Void result) {
                getActivity().finish();
            }
        }
    }

    @Override
    public void onResult (boolean granted, boolean blocked, String permName) {
        if (Constants.permissions.WRITE_SETTINGS.equalsIgnoreCase(permName)) {
            if (granted) {
                mTask = new LoadTask(mPkg);
                mTask.execute();
            } else {
                Toast.makeText(this, getString(top.trumeet.common.R.string.request_permission,
                        PermissionUtils.getName(permName)), Toast.LENGTH_LONG)
                        .show();
                if (blocked) {
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    PermissionUtils.requestPermissionsIfNeeded(this,
                            new String[]{permName});
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (PermissionUtils.handle(this, requestCode, permissions,
                grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
