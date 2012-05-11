/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mb.toggle2g.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.mb.toggle2g.SetPhoneSettingsV2;
import com.mb.toggle2g.Toggle2GService;

public class LocaleFireReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (com.twofortyfouram.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
		{
			long setting = intent.getLongExtra(LocaleEditActivity.INTENT_EXTRA_SETTING, 0);
			
            boolean dataOff = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dataoff_switch", false );
			if ( setting == 1 )
			{
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putBoolean("enableService", true);
				edit.commit();
				
				Toggle2GService.checkLockService(context, true);
			}
			else if ( setting == 2 )
			{
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putBoolean("enableService", false);
				edit.commit();
				
				Toggle2GService.checkLockService(context, false);
				new SetPhoneSettingsV2( context ).set2gNow("locale plugin changed", dataOff);
			}
			else if ( setting == 3 )
			{
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putBoolean("enableService", false);
				edit.commit();
				
				Toggle2GService.checkLockService(context, false);
                new SetPhoneSettingsV2( context ).set3gNow("locale plugin changed", dataOff);
			}
			else if ( setting == 4 )
			{
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putBoolean("enableService", false);
				edit.commit();
				
				Toggle2GService.checkLockService(context, false);
				int net = intent.getIntExtra(LocaleEditActivity.INTENT_EXTRA_SETTING_NETWORK, -1);
				if ( net >= 0 )
				{
					new SetPhoneSettingsV2( context ).setNetworkNow("locale plugin changed", net);
				}
			}
		}
	}
}
