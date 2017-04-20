package vn.rta.ipcall.ui;

import org.linphone.core.LinphoneCall;

import vn.rta.ipcall.api.SipCallSession;

/**
 * Created by tamdoan on 19/04/2017.
 */

public interface IOnCallActionTrigger {

    /**
     * When user clics on clear call
     */
    int TERMINATE_CALL = 1;
    /**
     * When user clics on take call
     */
    int TAKE_CALL = TERMINATE_CALL + 1;
    /**
     * When user clics on not taking call
     */
    int DONT_TAKE_CALL = TAKE_CALL + 1;
    /**
     * When user clics on reject call
     */
    int REJECT_CALL = DONT_TAKE_CALL + 1;
    /**
     * When mute is set on
     */
    int MUTE_ON = REJECT_CALL + 1;
    /**
     * When mute is set off
     */
    int MUTE_OFF = MUTE_ON + 1;
    /**
     * When bluetooth is set on
     */
    int BLUETOOTH_ON = MUTE_OFF + 1;
    /**
     * When bluetooth is set off
     */
    int BLUETOOTH_OFF = BLUETOOTH_ON + 1;
    /**
     * When speaker is set on
     */
    int SPEAKER_ON = BLUETOOTH_OFF + 1;
    /**
     * When speaker is set off
     */
    int SPEAKER_OFF = SPEAKER_ON + 1;
    /**
     * When detailed display is asked
     */
    int DETAILED_DISPLAY = SPEAKER_OFF + 1;
    /**
     * When hold / reinvite is asked
     */
    int TOGGLE_HOLD = DETAILED_DISPLAY + 1;
    /**
     * When media settings is asked
     */
    int MEDIA_SETTINGS = TOGGLE_HOLD + 1;
    /**
     * When add call is asked
     */
    int ADD_CALL = MEDIA_SETTINGS + 1;
    /**
     * When xfer to a number is asked
     */
    int XFER_CALL = ADD_CALL + 1;
    /**
     * When transfer to a call is asked
     */
    int TRANSFER_CALL = XFER_CALL + 1;
    /**
     * When start recording is asked
     */
    int START_RECORDING = TRANSFER_CALL + 1;
    /**
     * When stop recording is asked
     */
    int STOP_RECORDING = START_RECORDING + 1;
    /**
     * Open the DTMF view
     */
    int DTMF_DISPLAY = STOP_RECORDING +1;
    /**
     * Start the video stream
     */
    int START_VIDEO = DTMF_DISPLAY + 1;
    /**
     * Stop the video stream
     */
    int STOP_VIDEO = START_VIDEO + 1;
    /**
     * Stop the video stream
     */
    int ZRTP_TRUST = STOP_VIDEO + 1;
    /**
     * Stop the video stream
     */
    int ZRTP_REVOKE = ZRTP_TRUST + 1;

    /**
     * Called when the user make an action
     *
     * @param whichAction what action has been done
     */
    void onTrigger(int whichAction, LinphoneCall call);

    void onDisplayVideo(boolean show);
}

