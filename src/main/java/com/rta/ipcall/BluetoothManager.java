package com.rta.ipcall;
/*
BluetoothManager.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * @author Sylvain Berfini
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BluetoothManager extends BroadcastReceiver {
	private static final String TAG = BluetoothManager.class.getSimpleName();
	private static BluetoothManager instance;
	public int PLANTRONICS_BUTTON_PRESS = 1;
	public int PLANTRONICS_BUTTON_LONG_PRESS = 2;
	public int PLANTRONICS_BUTTON_DOUBLE_PRESS = 5;
	public int PLANTRONICS_BUTTON_CALL = 2;
	public int PLANTRONICS_BUTTON_MUTE = 3;
	private Context mContext;
	private AudioManager mAudioManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothHeadset mBluetoothHeadset;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothProfile.ServiceListener mProfileListener;
	private boolean isBluetoothConnected;
	private boolean isScoConnected;

	public BluetoothManager() {
		isBluetoothConnected = false;
		if (!ensureInit()) {
			Log.w(TAG, "[Bluetooth] Manager tried to init but LinphoneService not ready yet...");
		}
		instance = this;
	}

	public static BluetoothManager getInstance() {
		if (instance == null) {
			instance = new BluetoothManager();
		}
		return instance;
	}

	public void initBluetooth() {
		if (!ensureInit()) {
			Log.w(TAG, "[Bluetooth] Manager tried to init bluetooth but LinphoneService not ready yet...");
			return;
		}

		IntentFilter filter = new IntentFilter();
		filter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + BluetoothAssignedNumbers.PLANTRONICS);
		filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
		filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
		mContext.registerReceiver(this,  filter);
		Log.d(TAG, "[Bluetooth] Receiver started");

		startBluetooth();
	}

	private void startBluetooth() {
		if (isBluetoothConnected) {
			Log.e(TAG, "[Bluetooth] Already started, skipping...");
			return;
		}

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
			if (mProfileListener != null) {
				Log.w(TAG, "[Bluetooth] Headset profile was already opened, let's close it");
				mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
			}

			mProfileListener = new BluetoothProfile.ServiceListener() {
				public void onServiceConnected(int profile, BluetoothProfile proxy) {
				    if (profile == BluetoothProfile.HEADSET) {
				        Log.d(TAG, "[Bluetooth] Headset connected");
				        mBluetoothHeadset = (BluetoothHeadset) proxy;
				        isBluetoothConnected = true;
				    }
				}
				public void onServiceDisconnected(int profile) {
				    if (profile == BluetoothProfile.HEADSET) {
				        mBluetoothHeadset = null;
				        isBluetoothConnected = false;
				        Log.d(TAG, "[Bluetooth] Headset disconnected");
				        LinphoneManager.getInstance().routeAudioToReceiver();
				    }
				}
			};
			boolean success = mBluetoothAdapter.getProfileProxy(mContext, mProfileListener, BluetoothProfile.HEADSET);
			if (!success) {
				Log.e(TAG, "[Bluetooth] getProfileProxy failed !");
			}
		} else {
			Log.w(TAG, "[Bluetooth] Interface disabled on device");
		}
	}

	private boolean ensureInit() {
		if (mBluetoothAdapter == null) {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}
		if (mContext == null) {
			if (LinphoneService.isReady()) {
				mContext = LinphoneService.instance().getApplicationContext();
			} else {
				return false;
			}
		}
		if (mContext != null && mAudioManager == null) {
			mAudioManager = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE));
		}
		return true;
	}

	public boolean routeAudioToBluetooth() {
		ensureInit();

		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mAudioManager != null && mAudioManager.isBluetoothScoAvailableOffCall()) {
			if (isBluetoothHeadsetAvailable()) {
				if (mAudioManager != null && !mAudioManager.isBluetoothScoOn()) {
					Log.d(TAG, "[Bluetooth] SCO off, let's start it");
					mAudioManager.setBluetoothScoOn(true);
					mAudioManager.startBluetoothSco();
				}
			} else {
				return false;
			}

			// Hack to ensure bluetooth sco is really running
			boolean ok = isUsingBluetoothAudioRoute();
			int retries = 0;
			while (!ok && retries < 5) {
				retries++;

				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}

				if (mAudioManager != null) {
					mAudioManager.setBluetoothScoOn(true);
					mAudioManager.startBluetoothSco();
				}

				ok = isUsingBluetoothAudioRoute();
			}
			if (ok) {
				if (retries > 0) {
					Log.d(TAG, "[Bluetooth] Audio route ok after " + retries + " retries");
				} else {
					Log.d(TAG, "[Bluetooth] Audio route ok");
				}
			} else {
				Log.d(TAG, "[Bluetooth] Audio route still not ok...");
			}

			return ok;
		}

		return false;
	}

	public boolean isUsingBluetoothAudioRoute() {
		return mBluetoothHeadset != null && mBluetoothHeadset.isAudioConnected(mBluetoothDevice) && isScoConnected;
	}

	public boolean isBluetoothHeadsetAvailable() {
		ensureInit();
		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mAudioManager != null && mAudioManager.isBluetoothScoAvailableOffCall()) {
			boolean isHeadsetConnected = false;
			if (mBluetoothHeadset != null) {
				List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
				mBluetoothDevice = null;
				for (final BluetoothDevice dev : devices) {
					if (mBluetoothHeadset.getConnectionState(dev) == BluetoothHeadset.STATE_CONNECTED) {
						mBluetoothDevice = dev;
						isHeadsetConnected = true;
						break;
					}
				}
				Log.d(TAG, isHeadsetConnected ? "[Bluetooth] Headset found, bluetooth audio route available" : "[Bluetooth] No headset found, bluetooth audio route unavailable");
			}
			return isHeadsetConnected;
		}

		return false;
	}

	public void disableBluetoothSCO() {
		if (mAudioManager != null && mAudioManager.isBluetoothScoOn()) {
			mAudioManager.stopBluetoothSco();
			mAudioManager.setBluetoothScoOn(false);

			// Hack to ensure bluetooth sco is really stopped
			int retries = 0;
			while (isScoConnected && retries < 10) {
				retries++;

				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}

				mAudioManager.stopBluetoothSco();
				mAudioManager.setBluetoothScoOn(false);
			}
			Log.w(TAG, "[Bluetooth] SCO disconnected!");
		}
	}

	public void stopBluetooth() {
		Log.w(TAG, "[Bluetooth] Stopping...");
		isBluetoothConnected = false;

		disableBluetoothSCO();

		if (mBluetoothAdapter != null && mProfileListener != null && mBluetoothHeadset != null) {
			mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
			mProfileListener = null;
		}
		mBluetoothDevice = null;

		Log.w(TAG, "[Bluetooth] Stopped!");

		if (LinphoneManager.isInstanciated()) {
			LinphoneManager.getInstance().routeAudioToReceiver();
		}
	}

	public void destroy() {
		try {
			stopBluetooth();

			try {
				mContext.unregisterReceiver(this);
				Log.d(TAG, "[Bluetooth] Receiver stopped");
			} catch (Exception e) {}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void onReceive(Context context, Intent intent) {
        if (!LinphoneManager.isInstanciated())
        	return;

        String action = intent.getAction();
        if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
        	int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, 0);
    		if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
    			Log.d(TAG, "[Bluetooth] SCO state: connected");
//				LinphoneManager.getInstance().audioStateChanged(AudioState.BLUETOOTH);
    			isScoConnected = true;
        	} else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
        		Log.d(TAG, "[Bluetooth] SCO state: disconnected");
//				LinphoneManager.getInstance().audioStateChanged(AudioState.SPEAKER);
        		isScoConnected = false;
        	} else {
        		Log.d(TAG, "[Bluetooth] SCO state: " + state);
        	}
        }
        else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
        	int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
        	if (state == 0) {
        		Log.d(TAG, "[Bluetooth] State: disconnected");
        		stopBluetooth();
        	} else if (state == 2) {
        		Log.d(TAG, "[Bluetooth] State: connected");
        		startBluetooth();
        	} else {
        		Log.d(TAG, "[Bluetooth] State: " + state);
        	}
        }
        else if (intent.getAction().equals(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)) {
			String command = intent.getExtras().getString(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
			//int type = intent.getExtras().getInt(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE);

			Object[] args = (Object[]) intent.getExtras().get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
			String eventName = (String) args[0];

			if (eventName.equals("BUTTON") && args.length >= 3) {
				Integer buttonID = (Integer) args[1];
				Integer mode = (Integer) args[2];
				Log.d(TAG, "[Bluetooth] Event: " + command + " : " + eventName + ", id = " + buttonID + " (" + mode + ")");
			}
    	}
    }
}
