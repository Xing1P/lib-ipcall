package vn.rta.ipcall.ui;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.rta.ipcall.ContactsManager;
import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphoneService;

import org.linphone.core.LinphoneCore;

import vn.rta.survey.android.R;

/**
 * Created by Genius Doan on 30/05/2017.
 */

public class DialerFragment extends Fragment {
    private static DialerFragment instance;
    private static boolean isCallTransferOngoing = false;

    private AddressAware numpad;
    private AddressText mAddress;
    private boolean shouldEmptyAddressField = true;

    /**
     * @return null if not ready yet
     */
    public static DialerFragment getInstance() {
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.call_dialer_fragment, container, false);

        mAddress = (AddressText) view.findViewById(R.id.address);
        mAddress.setKeyListener(null);

        EraseButton erase = (EraseButton) view.findViewById(R.id.erase);
        erase.setAddressWidget(mAddress);

        numpad = (AddressAware) view.findViewById(R.id.numpad);
        if (numpad != null) {
            numpad.setAddressWidget(mAddress);
        }

        resetLayout(isCallTransferOngoing);

        if (getArguments() != null) {
            shouldEmptyAddressField = false;
            String number = getArguments().getString("SipUri");
            String displayName = getArguments().getString("DisplayName");
            String photo = getArguments().getString("PhotoUri");
            mAddress.setText(number);
            if (displayName != null) {
                mAddress.setDisplayedName(displayName);
            }
            if (photo != null) {
                mAddress.setPictureUri(Uri.parse(photo));
            }
        }

        instance = this;

        return view;
    }

    @Override
    public void onPause() {
        instance = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        instance = this;

        if (IPCallActivity.isInstanciated()) {
            IPCallActivity.getInstance().selectMenu(IPCallActivity.FRAGMENT_TYPE_DIALER);
            IPCallActivity.getInstance().updateDialerFragment(this);
            IPCallActivity.getInstance().hideBottomTabbar(false);
        }

        boolean isOrientationLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isOrientationLandscape && !getResources().getBoolean(R.bool.isTablet)) {
            ((LinearLayout) numpad).setVisibility(View.GONE);
        } else {
            ((LinearLayout) numpad).setVisibility(View.VISIBLE);
        }

        if (shouldEmptyAddressField) {
            mAddress.setText("");
        } else {
            shouldEmptyAddressField = true;
        }
        resetLayout(isCallTransferOngoing);
    }

    public void resetLayout(boolean callTransfer) {
        if (!IPCallActivity.isInstanciated()) {
            return;
        }
        isCallTransferOngoing = IPCallActivity.getInstance().isCallTransfer();
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc == null) {
            return;
        }
    }

    public void displayTextInAddressBar(String numberOrSipAddress) {
        shouldEmptyAddressField = false;
        mAddress.setText(numberOrSipAddress);
    }

    public AddressText getAddressText() {
        return mAddress;
    }

    public void newOutgoingCall(String numberOrSipAddress) {
        displayTextInAddressBar(numberOrSipAddress);
        LinphoneManager.getInstance().newOutgoingCall(mAddress);
    }

    public void newOutgoingCall(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            if (scheme.startsWith("imto")) {
                mAddress.setText("sip:" + intent.getData().getLastPathSegment());
            } else if (scheme.startsWith("call") || scheme.startsWith("sip")) {
                mAddress.setText(intent.getData().getSchemeSpecificPart());
            } else {
                Uri contactUri = intent.getData();
                String address = ContactsManager.getAddressOrNumberForAndroidContact(LinphoneService.instance().getContentResolver(), contactUri);
                if (address != null) {
                    mAddress.setText(address);
                } else {
                    Log.e(DialerFragment.class.getSimpleName(), "Unknown scheme: " + scheme);
                    mAddress.setText(intent.getData().getSchemeSpecificPart());
                }
            }

            mAddress.clearDisplayedName();
            intent.setData(null);

            LinphoneManager.getInstance().newOutgoingCall(mAddress);
        }
    }
}
