package com.mb.toggle2g;

import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Toggle2GService extends Service
{
	static final String DEFAULT_2G_LOW_BATTERY = "30";
	static final String DEFAULT_2G_SLEEP_DELAY = "10";
	static final int DELAY_SECONDS_2G_ON_WIFI_ON = 30;
	static final String ACTION_SHOW_NOTIFICATION = "com.mb.notification.CONTROL";

	static Toggle2GService running = null;

	boolean isScreenOff = false;
	long screenOffTime = 0;
	long wifiOnTime = 0;
	
	int mBatteryLevel = 100;
	int phoneState = TelephonyManager.CALL_STATE_IDLE;
	boolean isPluggedIn = true;
	boolean isWifiOn = false;
	NetworkInfo networkInfo = null;
	boolean isMobileDataEnabled = true;

	final IBinder mBinder = new LocalBinder();
	LockReceiver lockReceiver = new LockReceiver();
	UnLockReceiver unlockReceiver = new UnLockReceiver();
	WiFiReceiver wifiReceiver = new WiFiReceiver();
	PhoneStateReceiver phoneReceiver = new PhoneStateReceiver();
	BatteryReceiver batteryReceiver = new BatteryReceiver();
	TimeReceiver timeReceiver = new TimeReceiver();
	PhoneStateListener phoneListener = new MyPhoneStateListener();
    PhoneNetworkListener phoneNetworkListener = new PhoneNetworkListener();
    BackgroundDataStateReceiver backgrounDataListener = new BackgroundDataStateReceiver();

	WifiManager wifiManager;
	SetPhoneSettingsV2 phoneSetter;
	TelephonyManager telephonyManager;
	SharedPreferences preferences;
	
	boolean timerRegistered = true;

	public class LocalBinder extends Binder
	{
		Toggle2GService getService()
		{
			return Toggle2GService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean service = preferences.getBoolean("enableService", false);

//		Log.i(Toggle2G.TOGGLE2G, "kbps=" + getKBps() );
		if (running != null || !service)
		{
			//Log.i(Toggle2G.TOGGLE2G, "service already running");
			this.stopSelf();
			return;
		}
		running = this;
		//ToggleAppWidgetProvider.updateWidgets();

		Log.i(Toggle2G.TOGGLE2G, "service created");

		phoneSetter = new SetPhoneSettingsV2(this);

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		telephonyManager.listen(phoneNetworkListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

		IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(wifiReceiver, intentFilter);
		
		registerReceiver(phoneReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
		registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(lockReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		
		IntentFilter screenOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenOn.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(unlockReceiver, screenOn);
		
        isMobileDataEnabled = phoneSetter.getMobileData(this);
		registerReceiver(backgrounDataListener, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		timerRegistered = true;
 
		//Log.i(Toggle2G.TOGGLE2G, "notfyPlugin()");
		Intent notifyIntent = new Intent(Toggle2GWidgetReceiver.SET_ACTION);
		notifyIntent.putExtra("setting", "auto");
		sendBroadcast(notifyIntent);
	}

	@Override
	public void onDestroy()
	{
		unregisterReceiver(lockReceiver);
		unregisterReceiver(unlockReceiver);
		unregisterReceiver(wifiReceiver);
        unregisterReceiver(backgrounDataListener);
		unregisterReceiver(phoneReceiver);
		unregisterReceiver(batteryReceiver);

		if (timerRegistered)
		{
			unregisterReceiver(timeReceiver);
			timerRegistered = false;
		}

//		Log.i(Toggle2G.TOGGLE2G, "service destroyed");
		super.onDestroy();

		running = null;
		phoneSetter.getNetwork();
		//ToggleAppWidgetProvider.updateWidgets();
	}

	public class LockReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent arg1)
		{
			screenOffTime = System.currentTimeMillis();
			isScreenOff = true;

			if (noWifi() || noData() || saveBattery())
			{
				Toggle2GService.showNotification( context, false );
			}
			
			set2g("screen turned off");
		}
	}

	public class UnLockReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			isScreenOff = false;

//			Log.i(Toggle2G.TOGGLE2G, "unlock isWifiOn=" + isWifiOn + ", saveBattery()=" + saveBattery() + ", getCallState()=" + telephonyManager.getCallState());
			if (noWifi() || noData() || saveBattery())
			{
				Toggle2GService.showNotification( context, false );
			}
			else if ( intent.getAction().equals(Intent.ACTION_USER_PRESENT ))
			{
			    System.out.println("ACTION_USER_PRESENT " + preferences.getString("when2Switch", "0"));
			    if ( "1".equals(preferences.getString("when2Switch", "0"))) 
			    {
	                set3gNow("screen unlocked", true);
			    }
			}
			else
			{
                System.out.println("NOT ACTION_USER_PRESENT " + preferences.getString("when2Switch", "0"));
                if ( "0".equals(preferences.getString("when2Switch", "0"))) 
                {
                    set3gNow("screen turned on", true);
                }
			}
		}
	}

	public class PhoneStateReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent arg1)
		{
			if (noWifi() || noData() || saveBattery())
			{
				Toggle2GService.showNotification( context, false );
				set2g("phone state changed");
			}
			else if (isScreenOff)
			{
				set2g("phone state changed");
			}
			else
			{
				set3gNow("phone state changed", true);
			}
		}
	}

    public class BackgroundDataStateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(final Context context, Intent intent)
        {
            if ( phoneSetter.mCurrentDataSetting == null )
            {
                isMobileDataEnabled = phoneSetter.getMobileData(context);
                Log.i(Toggle2G.TOGGLE2G, "isMobileDataEnabled=" + isMobileDataEnabled);
                
                if (!isMobileDataEnabled)
                {
                    Toggle2GService.showNotification( context, false );
                    set2g("background data is off");
                }
                else
                {
                    Toggle2GService.showNotification( context, phoneSetter.currentNetwork == Toggle2G.network2GSelect );
                }
            }
        }
    }

	public class BatteryReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			isPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
			mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

			if (noWifi() || noData() || saveBattery())
			{
				Toggle2GService.showNotification( context, false );
				set2g("battery level changed");
			}
			else if (isScreenOff)
			{
				set2g("battery level changed");
			}
		}
	}

	public class TimeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			if (noWifi() || noData() || saveBattery())
			{
				Toggle2GService.showNotification( context, false );
			}
			set2g("it's been awhile");
		}
	}

	public class WiFiReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(final Context context, Intent intent)
		{
			networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			isWifiOn = (networkInfo != null && wifiManager.isWifiEnabled() && networkInfo.isConnectedOrConnecting());
			
			if( isWifiOn )
			{
				if (wifiOnTime == 0)
				{
					wifiOnTime = System.currentTimeMillis();
				}
			}
			else
			{
				 wifiOnTime = 0; 
			}

			if (noWifi() || noData() || saveBattery())
			{
				Toggle2GService.showNotification( context, false );
				set2g("wifi state changed");
			}
			else if (isScreenOff)
			{
				set2g("wifi state changed");
			}
			else
			{
				set3gNow("wifi state changed", true);
			}
		}
	}

	public static void checkLockService(Context context, boolean on)
	{
		Intent intent = new Intent(context, Toggle2GService.class);
		if (on && running == null)
		{
//			Log.i(Toggle2G.TOGGLE2G, "setting service on");
			context.startService(intent);
		}
		else if (!on && running != null)
		{
//			Log.i(Toggle2G.TOGGLE2G, "setting service off");
			Toggle2GService.showNotification( context, false );
			context.stopService(intent);
		}
		else
		{
//			Log.i(Toggle2G.TOGGLE2G, "setting service already set on=" + on + " running=" + running);
		}
	}

	private boolean delayScreenOffEnough()
	{
		if( !preferences.getBoolean("delay2GEnabled", true) )
		{
			return false;
		}
		int delay2GScreenOff = Integer.valueOf(preferences.getString("delay2GTime", DEFAULT_2G_SLEEP_DELAY));
		return isScreenOff && screenOffTime + (delay2GScreenOff * 60 * 1000) < System.currentTimeMillis();
	}

	private boolean delayWifiOffEnough()
	{
		return isWifiOn && wifiOnTime + (DELAY_SECONDS_2G_ON_WIFI_ON * 1000) < System.currentTimeMillis();
	}

	private boolean saveBattery()
	{
		if( !preferences.getBoolean("batteryLevelEnabled", true) )
		{
			return false;
		}
		int lowBatteryLevel = Integer.valueOf(preferences.getString("batteryLevel", DEFAULT_2G_LOW_BATTERY));
		return (mBatteryLevel < lowBatteryLevel && !isPluggedIn());
	}

	private boolean isPluggedIn()
	{
		if (isPluggedIn)
		{
			if (preferences.getBoolean("dontCheckPluggedIn", false))
			{
				return false;
			}
		}

		return isPluggedIn;
	}

	private void set2g(String reason)
	{
		boolean needTimer = false;
		if ( phoneSetter.currentNetwork != Toggle2G.network2GSelect && !isPluggedIn())
		{
			boolean saveBattery = saveBattery();
            boolean checkWifi = noWifi();
            boolean checkDataOff = noData();
			
			// Log.i(Toggle2G.TOGGLE2G, "set2g " + !is2g + ", " + !isPluggedIn() + ", " + isScreenOff + ", " + isWifiOn );
			if (isScreenOff || checkWifi || checkDataOff || saveBattery)
			{
				boolean delayScreenOffEnough = delayScreenOffEnough();
				boolean delayWifiEnough = checkWifi && delayWifiOffEnough();
				
				// Log.i(Toggle2G.TOGGLE2G, "delay or low battery check " + delayEnough + ", " + saveBattery + "," + delayWifiEnough);
				if (delayScreenOffEnough || saveBattery || delayWifiEnough || checkDataOff)
				{
					long kbps = getKBps();
					if ( kbps >= 0 )
					{
						set2gNow(reason + " and delayScreenOffEnough=" + delayScreenOffEnough + ", delayWifiEnough=" + delayWifiEnough + ", saveBattery=" + saveBattery + ", isWifiOn=" + isWifiOn + ", isBackgroundDataEnabled=" + isMobileDataEnabled + ", KBps=" + kbps);
					}
					else
					{
						Log.i(Toggle2G.TOGGLE2G, "not setting 2G, net speed of  " + ( -1 * kbps ) + " KBps is above threshold");
						needTimer = true;
					}
				}
				else
				{
					needTimer = true;
				}
			}
		}

		if (!needTimer && timerRegistered)
		{
			// nothing needs to be done, stop watching
			unregisterReceiver(timeReceiver);
			timerRegistered = false;
			Log.i(Toggle2G.TOGGLE2G, "disable 2g delayer");
		}
		else if (needTimer && !timerRegistered)
		{
			// we need to keep tabs on when to switch to 2g
			registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
			timerRegistered = true;
			Log.i(Toggle2G.TOGGLE2G, "enable 2g delayer");
		}

	}

    private boolean noData()
    {
        return !isMobileDataEnabled && preferences.getBoolean("2g_dataoff", true);
    }

    private boolean noWifi()
    {
        return isWifiOn && preferences.getBoolean("2g_wifi", true);
    }
	
	private void set2gNow(String reason)
	{
		if ( phoneState == TelephonyManager.CALL_STATE_IDLE && telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE )
		{
			phoneSetter.set2g(reason, preferences.getBoolean("dataoff_switch", true));
		}
		else
		{
//			Log.i(Toggle2G.TOGGLE2G, "2g not set, phone in use");
		}
	}

	private void set3gNow(String reason, boolean wait4user)
	{
		if ( phoneState == TelephonyManager.CALL_STATE_IDLE && telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE )
		{
			boolean wait = preferences.getBoolean("wait4user", false);
			if ( !wait || !wait4user || !Toggle2GService.isNotificationAppInstalled(this))
			{
				phoneSetter.set3g(reason, preferences.getBoolean("dataoff_switch", true) );
			}
			else  
			{
				phoneSetter.getNetwork();
			}
		}
		else
		{
//			Log.i(Toggle2G.TOGGLE2G, "3g not set, phone in use");
		}
	}

	public static void showNotification( Context context, boolean show )
	{
		boolean wait = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wait4user", false);
		boolean waitNotify = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wait4userNotification", false);
		boolean running = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("enableService", false);
		
		if ( running && ( Toggle2GService.running.noWifi() || Toggle2GService.running.noData() || Toggle2GService.running.saveBattery() ))
		{
			// no 3G if wifi or battery saver
			show = false;
		}
		
		Intent notifyIntent = new Intent(ACTION_SHOW_NOTIFICATION);
		//Log.i(Toggle2G.TOGGLE2G, "make notification=" + ( running && wait && show ));
		notifyIntent.putExtra("show", running && wait && waitNotify && show);
		context.sendBroadcast(notifyIntent);
	}

	public static void UserNotification()
	{
		//Log.i(Toggle2G.TOGGLE2G, "user notification!");
		if ( running != null )
		{
			running.userEnabled3g();
		}
	}

	public void userEnabled3g()
	{
		set3gNow("User said it was ok", false);
	}

	public static boolean isNotificationAppInstalled( Context context )
	{
		Intent notifyIntent = new Intent(Toggle2GService.ACTION_SHOW_NOTIFICATION);
		List<ResolveInfo> list = context.getPackageManager().queryBroadcastReceivers(notifyIntent, 0);  
		return ( list.size() > 0 );
	}
	
	public static boolean isRunning()
	{
		return running != null;
	}

	private long getKBps()
	{
		boolean enabled = preferences.getBoolean("kbps_enabled", false);
		long wait = Long.parseLong(preferences.getString("kbps", "0"));
		if ( !enabled || wait == 0)
		{
			// it's not on, so 1 is good enough
			return 0;
		}

		long timeBefore = System.currentTimeMillis();
		long bytesRx = TrafficStats.getTotalRxBytes();
		long bytesTx = TrafficStats.getTotalTxBytes();
		try
		{
			Thread.sleep( 1000 );
		}
		catch (InterruptedException e)
		{
		}
		
		long timeDiff = System.currentTimeMillis() - timeBefore ;
		long bytesDiff = Math.max( TrafficStats.getTotalRxBytes() - bytesRx, TrafficStats.getTotalTxBytes() - bytesTx );
		
		bytesDiff *= 1000;
		
		long kbps = ( bytesDiff / timeDiff ) / 1024;
		
		if ( kbps >= wait )
		{
			// < 0 means it didn't meet the low threshold
			kbps *= -1;
		}
		return kbps;
	}

	public class MyPhoneStateListener extends PhoneStateListener
	{
		public void onCallStateChanged(int state, String incomingNumber)
		{
			Log.i(Toggle2G.TOGGLE2G, "onCallStateChanged=" + state);
			//super.onCallStateChanged(state, incomingNumber);
			phoneState = state;
		}
	}

	public class PhoneNetworkListener extends PhoneStateListener
	{
		@Override
		public void onDataConnectionStateChanged(int state, int networkType)
		{
			super.onDataConnectionStateChanged(state, networkType);
			//Log.i(Toggle2G.TOGGLE2G,  "onDataConnectionStateChanged state=" + state + ", networkType=" + networkType);
			//phoneState = state;
			phoneSetter.getNetwork();
		}
	}
}
