package com.mb.toggle2g;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Toggle2GNotificationReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		//Log.i(Toggle2G.TOGGLE2G, "user notification onReceive!");
		Toggle2GService.UserNotification();
	}
}
