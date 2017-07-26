package com.rta.ipcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class HookReceiver extends BroadcastReceiver {
    private static final String TAG = HookReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(isOrderedBroadcast())
            abortBroadcast();
        Bundle extras = intent.getExtras();
        boolean b = extras.getBoolean("hookoff");
        if(b){
            //handset on
            Log.i(TAG, " ======>>>>>> HookReceiver - handset ON");
            LinphoneManager.getLc().enableSpeaker(false);
            LinphoneManager.getInstance().setHandsetMode(true);


        }else{
            //handset off
            Log.i(TAG, " ======>>>>>> HookReceiver - handset OFF");
            LinphoneManager.getLc().enableSpeaker(true);
            LinphoneManager.getInstance().setHandsetMode(false);
        }
    }
}
