package vn.rta.survey.android.manager;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphonePreferences;
import com.rta.ipcall.LinphoneService;
import com.rta.ipcall.ui.AddressText;

import org.javarosa.core.model.FormIndex;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.logic.FormController;

import vn.rta.ipcall.api.SipConfigManager;
import vn.rta.ipcall.api.SipManager;
import vn.rta.ipcall.api.SipProfile;
import vn.rta.ipcall.api.SipUri;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.entities.SIPProfile;
import vn.rta.survey.android.listeners.SaveCurrentAnswerListnener;
import vn.rta.survey.android.listeners.SipCallUpdateUIListener;

/**
 * Created by Genius Doan on 18/04/2017.
 */

public class IPCallManager {
    private static final String TAG = IPCallManager.class.getSimpleName();
    private static final int PERMISSIONS_ENABLED_CAMERA = 203;

    private boolean isRunning = false;
    private static IPCallManager instance;

    public ServiceConnection connection;
    public LinphoneService service;
    private int accountID = -1;

    private boolean globIntegrate = true;
    private boolean globProfileAlways = true;
    private boolean globProfileWifi = false;
    private boolean globProfileNever = false;
    private boolean globGsm = true;
    private SIPProfile sipProfile;
    LinphoneManager.AddressType recipient;

    private SipCallUpdateUIListener listener;
    private SaveCurrentAnswerListnener saveListener;
    private FormEntryActivity mEntryActivity;

    public static IPCallManager getInstance() {
        if (instance == null)
            instance = new IPCallManager();
        return instance;
    }

    private IPCallManager()
    {

    }

    public void setFormEntryActivity(FormEntryActivity activity) {
        this.mEntryActivity = activity;
    }

    public void setSipProfile(SIPProfile profile) {
        this.sipProfile = profile;
    }

    public void setUpdateUIListener(SipCallUpdateUIListener listener) {
        this.listener = listener;
    }

    public void setSaveListener(SaveCurrentAnswerListnener listener) {
        this.saveListener = listener;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void connectToServer() {
        if (isRunning)
            return;

        Intent serviceIntent = new Intent(Intent.ACTION_MAIN);

        /*
        if (!SipConfigManager.getPreferenceBooleanValue(mEntryActivity, PreferencesWrapper.HAS_ALREADY_SETUP, false)) {
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, PreferencesWrapper.HAS_ALREADY_SETUP, true);
        }
        applyPrefs();
        */

        //TODO: Call listener to update call button when ready
        if (LinphoneService.isReady())
        {
            service = LinphoneService.instance();
            isRunning = true;
            listener.updateCallButton(true);
            mEntryActivity.prepareLogIn();
            mEntryActivity.genericLogIn(sipProfile.getUserName(), sipProfile.getPassword(), null, sipProfile.getUrl(), LinphoneAddress.TransportType.LinphoneTransportUdp);
        }
        else {
            serviceIntent.setPackage(mEntryActivity.getPackageName());
            connection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName arg0, IBinder arg1) {
                    service = LinphoneService.instance();
                    isRunning = true;
                    listener.updateCallButton(true);
                    mEntryActivity.prepareLogIn();
                    mEntryActivity.genericLogIn(sipProfile.getUserName(), sipProfile.getPassword(), null, sipProfile.getUrl(), LinphoneAddress.TransportType.LinphoneTransportUdp);
                }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                    service = null;
                    listener.updateCallButton(false);
                }
            };

