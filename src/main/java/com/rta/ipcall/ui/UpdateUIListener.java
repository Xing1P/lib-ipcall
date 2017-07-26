package com.rta.ipcall.ui;

/**
 * Created by Genius Doan on 27/04/2017.
 */

public interface OnUpdateUIListener {
    void updateUIByServiceStatus(boolean serviceConnected);
    void registrationState(boolean isConnected, String statusMessage);
    void updateToCallWidget(boolean isCalled);
    void launchIncomingCallActivity();
    void dismissCallActivity();
}
