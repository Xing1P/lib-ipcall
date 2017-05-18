package com.rta.ipcall.xmlrpc;

public interface XmlRpcListener {
    void onError(String error);

    void onAccountCreated(String result);

    void onAccountExpireFetched(String result);

    void onAccountExpireUpdated(String result);

    void onAccountActivated(String result);

    void onAccountActivatedFetched(boolean isActivated);

    void onTrialAccountFetched(boolean isTrial);

    void onAccountFetched(boolean isExisting);

    void onAccountEmailChanged(String result);

    void onAccountPasswordChanged(String result);

    void onRecoverPasswordLinkSent(String result);

    void onActivateAccountLinkSent(String result);

    void onSignatureVerified(boolean success);

    void onUsernameSent(String result);

    void onRemoteProvisioningFilenameSent(String result);
}
