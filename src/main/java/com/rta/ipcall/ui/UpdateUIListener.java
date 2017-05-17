package com.rta.ipcall.ui;

/**
 * Created by Genius Doan on 27/04/2017.
 */

public interface UpdateUIListener {
    void updateUIByServiceStatus(boolean serviceConnected);
    void updateToCallWidget(boolean isCalled);
    void launchIncomingCallActivity();
    void dismissCallActivity();
}
