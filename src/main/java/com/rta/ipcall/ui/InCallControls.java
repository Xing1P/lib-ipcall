package com.rta.ipcall.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by Genius Doan on 19/04/2017.
 */

public class InCallControls extends FrameLayout {
    private static final String THIS_FILE = "InCallControls";

    //private MenuBuilder btnMenuBuilder;
    private boolean supportMultipleCalls = false;


    public InCallControls(Context context) {
        this(context, null, 0);
    }

    public InCallControls(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InCallControls(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Finalize object style
        setEnabledMediaButtons(false);
    }



    private boolean callOngoing = false;
    public void setEnabledMediaButtons(boolean isInCall) {
        callOngoing = isInCall;
    }
}
