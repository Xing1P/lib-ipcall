package vn.rta.survey.android.views;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.linphone.core.LinphoneCall;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.rta.ipcall.ui.IOnCallActionTrigger;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.Constants;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.audiorecord.manager.MediaManager;
import vn.rta.survey.android.manager.ActivityLogManager;
import vn.rta.survey.android.utilities.RTALog;

/**
 * Created by ThiNguyen on 8/29/16.
 */
public class InCallControlView {
    public interface ChangeViewListener {
        public void onChangeViewListener(int mode);
    }

    public interface OnDtmfListener {
        void onDtmf(LinphoneCall call, int keyCode, int dialTone);
    }

    // Here we need a map to quickly find if the clicked button id is in the map keys
    @SuppressLint("UseSparseArrays")
    private static final Map<Integer, int[]> DIGITS_BTNS = new HashMap<Integer, int[]>();

    static {
        DIGITS_BTNS.put(0, new int[]{ToneGenerator.TONE_DTMF_0, KeyEvent.KEYCODE_0});
        DIGITS_BTNS.put(1, new int[]{ToneGenerator.TONE_DTMF_1, KeyEvent.KEYCODE_1});
        DIGITS_BTNS.put(2, new int[]{ToneGenerator.TONE_DTMF_2, KeyEvent.KEYCODE_2});
        DIGITS_BTNS.put(3, new int[]{ToneGenerator.TONE_DTMF_3, KeyEvent.KEYCODE_3});
        DIGITS_BTNS.put(4, new int[]{ToneGenerator.TONE_DTMF_4, KeyEvent.KEYCODE_4});
        DIGITS_BTNS.put(5, new int[]{ToneGenerator.TONE_DTMF_5, KeyEvent.KEYCODE_5});
        DIGITS_BTNS.put(6, new int[]{ToneGenerator.TONE_DTMF_6, KeyEvent.KEYCODE_6});
        DIGITS_BTNS.put(7, new int[]{ToneGenerator.TONE_DTMF_7, KeyEvent.KEYCODE_7});
        DIGITS_BTNS.put(8, new int[]{ToneGenerator.TONE_DTMF_8, KeyEvent.KEYCODE_8});
        DIGITS_BTNS.put(9, new int[]{ToneGenerator.TONE_DTMF_9, KeyEvent.KEYCODE_9});
        DIGITS_BTNS.put(10, new int[]{ToneGenerator.TONE_DTMF_P, KeyEvent.KEYCODE_POUND});
        DIGITS_BTNS.put(11, new int[]{ToneGenerator.TONE_DTMF_S, KeyEvent.KEYCODE_STAR});
    }

    ;

    private static final String THIS_FILE = "InCallControls";
    private static final String TAG = "InCallControlView";
    public static final int MODE_INCALL_MAIN = 1;
    public static final int MODE_INCALL_MINI = 2;
    public static final int MODE_INCALL_KEY_PAD = 3;
    public static final int MODE_END_CALL = 4;

    IOnCallActionTrigger onTriggerListener;

    private LinphoneCall currentCall;
    //private MenuBuilder btnMenuBuilder;
    private boolean supportMultipleCalls = false;
    private View mView;
    private TextView statusCallingInMain;
    private ImageView muteButton;
    private ImageView keyBoardButton;
    private ImageView miniView;
    private ImageView holdButton;
    private ImageView speakerButton;
    private LinearLayout endCallView;
    private Chronometer elapsedTime;

    private TextView endTime;
    private OnDtmfListener onDtmfListener;
    private int callID;
    private int keyTone;
    private long timeStart;
    //keep track
    private boolean isMute = false;
    private boolean isHold = false;
    private boolean isSpeaker = false;

    //mini view mode
    private ImageView expandButton;

    //key pad mode
    private ImageView backspace;
    private TextView phoneNumber;
    private TextView statusCallingInPad;
    private ImageView viewBackButton;
    private ImageView oneButton;
    private ImageView twoButton;
    private ImageView threeButton;
    private ImageView fourButton;
    private ImageView fiveButton;
    private ImageView sixButton;
    private ImageView sevenButton;
    private ImageView eightButton;
    private ImageView nineButton;
    private ImageView zeroButton;
    private ImageView starButton;
    private ImageView shapeButton;
    private LinearLayout doneButton;
    private LinearLayout endCallButtonInPad;
    private View inMainView;
    private View keyPadView;

    private StringBuilder numberCall = new StringBuilder();

    private static Service mService;
    private WindowManager.LayoutParams params;
    private WindowManager wm;
    private LayoutInflater inflater;
    private String title;
    private Context mcontext;
    private int modeView = MODE_INCALL_MAIN;
    private String mDualTime = "";

