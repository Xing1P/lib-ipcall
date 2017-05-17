package vn.rta.survey.android.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphoneService;
import com.rta.ipcall.LinphoneUtils;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.Reason;
import org.odk.collect.android.activities.FormEntryActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import vn.rta.ipcall.api.SipManager;
import vn.rta.ipcall.ui.IOnCallActionTrigger;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.Constants;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.entities.DialingFeedback;
import vn.rta.survey.android.manager.ActivityLogManager;
import vn.rta.survey.android.manager.IPCallManager;
import vn.rta.survey.android.views.InCallControlView;

/**
 * Created by ThiNguyen on 8/29/16.
 *
 * @modified by Genius Doan on 4/20/17
 */

public class InIpCallService extends Service implements InCallControlView.ChangeViewListener, IOnCallActionTrigger, InCallControlView.OnDtmfListener {
    //TODO: Add Call Proximity Manager
    public static final String ACTION = "vn.rta.sipcall.service";
    public static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    private View mView;
    private WindowManager.LayoutParams params;
    private WindowManager wm;

    public static final int MOVE_THRESHOLD = 15;
    public static final int LONG_TIME_THRESHOLD = 300;
    private final String TAG = "overlay service";
    private static final int QUIT_DELAY = 3000;

    private InCallControlView incallView;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds
    private long lastClickTime = 0;
    private float oldX;
    private float oldY;

    private Object callMutex = new Object();

    private List<LinphoneCall> callsInfo = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
    private ViewGroup mainFrame;
    private LinphoneCall currentCall;

    // Screen wake lock for incoming call
    private PowerManager.WakeLock wakeLock;
    // Screen wake lock for video
    private PowerManager.WakeLock videoWakeLock;

    private Timer quitTimer;

    // private LinearLayout detailedContainer, holdContainer;

    // True if running unit tests
    // private boolean inTest;


    private DialingFeedback dialFeedback;
    private PowerManager powerManager;
    //private PreferencesProviderWrapper prefsWrapper;

    // Dnd views
    //private ImageView endCallTarget, holdTarget, answerTarget, xferTarget;
    //private Rect endCallTargetRect, holdTargetRect, answerTargetRect, xferTargetRect;


    private SurfaceView cameraPreview;
    private String result = "";
    private boolean isPressEndCall = false;
    private LinphoneCoreListenerBase mListener;

    /**
     * Service binding
     */
    private boolean serviceConnected = false;
    private int mode = InCallControlView.MODE_INCALL_MAIN;
    private LinphoneService service;
    private ServiceConnection connection;
    private AlertDialog infoDialog;

