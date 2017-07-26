package vn.rta.ipcall.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rta.ipcall.ContactsManager;
import com.rta.ipcall.LinphoneContact;
import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphonePreferences;
import com.rta.ipcall.LinphoneService;
import com.rta.ipcall.LinphoneUtils;
import com.rta.ipcall.ui.OnUpdateUIListener;

import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import vn.rta.cpms.activities.NetworkHelper;
import vn.rta.survey.android.R;

interface ContactPicked {
    void setAddresGoToDialerAndCall(String number, String name, Uri photo);
}

public class IPCallActivity extends AppCompatActivity implements ContactPicked {
    public static final String FRAGMENT_TYPE_EMPTY = "empty_fragment";
    public static final String FRAGMENT_TYPE_DIALER = "dialer_fragment";
    public static final String FRAGMENT_TYPE_HISTORY_LIST = "history_list_fragment";
    public static final String FRAGMENT_TYPE_HISTORY_DETAIL = "history_detail_fragment";
    public static final String KEY_IPCALL_SERVICE_READY = "key_ipcall_service_ready";
    private static IPCallActivity instance;
    private final String TAG = this.getClass().getSimpleName();
    private View dialerSelect, historySelect;
    private Toolbar toolbar;
    private ImageButton btnCall;
    private ImageButton btnCallHistory;
    private ImageButton btnDialer;
    private LinearLayout mBottomTabbar;
    private String currentFragmentType = FRAGMENT_TYPE_EMPTY;
    private Fragment fragment;
    private boolean callTransfer = false;
    private LinphoneCoreListenerBase mListener;
    private OnUpdateUIListener updateUIListener;
    private boolean isIPCallReady = false;

    static final boolean isInstanciated() {
        return instance != null;
    }

    public static final IPCallActivity getInstance() {
        if (instance != null)
            return instance;
        throw new RuntimeException("IPCallActivity not instantiated yet");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_ipcall);

        isIPCallReady = getIntent().getBooleanExtra(KEY_IPCALL_SERVICE_READY, false);

        setupUI();
        setupEventHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
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

    private void setupUI() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        btnCall = (ImageButton) findViewById(R.id.btn_call);
        btnCallHistory = (ImageButton) findViewById(R.id.btn_call_history);
        btnDialer = (ImageButton) findViewById(R.id.btn_dialer);
        mBottomTabbar = (LinearLayout) findViewById(R.id.bottom_bar_button);
        dialerSelect = findViewById(R.id.view_dialer_select);
        historySelect = findViewById(R.id.view_history_select);

        if (toolbar != null)
            setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        changeCurrentFragment(FRAGMENT_TYPE_DIALER, getIntent().getExtras());
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
                if (NetworkHelper.newInstance(IPCallActivity.this).isOnline()) {
                    LinphoneManager.getLc().refreshRegisters();
                }
                else {
                    Toast.makeText(IPCallActivity.this, getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                }
            }
            catch (IllegalStateException ise) {
                ise.printStackTrace();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (updateUIListener != null)
            LinphoneService.removeOnUpdateUIListener(updateUIListener);
        super.onDestroy();
    }

