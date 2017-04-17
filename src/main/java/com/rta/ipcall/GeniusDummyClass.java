package com.rta.ipcall;

import android.content.Context;
import android.content.Intent;

import com.rta.ipcall.ui.AddressText;

/**
 * Created by tamdoan on 15/04/2017.
 */

public class GeniusDummyClass {
    public GeniusDummyClass(Context context) {
        if (!LinphoneService.isReady()) {
            context.startService(new Intent(Intent.ACTION_MAIN).setClass(context, LinphoneService.class));
        }
    }
}
