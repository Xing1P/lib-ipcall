package vn.rta.ipcall.ui;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rta.ipcall.ContactsManager;
import com.rta.ipcall.LinphoneContact;
import com.rta.ipcall.LinphoneUtils;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;

import vn.rta.survey.android.R;

/**
 * Created by Genius Doan on 31/05/2017.
 */

public class HistoryDetailFragment extends Fragment implements View.OnClickListener {
    private ImageView dialBack, chat;
    private View view;
    private ImageView contactPicture, callDirection;
    private TextView contactName, contactAddress, time, date;
    private String sipUri, displayName, pictureUri;
    private LinphoneContact contact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        sipUri = getArguments().getString("SipUri");
        displayName = getArguments().getString("DisplayName");
        pictureUri = getArguments().getString("PictureUri");
        String status = getArguments().getString("CallStatus");
        String callTime = getArguments().getString("CallTime");
        String callDate = getArguments().getString("CallDate");

        view = inflater.inflate(R.layout.call_history_detail_fragment, container, false);

        dialBack = (ImageView) view.findViewById(R.id.call);
        dialBack.setOnClickListener(this);

        chat = (ImageView) view.findViewById(R.id.chat);
        chat.setOnClickListener(this);
        if (getResources().getBoolean(R.bool.disable_chat))
            view.findViewById(R.id.chat).setVisibility(View.GONE);

        contactPicture = (ImageView) view.findViewById(R.id.contact_picture);

        contactName = (TextView) view.findViewById(R.id.contact_name);
        contactAddress = (TextView) view.findViewById(R.id.contact_address);

        callDirection = (ImageView) view.findViewById(R.id.direction);

        time = (TextView) view.findViewById(R.id.time);
        date = (TextView) view.findViewById(R.id.date);

        displayHistory(status, callTime, callDate);

        return view;
    }

    private void displayHistory(String status, String callTime, String callDate) {
        if (status.equals(getResources().getString(R.string.missed))) {
            callDirection.setImageResource(R.drawable.ic_missed_phone);
        } else if (status.equals(getResources().getString(R.string.incoming))) {
            callDirection.setImageResource(R.drawable.ic_incoming_call);
        } else if (status.equals(getResources().getString(R.string.outgoing))) {
            callDirection.setImageResource(R.drawable.ic_outgoing_call);
        }

        time.setText(callTime == null ? "" : callTime);
        Long longDate = Long.parseLong(callDate);
        date.setText(LinphoneUtils.timestampToHumanDate(getActivity(), longDate, getString(R.string.history_detail_date_format)));

        LinphoneAddress lAddress = null;
        try {
            lAddress = LinphoneCoreFactory.instance().createLinphoneAddress(sipUri);
        } catch (LinphoneCoreException e) {
            Log.e(HistoryDetailFragment.class.getSimpleName(), e.getMessage());
        }

        if (lAddress != null) {
            contactAddress.setText(lAddress.asStringUriOnly());
            contact = ContactsManager.getInstance().findContactFromAddress(lAddress);
            if (contact != null) {
                contactName.setText(contact.getFullName());
                LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture, contact.getPhotoUri(), contact.getThumbnailUri());
            } else {
                contactName.setText(displayName == null ? LinphoneUtils.getAddressDisplayName(sipUri) : displayName);
                contactPicture.setImageBitmap(ContactsManager.getInstance().getDefaultAvatarBitmap());
            }
        } else {
            contactAddress.setText(sipUri);
            contactName.setText(displayName == null ? LinphoneUtils.getAddressDisplayName(sipUri) : displayName);
        }
    }

    public void changeDisplayedHistory(String sipUri, String displayName, String pictureUri, String status, String callTime, String callDate) {
        if (displayName == null) {
            displayName = LinphoneUtils.getUsernameFromAddress(sipUri);
        }

        this.sipUri = sipUri;
        this.displayName = displayName;
        this.pictureUri = pictureUri;
        displayHistory(status, callTime, callDate);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (IPCallActivity.isInstanciated()) {
            IPCallActivity.getInstance().selectMenu(IPCallActivity.FRAGMENT_TYPE_HISTORY_DETAIL);
            //IPCallActivity.getInstance().hideTabBar(false);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.call) {
            IPCallActivity.getInstance().setAddresGoToDialerAndCall(sipUri, displayName, pictureUri == null ? null : Uri.parse(pictureUri));
        } else if (id == R.id.chat) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", displayName, null)));
        }
    }
}