            mEntryActivity.bindService(serviceIntent, connection,
                    Context.BIND_AUTO_CREATE);
            startSipService();
        }
    }

    private void startSipService() {
        Thread t = new Thread("StartSip") {
            public void run() {
                Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
                serviceIntent.setPackage(mEntryActivity.getPackageName());
                serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(mEntryActivity, FormEntryActivity.class));
                mEntryActivity.startService(serviceIntent);
            }
        };
        t.start();
    }

    public void placeCall(FormIndex index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(mEntryActivity)) {
            Toast.makeText(mEntryActivity, R.string.cpms_drawing_over_other_app_request, Toast.LENGTH_LONG).show();
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mEntryActivity.getPackageName()));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mEntryActivity.startActivity(i);
            //return START_NOT_STICKY;
        } else {
            placeCallWithOption(null, index);
        }
        //placeCallWithOption(null, index);
    }


    private void placeCallWithOption(Bundle b, FormIndex index) {
        if (service == null) {
            Log.e(TAG, "There no connection to server");
            return;
        }
        FormController formController = RTASurvey.getInstance().getFormController();
        if (formController == null) {
            return;
        }
        saveListener.onSaverCurrentAnswerForCall();
        formController.setSipCallIndexs(index);
        //mCaptureButton.setText("Registering...");
        //TODO: GEnius
        saveAccount();
        updateWidget();

    }

    private void saveAccount() {
        SipProfile builtProfile = new SipProfile();
        buildAccount(builtProfile);
        //builtProfile.wizard = WizardUtils.BASIC_WIZARD_TAG;

        ContentValues builtValues = builtProfile.getDbContentValues();

        /*
        Cursor c = mEntryActivity.getContentResolver().query(SipProfile.ACCOUNT_URI, new String[]{
                SipProfile.FIELD_ID, SipProfile.FIELD_USERNAME
        }, null, null, null);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    String username = c.getString(1);
                    if (username.equals(builtProfile.username)) ;
                    {
                        accountID = c.getInt(0);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        if (accountID == -1) {
            Uri savedUri = mEntryActivity.getContentResolver().insert(SipProfile.ACCOUNT_URI, builtValues);
            if (savedUri != null) {
                accountID = (int) ContentUris.parseId(savedUri);
            } else {
                accountID = -1;
            }
        }else{
            int updated = mEntryActivity.getContentResolver().update(SipProfile.ACCOUNT_URI, builtValues, SipProfile.FIELD_ID+" = ?",new String[]{String.valueOf(accountID)});
            if (updated == -1) {
                accountID = updated;
            }
        }
        */
    }


    private void applyPrefs() {
        boolean integrate = globIntegrate;
        boolean useGsm = globGsm;
        Profile mode = Profile.UNKOWN;
        if (globProfileAlways) {
            mode = Profile.ALWAYS;
        } else if (globProfileWifi) {
            mode = Profile.WIFI;
        } else if (globProfileNever) {
            mode = Profile.NEVER;
        }

        // About integration
        SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.INTEGRATE_WITH_DIALER, integrate);
        SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.INTEGRATE_WITH_CALLLOGS, integrate);

        // About out/in mode
        if (mode != Profile.UNKOWN) {

            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_3G_IN, (useGsm && mode == Profile.ALWAYS));
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_3G_OUT, useGsm);
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_GPRS_IN, (useGsm && mode == Profile.ALWAYS));
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_GPRS_OUT, useGsm);
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_EDGE_IN, (useGsm && mode == Profile.ALWAYS));
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_EDGE_OUT, useGsm);

            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_WIFI_IN, mode != Profile.NEVER);
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_WIFI_OUT, true);

            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_OTHER_IN, mode != Profile.NEVER);
            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.USE_OTHER_OUT, true);

            SipConfigManager.setPreferenceBooleanValue(mEntryActivity, SipConfigManager.LOCK_WIFI, (mode == Profile.ALWAYS) && !useGsm);
        }

    }


    public SipProfile buildAccount(SipProfile account) {

        if (sipProfile == null) {
            sipProfile = new SIPProfile("thule", "thule123", "192.168.11.90", "0981959587", "thule", 5060);
        }
        account.display_name = sipProfile.getUserName();

        String[] serverParts = sipProfile.getUrl().split(":");
        account.acc_id = "<sip:" + SipUri.encodeUser(sipProfile.getUserName().trim()) + "@" + serverParts[0].trim() + ">";

        String regUri = "sip:" + sipProfile.getUrl() + ":" + sipProfile.getPort();
        account.reg_uri = regUri;
        account.proxies = new String[]{regUri};


        account.realm = "*";
        account.username = sipProfile.getAuthenticationUser();
        account.data = sipProfile.getPassword();
        account.scheme = SipProfile.CRED_SCHEME_DIGEST;
        account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
        //By default auto transport
        account.transport = LinphoneAddress.TransportType.LinphoneTransportUdp;

        return account;
    }

    public void updateWidget() {
        //TODO: Genius
        String numberToCall = sipProfile.getPhoneNumber();
        numberToCall = formatNumber(numberToCall);
        recipient = new AddressText(mEntryActivity, null);
        recipient.setDisplayedName("Genius");
        recipient.setText(numberToCall);

        if (sipProfile != null) {
            try {
                LinphoneManager.getInstance().newOutgoingCall(recipient);

                LinphoneCall[] list = LinphoneManager.getLc().getCalls();
                LinphoneCall linphoneCall = null;
                for (int i = 0; i < list.length; i++)
                {
                    if (list[i].getState() == LinphoneCall.State.OutgoingProgress
                            || list[i].getState() == LinphoneCall.State.OutgoingInit
                            || list[i].getState() == LinphoneCall.State.OutgoingEarlyMedia
                            || list[i].getState() == LinphoneCall.State.OutgoingEarlyMedia)
                    {
                        linphoneCall = list[i];
                        break;
                    }
                }

                Intent callHandlerIntent = service.buildCallUiIntent(mEntryActivity, linphoneCall, mEntryActivity.getPackageName(), SipManager.ACTION_SIP_CALL_FLOATING_UI);
                mEntryActivity.startService(callHandlerIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            listener.updateToCallWidget(false);
            //mCaptureButton.setText("Calling...");
        } else {
            listener.updateToCallWidget(true);
        }
    }

    public void requestPermissionAndToggleVideo()
    {
        int camera = mEntryActivity.getPackageManager().checkPermission(Manifest.permission.CAMERA, mEntryActivity.getPackageName());
        org.linphone.mediastream.Log.i("[Permission] Camera permission is " + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (camera == PackageManager.PERMISSION_GRANTED) {
            LinphoneManager.getInstance().toggleVideo();
        } else {
            checkAndRequestPermission(Manifest.permission.CAMERA, PERMISSIONS_ENABLED_CAMERA);

        }
    }

    public void checkAndRequestPermission(String permission, int result) {
        int permissionGranted = mEntryActivity.getPackageManager().checkPermission(permission, mEntryActivity.getPackageName());
        org.linphone.mediastream.Log.i("[Permission] " + permission + " is " + (permissionGranted == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (permissionGranted != PackageManager.PERMISSION_GRANTED) {
            if (LinphonePreferences.instance().firstTimeAskingForPermission(permission) || ActivityCompat.shouldShowRequestPermissionRationale(mEntryActivity, permission)) {
                org.linphone.mediastream.Log.i("[Permission] Asking for " + permission);
                ActivityCompat.requestPermissions(mEntryActivity, new String[] { permission }, result);
            }
        }
    }


    /**
     * This function help re format number to phone number
     *
     * @param number
     * @return
     */
    private String formatNumber(String number) {
        String result = "";
        // Convert String -> Num -> String to remove "+" and "0"
        number = number.trim();
        //int temp = Integer.parseInt(number);
        //number = String.valueOf(temp);
        if (number.startsWith("+84"))
            result = "0" + number.substring("+84".length());
        else if (number.startsWith("84"))
            result = "0" + number.substring("84".length());
        else if (number.length() == 9) {
            // with out 84
            result = "0" + number;
        } else if (number.length() == 10) {
            // with out 84
            if (number.startsWith("0"))
                result = number;
            else result = "0" + number;
        } else if (number.length() == 11) {
            result = number;
        } else {
            return null;
        }
        return result;
    }

    public void stopService() {
        if (connection != null && mEntryActivity != null && isRunning && LinphoneService.isReady()) {
            mEntryActivity.unbindService(connection);
            Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
            serviceIntent.setPackage(mEntryActivity.getPackageName());
            serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(mEntryActivity, FormEntryActivity.class));
            mEntryActivity.stopService(serviceIntent);
            mEntryActivity.stopService(new Intent(mEntryActivity, LinphoneService.class));
            isRunning = false;
            service = null;
            connection = null;
        }
    }

    //Additional
    enum Profile {
        UNKOWN,
        ALWAYS,
        WIFI,
        NEVER
    }
}