    @Override
    public void onCreate() {
        super.onCreate();
        mListener = new LinphoneCoreListenerBase() {
            //Linphone Core Listener control the UI when already in a call
            @Override
            public void callState(LinphoneCore lc, final LinphoneCall call, LinphoneCall.State state, String message) {
                if (state == LinphoneCall.State.IncomingReceived && LinphoneManager.isAllowIncomingCall()) {
                    Toast.makeText(InIpCallService.this, "Another call is waiting for response", Toast.LENGTH_SHORT).show();
                    return;
                } else if (state == LinphoneCall.State.Connected)
                {
                    incallView.setCallState(call);
                }
                else if (state == LinphoneCall.State.Paused || state == LinphoneCall.State.PausedByRemote || state == LinphoneCall.State.Pausing) {
                    ///TODO: Change UI when pause the call
                } else if (state == LinphoneCall.State.Resuming) {
                    ///TODO: Change UI when resume the call
                } else if (state == LinphoneCall.State.StreamsRunning) {
                    //TODO: Change UI when The call are on going after connected (after user pick the phone)
                } else if (state == LinphoneCall.State.CallEnd || state == LinphoneCall.State.Error || state == LinphoneCall.State.CallReleased) {
                    //TODO: Close
                    if (service == null && !LinphoneService.isReady())
                        return;

                    if (call != null) {
                        currentCall = call;
                        incallView.setCallState(call);
                    }
                }
            }
        };
    }


    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.cpms_drawing_over_other_app_request, Toast.LENGTH_LONG).show();
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return START_NOT_STICKY;
        } else {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (mView == null) {
                incallView = new InCallControlView(this, wm, getApplicationContext());
                params = incallView.getLayoutParams();
                mView = incallView.getView();
                incallView.setOnchangeViewListener(this);
                incallView.setOnTriggerListener(this);
                mView.setOnTouchListener(new MyTouch());
                wm.addView(mView, params);
                LinphoneManager.getLc().addListener(mListener);
            }
        }
        if (serviceConnected)
            return START_NOT_STICKY;
        if (intent != null) {
            connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName arg0, IBinder arg1) {
                    service = LinphoneService.instance();
                    // Log.d(THIS_FILE,
                    // "Service started get real call info "+callInfo.getCallId());
                    callsInfo = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
                    serviceConnected = true;

                       /* if (incallView != null) {
                            incallView.setCallState(callsInfo[callsInfo.length - 1]);
                        }*/

                    LinphoneCall mainCallInfo = callsInfo.get(callsInfo.size() - 1);
                    if (callsInfo != null) {
                        LinphoneCall.State state = mainCallInfo.getState();
                        //int backgroundResId = R.drawable.bg_in_call_gradient_unidentified
                        // We manage wake lock
                        switch (state.toString()) {
                            case "IncomingReceived":
                            case "IncomingEarlyMedia":
                            case "EarlyUpdating":
                            case "OutgoingInit":
                            case "OutgoingProgress":
                            case "OutgoingRinging":
                            case "OutgoingEarlyMedia":
                            case "StreamsRunning":
                            case "Connected":
                            case "Resuming":
                            case "Updating":
                                if (wakeLock != null && !wakeLock.isHeld()) {
                                    wakeLock.acquire();
                                }
                                break;
                            case "Idle":
                            case "CallEnd":
                            case "Released":
                            case "Error":
                                // This will release locks
                                if (wakeLock != null && wakeLock.isHeld()) {
                                    wakeLock.release();
                                }
                                return;
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                    serviceConnected = false;
                    callsInfo = null;
                    incallView = null;
                    mView = null;
                    service = null;
                }
            };

            LinphoneCall initialSession = LinphoneService.instance().getExtraValue(LinphoneService.EXTRA_CALL_INFO);
            synchronized (callMutex) {
                callsInfo = new ArrayList<>();
                callsInfo.add(initialSession);
                currentCall = initialSession;
            }
            incallView.setCallState(initialSession);

            bindService(new Intent(this, LinphoneService.class), connection, Context.BIND_AUTO_CREATE);
            //prefsWrapper = new PreferencesProviderWrapper(this);

            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "vn.rta.ipcall.onIncomingCall");


            dialFeedback = new DialingFeedback(this, true);

            if (quitTimer == null) {
                quitTimer = new Timer("Quit-timer");
            }

            // Listen to media & sip events to update the UI
            IntentFilter filter = new IntentFilter();
            filter.addAction(SipManager.ACTION_SIP_CALL_CHANGED);
            filter.addAction(ACTION_PHONE_STATE);
            registerReceiver(callStateReceiver, filter);

            wakeLock.setReferenceCounted(false);
        }

        return Service.START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        incallView.endCurrentCall();
        Intent mIntent = new Intent(FormEntryActivity.ACTION_FLOATING_SIP_CALL);
        if (isPressEndCall) {
            result = result + " 1";
        } else
            result = result + " 0";
        mIntent.putExtra("SCAN_RESULT", result);
        LocalBroadcastManager.getInstance(RTASurvey.getInstance().getActivity()).sendBroadcast(mIntent);

        LinphoneManager.getLc().removeListener(mListener);

        try {
            unbindService(connection);
            unregisterReceiver(callStateReceiver);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (mView != null) {
            wm.removeView(mView);
            serviceConnected = false;
            callsInfo = null;
            currentCall = null;
            incallView = null;
            mView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onTrigger(int whichAction, final LinphoneCall callSession) {
        // Sanity check for actions requiring valid call id
        if (whichAction == TAKE_CALL || whichAction == REJECT_CALL || whichAction == DONT_TAKE_CALL ||
                whichAction == TERMINATE_CALL || whichAction == DETAILED_DISPLAY ||
                whichAction == TOGGLE_HOLD || whichAction == START_RECORDING ||
                whichAction == STOP_RECORDING || whichAction == DTMF_DISPLAY ||
                whichAction == XFER_CALL || whichAction == TRANSFER_CALL ||
                whichAction == START_VIDEO || whichAction == STOP_VIDEO) {
            // We check that current call is valid for any actions
            if (callSession == null) {
                return;
            }
            if (callSession.getState() == LinphoneCall.State.Error) {
                return;
            }
        }


        // Reset proximity sensor timer
        //proximityManager.restartTimer();
        switch (whichAction) {
            case TAKE_CALL: {
                if (LinphoneService.isReady()) {
                    boolean shouldHoldOthers = false;

                    // Well actually we should be always before confirmed
                    if (callSession.getState() == LinphoneCall.State.CallIncomingEarlyMedia
                            || callSession.getState() == LinphoneCall.State.OutgoingEarlyMedia
                            || callSession.getState() == LinphoneCall.State.Connected) {
                        shouldHoldOthers = true;
                    }

                    LinphoneManager.getInstance().acceptCall(callSession);

                    // if it's a ringing call, we assume that user wants to
                    // hold other calls
                    if (shouldHoldOthers && callsInfo != null) {
                        for (LinphoneCall callInfo : callsInfo) {
                            // For each active and running call
                            if (LinphoneCall.State.IncomingReceived == callInfo.getState()
                                    && callInfo.getState() != LinphoneCall.State.Paused
                                    && !callInfo.equals(callSession) && LinphoneManager.isAllowIncomingCall()) {
                                LinphoneManager.getInstance().pauseOrResumeCall(callInfo);
                            }
                        }
                    }
                }
                break;
            }
            case DONT_TAKE_CALL: {

                if (LinphoneService.isReady()) {
                    LinphoneManager.getLc().declineCall(callSession, Reason.Busy);
                }
                onDestroy();
                break;
            }
            case REJECT_CALL:
            case TERMINATE_CALL: {
                //incallView.changeView(InCallControlView.MODE_END_CALL);
                if (LinphoneService.isReady()) {
                    LinphoneManager.getLc().terminateCall(callSession);
                    result = result + incallView.getTimeStamp();
                    isPressEndCall = true;
                    incallView.stopElapsedTimer();
                }
                //stopSelf();
                //onDestroy();
                break;
            }

            case MUTE_ON:
                if (LinphoneService.isReady()) {
                    LinphoneManager.getInstance().setEnableMicro(false);
                }
                break;
            case MUTE_OFF: {
                if (LinphoneService.isReady()) {
                   LinphoneManager.getInstance().setEnableMicro(true);
                }
                break;
            }
            case SPEAKER_ON: {
                if (LinphoneService.isReady()) {
                    LinphoneManager.getInstance().routeAudioToSpeaker();
                    LinphoneManager.getLc().enableSpeaker(true);
                } else
                    Toast.makeText(getApplicationContext(), "Speaker is not working!", Toast.LENGTH_SHORT).show();
                break;
            }
            case SPEAKER_OFF: {
                if (LinphoneService.isReady()) {
                    LinphoneManager.getInstance().routeAudioToReceiver();
                    LinphoneManager.getLc().enableSpeaker(false);
                }
                else
                    Toast.makeText(getApplicationContext(), "Speaker is not working!", Toast.LENGTH_SHORT).show();
                break;
            }
            case BLUETOOTH_ON:
            case BLUETOOTH_OFF: {
                if (LinphoneService.isReady()) {
                    //TODO: I dont know what bluetooth gonna help us here
                    //LinphoneManager.getInstance().startBluetooth();
                }
                break;
            }
            case DTMF_DISPLAY: {
                //showDialpad(call.getCallId());
                break;
            }
            case DETAILED_DISPLAY: {
                //TODO: Check here
                /*
                if (service != null) {
                    if (infoDialog != null) {
                        infoDialog.dismiss();
                    }
                    String infos = service.showCallInfosDialog(callSession.getCallId());
                    String natType = service.getLocalNatType();
                    SpannableStringBuilder buf = new SpannableStringBuilder();
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    buf.append(infos);
                    if (!TextUtils.isEmpty(natType)) {
                        buf.append("\r\nLocal NAT type detected : ");
                        buf.append(natType);
                    }
                    TextAppearanceSpan textSmallSpan = new TextAppearanceSpan(this,
                            android.R.style.TextAppearance_Small);
                    buf.setSpan(textSmallSpan, 0, buf.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    infoDialog = builder.setIcon(android.R.drawable.ic_dialog_info)
                            .setMessage(buf)
                            .setNeutralButton(com.csipsimple.R.string.ok, null)
                            .create();
                    infoDialog.show();
                }
                */
                break;
            }
            case TOGGLE_HOLD: {
                if (LinphoneService.isReady()) {
                    // Log.d(THIS_FILE,
                    // "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
                    if (LinphoneManager.getLc().getCurrentCall() != null)
                        LinphoneManager.getInstance().pauseOrResumeCall(LinphoneManager.getLc().getCurrentCall());
                    else
                        LinphoneManager.getInstance().pauseOrResumeCall(callSession);
                }
                break;
            }
            case MEDIA_SETTINGS: {
                //startActivity(new Intent(this, SettingActivity.class));
                break;
            }
            case XFER_CALL: {
                /*Intent pickupIntent = new Intent(this, PickupSipUri.class);
                pickupIntent.putExtra(CALL_ID, call.getCallId());
                startActivityForResult(pickupIntent, PICKUP_SIP_URI_XFER);*/
                break;
            }
            case TRANSFER_CALL: {
                final ArrayList<LinphoneCall> remoteCalls = new ArrayList<>();
                if (callsInfo != null) {
                    for (LinphoneCall remoteCall : callsInfo) {
                        // Verify not current call
                        if (!remoteCall.equals(callSession) && remoteCall.getState() != LinphoneCall.State.Paused && remoteCall.getState() != LinphoneCall.State.CallEnd) {
                            remoteCalls.add(remoteCall);
                        }
                    }
                }

                if (remoteCalls.size() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    CharSequence[] simpleAdapter = new String[remoteCalls.size()];
                    for (int i = 0; i < remoteCalls.size(); i++) {
                        simpleAdapter[i] = remoteCalls.get(i).getRemoteContact();
                    }
                    builder.setSingleChoiceItems(simpleAdapter, -1, new Dialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (service != null) {
                                // 1 = PJSUA_XFER_NO_REQUIRE_REPLACES\

                                LinphoneManager.getLc().transferCallToAnother(LinphoneManager.getLc().getCurrentCall(), remoteCalls.get(which));
                            }
                            dialog.dismiss();
                        }
                    })
                            .setCancelable(true)
                            .setNeutralButton(R.string.cancel, new Dialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }

                break;
            }
            case ADD_CALL: {
                /*Intent pickupIntent = new Intent(this, PickupSipUri.class);
                startActivityForResult(pickupIntent, PICKUP_SIP_URI_NEW_CALL);*/
                break;
            }
            case START_RECORDING: {
                if (LinphoneService.isReady()) {
                    // TODO : add a tweaky setting for two channel recording in different files.
                    // Would just result here in two calls to start recording with different bitmask
                    //service.startRecording(callSession.getCallId(), SipManager.BITMASK_ALL);
                }
                break;
            }
            case STOP_RECORDING: {
                if (LinphoneService.isReady()) {
                    //service.stopRecording(callSession.getCallId());
                }
                break;
            }
            case START_VIDEO:
            case STOP_VIDEO: {
                if (LinphoneService.isReady()) {
                    IPCallManager.getInstance().requestPermissionAndToggleVideo();
                }
                break;
            }
            case ZRTP_TRUST: {
                if (LinphoneService.isReady()) {
                    //LinphonePreferences.instance().setMediaEncryption(LinphoneCore.MediaEncryption.ZRTP);
                    //service.zrtpSASVerified(callSession.getCallId());
                }
                break;
            }
            case ZRTP_REVOKE: {
                if (LinphoneService.isReady()) {
                    //service.zrtpSASRevoke(callSession.getCallId());
                }
                break;
            }
        }
    }

    @Override
    public void onDisplayVideo(boolean show) {

    }


    public boolean shouldActivateProximity() {
        return false;
    }

    @Override
    public void onDtmf(LinphoneCall call, int keyCode, int dialTone) {
        //proximityManager.restartTimer();
        if (service != null) {
            if (call != null) {
                LinphoneManager.getLc().sendDtmf((char) keyCode);
                dialFeedback.giveFeedback(dialTone);
            }
        }
    }

    private final class MyTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent me) {
            if (wm == null || incallView == null)
                return true;
            if (me.getAction() == MotionEvent.ACTION_DOWN) {
                DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                float Xvalue = width - me.getRawX();
                float Yvalue = me.getRawY();
                oldX = Xvalue;
                oldY = Yvalue;
                params.x = (int) Xvalue;
                params.y = (int) Yvalue;
                lastClickTime = System.currentTimeMillis();
                incallView.updateParam(params);
            } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
                DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                float Xvalue = width - me.getRawX();
                float Yvalue = me.getRawY();
                params.x = (int) Xvalue;
                params.y = (int) Yvalue;
                long time = System.currentTimeMillis() - lastClickTime;
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUICK_NOTE, "Update Location: x:" + Xvalue + " y:" + Yvalue);
                if (Math.abs(oldX - Xvalue) > MOVE_THRESHOLD || Math.abs(oldY - Yvalue) > MOVE_THRESHOLD) {
                    wm.updateViewLayout(mView, params);
                }
                incallView.updateParam(params);
            } else if (me.getAction() == MotionEvent.ACTION_UP) {
                if (mode == InCallControlView.MODE_INCALL_MINI) {
                    long time = System.currentTimeMillis() - lastClickTime;
                    DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();

                    int width = metrics.widthPixels;
                    int height = metrics.heightPixels;
                    float Xvalue = width - me.getRawX();
                    float Yvalue = me.getRawY();

                    params.x = (int) Xvalue;
                    params.y = (int) Yvalue;
                    if ((Math.abs(oldX - Xvalue) <= MOVE_THRESHOLD && Math.abs(oldY - Yvalue) <= MOVE_THRESHOLD) && (time < LONG_TIME_THRESHOLD)) {
                        incallView.changeView(InCallControlView.MODE_INCALL_MAIN);
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public void onChangeViewListener(int mode) {
        wm.removeView(mView);
        mView = incallView.getView();
        params = incallView.getLayoutParams();
        wm.addView(mView, params);
        this.mode = mode;
        incallView.setOnDtmfListener(this,/*callsInfo[0].getCallId()*/currentCall);
        mView.setOnTouchListener(new MyTouch());
        if (mode == InCallControlView.MODE_END_CALL) {
            stopSelf();
        }
    }

    private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(SipManager.ACTION_SIP_CALL_CHANGED)) {
                if (service != null) {
                    synchronized (callMutex) {
                        if ((service == null && !LinphoneService.isReady()) || LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc()).isEmpty())
                            return;
                        callsInfo = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
                        for (int i = 0; i < callsInfo.size(); i++) {
                            currentCall = callsInfo.get(i);
                            LinphoneCall.State state = currentCall.getState();
                            if (state != LinphoneCall.State.Idle || state != LinphoneCall.State.CallReleased) {
                                break;
                            }
                        }
                        if (currentCall != null) {
                            LinphoneCall mainCallInfo = currentCall;
                            LinphoneCall.State state = mainCallInfo.getState();

                            //int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;

                            // We manage wake lock
                            switch (state.toString()) {
                                case "IncomingReceived":
                                case "IncomingEarlyMedia":
                                case "EarlyUpdating":
                                case "OutgoingInit":
                                case "OutgoingProgress":
                                case "OutgoingRinging":
                                case "OutgoingEarlyMedia":
                                case "StreamsRunning":
                                case "Connected":
                                case "Resuming":
                                case "Updating":
                                    if (wakeLock != null && !wakeLock.isHeld()) {
                                        wakeLock.acquire();
                                    }
                                    break;
                                case "Idle":
                                case "CallEnd":
                                case "Released":
                                case "Error":
                                    // This will release locks
                                    //incallView.stopElapsedTimer();
                                    result = result + incallView.getTimeStamp();
                                    //RTALog.d("test call", " state = DISCONNECTED + start time = " + mainCallInfo.getConnectStart());
                                    //RTALog.d("test call", " state = DISCONNECTED + start time = " + mainCallInfo.getConnectStart() + " and last status = " + mainCallInfo.getLastStatusComment());
                                    //stopSelf();
                                    //onDestroy();
                                    return;

                            }
                        }
                    }
                }
            } else if (action.equals(SipManager.ACTION_SIP_MEDIA_CHANGED)) {

            } else if (action.equals(SipManager.ACTION_ZRTP_SHOW_SAS)) {

            } else if (action.equals(ACTION_PHONE_STATE)) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                        TelephonyManager.EXTRA_STATE_RINGING)) {

                    // Phone number
                    String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    // Ringing state
                    // This code will execute when the phone has an incoming call
                } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                        TelephonyManager.EXTRA_STATE_IDLE)
                        || intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                        TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    // This code will execute when the call is answered or disconnected
                }
            }
        }
    };


    public class CallReciever extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


        }
    }

}