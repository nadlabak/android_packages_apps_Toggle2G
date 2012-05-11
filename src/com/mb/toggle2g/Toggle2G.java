package com.mb.toggle2g;

import com.mb.toggle2g.RootCommand.CommandResult;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class Toggle2G extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener
{
    private static final String APP_FOLDER = "/data/data/com.mb.toggle2g";
    private static final String SHARED_PREFS_FOLDER = APP_FOLDER + "/shared_prefs";
    private static final String SHARED_PREFS_FILE = APP_FOLDER + "/shared_prefs/com.mb.toggle2g_preferences.xml";

    SharedPreferences DEFAULT_SHARED_PREFERENCES;

    static int network2GSelect = 1;
    static int network3GSelect = 0;

    static int NETWORK_MODE_WCDMA_PREF = 0; /* GSM/WCDMA (WCDMA preferred) */
    static int NETWORK_MODE_GSM_ONLY = 1; /* GSM only */
    static int NETWORK_MODE_WCDMA_ONLY = 2; /* WCDMA only */
    static int NETWORK_MODE_GSM_UMTS = 3; /*
                                           * GSM/WCDMA (auto mode, according to
                                           * PRL) AVAILABLE Application Settings
                                           * menu
                                           */
    static int NETWORK_MODE_CDMA = 4; /*
                                       * CDMA and EvDo (auto mode, according to
                                       * PRL) AVAILABLE Application Settings
                                       * menu
                                       */
    static int NETWORK_MODE_CDMA_NO_EVDO = 5; /* CDMA only */
    static int NETWORK_MODE_EVDO_NO_CDMA = 6; /* EvDo only */
    static int NETWORK_MODE_GLOBAL = 7; /*
                                         * GSM/WCDMA, CDMA, and EvDo (auto mode,
                                         * according to PRL) AVAILABLE
                                         * Application Settings menu
                                         */

    public static final String TOGGLE2G = "Toggle2G";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);

        preparePreferences(this);

        DEFAULT_SHARED_PREFERENCES = PreferenceManager.getDefaultSharedPreferences(this);
        DEFAULT_SHARED_PREFERENCES.registerOnSharedPreferenceChangeListener(this);

        if (!DEFAULT_SHARED_PREFERENCES.contains("wait4userNotification"))
        {
            // backward compatibility
            boolean w4u = DEFAULT_SHARED_PREFERENCES.getBoolean("wait4user", false);
            Editor edit = DEFAULT_SHARED_PREFERENCES.edit();
            edit.putBoolean("wait4userNotification", w4u);
            edit.commit();
            ((CheckBoxPreference) getPreferenceScreen().findPreference("wait4userNotification")).setChecked(w4u);
        }

        if (!DEFAULT_SHARED_PREFERENCES.contains("batteryLevelEnabled"))
        {
            // backward compatibility
            int bat = Integer.valueOf(DEFAULT_SHARED_PREFERENCES.getString("batteryLevel", Toggle2GService.DEFAULT_2G_LOW_BATTERY));
            Editor edit = DEFAULT_SHARED_PREFERENCES.edit();
            edit.putBoolean("batteryLevelEnabled", bat > 0);
            if (bat == 0)
            {
                edit.putString("batteryLevel", Toggle2GService.DEFAULT_2G_LOW_BATTERY);
            }
            edit.commit();
            ((CheckBoxPreference) getPreferenceScreen().findPreference("batteryLevelEnabled")).setChecked(bat > 0);
        }

        Preference p = getPreferenceScreen().findPreference("wait4user");
        p.setOnPreferenceClickListener(this);

        p = getPreferenceScreen().findPreference("wait4userNotification");
        p.setOnPreferenceClickListener(this);

        // boolean service = getPrefs( this ).getBoolean("enableService",
        // false);
        boolean service = DEFAULT_SHARED_PREFERENCES.getBoolean("enableService", false);
        if (service)
        {
            Toggle2GService.checkLockService(this, true);
        }
        otherSettings(service);

        loadNetworkSettings(this);

        try
        {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            setTitle(getTitle() + " Version: " + version);
        }
        catch (Exception e)
        {
        }
    }

    public static void preparePreferences(final Context context)
    {
        try
        {
            PreferenceManager.getDefaultSharedPreferences(context);

            RootCommand cmd = new RootCommand();
            if (cmd.canSU())
            {
                CommandResult runWaitFor = cmd.su.runWaitFor("mkdir " + SHARED_PREFS_FOLDER);
                Log.i(TOGGLE2G, "mkdir " + SHARED_PREFS_FOLDER + ": " + runWaitFor.stdout);
                
                runWaitFor = cmd.su.runWaitFor("chmod 777 " + APP_FOLDER);
                Log.i(TOGGLE2G, "chmod 777 " + APP_FOLDER + ": " + runWaitFor.stdout);

                runWaitFor = cmd.su.runWaitFor("chmod 777 " + SHARED_PREFS_FOLDER);
                Log.i(TOGGLE2G, "chmod 777 " + SHARED_PREFS_FOLDER + ": " + runWaitFor.stdout);

                runWaitFor = cmd.su.runWaitFor("chmod 777 " + SHARED_PREFS_FILE);
                Log.i(TOGGLE2G, "chmod 777 " + SHARED_PREFS_FILE + ": " + runWaitFor.stdout);
            }
        }
        catch (Exception e)
        {
            Log.e(TOGGLE2G, "error prcessing preferences directory", e);
        }
    }

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();

        DEFAULT_SHARED_PREFERENCES.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if ("enableService".equals(key))
        {
            boolean service = sharedPreferences.getBoolean("enableService", false);
            Toggle2GService.checkLockService(this, service);
            otherSettings(service);
        }
        else if ("network2gselect".equals(key))
        {
            network2GSelect = Integer.parseInt(sharedPreferences.getString("network2gselect", "1"));
        }
        else if ("network3gselect".equals(key))
        {
            network3GSelect = Integer.parseInt(sharedPreferences.getString("network3gselect", "0"));
        }
        else if ("kbps_enabled".equals(key))
        {
            getPreferenceScreen().findPreference("kbps").setEnabled(sharedPreferences.getBoolean("kbps_enabled", false));
        }
        else if ("delay2GEnabled".equals(key))
        {
            getPreferenceScreen().findPreference("delay2GTime").setEnabled(sharedPreferences.getBoolean("delay2GEnabled", false));
        }
        else if ("batteryLevelEnabled".equals(key))
        {
            getPreferenceScreen().findPreference("batteryLevel").setEnabled(sharedPreferences.getBoolean("batteryLevelEnabled", false));
        }
        else if ("wait4user".equals(key))
        {
            boolean wait = sharedPreferences.getBoolean("wait4user", false);
            getPreferenceScreen().findPreference("wait4userNotification").setEnabled(wait);
            getPreferenceScreen().findPreference("when2Switch").setEnabled(!wait);
        }
    }

    private void otherSettings(boolean enabled)
    {
        getPreferenceScreen().findPreference("when2Switch").setEnabled(enabled);
        getPreferenceScreen().findPreference("2g_wifi").setEnabled(enabled);
        getPreferenceScreen().findPreference("2g_dataoff").setEnabled(enabled);
        getPreferenceScreen().findPreference("dataoff_switch").setEnabled(enabled);
        getPreferenceScreen().findPreference("dontCheckPluggedIn").setEnabled(enabled);

        boolean w4u = DEFAULT_SHARED_PREFERENCES.getBoolean("wait4user", false);
        getPreferenceScreen().findPreference("wait4user").setEnabled(enabled);
        getPreferenceScreen().findPreference("wait4userNotification").setEnabled(w4u && enabled);

        boolean bat = DEFAULT_SHARED_PREFERENCES.getBoolean("delay2GEnabled", true);
        getPreferenceScreen().findPreference("delay2GEnabled").setEnabled(enabled);
        getPreferenceScreen().findPreference("delay2GTime").setEnabled(bat && enabled);

        boolean sleep2g = DEFAULT_SHARED_PREFERENCES.getBoolean("batteryLevelEnabled", true);
        getPreferenceScreen().findPreference("batteryLevelEnabled").setEnabled(enabled);
        getPreferenceScreen().findPreference("batteryLevel").setEnabled(sleep2g && enabled);

        boolean kbps = DEFAULT_SHARED_PREFERENCES.getBoolean("kbps_enabled", false);
        getPreferenceScreen().findPreference("kbps_enabled").setEnabled(enabled);
        getPreferenceScreen().findPreference("kbps").setEnabled(kbps && enabled);

    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        if ("wait4user".equals(preference.getKey()))
        {
            CheckBoxPreference cb = (CheckBoxPreference) preference;
            if (cb.isChecked())
            {
                Toggle2GService running = Toggle2GService.running;
                if (running != null && running.phoneSetter != null)
                {
                    Toggle2GService.running.phoneSetter.getNetwork();
                }
            }
            else
            {
                Toggle2GService.showNotification(this, false);
            }
        }
        else if ("wait4userNotification".equals(preference.getKey()))
        {
            final CheckBoxPreference cb = (CheckBoxPreference) preference;

            // Doesn't matter what the user clicks. The end result is based on
            // the remote login.
            if (cb.isChecked())
            {
                if (!Toggle2GService.isNotificationAppInstalled(this))
                {
                    cb.setChecked(false);

                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle(R.string.missingPlugin_title);
                    alertDialog.setMessage(getString(R.string.missingPlugin_message));
                    alertDialog.setButton(getString(R.string.missingPlugin_download), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("market://search?q=pname:com.mb.toggle2g.plugin.notification"));
                            startActivity(intent);
                            dialog.cancel();
                            return;
                        }
                    });
                    alertDialog.setButton2(getString(R.string.missingPlugin_cancel), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.cancel();
                            return;
                        }
                    });
                    alertDialog.show();
                }

                Toggle2GService running = Toggle2GService.running;
                if (running != null && running.phoneSetter != null)
                {
                    Toggle2GService.running.phoneSetter.getNetwork();
                }
            }
            else
            {
                Toggle2GService.showNotification(this, false);
            }
        }

        return false;
    }

    public static void loadNetworkSettings(Context context)
    {
        network2GSelect = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("network2gselect", "1"));
        network3GSelect = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("network3gselect", "0"));
    }

    public static SharedPreferences getPrefs(Context context)
    {
        return context.getSharedPreferences("com.mb.toggle2G", Context.MODE_WORLD_WRITEABLE);
    }
}
