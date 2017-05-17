package vn.rta.cpms.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.rta.ipcall.LinphoneService;

import im.vector.activity.CommonActivityUtils;
import vn.rta.cpms.activities.ActivitySwitcher;
import vn.rta.cpms.services.ConnectionService;
import vn.rta.cpms.services.ManagerService;
import vn.rta.cpms.services.model.Response;
import vn.rta.cpms.utils.Common;
import vn.rta.cpms.utils.MessageUtils;
import vn.rta.cpms.utils.StdCodeDBHelper;
import vn.rta.cpms.view.NoPercentNumberProgressDialog;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.RTASurvey;

//import vn.rta.cpms.communication.activities.CommonActivityUtils;

/**
 * @author DungVu (dungvu@rta.vn)
 * @since October 12, 2016
 */
public class DeactivateTask extends AsyncTask<Void, Integer, Object> {
    RTASurvey app = RTASurvey.getInstance();
    protected Activity activity;
    protected NoPercentNumberProgressDialog progressDialog;

    public DeactivateTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Object doInBackground(Void... params) {
        app.setDeactivatingLock(true);

        //connect to server to acknowledge deactivation
        String host = app.getServerUrl(), key = app.getServerKey(), deviceId = app.getDeviceId(),
                username = app.getCredential(RTASurvey.KEY_CREDENTIAL_USERNAME);
        Response response = ConnectionService.getInstance()
                .deactivate(activity, host, key, deviceId, username);

        //stop background services, set necessary flag, cleanup user data
        if (response!=null && StdCodeDBHelper.STT_S1001.equals(response.getSttCode())) {
            app.setAppToInactiveState();
            activity.stopService(new Intent(activity, ManagerService.class));
            activity.stopService(new Intent(Intent.ACTION_MAIN).setClass(activity, LinphoneService.class));
            Common.cleanUpPersonalData();
            CommonActivityUtils.logout(null);
        }
        return response;
    }

    @Override
    protected void onPreExecute() {
        if (activity!=null) {
            progressDialog = new NoPercentNumberProgressDialog(activity);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(R.string.deactivate_progress_title);
            progressDialog.setMessage(activity.getString(R.string.deactivate_progress_msg));
            progressDialog.show();
        }
    }

    @Override
    protected void onPostExecute(Object result) {
        app.setDeactivatingLock(false);

        if (activity!=null && !activity.isDestroyed() && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Response response = (Response)result;
        if (response!=null && StdCodeDBHelper.STT_S1001.equals(response.getSttCode())) {
            MessageUtils.showToastInfo(activity, R.string.deactivate_result_successful);
            ActivitySwitcher.startRegistrationFlow(activity);
        } else {
            String failedMsg = (response!=null && !TextUtils.isEmpty(response.getMsgCode()))?
                    StdCodeDBHelper.getInstance().getCodeDesc(response.getMsgCode()):"";
            String msg = String.format("%s\n%s",
                    activity.getString(R.string.deactivate_result_failed),
                    response == null ? "JSONException" : failedMsg);
            MessageUtils.showDialogInfo(activity, msg);
        }
    }

}
