package vn.rta.ipcall.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rta.ipcall.ContactsManager;
import com.rta.ipcall.LinphoneContact;
import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphoneService;
import com.rta.ipcall.LinphoneUtils;
import com.rta.ipcall.ui.OnUpdateUIListener;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.Reason;
import org.odk.collect.android.activities.FormEntryActivity;

import java.util.List;

import vn.rta.cpms.activities.NetworkHelper;
import vn.rta.survey.android.R;

import static vn.rta.ipcall.ui.IPCallActivity.KEY_IPCALL_SERVICE_READY;

public class OutgoingIPCallActivity extends AppCompatActivity {
    private static final String TAG = OutgoingIPCallActivity.class.getSimpleName();
    private static boolean isInstantiated = false;
    private ImageView btnHangUp;
    private ImageView btnMic, btnSpeaker;
    private TextView txtPhoneNumber;
    private LinphoneCall mCall;
    private LinphoneCoreListenerBase mListener;
    private CallLoadingIndicatorView indicator;
    private Chronometer timer;
    private Toolbar toolbar;
    private String registrationStatus = "";
    private OnUpdateUIListener updateUIListener;

    private boolean isMicMuted = false;
    private boolean isSpeakerEnabled = false;
    private boolean isIPCallReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_ipcall);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        getWindow().addFlags(flags);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.USE_SIP) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.USE_SIP, Manifest.permission.RECORD_AUDIO},
                    FormEntryActivity.PERMISSIONS_REQUEST);
        }

        if (isInstantiated) {
            finish();
        } else {
            isInstantiated = true;
        }

        isIPCallReady = getIntent().getBooleanExtra(KEY_IPCALL_SERVICE_READY, false);

        setupUI();
        setupEventHandler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ipcall_status, menu);
        if (isIPCallReady)
            menu.findItem(R.id.mi_status).setIcon(R.drawable.ic_high_connection);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.mi_status) {
            try {
                if (NetworkHelper.newInstance(OutgoingIPCallActivity.this).isOnline()) {
                    if (!registrationStatus.isEmpty())
                        Toast.makeText(OutgoingIPCallActivity.this, registrationStatus, Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(OutgoingIPCallActivity.this, "No Internet Connection", Toast.LENGTH_LONG).show();
                }
            }
            catch (IllegalStateException ise) {
                ise.printStackTrace();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupEventHandler() {
        btnHangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinphoneManager.getLc().terminateCall(mCall);
                finish();
            }
        });

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMicMuted = !isMicMuted;
                if (isMicMuted) {
                    //Mic is muting -> unmute
                    btnMic.setImageResource(R.drawable.ic_mic_off_white);
                } else {
                    btnMic.setImageResource(R.drawable.ic_mic_white);
                }
                //Mute mic = true -> set enable mic = false
                LinphoneManager.getInstance().setEnableMicro(!isMicMuted);
            }
        });

        btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSpeakerEnabled = !isSpeakerEnabled;
                if (isSpeakerEnabled) {
                    btnSpeaker.setImageResource(R.drawable.ic_volume_white);
                    LinphoneManager.getInstance().routeAudioToSpeaker();
                    LinphoneManager.getLc().enableSpeaker(isSpeakerEnabled);
                } else {
                    btnSpeaker.setImageResource(R.drawable.ic_low_volume_white);
                    LinphoneManager.getInstance().routeAudioToReceiver();
                }
            }
        });

        if (LinphoneService.isReady()) {
            if (updateUIListener != null && LinphoneService.isReady()) {
                LinphoneService.removeOnUpdateUIListener(updateUIListener);
            }
            updateUIListener = new OnUpdateUIListener() {
                @Override
                public void updateUIByServiceStatus(boolean serviceConnected) {
                    changeIPCallStatus(serviceConnected);
                }

                @Override
                public void registrationState(boolean isConnected, String statusMessage) {
                    registrationStatus = statusMessage;
                    changeIPCallStatus(isConnected);
                }

                @Override
                public void updateToCallWidget(boolean isCalled) {

                }

                @Override
                public void launchIncomingCallActivity() {

                }

                @Override
                public void dismissCallActivity() {

                }
            };
            LinphoneService.addOnUpdateUIListener(updateUIListener);
        }


        mListener = new LinphoneCoreListenerBase() {
            @Override
            public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
                try {
                    if (call == mCall && LinphoneCall.State.Connected == state) {
                        changeToIncallScreen();
                    } else if (state == LinphoneCall.State.Error) {
                        // Convert LinphoneCore message for internalization
                        if (call.getErrorInfo().getReason() == Reason.Declined) {
                            Toast.makeText(OutgoingIPCallActivity.this, getString(R.string.error_call_declined), Toast.LENGTH_SHORT).show();
                        } else if (call.getErrorInfo().getReason() == Reason.NotFound) {
                            Toast.makeText(OutgoingIPCallActivity.this, getString(R.string.error_user_not_found), Toast.LENGTH_SHORT).show();
                        } else if (call.getErrorInfo().getReason() == Reason.Media) {
                            Toast.makeText(OutgoingIPCallActivity.this, getString(R.string.error_incompatible_media), Toast.LENGTH_SHORT).show();
                        } else if (call.getErrorInfo().getReason() == Reason.Busy) {
                            Toast.makeText(OutgoingIPCallActivity.this, getString(R.string.error_user_busy), Toast.LENGTH_SHORT).show();
                        } else if (message != null) {
                            Toast.makeText(OutgoingIPCallActivity.this, getString(R.string.error_unknown) + " - " + message, Toast.LENGTH_SHORT).show();
                        }

                    } else if (state == LinphoneCall.State.CallEnd) {
                        // Convert LinphoneCore message for internalization
                        if (call.getErrorInfo().getReason() == Reason.Declined) {
                            Toast.makeText(OutgoingIPCallActivity.this, getString(R.string.error_call_declined), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (UnsatisfiedLinkError error) {
                    error.printStackTrace();
                    Toast.makeText(OutgoingIPCallActivity.this, message, Toast.LENGTH_SHORT).show();
                }

                if (LinphoneManager.getLc().getCallsNb() == 0) {
                    finish();
                }
            }
        };
    }

    private void setupUI() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnHangUp = (ImageView) findViewById(R.id.outgoing_hang_up);
        btnMic = (ImageView) findViewById(R.id.micro);
        btnSpeaker = (ImageView) findViewById(R.id.speaker);
        indicator = (CallLoadingIndicatorView) findViewById(R.id.av_loading);
        timer = (Chronometer) findViewById(R.id.elapsedTime);
        txtPhoneNumber = (TextView) findViewById(R.id.txt_phone_number);
        indicator.smoothToShow();
        if (toolbar != null)
            setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isMicMuted = LinphoneManager.getLc().isMicMuted();
        if (isMicMuted)
            btnMic.setImageResource(R.drawable.ic_mic_off_white);
        else
            btnMic.setImageResource(R.drawable.ic_mic_white);

        isSpeakerEnabled = LinphoneManager.getLc().isSpeakerEnabled();
        if (isSpeakerEnabled)
            btnSpeaker.setImageResource(R.drawable.ic_volume_white);
        else
            btnSpeaker.setImageResource(R.drawable.ic_low_volume_white);
    }


    @Override
    protected void onResume() {
        super.onResume();
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }

        // Only one call ringing at a time is allowed
        if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
            List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
            for (LinphoneCall call : calls) {
                LinphoneCall.State cstate = call.getState();
                if (LinphoneCall.State.OutgoingInit == cstate || LinphoneCall.State.OutgoingProgress == cstate
                        || LinphoneCall.State.OutgoingRinging == cstate || LinphoneCall.State.OutgoingEarlyMedia == cstate) {
                    mCall = call;
                    break;
                }
                if (LinphoneCall.State.StreamsRunning == cstate) {
                    if (!IPCallActivity.isInstanciated()) {
                        return;
                    }
                    changeToIncallScreen();
                    return;
                }
            }
        }
        if (mCall == null || LinphoneManager.getLc().getCalls().length == 0) {
            Log.e(TAG, "Couldn't find outgoing call");
            finish();
            return;
        }

        LinphoneAddress address = mCall.getRemoteAddress();
        LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        if (contact != null) {
            //LinphoneUtils.setImagePictureFromUri(this, contactPicture, contact.getPhotoUri(), contact.getThumbnailUri());
            txtPhoneNumber.setText(contact.getFullName());
        } else {
            txtPhoneNumber.setText(LinphoneUtils.getAddressDisplayName(address));
        }
    }

    @Override
    protected void onPause() {
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.removeListener(mListener);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        isInstantiated = false;
        LinphoneManager.getInstance().terminateCall();
        if (updateUIListener != null)
            LinphoneService.removeOnUpdateUIListener(updateUIListener);
        super.onDestroy();
    }

    public void changeIPCallStatus(boolean isReady) {
        isIPCallReady = isReady;
        if (isReady)
        {
            MenuItem status = toolbar.getMenu().findItem(R.id.mi_status);
            status.setIcon(R.drawable.ic_high_connection);
        }
        else {
            MenuItem status = toolbar.getMenu().findItem(R.id.mi_status);
            status.setIcon(R.drawable.ic_no_connection);
        }
    }

    private void changeToIncallScreen() {
        indicator.smoothToHide();
        TextView tv = (TextView) findViewById(R.id.txt_call_status);
        if (tv != null) {
            tv.setVisibility(View.GONE);
        }
        timer.setVisibility(View.VISIBLE);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
    }
}
