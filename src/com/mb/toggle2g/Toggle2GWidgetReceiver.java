package com.mb.toggle2g;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class Toggle2GWidgetReceiver extends BroadcastReceiver
{
	public static String SET_ACTION = "com.mb.toggle.widget.SET";
	static SetPhoneSettingsV2 setPhoneSettingsV2;
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		//Log.i(Toggle2G.TOGGLE2G, "user notification onReceive!");
		String stringExtra = intent.getStringExtra("command");
		setPhoneSettingsV2 = new SetPhoneSettingsV2( context );
		
		Log.i(Toggle2G.TOGGLE2G, "widget request=" + stringExtra);

		if ( "auto".equals( stringExtra ))
		{
			Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
			edit.putBoolean("enableService", true);
			edit.commit();
			Toggle2GService.checkLockService(context, true);
		}
		else
		{
            boolean dataOff = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dataoff_switch", false );
			if ( "2g".equals( stringExtra ))
			{
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putBoolean("enableService", false);
				edit.commit();
				Toggle2GService.checkLockService(context, false);
				setPhoneSettingsV2.set2gNow("widget toggle", dataOff);
			}
			else if ( "3g".equals( stringExtra ))
			{
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putBoolean("enableService", false);
				edit.commit();
				Toggle2GService.checkLockService(context, false);
				setPhoneSettingsV2.set3gNow("widget toggle", dataOff);
			}
			else if ( "get".equals( stringExtra ))
			{
				if ( Toggle2GService.isRunning() )
				{
					Intent notifyIntent = new Intent(Toggle2GWidgetReceiver.SET_ACTION);
					notifyIntent.putExtra("setting", "auto");
					context.sendBroadcast(notifyIntent);
				}
				else
				{
					setPhoneSettingsV2.getNetwork();
				}
			}
		}
	}
}