    public static final String EXT_RTA = ".rta";
    private static final String DEFAULT_TEXT_NOTE_FILE_NAME = "ip_call_%s%s";
    //recording
    private static final int COUNTER_UPDATE_MSG_ID = 123;
    private static final String DEFAULT_AUDIO_FILE_NAME = "rs_audio_%s%s";
    public static final String AUDIO_FILE_NAME_PREFIX = "rs_audio";
    public static final String EXT_3GP = ".3gp";
    private static final String FILE_NAME_DEFAUL = "defaut";
    // default setting
    private File mRecordingLocation;

    private Handler mHandler;
    private static String fileName = FILE_NAME_DEFAUL;
    private AtomicBoolean mIsPlaying = new AtomicBoolean(false);
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);

    // counter that will be displayed on the screen
    private TextView callStatus;

    // Thread pool
    private ExecutorService mThreadPool;

    // Media Manager object reference
    private MediaManager mMediaManager;
    private AlertDialog alert;
    private ChangeViewListener changeViewListener;
    private boolean isFullView = true;
    private boolean isRecoding = false;
    private boolean isFirstChangeKeyPad = true;

    private static int count = 0;
    public static final WindowManager.LayoutParams PARAM = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,//width
            WindowManager.LayoutParams.WRAP_CONTENT,//height
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT);

    public InCallControlView(Service ser, WindowManager wm, Context mcontext) {
        this.mcontext = mcontext;
        InCallControlView.mService = ser;
        this.setWm(wm);
        params = PARAM;
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        DisplayMetrics metrics = ser.getResources().getDisplayMetrics();
        RTALog.d("test quick note", " metrics width pixels " + metrics.widthPixels + " height = " + metrics.heightPixels);

        params.x = metrics.widthPixels / 5;
        params.y = metrics.heightPixels / 4;
        RTALog.dWithRec(TAG, "x: " + metrics.widthPixels / 5 + " y = " + metrics.heightPixels / 4);

        inflater = (LayoutInflater) mService.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.sip_incall, null);
        inMainView = mView;
        initMainControls();

    }

    public void setOnchangeViewListener(ChangeViewListener listener) {
        changeViewListener = listener;
    }

    public void endCurrentCall() {
        dispatchTriggerEvent(IOnCallActionTrigger.REJECT_CALL);
    }

    public void updateParam(WindowManager.LayoutParams params) {
        this.params = params;
    }

    private void initMainControls() {
        if (muteButton == null) {
            muteButton = (ImageView) mView.findViewById(R.id.mute_button);
            muteButton.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onClick(View v) {
                    if (isMute) {
                        dispatchTriggerEvent(IOnCallActionTrigger.MUTE_OFF);
                        isMute = false;
                        muteButton.setBackground(mService.getResources().getDrawable(R.drawable.sip_ic_mute));
                    } else {
                        dispatchTriggerEvent(IOnCallActionTrigger.MUTE_ON);
                        isMute = true;
                        muteButton.setBackground(mService.getResources().getDrawable(R.drawable.sip_ic_mute_press));
                    }
                }
            });
        }
        if (keyBoardButton == null) {
            keyBoardButton = (ImageView) mView.findViewById(R.id.key_pad_button);
            keyBoardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mView = inflater.inflate(R.layout.sip_incall_number_key, null);
                    initKeyPadView();
                    changeViewListener.onChangeViewListener(MODE_INCALL_KEY_PAD);
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUICK_NOTE, Constants.BUTTON_MINI_VIEW_QUICK_NOTE);
                }
            });

        }

        if (miniView == null) {
            miniView = (ImageView) mView.findViewById(R.id.mini_view_button);
            miniView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mView = inflater.inflate(R.layout.sip_call_mini_view, null);
                    initMiniView();
                    changeViewListener.onChangeViewListener(MODE_INCALL_MINI);
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUICK_NOTE, Constants.BUTTON_MINI_VIEW_QUICK_NOTE);
                }
            });
        }
        if (speakerButton == null) {
            speakerButton = (ImageView) mView.findViewById(R.id.loud_button);
            speakerButton.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onClick(View v) {
                    if (!isSpeaker) {
                        dispatchTriggerEvent(IOnCallActionTrigger.SPEAKER_ON);
                        isSpeaker = true;
                        speakerButton.setBackground(mService.getResources().getDrawable(R.drawable.sip_ic_speaker_press));
                    } else {
                        dispatchTriggerEvent(IOnCallActionTrigger.SPEAKER_OFF);
                        speakerButton.setBackground(mService.getResources().getDrawable(R.drawable.sip_ic_speaker));
                        isSpeaker = false;
                    }
                }
            });


        }
        if (holdButton == null) {
            holdButton = (ImageView) mView.findViewById(R.id.hold_button);
            holdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTriggerEvent(IOnCallActionTrigger.TOGGLE_HOLD);

                }
            });
        }

        if (endCallView == null) {
            endCallView = (LinearLayout) mView.findViewById(R.id.end_call_button);
            endCallView.setVisibility(View.VISIBLE);
            endCallView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDualTime = elapsedTime.getText().toString();
                    changeView(MODE_END_CALL);
                    dispatchTriggerEvent(IOnCallActionTrigger.REJECT_CALL);
                    /*elapsedTime.stop();
                    elapsedTime.setVisibility(View.VISIBLE);*/
                }
            });

        }
        if (elapsedTime == null)
            elapsedTime = (Chronometer) mView.findViewById(R.id.elapsedTime);

    }

    public void changeView(int mode) {
        modeView = mode;
        if (modeView == MODE_INCALL_MINI) {
            mView = inflater.inflate(R.layout.sip_call_mini_view, null);
            initMiniView();
        } else if (modeView == MODE_INCALL_KEY_PAD) {
            if (keyPadView == null) {
                mView = inflater.inflate(R.layout.sip_incall_number_key, null);
                keyPadView = mView;
            } else mView = keyPadView;
            initKeyPadView();
        } else if (modeView == MODE_INCALL_MAIN) {
            if (inMainView == null) {
                mView = inflater.inflate(R.layout.sip_incall, null);
                inMainView = mView;
            } else mView = inMainView;
            initMainControls();
        } else {
            mView = inflater.inflate(R.layout.sip_end_call, null);
            initEndControl();
        }
        changeViewListener.onChangeViewListener(mode);
    }

    public void updateStatusRecoder() {

    }

    private void initEndControl() {
        endTime = (TextView) mView.findViewById(R.id.end_sip_time);
        if (mDualTime != null)
            endTime.setText(mDualTime);
    }

    private void initMiniView() {
    }

    private void initKeyPadView() {
        numberCall = new StringBuilder("");
        viewBackButton = (ImageView) mView.findViewById(R.id.back_sip);
        oneButton = (ImageView) mView.findViewById(R.id.button1);
        twoButton = (ImageView) mView.findViewById(R.id.button2);
        threeButton = (ImageView) mView.findViewById(R.id.button3);
        fourButton = (ImageView) mView.findViewById(R.id.button4);
        fiveButton = (ImageView) mView.findViewById(R.id.button5);
        sixButton = (ImageView) mView.findViewById(R.id.button6);
        sevenButton = (ImageView) mView.findViewById(R.id.button7);
        eightButton = (ImageView) mView.findViewById(R.id.button8);
        nineButton = (ImageView) mView.findViewById(R.id.button9);
        zeroButton = (ImageView) mView.findViewById(R.id.button0);
        starButton = (ImageView) mView.findViewById(R.id.button_star);
        shapeButton = (ImageView) mView.findViewById(R.id.button_shape);
        doneButton = (LinearLayout) mView.findViewById(R.id.done_layout);
        phoneNumber = (TextView) mView.findViewById(R.id.number_sip);
        phoneNumber.setText("");
        endCallButtonInPad = (LinearLayout) mView.findViewById(R.id.end_call_layout);
        backspace = (ImageView) mView.findViewById(R.id.backspace_sip);

        viewBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeView(MODE_INCALL_MAIN);
            }
        });

        oneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("1");
                phoneNumber.setText(numberCall.toString());
            }
        });

        twoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("2");
                phoneNumber.setText(numberCall.toString());
            }
        });

        threeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("3");
                phoneNumber.setText(numberCall.toString());

            }
        });

        fourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("4");
                phoneNumber.setText(numberCall.toString());

            }
        });

        fiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("5");
                phoneNumber.setText(numberCall.toString());

            }
        });

        sixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("6");
                phoneNumber.setText(numberCall.toString());

            }
        });

        sevenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("7");
                phoneNumber.setText(numberCall.toString());

            }
        });

        eightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("8");
                phoneNumber.setText(numberCall.toString());
            }
        });
        nineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("9");
                phoneNumber.setText(numberCall.toString());

            }
        });

        zeroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("0");
                phoneNumber.setText(numberCall.toString());
            }
        });

        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("*");
                phoneNumber.setText(numberCall.toString());

            }
        });

        shapeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberCall.append("#");
                phoneNumber.setText(numberCall.toString());

            }
        });

        backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numberCall.length() > 0) {
                    numberCall.deleteCharAt(numberCall.length() - 1);
                    phoneNumber.setText(numberCall.toString());

                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numberCall.length() > 0) {
                    if (numberCall.toString().contains("*") || numberCall.toString().contains("#"))
                        return;
                    int keycode = Integer.parseInt(numberCall.toString());
                    onDtmfListener.onDtmf(currentCall, keycode, keycode);
                    numberCall = new StringBuilder("");


                }

            }
        });

        endCallButtonInPad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTriggerEvent(IOnCallActionTrigger.REJECT_CALL);
                if (elapsedTime != null) {
                    elapsedTime.stop();
                    elapsedTime.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public View getView() {
        return mView;
    }

    public WindowManager.LayoutParams getLayoutParams() {
        return params;
    }

    private boolean callOngoing = false;

    public void setEnabledMediaButtons(boolean isInCall) {
        callOngoing = isInCall;
    }

    public String getTimeStamp() {
        return "" + (System.currentTimeMillis() - timeStart);
    }

    public void setCallState(LinphoneCall callInfo) {
        currentCall = callInfo;
        if (currentCall == null) {
            mView.setVisibility(View.GONE);
            return;
        }

        LinphoneCall.State state = currentCall.getState();

        switch (state.toString()) {
            case "IncomingEarlyMedia":
                mView.setVisibility(View.GONE);
                break;

            case "StreamsRunning":
            case "Updating":
            case "Connected":
                mView.setVisibility(View.VISIBLE);
                setEnabledMediaButtons(true);
                break;

            case "Idle":
            case "CallEnd":
            case "Released":
            case "Error":
                mDualTime = elapsedTime.getText().toString();
                changeView(MODE_END_CALL);
                break;

            default:

                if (currentCall.getState() == LinphoneCall.State.IncomingReceived || currentCall.getState() == LinphoneCall.State.CallIncomingEarlyMedia) {
                    mView.setVisibility(View.GONE);
                } else {
                    mView.setVisibility(View.VISIBLE);
                    setEnabledMediaButtons(true);
                }
                break;
        }

        updateElapsedTimer();
    }

    /**
     * Registers a callback to be invoked when the user triggers an event.
     *
     * @param listener the OnTriggerListener to attach to this view
     */
    public void setOnTriggerListener(IOnCallActionTrigger listener) {
        onTriggerListener = listener;
    }


    public WindowManager getWm() {
        return wm;
    }

    public void setWm(WindowManager wm) {
        this.wm = wm;
    }

    /**
     * @param variableName - name of drawable, e.g R.drawable.<b>image</b>
     * @return integer id of resource
     * @author Lonkly
     */
    public static int getResId(String variableName, Class<?> class1) {

        Field field = null;
        int resId = 0;
        try {
            field = class1.getField(variableName);
            try {
                resId = field.getInt(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resId;

    }

    //----------Method to create an AlertBox -------------
    public void alertbox(String title, String mymessage) {
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUICK_NOTE, "Microphone is in use, please try again later");
        AlertDialog.Builder builder = new AlertDialog.Builder(RTASurvey.getInstance().getActivity());
        builder.setMessage("Microphone is in use, please try again later")
                .setTitle("Error!!")
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                alert.dismiss();
                            }

                        });
        alert = builder.create();
        try {
            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dispatchTriggerEvent(int whichHandle) {
        if (onTriggerListener != null) {
            onTriggerListener.onTrigger(whichHandle, currentCall);
        }
    }

    public void setOnDtmfListener(OnDtmfListener onDtmfListener, LinphoneCall call) {
        currentCall = call;
        this.onDtmfListener = onDtmfListener;
    }

    public void stopElapsedTimer() {
        elapsedTime.stop();
        elapsedTime.setVisibility(View.VISIBLE);
    }

    public void updateElapsedTimer() {
        if (currentCall == null) {
            elapsedTime.stop();
            elapsedTime.setVisibility(View.VISIBLE);
            return;
        }

        LinphoneCall.State state = currentCall.getState();
        switch (state.toString()) {
            case "IncomingReceived":
            case "IncomingEarlyMedia":
            case "EarlyUpdating":
            case "OutgoingInit":
            case "OutgoingProgress":
            case "OutgoingRinging":
            case "OutgoingEarlyMedia":
                elapsedTime.setVisibility(View.GONE);
                break;
            case "Connected":
                timeStart = System.currentTimeMillis();
                if (currentCall.getState() == LinphoneCall.State.Paused) {
                    elapsedTime.stop();
                    elapsedTime.setVisibility(View.GONE);
                } else {
                    elapsedTime.start();
                    elapsedTime.setVisibility(View.VISIBLE);
                    elapsedTime.setBase(SystemClock.elapsedRealtime());
                }
                updateView();
                endCallView.setVisibility(View.VISIBLE);
                break;
            case "Idle":
            case "CallEnd":
            case "Released":
            case "Error":
                elapsedTime.stop();
                elapsedTime.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }

    }

    public void updateView() {
        if (muteButton != null)
            muteButton.setEnabled(true);
        if (miniView != null)
            miniView.setEnabled(true);
        if (holdButton != null)
            holdButton.setEnabled(true);
        if (speakerButton != null)
            speakerButton.setEnabled(true);
        if (keyBoardButton != null)
            keyBoardButton.setEnabled(true);
    }
}