    private void setupEventHandler() {
        btnCallHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCurrentFragment(FRAGMENT_TYPE_HISTORY_LIST, null);
                historySelect.setVisibility(View.VISIBLE);
                LinphoneManager.getLc().resetMissedCallsCount();
            }
        });

        btnDialer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCurrentFragment(FRAGMENT_TYPE_DIALER, null);
                dialerSelect.setVisibility(View.VISIBLE);
            }
        });

        mListener = new LinphoneCoreListenerBase() {
            @Override
            public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
                if (state == LinphoneCall.State.IncomingReceived) {
                    startActivity(new Intent(IPCallActivity.this, IncomingIPCallActivity.class));
                } else if (state == LinphoneCall.State.OutgoingInit || state == LinphoneCall.State.OutgoingProgress) {
                    Intent intent = new Intent(IPCallActivity.this, OutgoingIPCallActivity.class);
                    intent.putExtra(IPCallActivity.KEY_IPCALL_SERVICE_READY, isIPCallReady);
                    startActivity(intent);
                } else if (state == LinphoneCall.State.CallEnd || state == LinphoneCall.State.Error || state == LinphoneCall.State.CallReleased) {
                    resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
                }
            }
        };

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

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (NetworkHelper.newInstance(IPCallActivity.this).isOnline()) {
                        if (LinphoneService.isReady() && isIPCallReady) {
                            if (!LinphoneManager.getInstance().acceptCallIfIncomingPending() && fragment != null && fragment instanceof DialerFragment) {
                                AddressText addressText = ((DialerFragment) fragment).getAddressText();
                                if (addressText != null) {
                                    if (addressText.getText().length() > 0) {
                                        LinphoneManager.getInstance().newOutgoingCall(addressText);
                                    } else {
                                        if (LinphonePreferences.instance().isBisFeatureEnabled()) {
                                            LinphoneCallLog[] logs = LinphoneManager.getLc().getCallLogs();
                                            LinphoneCallLog log = null;
                                            for (LinphoneCallLog l : logs) {
                                                if (l.getDirection() == CallDirection.Outgoing) {
                                                    log = l;
                                                    break;
                                                }
                                            }
                                            if (log == null) {
                                                return;
                                            }

                                            LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
                                            if (lpc != null && log.getTo().getDomain().equals(lpc.getDomain())) {
                                                addressText.setText(log.getTo().getUserName());
                                            } else {
                                                addressText.setText(log.getTo().asStringUriOnly());
                                            }
                                            addressText.setSelection(addressText.getText().toString().length());
                                            addressText.setDisplayedName(log.getTo().getDisplayName());
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            Toast.makeText(IPCallActivity.this, getString(R.string.no_voip_connection), Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        //Not online
                        Toast.makeText(IPCallActivity.this, getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                    }
                } catch (LinphoneCoreException e) {
                    LinphoneManager.getInstance().terminateCall();
                    Toast.makeText(IPCallActivity.this,
                            String.format(getResources().getString(R.string.warning_wrong_destination_address), ""),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public Boolean isCallTransfer() {
        return callTransfer;
    }

    public void resetClassicMenuLayoutAndGoBackToCallIfStillRunning() {
        if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0) {
            LinphoneCall call = LinphoneManager.getLc().getCalls()[0];
            if (call.getState() == LinphoneCall.State.IncomingReceived) {
                startActivity(new Intent(IPCallActivity.this, IncomingIPCallActivity.class));
            } else {
                Intent intent = new Intent(IPCallActivity.this, OutgoingIPCallActivity.class);
                intent.putExtra(IPCallActivity.KEY_IPCALL_SERVICE_READY, isIPCallReady);
                startActivity(intent);
            }
        }
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

    public void displayEmptyFragment() {
        changeCurrentFragment(FRAGMENT_TYPE_EMPTY, new Bundle());
    }

    private void changeCurrentFragment(String newFragmentType, Bundle extras) {
        changeCurrentFragment(newFragmentType, extras, false);
    }

    private void changeCurrentFragment(String newFragmentType, Bundle extras, boolean withoutAnimation) {
        if (newFragmentType.equals(currentFragmentType)) {
            return;
        }

        if (currentFragmentType.equals(FRAGMENT_TYPE_DIALER)) {
            try {
                DialerFragment dialerFragment = DialerFragment.getInstance();
                //dialerSavedState = getFragmentManager().saveFragmentInstanceState(dialerFragment);
            } catch (Exception e) {
            }
        }

        fragment = null;

        switch (newFragmentType) {
            case FRAGMENT_TYPE_HISTORY_LIST:
                fragment = new HistoryListFragment();
                break;
            case FRAGMENT_TYPE_HISTORY_DETAIL:
                fragment = new HistoryDetailFragment();
                break;
            case FRAGMENT_TYPE_DIALER:
                fragment = new DialerFragment();
                if (extras == null) {
                    //fragment.setInitialSavedState(dialerSavedState);
                }
                break;
            default:
                break;
        }

        if (fragment != null) {
            fragment.setArguments(extras);
            changeFragment(fragment, newFragmentType, withoutAnimation);
        }
    }

    private void changeFragment(Fragment newFragment, String newFragmentType, boolean withoutAnimation) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        if (!newFragmentType.equals(FRAGMENT_TYPE_DIALER)
                && !newFragmentType.equals(FRAGMENT_TYPE_HISTORY_LIST)) {
            transaction.addToBackStack(newFragmentType);
        } else {
            while (fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

        transaction.replace(R.id.fragmentContainer, newFragment, newFragmentType);
        transaction.commitAllowingStateLoss();
        fm.executePendingTransactions();

        currentFragmentType = newFragmentType;
    }

    @SuppressWarnings("incomplete-switch")
    public void selectMenu(String menuToSelect) {
        currentFragmentType = menuToSelect;
        resetSelection();

        switch (menuToSelect) {
            case FRAGMENT_TYPE_HISTORY_LIST:
            case FRAGMENT_TYPE_HISTORY_DETAIL:
                historySelect.setVisibility(View.VISIBLE);
                break;
            case FRAGMENT_TYPE_DIALER:
                dialerSelect.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void resetSelection() {
        //REset selection underline
        historySelect.setVisibility(View.INVISIBLE);
        dialerSelect.setVisibility(View.INVISIBLE);
    }

    public void updateDialerFragment(DialerFragment fragment) {
        // Hack to maintain soft input flags
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void hideBottomTabbar(Boolean hide) {
        if (hide) {
            mBottomTabbar.setVisibility(View.GONE);
        } else {
            mBottomTabbar.setVisibility(View.VISIBLE);
        }
    }

    public void displayHistoryDetail(String sipUri, LinphoneCallLog log) {
        LinphoneAddress lAddress;
        try {
            lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
        } catch (LinphoneCoreException e) {
            Log.e(TAG, e.getMessage());
            //TODO display error message
            return;
        }
        LinphoneContact c = ContactsManager.getInstance().findContactFromAddress(lAddress);

        String displayName = c != null ? c.getFullName() : LinphoneUtils.getAddressDisplayName(sipUri);
        String pictureUri = c != null && c.getPhotoUri() != null ? c.getPhotoUri().toString() : null;

        String status;
        if (log.getDirection() == CallDirection.Outgoing) {
            status = getString(R.string.outgoing);
        } else {
            if (log.getStatus() == LinphoneCallLog.CallStatus.Missed) {
                status = getString(R.string.missed);
            } else {
                status = getString(R.string.incoming);
            }
        }


        String callTime = secondsToDisplayableString(log.getCallDuration());
        String callDate = String.valueOf(log.getTimestamp());

        Fragment fragment2 = getFragmentManager().findFragmentById(R.id.fragmentContainer2);
        if (fragment2 != null && fragment2.isVisible() && currentFragmentType.equals(FRAGMENT_TYPE_HISTORY_DETAIL)) {
            HistoryDetailFragment historyDetailFragment = (HistoryDetailFragment) fragment2;
            historyDetailFragment.changeDisplayedHistory(lAddress.asStringUriOnly(), displayName, pictureUri, status, callTime, callDate);
        } else {
            Bundle extras = new Bundle();
            extras.putString("SipUri", lAddress.asString());
            if (displayName != null) {
                extras.putString("DisplayName", displayName);
                extras.putString("PictureUri", pictureUri);
            }
            extras.putString("CallStatus", status);
            extras.putString("CallTime", callTime);
            extras.putString("CallDate", callDate);

            changeCurrentFragment(FRAGMENT_TYPE_HISTORY_DETAIL, extras);
        }
    }

    @SuppressLint("SimpleDateFormat")
    private String secondsToDisplayableString(int secs) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.set(0, 0, 0, 0, 0, secs);
        return dateFormat.format(cal.getTime());
    }

    public Dialog displayDialog(String text) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable d = new ColorDrawable(ContextCompat.getColor(this, R.color.colorC));
        d.setAlpha(200);
        dialog.setContentView(R.layout.ipcall_dialog);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(d);

        TextView customText = (TextView) dialog.findViewById(R.id.customText);
        customText.setText(text);
        return dialog;
    }

    @Override
    public void setAddresGoToDialerAndCall(String number, String name, Uri photo) {
//		Bundle extras = new Bundle();
//		extras.putString("SipUri", number);
//		extras.putString("DisplayName", name);
//		extras.putString("Photo", photo == null ? null : photo.toString());
//		changeCurrentFragment(FragmentsAvailable.DIALER, extras);

        LinphoneManager.AddressType address = new AddressText(this, null);
        address.setDisplayedName(name);
        address.setText(number);
        LinphoneManager.getInstance().newOutgoingCall(address);
    }
}

