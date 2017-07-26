package vn.rta.cpms.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.rta.ipcall.LinphoneService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.vector.activity.CommonActivityUtils;
import vn.rta.cpms.activities.AnnouncementActivity;
import vn.rta.cpms.activities.SplashScreenActivity;
import vn.rta.cpms.adapter.ReportAdapter;
import vn.rta.cpms.application.Constants;
import vn.rta.cpms.application.Params;
import vn.rta.cpms.audiorecord.SoundTraceRecorder;
import vn.rta.cpms.fragments.ProcessFragment;
import vn.rta.cpms.listener.DownloadTaskListener;
import vn.rta.cpms.listener.UploadTaskListener;
import vn.rta.cpms.preference.AppSettingSharedPreferences;
import vn.rta.cpms.providers.RTInstanceCounter;
import vn.rta.cpms.providers.ReportProviderAPI;
import vn.rta.cpms.rcm.IllegalAccessException;
import vn.rta.cpms.rcm.RCMService;
import vn.rta.cpms.rcm.UpdateRegistrarService;
import vn.rta.cpms.services.model.CurrentFilesInMediaLibReport;
import vn.rta.cpms.services.model.CurrentFormsReport;
import vn.rta.cpms.services.model.FormInFamily;
import vn.rta.cpms.services.model.FormInfo;
import vn.rta.cpms.services.model.FormMediaInfo;
import vn.rta.cpms.services.model.InstanceInfo;
import vn.rta.cpms.services.model.InstancesReport;
import vn.rta.cpms.services.model.Interaction;
import vn.rta.cpms.services.model.MediaLibFileInfo;
import vn.rta.cpms.services.model.OnlineReport;
import vn.rta.cpms.services.model.UserCredential;
import vn.rta.cpms.services.model.ssinteraction.SSInteraction;
import vn.rta.cpms.services.sync.AfterJob;
import vn.rta.cpms.services.sync.doc.DocumentDownloadService;
import vn.rta.cpms.services.sync.form.FormLazyDownloadService;
import vn.rta.cpms.services.sync.form.FormLockReportService;
import vn.rta.cpms.services.sync.instance.GetBackupListService;
import vn.rta.cpms.services.sync.instance.InstanceSyncDbHelper;
import vn.rta.cpms.services.sync.instance.OldFormDownloadService;
import vn.rta.cpms.services.sync.instance.ReturnInstanceService;
import vn.rta.cpms.services.sync.instance.SyncInstanceToServerService;
import vn.rta.cpms.services.sync.localdb.LocalDatabaseLog;
import vn.rta.cpms.services.sync.report.RReportDownloadService;
import vn.rta.cpms.services.sync.resource.DownloadResourceService;
import vn.rta.cpms.services.sync.resource.UploadResourceService;
import vn.rta.cpms.tasks.AdaptFollowUpInstanceTask;
import vn.rta.cpms.tasks.ConnectionTask;
import vn.rta.cpms.tasks.DownloadCollectSettingsTask;
import vn.rta.cpms.tasks.DownloadInstanceTemplate;
import vn.rta.cpms.tasks.DownloadQAFileTask;
import vn.rta.cpms.tasks.DownloadQARuleTask;
import vn.rta.cpms.tasks.ForwardInstanceTask;
import vn.rta.cpms.tasks.UploadODKsettingsTask;
import vn.rta.cpms.tasks.UploadRSSettingTask;
import vn.rta.cpms.tasks.UploadSoundTraceTask;
import vn.rta.cpms.tasks.formdelete.DeleteFormsListener;
import vn.rta.cpms.tasks.formdelete.DeleteFormsTask;
import vn.rta.cpms.tasks.formdelete.DeleteInstancesListener;
import vn.rta.cpms.tasks.formdelete.DeleteInstancesTask;
import vn.rta.cpms.ui.notificationlist.NotificationListActivity;
import vn.rta.cpms.utils.Common;
import vn.rta.cpms.utils.FAFileUtils;
import vn.rta.cpms.utils.MessageUtils;
import vn.rta.cpms.utils.RCMCmdUtils;
import vn.rta.cpms.utils.SimpleCrypto;
import vn.rta.cpms.utils.StdCodeDBHelper;
import vn.rta.cpms.utils.StringUtil;
import vn.rta.cpms.utils.SurveyCollectorUtils;
import vn.rta.cpms.utils.ZIPPackUtils;
import vn.rta.rtcenter.RTCenterServiceHelper;
import vn.rta.rtcenter.data.ModuleDbHelper;
import vn.rta.rtcenter.data.ModuleProviderAPI;
import vn.rta.rtcenter.model.Category;
import vn.rta.rtcenter.model.CategoryList;
import vn.rta.rtcenter.model.Module;
import vn.rta.rtcenter.model.ModuleDetail;
import vn.rta.rtcenter.model.Subscription;
import vn.rta.rtsurvey.providers.QaOnlineProviderAPI;
import vn.rta.survey.android.BuildConfig;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.provider.NtfActionHelper;
import vn.rta.survey.android.provider.SamplingArea;
import vn.rta.survey.android.provider.SamplingDbHelper;
import vn.rta.survey.android.provider.SamplingFormJson;
import vn.rta.survey.android.provider.SamplingResponse;

//import vn.rta.cpms.communication.activities.CommonActivityUtils;

/**
 * RCM message filter & processor
 *
 * @author DungVu (dungvu@rta.vn)
 * @since February 26 2016
 */
public class ProcessRCMCommandService extends IntentService {
    public static final int MIN_NTF_SOUND_TYPE = 1;
    public static final int MAX_NTF_SOUND_TYPE = 5;
    // clean pilot
    public static final int CLEAN_FORMS = 0;
    public static final int CLEAN_LIB = 1;
    public static final int CLEAN_LOG = 2;
    // SS resource transferring type
    public static final int TYPE_DOWNLOAD = 0;
    public static final int TYPE_UPLOAD = 1;
    // download resource JSON message
    public final static String KEY_DIRPATH = "file_path";
    public final static String KEY_LINK = "file_link";
    public final static String KEY_OPTION = "action";
    public final static String OPTION_RENAME = "rename";
    public final static String OPTION_OVERWRITE = "overwrite";
    //report types
    public final static String REPORT_INSTANCE_STT = "instanceStt";
    //Intent action for local broadcast receiver
    public static final String NTF_COUNTER_UPDATE = "vn.rta.cpms.android.ntf.update";
    public static final String TSK_COUNTER_UPDATE = "vn.rta.cpms.android.tsk.update";
    public static final String EXTRA_RELOAD = "reload";
    public static final String STOP_ADVERTISE_ACTIVITY = "cn.rta.cpms.android.advertise.activity";
    public static final String REPORT_COUNTER_UPDATE = "vn.rta.cpms.android.report.update";
    public static final String REPORT_UPDATE_UI = "vn.rta.cpms.android.report.update.ui";
    public static final String UPDATE_MAIN_ITEMS = "vn.rta.cpms.android.report.update.main.items";
    private static final String TAG = "ProcessRCMCommand";
    // notification and task reminder alarm
    public static long snoozeTime = 5 * Params.MINUTE;    // default snoozeTime
    private final Logger log = Logger.getLogger(ProcessRCMCommandService.class);

    public ProcessRCMCommandService() {
        super(TAG);
    }

    private static void makeNotificationSound(Context context, int soundId) {
        if (soundId < MIN_NTF_SOUND_TYPE || soundId > MAX_NTF_SOUND_TYPE) {
            soundId = MIN_NTF_SOUND_TYPE;
        }
        Uri notification = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/raw/sound" + soundId);
        MediaPlayer player = MediaPlayer.create(context, notification);
        player.start();
    }

    public static void broadcastUIUpdate(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void broadcastUIUpdate(Context context, String action) {
        broadcastUIUpdate(context, new Intent(action));
    }

    private static void reportRCMCommand(Context context, String id, String status) {
        RCMStatusReportService.requestService(context, id, status);
    }

    public static void validateFormFamilyFromAsyncTask(Context context, String formID, String version) {
        new ConnectionTask(context) {
            @Override
            protected List<FormInFamily> doInBackground(String... params) {
                String formID = params[0], version = params[1];
                return ConnectionService.getInstance().getFormFamily(context,
                        RTASurvey.getInstance().getServerUrl(),
                        RTASurvey.getInstance().getServerKey(),
                        formID, version);
            }

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                if (result != null && result instanceof List) {
                    List<FormInFamily> family = (List<FormInFamily>) result;
                    for (FormInFamily f : family) {
                        try {
                            if (f.isAvailable == 1) {
                                SurveyCollectorUtils.lockForm(context, f.formID, f.version, false);
                                SurveyCollectorUtils.handleOldForms(context, f.formID, f.version);
                            } else {
                                SurveyCollectorUtils.lockForm(context, f.formID, f.version, true);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.execute(formID, version);
    }

    /**
     * @return true if formID-version is locked
     */
    private static boolean validateFormFamily(Context context, String formID, String version) {
        List<FormInFamily> family = ConnectionService.getInstance().getFormFamily(context,
                RTASurvey.getInstance().getServerUrl(),
                RTASurvey.getInstance().getServerKey(),
                formID, version);
        boolean lockItself = false;
        if (family != null) {
            for (FormInFamily f : family) {
                try {
                    if (f.isAvailable == 1) {
                        SurveyCollectorUtils.lockForm(context, f.formID, f.version, false);
                    } else {
                        lockItself = lockItself || (formID.equals(f.formID) && version.equals(f.version));
                        SurveyCollectorUtils.lockForm(context, f.formID, f.version, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return lockItself;
    }

    public static void reportReceivedMessage(Context context, final String mid) {
        if (!TextUtils.isEmpty(mid)) {
            reportRCMCommand(context, mid, Params.RCM_STATUS_RECEIVED);
        }
    }

    public static void reportRCMCommandExeFailed(Context context, final String mid) {
        if (!TextUtils.isEmpty(mid)) {
            reportRCMCommand(context, mid, Params.RCM_STATUS_FAILED);
        }
    }

    public static void reportRCMCommandExeSuccess(Context context, final String mid) {
        if (!TextUtils.isEmpty(mid)) {
            reportRCMCommand(context, mid, Params.RCM_STATUS_SUCCESS);
        }
    }

    /**
     * Issues a notification for App settings
     */
    private static void notifyNewAppSettings(Context context) {
        // Waking up mobile if it is sleeping
        WakeLocker.acquire(context);

        NotificationCompat.Style style = new NotificationCompat.BigTextStyle()
                .setBigContentTitle(context.getString(R.string.rta_app_name))
                .bigText(context.getString(R.string.app_settings_update_message_full))
                .setSummaryText(context.getString(R.string.app_settings_update_message));

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_default)
                .setContentTitle(context.getString(R.string.rta_app_name))
                .setContentText(context.getString(R.string.app_settings_update_message))
                .setStyle(style)
                .setAutoCancel(true);

        Intent appIntent = new Intent(context, SplashScreenActivity.class);
        mBuilder.setContentIntent(MessageUtils.getPendingIntentWithParentStack(context, appIntent, 0));
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = mBuilder.build();
        mNotificationManager.notify(R.string.app_settings_update_message, n);

        // update the background tracking works
        Intent serviceIntent = new Intent(context, ManagerService.class);
        serviceIntent.setAction(ManagerService.ACTION_UPDATE_SCHEDULE);
        context.startService(serviceIntent);

        // Releasing wake lock
        WakeLocker.release();
    }

    /**
     * Issues a notification with alarm for Notification
     */
    private static void notifyNewNotification(Context context, long intervalMillis) {
        // Waking up mobile if it is sleeping
        WakeLocker.acquire(context);

        NotificationCompat.Style style = null;
        List<SSInteraction> list = DBService.getInstance().getNotifications(false);
        if (list == null || list.size() == 0) {
            return;
        }

        String senderText = context.getString(R.string.cpms_ntf_from_sender,
                list.get(0).sender);

        if (list.size() == 1) {
            style = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(senderText)
                    .setSummaryText(context.getString(R.string.menu_item_ntf))
                    .bigText(Jsoup.parse(list.get(0).message).text());
        } else {
            int maxLine = 3, iLine = 0;
            String summaryText = list.size() > maxLine ?
                    context.getString(R.string.cpms_ntf_number_of_news_more, list.size() - maxLine) :
                    context.getString(R.string.menu_item_ntf);
            style = new NotificationCompat.InboxStyle()
                    .setBigContentTitle(context.getString(R.string.cpms_ntf_number_of_news, list.size()))
                    .setSummaryText(summaryText);
            while (iLine < list.size() && iLine < maxLine) {
                SSInteraction i = list.get(iLine);
                String line = String.format("%s -> %s", i.sender,
                        Jsoup.parse(i.message).text());
                ((NotificationCompat.InboxStyle) style).addLine(line);
                iLine++;
            }
        }

        String smallContentText = list.size() == 1 ? senderText
                : context.getString(R.string.cpms_ntf_number_of_news, list.size());

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context.getApplicationContext())
                .setSmallIcon(R.drawable.cpms_main_ntf)
                .setContentTitle(context.getString(R.string.menu_item_ntf))
                .setContentText(smallContentText)
                .setStyle(style);

        Intent appIntent = new Intent(context, NotificationListActivity.class);
        mBuilder.setContentIntent(MessageUtils.getPendingIntentWithParentStack(context, appIntent, 0));
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification n = mBuilder.build();
        if (intervalMillis > 0) {
            MessageUtils.snoozeTime = intervalMillis;
            mNotificationManager.cancel(MessageUtils.NTF_ID_FOR_NTFICATION);
            mNotificationManager.notify(MessageUtils.SOUND_NTF_ID_FOR_NTFICATION, n);
        } else {
            mNotificationManager.cancel(MessageUtils.SOUND_NTF_ID_FOR_NTFICATION);
            mNotificationManager.notify(MessageUtils.NTF_ID_FOR_NTFICATION, n);
        }

        // Releasing wake lock
        WakeLocker.release();
    }

    /**
     * Issues a notification with alarm for TaskReminder
     */
    private static void notifyNewTask(Context context, long intervalMillis) {
        // Waking up mobile if it is sleeping
        WakeLocker.acquire(context);

        NotificationCompat.Style style = null;
        List<SSInteraction> list = DBService.getInstance().getTaskReminders(false);
        if (list == null || list.size() == 0) {
            return;
        }

        String senderText = context.getString(R.string.cpms_tsk_from_sender,
                list.get(0).sender);

        if (list.size() == 1) {
            style = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(senderText)
                    .setSummaryText(context.getString(R.string.menu_item_task))
                    .bigText(Jsoup.parse(list.get(0).message).text());
        } else {
            int maxLine = 3, iLine = 0;
            String summaryText = list.size() > maxLine ?
                    context.getString(R.string.cpms_tsk_number_of_news_more, list.size() - maxLine) :
                    context.getString(R.string.menu_item_task);
            style = new NotificationCompat.InboxStyle()
                    .setBigContentTitle(context.getString(R.string.cpms_tsk_number_of_news, list.size()))
                    .setSummaryText(summaryText);
            while (iLine < list.size() && iLine < maxLine) {
                SSInteraction i = list.get(iLine);
                String line = String.format("%s -> %s", i.sender,
                        Jsoup.parse(i.message).text());
                ((NotificationCompat.InboxStyle) style).addLine(line);
                iLine++;
            }
        }

        String smallContentText = list.size() == 1 ? senderText
                : context.getString(R.string.cpms_tsk_number_of_news, list.size());

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context.getApplicationContext())
                .setSmallIcon(R.drawable.cpms_main_task)
                .setContentTitle(context.getString(R.string.menu_item_task))
                .setContentText(smallContentText)
                .setStyle(style);

        Intent appIntent = new Intent(context, NotificationListActivity.class);
        mBuilder.setContentIntent(MessageUtils.getPendingIntentWithParentStack(context, appIntent, 0));
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification n = mBuilder.build();
        if (intervalMillis > 0) {
            MessageUtils.snoozeTime = intervalMillis;
            mNotificationManager.cancel(MessageUtils.NTF_ID_FOR_TASKREMINDER);
            mNotificationManager.notify(MessageUtils.SOUND_NTF_ID_FOR_TASKREMINDER, n);
        } else {
            mNotificationManager.cancel(MessageUtils.SOUND_NTF_ID_FOR_TASKREMINDER);
            mNotificationManager.notify(MessageUtils.NTF_ID_FOR_TASKREMINDER, n);
        }

        // Releasing wake lock
        WakeLocker.release();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        log.debug("Checking message from RCM ...");
        RTASurvey pre = RTASurvey.getInstance();
        Context context = pre.getApplicationContext();

        //try to process old message (RCM Cmd) from cache before
        processOldMessages(context);

        Map<String, String> receivedMessages = null;
        try {
            String accessToken = RCMService.getInstance().getPref(RCMService.KEY_ACCESS_TOKEN);
            if (accessToken != null && !accessToken.equals("")) {
                receivedMessages = RCMService.getInstance().checkCmds(pre.getRCMUrl());
            } else {
                receivedMessages = RCMService.getInstance().checkMessageWithId(pre.getRCMUrl());
                context.startService(new Intent(context, UpdateRegistrarService.class));
            }
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
            deactivateUser(context);
            return;
        }
        Map<String, String> messages = new HashMap<String, String>();
        List<Long> msgIDs = new ArrayList<Long>();

        for (Map.Entry<String, String> entry : receivedMessages.entrySet()) {
            String message = entry.getKey();
            String id = entry.getValue();
            messages.put(id, message);
            RCMCmdCache.getInstance().cacheCmd(id, message);

            try {
                long _id = Long.parseLong(id);
                if (!msgIDs.contains(_id)) {
                    msgIDs.add(_id);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        Collections.sort(msgIDs);
        //logging for test
        if (msgIDs.size() > 0) {
            log.info("Bundle of RCM commands (sorted): " + msgIDs);
        }

        for (int i = 0; i < msgIDs.size(); i++) {
            String id = String.valueOf(msgIDs.get(i));
            String message = messages.get(id);
            message.trim();
            log.info("Process message: " + id + "\n" + message);
            RCMCmdCache.getInstance().deleteCmdCache(id);
            processMessage(context, id, message);
        }
    }

    private void processOldMessages(Context context) {
        Cursor c = RCMCmdCache.getInstance().getAllCache();
        if (c != null) {
            if (c.getCount() > 0) {
                log.info("Trying to process RCM Cmd from cache...");
                while (c.moveToNext()) {
                    String message = c.getString(c.getColumnIndex(RCMCmdCache.NewCmd.CMD));
                    String id = c.getString(c.getColumnIndex(RCMCmdCache.NewCmd.CMD_ID));
                    if (!TextUtils.isEmpty(message) && !TextUtils.isEmpty(id)) {
                        log.info("Process message (from cache): " + id + "\n" + message);
                        RCMCmdCache.getInstance().deleteCmdCache(id);
                        processMessage(context, id, message);
                    }
                }
                log.info("End clean RCM Cmd cache process.");
            }
            c.close();
        }
    }

    /**
     * @param message
     */
    private void processMessage(final Context context, final String mid, String message) {
        if (!RCMCmdUtils.getInstance().checkPermissionOfCmd(context, message)) {
            Log.e(TAG, "The cmd doesn't have enough permissions to work.");
            reportRCMCommandExeFailed(context, mid);
            return;
        }

        try {
            if (message.startsWith(context.getString(R.string.return_instance))) {    // return instance
                reportReceivedMessage(context, mid);
                String oldUuid = message.split(Constants.UNDERSCORE)[1];
                ReturnInstanceService.requestService(context, oldUuid, -1, mid,
                        new AfterJob<String, Void>() {
                            @Override
                            public Void apply(String newUuid) {
                                MessageUtils.generateNotification(context,
                                        R.string.instance_is_returned, R.string.instance_is_returned);
                                NtfActionHelper.getInstance().insertNtfAction(context,
                                        R.string.instance_is_returned, R.string.instance_is_returned_detail, null,
                                        new String[]{SurveyCollectorUtils.getInstanceName(context, newUuid)},
                                        SurveyCollectorUtils.getEditInstanceIntent(context, newUuid));
                                RCMStatusReportService.requestService(context, mid, Params.RCM_STATUS_SUCCESS);
                                return null;
                            }
                        });
            } else if (message.startsWith(context.getString(R.string.return_instance_with_json))) {    // return instance
                reportReceivedMessage(context, mid);
                String json = message.replaceFirst(context.getString(R.string.return_instance_with_json), "");
                JSONObject jsonObject = new JSONObject(json);
                String oldUuid = jsonObject.getString("uuid");
                long uuidCounter = jsonObject.getLong("uuid_counter");
                ReturnInstanceService.requestService(context, oldUuid, uuidCounter, mid,
                        new AfterJob<String, Void>() {
                            @Override
                            public Void apply(String newUuid) {
                                MessageUtils.generateNotification(context,
                                        R.string.instance_is_returned, R.string.instance_is_returned);
                                NtfActionHelper.getInstance().insertNtfAction(context,
                                        R.string.instance_is_returned, R.string.instance_is_returned_detail, null,
                                        new String[]{SurveyCollectorUtils.getInstanceName(context, newUuid)},
                                        SurveyCollectorUtils.getEditInstanceIntent(context, newUuid));
                                RCMStatusReportService.requestService(context, mid, Params.RCM_STATUS_SUCCESS);
                                return null;
                            }
                        });
            } else if (message.startsWith(context.getString(R.string.return_instance_with_json))) {    // return instance
                reportReceivedMessage(context, mid);
                String json = message.replaceFirst(context.getString(R.string.return_instance_with_json), "");

            } else if (message.equals(context.getString(R.string.ping))) {
                pingToServer(context, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_disapprove_req_to_edit))) {
                String uuid = message.replaceFirst(context.getString(R.string.rcm_cm_disapprove_req_to_edit), "");
                disapproveReqToEdit(context, uuid, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_credential_unassign))) {
                reportRCMCommandExeFailed(context, mid);
                log.error("This command (exe:unassign_user_xxx) has been deprecated");

            } else if (message.startsWith(context.getString(R.string.report_running_app))) {
                reportRunningApps(context, mid);

            } else if (message.startsWith(context.getString(R.string.return_to_QA))) {
                reportReceivedMessage(context, mid);

                String jsontext = message.replaceFirst(context
                        .getString(R.string.return_to_QA), "");
                JSONObject json = new JSONObject(jsontext);
                String uuid = json.getString("uuid");
                String link = json.getString("link");
                long uuidCounter = json.has("uuid_counter") ? json.getLong("uuid_counter") : -1;
                uuidCounter = uuidCounter == -1 ? RTASurvey.getInstance().getUniqueId(uuid) : uuidCounter;
                downloadQAfromUrl(context, link, uuidCounter, uuid, mid);

            } else if (message.startsWith(context.getString(R.string.get_report_rscript))) {
                reportReceivedMessage(context, mid);
                String rId = message.replaceFirst(context
                        .getString(R.string.get_report_rscript), "");
                RReportDownloadService.requestService(context, rId, mid);

            } else if (message.equals(context.getString(R.string.update_report_list))) {
                reportReceivedMessage(context, mid);
                RReportDownloadService.requestDownloadAll(context);

            } else if (message.startsWith(context.getString(R.string.get_report_url))) {
                reportReceivedMessage(context, mid);
                String json = message.replaceFirst(context
                        .getString(R.string.get_report_url), "");
                insertIntoReportDB(context, json);
                broadcastUIUpdate(context, REPORT_COUNTER_UPDATE);
                broadcastUIUpdate(context, REPORT_UPDATE_UI);
                broadcastUIUpdate(context, UPDATE_MAIN_ITEMS);
            } else if (message.startsWith(context.getString(R.string.remove_r_report))) {
                String ruleId = message.replaceFirst(context
                        .getString(R.string.remove_r_report), "");
                deleteRReport(context, ruleId, mid);
                broadcastUIUpdate(context, REPORT_COUNTER_UPDATE);
                broadcastUIUpdate(context, REPORT_UPDATE_UI);
                broadcastUIUpdate(context, UPDATE_MAIN_ITEMS);

            } else if (message.startsWith(context.getString(R.string.take_picture))) {
                reportReceivedMessage(context, mid);
                String ruleId = message.replaceFirst(context
                        .getString(R.string.take_picture), "");
                Intent intent = new Intent(context, CameraService.class);
                intent.putExtra("camera", ruleId);
                intent.putExtra("pid", mid);
                context.startService(intent);

            } else if (message.startsWith(context.getString(R.string.rcm_screen_save_collection))) {
                reportReceivedMessage(context, mid);
                String json = message.replaceFirst(context
                        .getString(R.string.rcm_screen_save_collection), "");
                Intent intent = new Intent(ProcessRCMCommandService.STOP_ADVERTISE_ACTIVITY);
                LocalBroadcastManager
                        .getInstance(context.getApplicationContext())
                        .sendBroadcast(intent);
                downloadCollections(context, json, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_on_off_app_switching))) {
                reportReceivedMessage(context, mid);
                String cmd = message.replaceFirst(context
                        .getString(R.string.rcm_on_off_app_switching), "");
                Intent intent = new Intent(context, SwitchAppService.class);
                if (cmd.toLowerCase().equals("on") && !RTASurvey.getInstance().getOnOffSwitchingApp()) {
                    RTASurvey.getInstance().setUpOnOffSwitchingApp(true);
                    context.startService(intent);
                } else if (cmd.toLowerCase().equals("off") && RTASurvey.getInstance().getOnOffSwitchingApp()) {
                    RTASurvey.getInstance().setUpOnOffSwitchingApp(false);
                    context.stopService(intent);
                } else {
                    Log.e(TAG, "Error json on off app: " + cmd);
                }

            } else if (message.startsWith(context.getString(R.string.cpms_update_media))) {
                String json = message.replaceFirst(context
                        .getString(R.string.cpms_update_media), "");
                downloadPreloadMediaFile(context, mid, json, false);

            } else if (message.startsWith(context.getString(R.string.cpms_update_family_media))) {
                String json = message.replaceFirst(context
                        .getString(R.string.cpms_update_family_media), "");
                downloadPreloadMediaFile(context, mid, json, true);

            }/* else if (message.startsWith(context.getString(R.string.reset_rcm_checking))) {
                long seconds = RTASurvey.getInstance().getRCMCheckPeriod();
                try {
                    seconds = Long.parseLong(
                            message.replaceFirst(context
                                    .getString(R.string.reset_rcm_checking), ""));
                } catch (NumberFormatException e) {
                    //if param is invalid, continue use current preference
                    e.printStackTrace();
                }
                if (seconds < 0) {
                    //if param is invalid, continue use current preference
                    seconds = RTASurvey.getInstance().getRCMCheckPeriod();
                }
                RTASurvey.getInstance().saveRCMCheckPeriod(seconds);
                reportReceivedMessage(context, mid);

                Intent intent = new Intent(context, ManagerService.class);
                intent.setAction((RTASurvey.getInstance().isGCMRegisteredFlag()
                        && Common.checkPlayServices(context)) ? ManagerService.ACTION_STOP_RCM : ManagerService.ACTION_START_RCM);
                context.startService(intent);
            } */else if (message.startsWith(context.getString(R.string.download_resource))) {
                String json = message.replaceFirst(context
                        .getString(R.string.download_resource), "");
                transferResource(context, json, mid, TYPE_DOWNLOAD);

            } else if (message.startsWith(context.getString(R.string.upload_resource))) {
                String json = message.replaceFirst(context
                        .getString(R.string.upload_resource), "");
                transferResource(context, json, mid, TYPE_UPLOAD);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_module))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_update_module), "");
                JSONArray array = new JSONArray(ids);
                tryToUpdateModule(context, array, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_delete_module))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_delete_module), "");
                JSONArray array = new JSONArray(ids);
                tryToDeleteModule(context, array, mid);

            } else if (message.equals(context.getString(R.string.rcm_cm_refresh_module_list))) {
                updateAllModule(context, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_category))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_update_category), "");
                JSONArray array = new JSONArray(ids);
                tryToUpdateCategory(context, array, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_delete_category))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_delete_category), "");
                JSONArray array = new JSONArray(ids);
                tryToDeleteCategory(context, array, mid, false);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_deactivate_category))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_deactivate_category), "");
                JSONArray array = new JSONArray(ids);
                tryToDeleteCategory(context, array, mid, true);

            } else if (message.equals(context.getString(R.string.rcm_cm_refresh_category_list))) {
                updateAllCategory(context, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_area))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_update_area), "");
                JSONArray array = new JSONArray(ids);
                tryToUpdateSamplingArea(context, array, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_delete_area))) {
                String ids = message.replaceFirst(context.getString(R.string.rcm_cm_delete_area), "");
                JSONArray array = new JSONArray(ids);
                tryToDeleteSamplingArea(context, array, mid);

            } else if (message.equals(context.getString(R.string.rcm_cm_refresh_area))) {
                updateAllSamplingArea(context, mid);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_area_form))) {
                String json = message.replaceFirst(context.getString(R.string.rcm_cm_update_area_form), "");
                SamplingFormJson form = (SamplingFormJson) StringUtil.json2Object(json, SamplingFormJson.class);
                RTASurvey.getInstance().setSamplingForm(RTASurvey.KEY_SAMPLING_FORM_AREA_ID,
                        form.formId, form.version, form.lockStt, form.mainKey, form.geoKey);
                SurveyCollectorUtils.lockForm(context, form.formId, form.version, form.lockStt == 1);

            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_listing_form))) {
                String json = message.replaceFirst(context.getString(R.string.rcm_cm_update_listing_form), "");
                SamplingFormJson form = (SamplingFormJson) StringUtil.json2Object(json, SamplingFormJson.class);
                RTASurvey.getInstance().setSamplingForm(RTASurvey.KEY_SAMPLING_FORM_LISTING_ID,
                        form.formId, form.version, form.lockStt, form.mainKey, form.geoKey);
                SurveyCollectorUtils.lockForm(context, form.formId, form.version, form.lockStt == 1);

            } else if (message.startsWith(context.getString(R.string.rcm_form_assigned))) {
                reportReceivedMessage(context, mid);
                String json = message.replaceFirst(context
                        .getString(R.string.rcm_form_assigned), "");
                ConnectionService.AssignedForm aForm = (ConnectionService.AssignedForm)
                        StringUtil.json2Object(json, ConnectionService.AssignedForm.class);
                if (aForm != null) {
                    assignNewForm(context, aForm.formId, aForm.version, aForm.title, aForm.lockStt == 1);
                }

            } else if (message.startsWith(context.getString(R.string.rcm_form_removed))) {
                reportReceivedMessage(context, mid);
                String json = message.replaceFirst(context
                        .getString(R.string.rcm_form_removed), "");
                ConnectionService.AssignedForm aForm = (ConnectionService.AssignedForm)
                        StringUtil.json2Object(json, ConnectionService.AssignedForm.class);
                if (aForm != null) {
                    removeAssignedForm(context, aForm.formId, aForm.version);
                }

            } else if (message.equals(context.getString(R.string.rcm_form_update_assigned_list))) {
                reportReceivedMessage(context, mid);
                applyAssignedForms(context, RTASurvey.getInstance().getServerUrl());

            } else if (message.startsWith(context.getString(R.string.rcm_secondary_user_assign))) {
                assignSecondaryUser(context, mid, message);

            } else if (message.startsWith(context.getString(R.string.rcm_secondary_user_unassign))) {
                String username = message.replaceFirst(context
                        .getString(R.string.rcm_secondary_user_unassign), "");
                removeSecondaryUser(context, mid, username);

            } else if (message.equals(context.getString(R.string.rcm_cm_update_staff_info))) {
                log.error("This command (exe:updateStaffInfo) has been deprecated");

            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_staff_info_by_user))) {
                reportReceivedMessage(context, mid);
                String username = message.replaceFirst(context
                        .getString(R.string.rcm_cm_update_staff_info_by_user), "");
                Common.updateStaffInfo(context, RTASurvey.getInstance().getServerUrl(),
                        RTASurvey.getInstance().getServerKey(), username, log);

            } else if (message.startsWith(context.getString(R.string.lockReport))) {
                reportReceivedMessage(context, mid);
                String reportType = message.replaceFirst(context
                        .getString(R.string.lockReport), "");
                lockReport(context, reportType, mid, true);

            } else if (message.startsWith(context.getString(R.string.unlockReport))) {
                reportReceivedMessage(context, mid);
                String reportType = message.replaceFirst(context
                        .getString(R.string.unlockReport), "");
                lockReport(context, reportType, mid, false);

            } else if (message.equals(context.getString(R.string.x2on))) {
                reportReceivedMessage(context, mid);
                RTASurvey.getInstance().setX2lock(false);

            } else if (message.equals(context.getString(R.string.x2off))) {
                reportReceivedMessage(context, mid);
                RTASurvey.getInstance().setX2lock(true);

            } else if (message.equals(context.getString(R.string.adscreen_on))) {
                reportReceivedMessage(context, mid);
                RTASurvey.getInstance().setAdCollectionLock(false);

            } else if (message.equals(context.getString(R.string.adscreen_off))) {
                reportReceivedMessage(context, mid);
                RTASurvey.getInstance().setAdCollectionLock(true);

            } else if (message.equals(context.getString(R.string.addUser_on))) {
                reportReceivedMessage(context, mid);
                RTASurvey.getInstance().setAddNewUserLock(false);

            } else if (message.equals(context.getString(R.string.addUser_off))) {
                reportReceivedMessage(context, mid);
                RTASurvey.getInstance().setAddNewUserLock(true);

            } else if (message.startsWith(context.getString(R.string.load_qatask))) {
                reportReceivedMessage(context, mid);
                String url = message.replaceFirst(context
                        .getString(R.string.load_qatask), "");
                downloadQAfromUrl(context, url, -1, null, mid);

            } else if (message.startsWith(context.getString(R.string.force_complete_instance))) {
                reportReceivedMessage(context, mid);
                String uuid = message.replaceFirst(context.getString(R.string.force_complete_instance), "");
                String logtext;
                if (SurveyCollectorUtils.forceComplete(context, uuid)) {
                    logtext = uuid + " has been force to [complete] status";
                    reportRCMCommandExeSuccess(context, mid);
                } else {
                    logtext = "cannot change status of " + uuid + " , please check its existence";
                    reportRCMCommandExeFailed(context, mid);
                }
                log.info(logtext);

            } else if (message.startsWith(context.getString(R.string.force_incomplete_instance))) {
                reportReceivedMessage(context, mid);
                String uuid = message.replaceFirst(context.getString(R.string.force_incomplete_instance), "");
                String logtext;
                if (SurveyCollectorUtils.forceIncomplete(context, uuid)) {
                    String iName = SurveyCollectorUtils.getInstanceName(context, uuid);
                    NotificationCompat.Style style = new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(context.getString(R.string.rta_app_name))
                            .bigText(context.getString(R.string.request_return_instance_successful_local, iName));
                    MessageUtils.generateNotification(context, style, R.drawable.ic_stat_default,
                            context.getString(R.string.rta_app_name),
                            context.getString(R.string.request_return_instance_successful_local, iName),
                            R.string.request_return_instance_successful_local);
                    NtfActionHelper.getInstance().insertNtfAction(context, R.string.rta_app_name,
                            R.string.request_return_instance_successful_local,
                            null, new String[]{iName},
                            SurveyCollectorUtils.getEditInstanceIntent(context, uuid));

                    logtext = uuid + " has been force to [incomplete] status";
                    reportRCMCommandExeSuccess(context, mid);
                } else {
                    logtext = "cannot change status of " + uuid + " , please check its existence";
                    reportRCMCommandExeFailed(context, mid);
                }
                log.info(logtext);

            } else if (message.startsWith(context.getString(R.string.force_submit_finalized_instances_byform))) {
                reportReceivedMessage(context, mid);
                String formId = message
                        .substring(message.indexOf(Constants.UNDERSCORE) + 1,
                                message.lastIndexOf(Constants.UNDERSCORE));
                String version = message.substring(message.lastIndexOf(Constants.UNDERSCORE) + 1);
                SurveyCollectorUtils.submitFinalizedInstances(context,
                        RTASurvey.getInstance().getServerUrl(),
                        formId, version, null, null,
                        UserListDbHelper.getInstance().getMainUsername());

            } else if (message.startsWith(context.getString(R.string.force_submit_finalized_instances_byuuid))) {
                reportReceivedMessage(context, mid);
                String uuid = message
                        .replaceFirst(context
                                .getString(R.string.force_submit_finalized_instances_byuuid), "");
                SurveyCollectorUtils.submitFinalizedInstances(context,
                        RTASurvey.getInstance().getServerUrl(),
                        null, null, uuid, null,
                        UserListDbHelper.getInstance().getMainUsername());

            } else if (message.equals(context.getString(R.string.force_submit_finalized_instances))) {
                reportReceivedMessage(context, mid);
                SurveyCollectorUtils.submitFinalizedInstances(context,
                        RTASurvey.getInstance().getServerUrl(),
                        null, null, null, null,
                        UserListDbHelper.getInstance().getMainUsername());

            } else if (message.startsWith(context.getString(R.string.editFinal_on))) {
                reportReceivedMessage(context, mid);
                String formId = message
                        .substring(message.indexOf(Constants.UNDERSCORE) + 1,
                                message.lastIndexOf(Constants.UNDERSCORE));
                String version = message.substring(message.lastIndexOf(Constants.UNDERSCORE) + 1);
                if (SurveyCollectorUtils.lockCanEditWhenComplete(context, formId, version, false)) {
                    reportLockStatus(context, formId, version, FormLockReportService.TYPE_FINAL, 0);
                }

            } else if (message.startsWith(context.getString(R.string.editFinal_off))) {
                reportReceivedMessage(context, mid);
                String formId = message
                        .substring(message.indexOf(Constants.UNDERSCORE) + 1,
                                message.lastIndexOf(Constants.UNDERSCORE));
                String version = message.substring(message.lastIndexOf(Constants.UNDERSCORE) + 1);
                if (SurveyCollectorUtils.lockCanEditWhenComplete(context, formId, version, true)) {
                    reportLockStatus(context, formId, version, FormLockReportService.TYPE_FINAL, 1);
                }

            } else if (message.startsWith(context.getString(R.string.editFinalOn_by_uuid))) {
                reportReceivedMessage(context, mid);
                String uuid = message.replaceFirst(
                        context.getString(R.string.editFinalOn_by_uuid), "");
                SurveyCollectorUtils.lockCanEditWhenComplete(context, uuid, false);

            } else if (message.startsWith(context.getString(R.string.editFinalOff_by_uuid))) {
                reportReceivedMessage(context, mid);
                String uuid = message.replaceFirst(
                        context.getString(R.string.editFinalOff_by_uuid), "");
                SurveyCollectorUtils.lockCanEditWhenComplete(context, uuid, true);

            } else if (message.startsWith(context.getString(R.string.lock_form))) {
                reportReceivedMessage(context, mid);
                String formId = message
                        .substring(message.indexOf(Constants.UNDERSCORE) + 1,
                                message.lastIndexOf(Constants.UNDERSCORE));
                String version = message.substring(message.lastIndexOf(Constants.UNDERSCORE) + 1);
                log.debug(String.format("lock formID=%s, version=%s", formId, version));
                SurveyCollectorUtils.lockForm(context, formId, version, true);

            } else if (message.startsWith(context.getString(R.string.unlock_form))) {
                reportReceivedMessage(context, mid);
                String formId = message
                        .substring(message.indexOf(Constants.UNDERSCORE) + 1,
                                message.lastIndexOf(Constants.UNDERSCORE));
                String version = message.substring(message.lastIndexOf(Constants.UNDERSCORE) + 1);
                log.debug(String.format("unlock formID=%s, version=%s", formId, version));
                SurveyCollectorUtils.lockForm(context, formId, version, false);

            } else if (message.startsWith(context.getString(R.string.del_instance_by_uuid))) {
                reportReceivedMessage(context, mid);
                LocalBroadcastManager.getInstance(RTASurvey.getInstance().getApplicationContext()).sendBroadcast(new Intent(ProcessFragment.DELETE_INSTANCE));
                String uuid = message.replaceFirst(context.getString(R.string.del_instance_by_uuid), "");
                long ins_id = SurveyCollectorUtils.getInstanceId(context, uuid);
                if (ins_id == -1) {
                    Log.e(TAG, "Cannot find instance - " + uuid);
                    reportRCMCommandExeFailed(context, mid);
                } else {
                    Uri contentUri = InstanceProviderAPI.InstanceColumns.CONTENT_URI;
                    long deletedRows = context.getContentResolver().delete(
                            Uri.withAppendedPath(contentUri, String.valueOf(ins_id)), null, null);
                    if (deletedRows > 0) {
                        reportRCMCommandExeSuccess(context, mid);
                    } else {
                        reportRCMCommandExeFailed(context, mid);
                    }
                }
                LocalBroadcastManager.getInstance(RTASurvey.getInstance().getApplicationContext()).sendBroadcast(new Intent(ProcessFragment.DELETE_INSTANCE_STOP));

            } else if (message.startsWith(context.getString(R.string.sync_to_newest_version))) {
                String uuid = message.replaceFirst(context.getString(R.string.sync_to_newest_version), "");
                syncToNewestFormVersion(context, mid, uuid);

            } else if (message.equals(context.getString(R.string.check_rta_license))) {    // check RTA Survey license
                checkAndSaveLicense(context, mid);

            } else if (message.equals(context.getString(R.string.send_a_record_file))) {    // record and send audio file to SS server
                recordAndUploadSoundTrace(context, mid);

            } else if (message.startsWith(context.getString(R.string.runout_of_battery))) {    // run out of battery ping
                reportReceivedMessage(context, mid);
                int btLevel = RTASurvey.getInstance().getBatteryLevel();
                int soundId;
                try {
                    soundId = Integer.parseInt(message
                            .split(Constants.UNDERSCORE)[1]);
                } catch (Exception e) {
                    soundId = 1;
                }
                makeNotificationSound(context, soundId);
                MessageUtils.generateNotification(context, context.getString(R.string.battery_low, btLevel),
                        R.string.battery_low);

            } else if (message.equalsIgnoreCase(context.getString(R.string.report_current_forms))) {    // report current forms
                reportReceivedMessage(context, mid);
                reportCurrentForms(context);

            } else if (message.equalsIgnoreCase(context.getString(R.string.report_current_instances))) {    // report current instances
                reportReceivedMessage(context, mid);
                reportCurrentInstances(context);

            } else if (message.equalsIgnoreCase(context.getString(R.string.report_current_lib))) {    // report current medialib
                reportReceivedMessage(context, mid);
                reportCurrentFilesInMediaLib(context);

            } else if (message.equalsIgnoreCase(context
                    .getString(R.string.report_current_version))) {    // report current version names
                reportReceivedMessage(context, mid);
                Common.reportCurrentVersionName(context);

            } else if (message.equalsIgnoreCase(context
                    .getString(R.string.report_rcm_frequency))) {    // report rcm frequency
                reportReceivedMessage(context, mid);
                reportRCMfrequency(context);

            } else if (message.startsWith(context    // upload collect.settings (if exist)
                    .getString(R.string.upload_odk_settings))) {
                uploadOdkSettings(context, mid, message);

            } else if (message.startsWith(context    // download and push collect.settings to RS
                    .getString(R.string.download_odk_settings))) {
                downloadOdkSettings(context, mid, message);

            } else if (message.equals(context    // report setting.json file of RS to server
                    .getString(R.string.report_rs_setting))) {
                reportRSSettingJSON(context, mid);

            } else if (message.startsWith(context    // follow-up instance
                    .getString(R.string.followup_instance))) {
                String json = message.replaceFirst(context.getString(R.string.followup_instance), "");
                createFollowupInstanceByTask(context, mid, json, false);

            } else if (message.startsWith(context    // follow-up instance
                    .getString(R.string.followup_instance_4sup))) {
                String json = message.replaceFirst(context.getString(R.string.followup_instance_4sup), "");
                createFollowupInstanceByTask(context, mid, json, true);

            } else if (message.startsWith(context
                    .getString(R.string.download_document))) {    //download a document
                String url = message
                        .replaceFirst(context.getString(R.string.download_document), "");
                reportRCMCommand(context, mid, Params.RCM_STATUS_RECEIVED);
                DocumentDownloadService.downloadDocument(context, url, mid);

            } else if (message.startsWith(context
                    .getString(R.string.update_document_list))) {
                //Sync all document
                reportRCMCommand(context, mid, Params.RCM_STATUS_RECEIVED);
                DocumentDownloadService.syncAllDocument(context);

            } else if (message.startsWith(context.getString(R.string.document_remove))) {
                //remove document
                reportRCMCommand(context, mid, Params.RCM_STATUS_RECEIVED);
                String fileName = message.replaceFirst(context.getString(R.string.document_remove), "");
                DocumentDownloadService.removeDocument(context, fileName, mid);
                NtfActionHelper.getInstance().deleteActionByUri(context,
                        DocumentDownloadService.getDocumentIntent(context, fileName).toUri(Intent.URI_INTENT_SCHEME));

            } else if (message.startsWith(context
                    .getString(R.string.download_instance_template))) {    //download instance template
                String url = message
                        .replaceFirst(context.getString(R.string.download_instance_template), "");
                downloadInstanceTemplate(context, mid, url);

            } else if (message.startsWith(context
                    .getString(R.string.forward_instance))) {    //forward working data to official
                String jsondata = message
                        .replaceFirst(context.getString(R.string.forward_instance), "");
                forwardWorkingData(context, jsondata, mid);

            } else if (message.startsWith(context.getString(R.string.notification))
                    || message.startsWith(context.getString(R.string.task))) { //received a notification/task
                receiveSSInteraction(context, mid, message);
            } else if (message.equals(context
                    .getString(R.string.clean_up_all))) {    //clean up all files and folders
                cleanUpAllData(context);
                reportReceivedMessage(context, mid);
            } else if (message.startsWith(context
                    .getString(R.string.clean_up_path))) {    //clean up file/folder follow the path
                cleanUpByPath(context, message);
                reportReceivedMessage(context, mid);
            } else if (message.startsWith(context
                    .getString(R.string.clean_up))) { //receive clean up pilot instruction
                String data[] = message.split(Constants.UNDERSCORE);
                if (data[1].equals(context.getString(R.string.clean_up_forms))) {
                    cleanUpPilot(context, CLEAN_FORMS, null);
                } else if (data[1].equals(context.getString(R.string.clean_up_log))) {
                    cleanUpPilot(context, CLEAN_LOG, null);
                } else if (data[1].equals(context.getString(R.string.clean_up_medialib))) {
                    cleanUpPilot(context, CLEAN_LIB, data.length > 2 ? data[2] : "");
                }
                reportReceivedMessage(context, mid);
            } else if (message.startsWith(context.getString(R.string.rcm_cm_qarule_update_form))) {
                String ruleId = message.replaceFirst(context
                        .getString(R.string.rcm_cm_qarule_update_form), "");
                downloadQARule(context, ruleId, "form", mid);
            } else if (message.startsWith(context.getString(R.string.rcm_cm_qarule_update_family))) {
                String ruleId = message.replaceFirst(context
                        .getString(R.string.rcm_cm_qarule_update_family), "");
                downloadQARule(context, ruleId, "family", mid);
            } else if (message.startsWith(context.getString(R.string.rcm_cm_qarule_delete_form))) {
                reportReceivedMessage(context, mid);
                String ruleId = message.replaceFirst(context.getString(R.string.rcm_cm_qarule_delete_form), "");
                deleteRule(ruleId, "form");
            } else if (message.startsWith(context.getString(R.string.rcm_cm_qarule_delete_family))) {
                reportReceivedMessage(context, mid);
                String ruleId = message.replaceFirst(context.getString(R.string.rcm_cm_qarule_delete_family), "");
                deleteRule(ruleId, "family");
            } else if (message.startsWith(context
                    .getString(R.string.rcm_reload_form_from_disk))) {
                String stt = message.replaceFirst(context
                        .getString(R.string.rcm_reload_form_from_disk), "");
                reportReceivedMessage(context, mid);
                if (stt.equalsIgnoreCase("on")) {
                    RTASurvey.getInstance().setOnOffReloadFormFromDisk(true);
                } else {
                    RTASurvey.getInstance().setOnOffReloadFormFromDisk(false);
                }
            } else if (message.startsWith(context
                    .getString(R.string.rcm_home_menu_move_item))) {
                String rule = message.replaceFirst(context
                        .getString(R.string.rcm_home_menu_move_item), "");
                reportReceivedMessage(context, mid);
                if (rule.equalsIgnoreCase("on")) {
                    RTASurvey.getInstance().setOnOffMoveHomeItem(true);
                } else {
                    RTASurvey.getInstance().setOnOffMoveHomeItem(false);
                }
            } else if (message.startsWith(context.getString(R.string.rcm_cm_update_app_settings))) {
                //update app Environment
                AppSettingSharedPreferences prefs = new AppSettingSharedPreferences(context);
                ConnectionService.getInstance().getAppSettings(context, prefs, RTASurvey.getInstance().getServerUrl(), Common.getMainUser().getUsername());
                notifyNewAppSettings(context);
            } else if (message.startsWith(context.getString(R.string.rcm_cm_get_folder_tree))) {
                //get folder tree and upload to server
                if (Common.isConnect(context)) {
                    String json = message.replaceFirst(context.getString(R.string.rcm_cm_get_folder_tree), "");
                    if (json == null || json.equals("")) {
                        reportRCMCommand(context, mid, Params.RCM_STATUS_FAILED);
                        return;
                    }
                    JSONObject jsonObject = new JSONObject(json);
                    String filePath = "";
                    int level = -1;
                    if (jsonObject.has("rootPath")) {
                        filePath = jsonObject.getString("rootPath");
                    } else {
                        reportRCMCommand(context, mid, Params.RCM_STATUS_FAILED);
                        return;
                    }
                    if (jsonObject.has("level")) {
                        level = jsonObject.getInt("level");
                    } else {
                        reportRCMCommand(context, mid, Params.RCM_STATUS_FAILED);
                        return;
                    }
                    GetFolderTreeService.requestService(context, GetFolderTreeService.ACTION_GET_FOLDER_TREE, filePath, level, mid);
                } else {
                    reportRCMCommand(context, mid, Params.RCM_STATUS_FAILED);
                }

            } else if (message
                    .startsWith(context.getString(R.string.exeprefix))) { //received another execute statement
                // pre-config message
                String[] data = message.split(Constants.COLON2);

                if (data[Constants.STATEMENT].equalsIgnoreCase(context    // delete interaction statement
                        .getString(R.string.delete_interaction))) {
                    String[] idList = data[Constants.PREFIX_ID]
                            .split(Constants.COMMA);

                    // delete interaction
                    for (String id : idList) {
                        DBService.getInstance().deleteInteraction(Integer.parseInt(id));
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(NTF_COUNTER_UPDATE));
                    reportReceivedMessage(context, mid);
                } else if (data[Constants.STATEMENT].startsWith(context     // get zip form
                        .getString(R.string.download_zipform))) {
                    reportReceivedMessage(context, mid);

                    if (!RCMCmdUtils.getInstance().checkPermissions(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Log.e(TAG, "The cmd doesn't have enough permissions to work.");
                        reportRCMCommandExeFailed(context, mid);
                        return;
                    }

                    int reportID = Integer.parseInt(data[Constants.PREFIX_ID]);
                    String url = message.substring(message.indexOf(Constants.HTTP2));
                    String decodedUrl = URLDecoder.decode(url, Constants.UTF8);
                    Log.i(TAG, context.getString(R.string.download_zipform) + "\n" +
                            decodedUrl);
                    String[] linkAndVer = decodedUrl.split(".bz2_");
                    String formId = FilenameUtils.getBaseName(linkAndVer[0]);
                    String version = linkAndVer[1].split(Constants.UNDERSCORE)[0];

                    downloadZIPForm(context, mid, reportID, formId, version, null);

                } else if (data[Constants.STATEMENT]    // delete forms by formId
                        .equalsIgnoreCase(context.getString(R.string.del_forms))) {
                    reportReceivedMessage(context, mid);
                    if (!RCMCmdUtils.getInstance().checkPermissions(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Log.e(TAG, "The cmd doesn't have enough permissions to work.");
                        reportRCMCommandExeFailed(context, mid);
                        return;
                    }

                    String[] formIds = data[Constants.PREFIX_ID]
                            .split(Constants.COMMA);
                    deleteForms(context, formIds);

                } else if (data[Constants.STATEMENT]    // delete instances by formId
                        .equalsIgnoreCase(context.getString(R.string.del_instances))) {
                    reportReceivedMessage(context, mid);
                    if (!RCMCmdUtils.getInstance().checkPermissions(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Log.e(TAG, "The cmd doesn't have enough permissions to work.");
                        reportRCMCommandExeFailed(context, mid);
                        return;
                    }
                    String[] formIds = data[Constants.PREFIX_ID]
                            .split(Constants.COMMA);
                    deleteInstances(context, SurveyCollectorUtils.getInstanceIdList(context, formIds));

                } else if (data[Constants.STATEMENT]    // delete forms and its instances by formId
                        .equalsIgnoreCase(context.getString(R.string.del_forms_and_instances))) {
                    reportReceivedMessage(context, mid);
                    if (!RCMCmdUtils.getInstance().checkPermissions(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Log.e(TAG, "The cmd doesn't have enough permissions to work.");
                        reportRCMCommandExeFailed(context, mid);
                        return;
                    }
                    String[] formIds = data[Constants.PREFIX_ID]
                            .split(Constants.COMMA);
                    deleteForms(context, formIds);
                    deleteInstances(context, SurveyCollectorUtils.getInstanceIdList(context, formIds));
                }

            } else { // received a normal message
                MessageUtils.generateNotification(context, message);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFollowupInstanceByTask(Context context, String mid,
                                              String json, boolean forSupervisor)
            throws JSONException {
        JSONObject jObj = new JSONObject(json);
        String formId = jObj.getString("formID");
        String formVersion = jObj.getString("version");
        String instanceLink = jObj.getString("instance_link");
        long uuidCounter = jObj.has("uuid_counter") ? jObj.getLong("uuid_counter") : -1L;

        AdaptFollowUpInstanceTask task = createFollowUpInstanceTask(context,
                instanceLink, uuidCounter, mid, forSupervisor);
        task.execute();
        if (!SurveyCollectorUtils.formIsInMetadataTable(context, formId, formVersion)) {
            String logMsg = String.format("Form %s, version %s isn't existed." +
                    " Try to download form before.", formId, formVersion);
            log.info(logMsg);
            OldFormDownloadService.requestService(context, formId, formVersion);
        }
    }

    private void deleteRule(String ruleId, String ruleType) throws IOException {
        File ruleDir = new File(RTASurvey.R_SCRIPT_DIRPATH, ruleId + "." + ruleType);
        if (ruleDir.exists()) {
            FileUtils.deleteDirectory(ruleDir);
        }
    }

    private void disapproveReqToEdit(Context context, String uuid, String mid) {
        reportReceivedMessage(context, mid);
        ContentValues values = new ContentValues();
        values.put(InstanceProviderAPI.InstanceColumns.REQUEST_TO_EDIT_DATE, 0);
        int updatedRows = context.getContentResolver()
                .update(InstanceProviderAPI.InstanceColumns.CONTENT_URI, values,
                        InstanceProviderAPI.InstanceColumns.INSTANCE_UUID + "=? and "
                                + InstanceProviderAPI.InstanceColumns.STATUS + " like ?",
                        new String[]{uuid, InstanceProviderAPI.STATUS_COMPLETE + "%"});
        if (updatedRows > 0) {
            String iName = SurveyCollectorUtils.getInstanceName(context, uuid);
            NotificationCompat.Style style = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(context.getString(R.string.rta_app_name))
                    .bigText(context.getString(R.string.request_return_instance_err_disapproved, iName));
            MessageUtils.generateNotification(context, style, android.R.drawable.stat_sys_warning,
                    context.getString(R.string.rta_app_name),
                    context.getString(R.string.request_return_instance_err_disapproved, iName),
                    R.string.request_return_instance_err_disapproved);
            reportRCMCommandExeSuccess(context, mid);

            //create instance package for sync to server
            SurveyCollectorUtils.addNewBackupPackageToUploadQueue(context, uuid);
        } else {
            reportRCMCommandExeFailed(context, mid);
        }
    }

    private void updateAllSamplingArea(Context context, String mid) {
        RTASurvey app = RTASurvey.getInstance();
        SamplingResponse samplingRes = SurveyConnectionService.getInstance()
                .getSamplingAreas(context, app.getServerUrl(),
                        UserListDbHelper.getInstance().getMainUsername());
        if (samplingRes != null) {
            if (samplingRes.areaDefiningForm != null) {
                app.setSamplingForm(RTASurvey.KEY_SAMPLING_FORM_AREA_ID,
                        samplingRes.areaDefiningForm.formId,
                        samplingRes.areaDefiningForm.version,
                        samplingRes.areaDefiningForm.lockStt,
                        samplingRes.areaDefiningForm.mainKey,
                        samplingRes.areaDefiningForm.geoKey);
                try {
                    SurveyCollectorUtils.lockForm(context,
                            samplingRes.areaDefiningForm.formId,
                            samplingRes.areaDefiningForm.version,
                            samplingRes.areaDefiningForm.lockStt == 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (samplingRes.listingForm != null) {
                app.setSamplingForm(RTASurvey.KEY_SAMPLING_FORM_LISTING_ID,
                        samplingRes.listingForm.formId,
                        samplingRes.listingForm.version,
                        samplingRes.listingForm.lockStt,
                        samplingRes.listingForm.mainKey,
                        samplingRes.listingForm.geoKey);
                try {
                    SurveyCollectorUtils.lockForm(context,
                            samplingRes.listingForm.formId,
                            samplingRes.listingForm.version,
                            samplingRes.listingForm.lockStt == 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (samplingRes.assignedAreas != null) {
                File zipDir = new File(RTASurvey.ZIP_PATH);
                File kmlDir = new File(RTASurvey.KML_DIR_PATH);
                if (!zipDir.exists()) zipDir.mkdir();
                if (!kmlDir.exists()) kmlDir.mkdir();
                for (SamplingArea area : samplingRes.assignedAreas) {
                    try {
                        SamplingDbHelper.getInstance().saveSamplingArea(context, area);
                        if (area.kml != null && !area.kml.equals("")) {
                            File kmlFile = new File(RTASurvey.ZIP_PATH, area.code + ".kml.bz2");
                            ConnectionService.getInstance().downloadFile(kmlFile, area.kml);
                            if (kmlFile.exists()) {
                                File old = new File(RTASurvey.KML_DIR_PATH, area.code + ".kml");
                                if (old.exists()) {
                                    old.delete();
                                }
                                ZIPPackUtils.unpack(RTASurvey.ZIP_PATH,
                                        RTASurvey.KML_DIR_PATH, area.code + ".kml.bz2");

                                //cleanup
                                File zip = new File(zipDir, area.code + ".kml.bz2");
                                if (zip.exists()) {
                                    zip.delete();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void tryToDeleteSamplingArea(Context context, JSONArray array, String mid) {
        reportReceivedMessage(context, mid);
        for (int i = 0; i < array.length(); i++) {
            try {
                String code = array.getString(i);
                SamplingDbHelper.getInstance().deleteArea(context, code);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void tryToUpdateSamplingArea(Context context, JSONArray array, String mid) {
        reportReceivedMessage(context, mid);
        File zipDir = new File(RTASurvey.ZIP_PATH);
        File kmlDir = new File(RTASurvey.KML_DIR_PATH);
        if (!zipDir.exists()) zipDir.mkdir();
        if (!kmlDir.exists()) kmlDir.mkdir();
        for (int i = 0; i < array.length(); i++) {
            try {
                String code = array.getString(i);
                SamplingArea area = SurveyConnectionService.getInstance().getSamplingArea(context,
                        RTASurvey.getInstance().getServerUrl(), code);
                SamplingDbHelper.getInstance().saveSamplingArea(context, area);
                if (area.kml != null && !area.kml.equals("")) {
                    File kmlFile = new File(zipDir, area.code + ".kml.bz2");
                    ConnectionService.getInstance().downloadFile(kmlFile, area.kml);
                    if (kmlFile.exists()) {
                        File old = new File(kmlDir, area.code + ".kml");
                        if (old.exists()) {
                            old.delete();
                        }
                        ZIPPackUtils.unpack(RTASurvey.ZIP_PATH,
                                RTASurvey.KML_DIR_PATH, area.code + ".kml.bz2");

                        //cleanup
                        File zip = new File(zipDir, area.code + ".kml.bz2");
                        if (zip.exists()) {
                            zip.delete();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void tryToDeleteCategory(Context context, JSONArray array, String mid, boolean deepDelete) {
        reportReceivedMessage(context, mid);
        for (int i = 0; i < array.length(); i++) {
            try {
                ModuleDbHelper db = ModuleDbHelper.getInstance();
                String code = array.getString(i);
                int deleted = db.deleteCategory(context, code);
                if (deleted > 0 && deepDelete) {   // delete its module-subscription as well
                    // try to get the all modules need to delete, by category code
                    Cursor m = getContentResolver().query(ModuleProviderAPI.Modules.CONTENT_URI,
                            new String[] {ModuleProviderAPI.Modules.CODE},
                            ModuleProviderAPI.Modules.CATEGORY + "=?",
                            new String[] {code}, null);
                    // clean them
                    if (m != null) {
                        while (m.moveToNext()) {
                            String mCode = m.getString(0);
                            if (mCode != null && !mCode.equals("")) {
                                db.cleanModuleData(context, mCode);
                            }
                        }
                        m.close();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateAllCategory(Context context, String mid) {
        reportReceivedMessage(context, mid);
        RTCenterServiceHelper conn = RTCenterServiceHelper.getInstance();
        String centerURL = conn
                .getPref(context, RTCenterServiceHelper.KEY_LOCAL_CENTER_URL, "");
        if (centerURL != null && !centerURL.equals("")) {
            CategoryList list = conn.getCategoryList(context, centerURL);
            if (list == null) {
                log.error("ERROR:: Cannot load module list");
                return;
            }
            ModuleDbHelper db = ModuleDbHelper.getInstance();
            db.cleanAllCategories(context);
            for (Category c : list.getCategories()) {
                db.saveCategory(context, c);
            }
        }
    }

    private void tryToUpdateCategory(Context context, JSONArray array, String mid) {
        reportReceivedMessage(context, mid);
        for (int i = 0; i < array.length(); i++) {
            try {
                String code = array.getString(i);
                RTCenterServiceHelper conn = RTCenterServiceHelper.getInstance();
                String centerURL = conn
                        .getPref(context, RTCenterServiceHelper.KEY_LOCAL_CENTER_URL, "");
                if (centerURL != null && !centerURL.equals("")) {
                    ModuleDbHelper db = ModuleDbHelper.getInstance();
                    Category c = conn.getCategory(context, centerURL, code);
                    if (c != null && StdCodeDBHelper.STT_S1001.equals(c.getSttCode())
                            && db.saveCategory(context, c)) {
//                        if (c.getOrder() > -1) {
//                            db.reOrderCategoryFromNewOne(context, c.getCategoryCode(), c.getOrder());
//                        }
                        reportRCMCommandExeSuccess(context, mid);
                    } else {
                        reportRCMCommandExeFailed(context, mid);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void tryToDeleteModule(Context context, JSONArray array, String mid) {
        reportReceivedMessage(context, mid);
        for (int i = 0; i < array.length(); i++) {
            try {
                String code = array.getString(i);
                ModuleDbHelper.getInstance().cleanModuleData(context, code);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateAllModule(Context context, String mid) {
        reportReceivedMessage(context, mid);
        if (BuildConfig.FLAVOR.equals("rtcpms")) {
            String host = RTASurvey.getInstance().getServerUrl();
            String username = UserListDbHelper.getInstance().getMainUsername();
            ModuleDbHelper moduleDb = ModuleDbHelper.getInstance();
            for (ModuleDetail m : ConnectionService.getInstance()
                    .getAppModules(context, host, username)) {
                if (moduleDb.saveModuleDetail(context, m)) {
                    moduleDb.cleanSubscriptionOptions(context, m.getCode());
                    if (!Module.TYPE_SYSTEM.equals(m.getType())) {
                        ContentValues values = new ContentValues();
                        values.put(ModuleProviderAPI.Subscriptions.CODE, m.getCode() + "_auto");
                        values.put(ModuleProviderAPI.Subscriptions.MODULE_CODE, m.getCode());
                        values.put(ModuleProviderAPI.Subscriptions.TITLE, "Dummy Option of " + m.getCode());
                        values.put(ModuleProviderAPI.Subscriptions.STATUS, ModuleProviderAPI.STT_SUBSCRIBED);
                        getContentResolver().insert(ModuleProviderAPI.Subscriptions.CONTENT_URI, values);
                    }
                }
            }
        } else if (BuildConfig.FLAVOR.equals("rthome")) {
            RTCenterServiceHelper conn = RTCenterServiceHelper.getInstance();
            String centerURL = conn
                    .getPref(context, RTCenterServiceHelper.KEY_LOCAL_CENTER_URL, "");
            String pCode = conn
                    .getPref(context, RTCenterServiceHelper.KEY_PROJECT_CODE, "");
            String username = UserListDbHelper.getInstance().getMainUsername();
            if (centerURL != null && !centerURL.equals("")) {
                List<Subscription> list = conn.getModuleSubscriptionList(context, centerURL, pCode, username);
                if (list == null) {
                    log.error("ERROR:: Cannot load module (subscription) list");
                    return;
                }
                ModuleDbHelper db = ModuleDbHelper.getInstance();
                for (Subscription s : list) {
                    db.saveModuleSubscription(context, s);
                    if (s.getSubscribeStt() != null && s.getSubscribeStt().size() > 0) {
                        ModuleDetail m = conn.getModuleDetail(context, centerURL,
                                s.getCode(), pCode, username);
                        if (m != null) {
                            db.saveModuleDetail(context, m);
                        }
                    }
                }
            }
        } else {
            log.error("This app (" + BuildConfig.FLAVOR + ") doesn't support module-cmds.");
        }

    }

    private void tryToUpdateModule(Context context, JSONArray array, String mid) {
        reportReceivedMessage(context, mid);
        for (int i = 0; i < array.length(); i++) {
            try {
                ModuleDbHelper db = ModuleDbHelper.getInstance();
                String code = array.getString(i);

                if (BuildConfig.FLAVOR.equals("rtcpms")) {
                    ModuleDetail m = ConnectionService.getInstance().getAppModule(context,
                            RTASurvey.getInstance().getServerUrl(), code,
                            UserListDbHelper.getInstance().getMainUsername());
                    if (m != null) {
                        if (db.saveModuleDetail(context, m)) {
                            db.cleanSubscriptionOptions(context, code);
                            if (!Module.TYPE_SYSTEM.equals(m.getType())) {
                                ContentValues values = new ContentValues();
                                values.put(ModuleProviderAPI.Subscriptions.CODE, m.getCode() + "_auto");
                                values.put(ModuleProviderAPI.Subscriptions.MODULE_CODE, m.getCode());
                                values.put(ModuleProviderAPI.Subscriptions.TITLE, "Dummy Option of " + m.getCode());
                                values.put(ModuleProviderAPI.Subscriptions.STATUS, ModuleProviderAPI.STT_SUBSCRIBED);
                                context.getContentResolver().insert(ModuleProviderAPI.Subscriptions.CONTENT_URI, values);
                            }
                        }
                    }
                } else if (BuildConfig.FLAVOR.equals("rthome")) {
                    RTCenterServiceHelper conn = RTCenterServiceHelper.getInstance();
                    String centerURL = conn
                            .getPref(context, RTCenterServiceHelper.KEY_LOCAL_CENTER_URL, "");
                    String pCode = conn
                            .getPref(context, RTCenterServiceHelper.KEY_PROJECT_CODE, "");
                    String username = UserListDbHelper.getInstance().getMainUsername();
                    if (centerURL != null && !centerURL.equals("")) {
                        Subscription s = conn.getModuleSubscription(context, centerURL, code, pCode, username);
                        if (s != null && StdCodeDBHelper.STT_S1001.equals(s.getSttCode())) {
                            s.setType(Module.TYPE_CUSTOM);
                            db.saveModuleSubscription(context, s);
                            if (s.getSubscribeStt() != null && s.getSubscribeStt().size() > 0) {
                                ModuleDetail m = conn.getModuleDetail(context, centerURL,
                                        s.getCode(), pCode, username);
                                if (m != null) {
                                    db.saveModuleDetail(context, m);
                                }
                            }
                        }
                    }
                } else {
                    log.error("This app (" + BuildConfig.FLAVOR + ") doesn't support module-cmds.");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteRReport(Context context, String reportID, String mid) {
        int deleted = 0;
        boolean offlineDeleted = false;
        String path = null, type = null;

        //clean to-download queue
        context.getContentResolver().delete(ReportProviderAPI.ToDownload.CONTENT_URI,
                ReportProviderAPI.ToDownload._ID + " = ?", new String[]{reportID});

        //try to remove offline report
        Cursor c = context.getContentResolver().query(ReportProviderAPI.RReport.CONTENT_URI, null,
                ReportProviderAPI.RReport._ID + " = ?", new String[]{reportID}, null);
        if (c != null) {
            if (c.getCount() > 0 && c.moveToFirst()) {
                //get report's path and type in order to clean up old ntf-action of rtHelper
                path = c.getString(c.getColumnIndex(ReportProviderAPI.RReport.PATH));
                type = c.getString(c.getColumnIndex(ReportProviderAPI.RReport.REPORTTYPE));
                StringBuilder key = new StringBuilder(RTASurvey.PREFIX_RREPORT + "-");
                key.append(c.getString(c.getColumnIndex(ReportProviderAPI.RReport.TITLE)) + "-");
                key.append(path + "-");
                key.append(reportID + "-");
                key.append(type + "-");
                key.append(BuildConfig.APPLICATION_ID);
                if (path != null && !path.equals("")) {
                    File file = new File(path);
                    if (file.exists()) {
                        if (FileUtils.deleteQuietly(file)) {
                            deleted += context.getContentResolver().delete(ReportProviderAPI.RReport.CONTENT_URI,
                                    ReportProviderAPI.RReport._ID + " = ?", new String[]{reportID});
                            RTASurvey.getInstance().removeItemToPinList(key.toString());
                        }
                    }
                }
            }
            c.close();
            offlineDeleted = deleted > 0;
        }

        if (!offlineDeleted) {
            //try to remove online report
            c = context.getContentResolver().query(ReportProviderAPI.OnlineReport.CONTENT_URI, null,
                    ReportProviderAPI.OnlineReport._ID + " = ?", new String[]{reportID}, null);
            StringBuilder key = new StringBuilder(RTASurvey.PREFIX_RREPORT + "-");
            if (c != null) {
                if (c.getCount() > 0 && c.moveToFirst()) {
                    //get report's link in order to clean up old ntf-action of rtHelper
                    path = c.getString(c.getColumnIndex(ReportProviderAPI.OnlineReport.REPORTLINK));
                    key.append(c.getString(c.getColumnIndex(ReportProviderAPI.OnlineReport.TITLE)) + "-");
                    key.append(path + "-");
                    key.append(reportID + "-");
                    key.append(BuildConfig.APPLICATION_ID);
                }
                c.close();
            }
            deleted += context.getContentResolver().delete(ReportProviderAPI.OnlineReport.CONTENT_URI,
                    ReportProviderAPI.OnlineReport._ID + " = ?", new String[]{reportID});
            RTASurvey.getInstance().removeItemToPinList(key.toString());
        }

        //nothing was deleted -> deletion failed
        if (deleted == 0) {
            reportRCMCommandExeFailed(context, mid);
        } else {
            //clean up old ntf-action of rtHelper
            Intent oldIntent = ReportAdapter.getReportIntent(context, reportID, path, type, offlineDeleted);
            NtfActionHelper.getInstance().deleteActionByUri(context, oldIntent.toUri(Intent.URI_INTENT_SCHEME));

            log.info(deleted + " R-Report record(s) has been deleted");
            reportRCMCommandExeSuccess(context, mid);
        }
    }

    private void assignSecondaryUser(Context context, String mid, String message) {
        reportReceivedMessage(context, mid);
        String jsonEncrypted = message.replaceFirst(context
                .getString(R.string.rcm_secondary_user_assign), "");
        RTASurvey app = RTASurvey.getInstance();
        try {
            String jsonStr = SimpleCrypto.decrypt(app.getServerKey(), jsonEncrypted);
            UserCredential userCred =
                    (UserCredential) StringUtil.json2Object(jsonStr, UserCredential.class);
            if (userCred != null) {
                UserListDbHelper.getInstance().saveUser(
                        userCred.getUsername(), userCred.getPassword(), userCred.getDefaultPIN(),
                        userCred.getMatrix_username(), userCred.getMatrix_password(), false);
                reportRCMCommandExeSuccess(context, mid);

                Common.updateStaffInfo(context, app.getServerUrl(), app.getServerKey(),
                        userCred.getUsername(), log);
            } else {
                throw new Exception("JSONException - failed to convert JSON");
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportRCMCommandExeFailed(context, mid);
        }
    }

    private void reportRSSettingJSON(final Context context, final String mid) {
        reportReceivedMessage(context, mid);
        new UploadRSSettingTask(context, RTASurvey.ZIP_PATH,
                RTASurvey.getInstance().getServerUrl(), new UploadTaskListener() {
            @Override
            public void uploadCompleted(int statusCode, String message, String filePath) {
                Log.e(TAG, String.format("reportRSSettingJSON() completed:\nstatus:%d\nmsg:%s",
                        statusCode, message));
                reportRCMCommandExeSuccess(context, mid);
            }

            @Override
            public void uploadFailed(int statusCode, String message, String filePath) {
                Log.e(TAG, String.format("reportRSSettingJSON() failed:\nstatus:%d\nmsg:%s",
                        statusCode, message));
                reportRCMCommandExeFailed(context, mid);
            }
        }).upload(RTASurvey.getInstance().getDeviceId());
    }

    private void cleanUpByPath(Context context, String cmd) throws JSONException, IOException {
        String json = cmd.replaceFirst(context.getString(R.string.clean_up_path), "");
        JSONObject jObj = new JSONObject(json);

        String path = jObj.has("path") ? jObj.getString("path") : "";
        if (!path.startsWith(Environment.getExternalStorageDirectory().toString())) {
            path = Environment.getExternalStorageDirectory() + File.separator + path;
        }
        File f = new File(path);

        //clean up file/folder
        if (f.exists()) {
            if (f.isDirectory()) {
                FileUtils.deleteDirectory(f);
            } else {
                f.delete();
            }
        }
    }

    private void cleanUpAllData(Context context) throws IOException {
        //clean up!
        File RSDir = new File(RTASurvey.RTA_ROOT);
        if (RSDir.exists()) {
            for (File f : RSDir.listFiles()) {
                if (!f.getName().equals("serverUrl")) {
                    if (f.isDirectory())
                        FileUtils.deleteDirectory(f);
                    else
                        f.delete();
                }
            }
        }

        //stop application activities
        stopManagerService(context);
        System.exit(0);
    }

    private void stopManagerService(Context context) {
        Intent intent = new Intent(context, ManagerService.class);
        context.stopService(intent);
        if (LinphoneService.isReady()) {
            context.stopService(new Intent(Intent.ACTION_MAIN).setClass(context, LinphoneService.class));
            //ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            //am.killBackgroundProcesses(context.getString(R.string.sync_account_type));
            //android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private void lockReport(Context context, String reportType, String mid,
                            boolean lock) {
        if (REPORT_INSTANCE_STT.equals(reportType)) {
            RTASurvey.getInstance().setInstanceReportStatusLock(lock);
        }
        // else if() {...}
    }

    private void forwardWorkingData(final Context context, String json, final String mid) {
        reportReceivedMessage(context, mid);
        ForwardInstanceInfo info =
                (ForwardInstanceInfo) StringUtil.json2Object(json, ForwardInstanceInfo.class);
        if (info == null || TextUtils.isEmpty(info.formID)
                || TextUtils.isEmpty(info.formUrl)
                || TextUtils.isEmpty(info.version)
                || TextUtils.isEmpty(info.uuid)
                || info.reportID < 0
                || TextUtils.isEmpty(info.instanceUrl)) {
            reportRCMCommandExeFailed(context, mid);
            log.info("Forward Working data - ERROR::Invalid info");
            return;
        }

        long _id = SurveyCollectorUtils.getInstanceId(context, info.uuid);
        if (_id > -1) {
            reportRCMCommandExeFailed(context, mid);
            log.info(String.format("Forward Working data - ERROR::%s is existed", info.uuid));
            return;
        }

        try {
            String decodedUrl = URLDecoder.decode(info.formUrl, Constants.UTF8);
            if (!SurveyCollectorUtils.formIsExist(context, info.formID, info.version)) {
                log.info(String.format("Form %s, version %s isn't existed." +
                                " Try to download form before.",
                        info.formID, info.version));
                ForwardInstanceTask task =
                        createForwardInstanceTask(context, info.instanceUrl,
                                info.uuid, info.counter, mid);
                downloadZIPForm(context, mid, info.reportID, info.formID,
                        info.version, task);

            } else {
                log.info(String.format("Form %s, version %s is existed." +
                                " Forward instance data without form downloading",
                        info.formID, info.version));
                createForwardInstanceTask(context, info.instanceUrl,
                        info.uuid, info.counter, mid).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportRCMCommandExeFailed(context, mid);
            Log.e(TAG, "Forwarding Instance data failed.");
            MessageUtils.generateNotification(context, R.string.forwardInstance_retrieve_fail,
                    R.string.forwardInstance_retrieve_fail);
        }

    }

    private void reportRunningApps(Context context, final String mid) {
        reportReceivedMessage(context, mid);
        if (ConnectionService.getInstance().reportRunningApps(context,
                RTASurvey.getInstance().getServerUrl(),
                RTASurvey.getInstance().getServerKey(),
                RTASurvey.getInstance().getDeviceId())) {
            reportRCMCommandExeSuccess(context, mid);
        } else {
            reportRCMCommandExeFailed(context, mid);
        }
    }

    private void downloadZIPForm(final Context context, final String mid,
                                 int reportID, String formId, String version,
                                 AsyncTask<String, Void, Boolean> nextTask) throws Exception {
        if (!SurveyCollectorUtils.formIsExist(context, formId, version)) {
            if (!SurveyCollectorUtils.formIsLocked(context, formId, version)) {
                SurveyCollectorUtils.downloadZipForm(context,
                        formId, version,
                        reportID, mid, nextTask);
            } else {
                reportRCMCommandExeFailed(context, mid);
                log.info("Form with formId: " + formId + " ; formVer: " + version
                        + " is locked --> report FAILED TO DOWNLOAD");
            }
        } else {
            reportRCMCommandExeSuccess(context, mid);
            log.info("Form with formId: " + formId + " ; formVer: " + version
                    + " is exist --> report SUCCEEDED");
        }
    }

    /**
     * @param context
     * @param mid
     * @param uuid
     */
    private void syncToNewestFormVersion(final Context context,
                                         final String mid, String uuid) {
        reportReceivedMessage(context, mid);
        long ins_id = SurveyCollectorUtils.getInstanceId(context, uuid);
        if (ins_id == -1) {
            Log.e(TAG, "Cannot find instance - " + uuid);
            reportRCMCommandExeFailed(context, mid);
        } else {
            ContentResolver cr = context.getContentResolver();
            Uri instanceUri = InstanceProviderAPI.InstanceColumns.CONTENT_URI;
            Uri formUri = FormsProviderAPI.FormsColumns.CONTENT_URI;
            ContentValues values = new ContentValues();

            Cursor cinstance = cr.query(Uri.withAppendedPath(instanceUri, String.valueOf(ins_id)),
                    null, null, null, null);
            if (cinstance == null || cinstance.getCount() == 0) {
                Log.e(TAG, "Cannot find instance - " + uuid);
                reportRCMCommandExeFailed(context, mid);
                return;
            }
            cinstance.moveToFirst();
            String formId = cinstance
                    .getString(cinstance.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_FORM_ID));
            String fversion = cinstance
                    .getString(cinstance.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_VERSION));

            // try to get newest and available Form version
            Cursor cver = cr.query(formUri,
                    new String[]{FormsProviderAPI.FormsColumns.JR_VERSION,
                            FormsProviderAPI.FormsColumns.DATE},
                    FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? and " +
                            FormsProviderAPI.FormsColumns.LOCKED + "=?",
                    new String[]{formId, String.valueOf(false)},
                    FormsProviderAPI.FormsColumns.DATE + " DESC");
            if (cver != null && cver.getCount() > 0) {
                cver.moveToFirst();
                fversion = cver.getString(cver
                        .getColumnIndex(FormsProviderAPI.FormsColumns.JR_VERSION));
                cver.close();
            }
            values.put(InstanceProviderAPI.InstanceColumns.JR_VERSION, fversion);

            int updatedRows = cr.update(Uri.withAppendedPath(instanceUri, String.valueOf(ins_id)),
                    values, null, null);
            if (updatedRows > 0) {
                Log.e(TAG, "Successfully sync instance(" + ins_id + ") version - " + uuid);
                reportRCMCommandExeSuccess(context, mid);
            } else {
                Log.e(TAG, "Cannot update version for instance(" + ins_id + ") - " + uuid);
                reportRCMCommandExeFailed(context, mid);
            }

            cinstance.close();
        }
    }

    private void downloadQAfromUrl(final Context context, String url,
                                   final long returnCount, final String returnUuid, final String mid)
            throws FileNotFoundException {
        new DownloadQAFileTask(new DownloadQAFileTask.DownloadQATaskListener() {
            @Override
            public void downloadFail(String filePath) {
                log.error("Download QA data file failed");
                reportRCMCommandExeFailed(context, mid);
            }

            @Override
            public void downloadCompleted(String filePath) {
                String downloadlog = "Download QA data file success, trying to parse data.";
                String parsingErr = "QA data parsing (from data file) is failed.";
                String parsingDone = "QA data parsing (from data file) is success.";
                log.error(downloadlog);
                try {
                    if (QATaskDbHelper.getInstance().loadFromFile(filePath, returnCount, true)) {
                        RTASurveySender.showQAData(context);
                        log.debug(parsingDone);

                        if (!TextUtils.isEmpty(returnUuid)) {
                            log.debug("Return: " + returnUuid);
                            ReturnInstanceService.requestService(context, returnUuid, returnCount, mid,
                                    new AfterJob<String, Void>() {
                                        @Override
                                        public Void apply(String newUuid) {
                                            if (newUuid != null && !newUuid.equals("")) {
                                                MessageUtils.generateNotification(context, R.string.instance_is_returned, R.string.instance_is_returned);
                                                reportRCMCommandExeSuccess(context, mid);
                                            } else {
                                                log.error(context.getString(R.string.instance_return_fail));
                                                reportRCMCommandExeFailed(context, mid);
                                            }

                                            String stt = (newUuid == null || newUuid.equals("")) ? "FAILED" : "SUCCESS";

                                            //update QA Online UI
                                            Intent intent = new Intent("vn.rta.rtsurvey.android.RequestQAReturnFragment.UPDATE_DATA");
                                            intent.putExtra("vn.rta.rtsurvey.android.RequestQAReturnFragment.EXTRA_STATUS", stt);
                                            broadcastUIUpdate(context, intent);

                                            ContentResolver cr = context.getContentResolver();
                                            ContentValues values = new ContentValues();

                                            //insert cmd information to cache
                                            values.put(QaOnlineProviderAPI.ReturnedCmdCache.RCM_ID, mid);
                                            values.put(QaOnlineProviderAPI.ReturnedCmdCache.ORIGIN_UUID, returnUuid);
                                            values.put(QaOnlineProviderAPI.ReturnedCmdCache.RETURNED_UUID, newUuid);
                                            cr.insert(QaOnlineProviderAPI.ReturnedCmdCache.CONTENT_URI, values);

                                            //update the table for directly display
                                            values = new ContentValues();
                                            values.put(QaOnlineProviderAPI.RCMIds.RETURNED_UUID, newUuid);
                                            values.put(QaOnlineProviderAPI.RCMIds.STATUS, stt);
                                            cr.update(QaOnlineProviderAPI.RCMIds.CONTENT_URI, values,
                                                    QaOnlineProviderAPI.RCMIds.RCMID + "=?", new String[]{mid});
                                            return null;
                                        }
                                    });
                        } else {
                            reportRCMCommandExeSuccess(context, mid);
                        }
                    } else {
                        RTASurveySender.loadQAdataFailed(context);
                        log.error(parsingErr);
                        reportRCMCommandExeFailed(context, mid);
                    }
                } catch (Exception e) {
                    log.error(parsingErr);
                    reportRCMCommandExeFailed(context, mid);
                    e.printStackTrace();
                }
            }
        }).download(url, RTASurvey.TEMP_ROOT_PATH);

    }

    private void downloadCollections(final Context context, String json, final String mid) {
        DownloadCollectionsService.requestService(context, json, mid, DownloadCollectionsService.ACTION_DOWNLOAD_COLLECTION);
    }

    private void insertIntoReportDB(Context context, String json) {
        try {
            Log.e(TAG, "Json: " + json);
            OnlineReport report = (OnlineReport) StringUtil.json2Object(json, OnlineReport.class);
            Intent intent1 = new Intent(ProcessFragment.DOWNLOAD_REPORT);
            intent1.putExtra("instanceName", report.getReportID() + "");
            LocalBroadcastManager.getInstance(RTASurvey.getInstance().getApplicationContext()).sendBroadcast(intent1);
            if (report.getVersion() == null || report.getVersion().equals("")) {
                report.setVersion("0");
            }
            if (report != null) {
                ContentValues values = new ContentValues();
                values.put(ReportProviderAPI.OnlineReport._ID, report.getReportID());
                values.put(ReportProviderAPI.OnlineReport.REPORTLINK, report.getReportLink());
                values.put(ReportProviderAPI.OnlineReport.TITLE, report.getTitle());
                values.put(ReportProviderAPI.OnlineReport.DESC, report.getDesc());
                values.put(ReportProviderAPI.OnlineReport.VERSION, report.getVersion());
                values.put(ReportProviderAPI.OnlineReport.UPDATETIME, System.currentTimeMillis());
                Cursor cursor = context.getContentResolver().query(ReportProviderAPI.OnlineReport.CONTENT_URI, null,
                        ReportProviderAPI.OnlineReport._ID + " = ?", new String[]{String.valueOf(report.getReportID())}, null);
                Uri res = null;
                long _id = -1;
                if (cursor != null && cursor.getCount() > 0) {
                    _id = context.getContentResolver().update(ReportProviderAPI.OnlineReport.CONTENT_URI, values, ReportProviderAPI.OnlineReport._ID + " = ?", new String[]{String.valueOf(report.getReportID())});
                } else {
                    res = context.getContentResolver().insert(ReportProviderAPI.OnlineReport.CONTENT_URI, values);
                }
                int resMsgId = R.string.download_form_completed;
                if (res == null && _id == -1) {
                    resMsgId = R.string.download_form_failed;
                }
                MessageUtils.generateNotification(context, android.R.drawable.stat_sys_download_done,
                        context.getString(R.string.util_report_download_ntf_bar_title),
                        context.getString(resMsgId),
                        R.string.util_report_download_ntf_bar_title);

                Intent intent = ReportAdapter.getReportIntent(context,
                        String.valueOf(report.getReportID()), report.getReportLink(), null, false);
                NtfActionHelper.getInstance()
                        .insertNtfAction(context, R.string.util_report_download_ntf_bar_title,
                                R.string.util_report_title, null, new String[]{report.getTitle()}, intent, true);

            } else {
                MessageUtils.showToastInfo(context, context.getResources().getString(R.string.error));
            }
            LocalBroadcastManager.getInstance(RTASurvey.getInstance().getApplicationContext()).sendBroadcast(new Intent(ProcessFragment.DOWNLOAD_REPORT_STOP));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reportLockStatus(Context context, String formId, String version,
                                  String type, int status) {
        FormLockReportService.requestService(context, formId, version, type, status);
    }

    private void transferResource(Context context, String json, String mid, int type) throws JSONException {
        reportReceivedMessage(context, mid);
        JSONObject jsonObject = new JSONObject(json);

        switch (type) {
            case TYPE_DOWNLOAD: {
                String url = jsonObject.getString(KEY_LINK);
                String dirpath = jsonObject.getString(KEY_DIRPATH);
                String option = jsonObject.getString(KEY_OPTION);
                if (StringUtil.isEmptyOrNull(url) || StringUtil.isEmptyOrNull(dirpath)
                        || StringUtil.isEmptyOrNull(option)) {
                    log.error("ERROR:: Main resource information is null");
                    return;
                }
                DownloadResourceService.requestService(context, mid, dirpath, url, option);
                break;
            }
            case TYPE_UPLOAD: {
                String dirpath = jsonObject.getString(KEY_DIRPATH);
                String option = jsonObject.getString(KEY_OPTION);
                if (StringUtil.isEmptyOrNull(dirpath)) {
                    log.error("ERROR:: Main resource information is null");
                    return;
                }
                UploadResourceService.requestService(context, mid, dirpath, null, option);
                break;
            }
            default:
                log.error("ERROR:: Transferring type is undefined");
                reportRCMCommandExeFailed(context, mid);
                break;
        }

    }

    private void pingToServer(final Context context, final String mid) {
        reportReceivedMessage(context, mid);
        if (ConnectionService.getInstance().pingToServer(context,
                RTASurvey.getInstance().getServerUrl(), RTASurvey.getInstance().getDeviceId())) {
            Log.i(TAG, "ping to server success");
            reportRCMCommandExeSuccess(context, mid);
        } else {
            Log.i(TAG, "ping to server failed");
            reportRCMCommandExeFailed(context, mid);
        }
    }

    private void deactivateUser(Context context) {
        RTASurvey app = RTASurvey.getInstance();
        app.setDeactivatingLock(true);
        SyncInstanceToServerService.requestServiceForAllPackage(context);

        //waiting for instance backup syncing (upstream)
        int qCount = SyncInstanceToServerService.getPackageQueueCount();
        while (qCount > 0) {
            qCount = SyncInstanceToServerService.getPackageQueueCount();
        }

        LocalDatabaseLog.sendAllLog(context);
        while (LocalDatabaseLog.getRequestCounter() > 0) {}

        String mainUser = UserListDbHelper.getInstance().getMainUsername();
        if (mainUser != null && !mainUser.equalsIgnoreCase("")) {
            //stop background services, set necessary flag, cleanup user data
            app.setAppToInactiveState();
            stopManagerService(context);
            Common.cleanUpPersonalData();
            CommonActivityUtils.logout(null);

            app.setDeactivatingLock(false);
            NotificationCompat.Style style = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(context.getString(R.string.rta_app_name))
                    .bigText(context.getString(R.string.user_unassigned_message, mainUser));
            MessageUtils.generateNotification(context, style, android.R.drawable.stat_sys_warning,
                    context.getString(R.string.rta_app_name),
                    context.getString(R.string.user_unassigned_message, mainUser),
                    R.string.user_unassigned_message);
            AnnouncementActivity
                    .open(context, context.getString(R.string.user_unassigned_message, mainUser));
        }
    }

    private void removeSecondaryUser(Context context, String mid, String username) {
        reportReceivedMessage(context, mid);
        UserListDbHelper.getInstance().removeUser(username);
        String currentUser = RTASurvey.getInstance().getCredential(RTASurvey.KEY_CREDENTIAL_USERNAME);
        if (!TextUtils.isEmpty(currentUser) && currentUser.equals(username)) {
            NotificationCompat.Style style = new NotificationCompat.BigTextStyle()
                    .setBigContentTitle(context.getString(R.string.rta_app_name))
                    .bigText(context.getString(R.string.user_unassigned_message, username));
            MessageUtils.generateNotification(context, style, android.R.drawable.stat_sys_warning,
                    context.getString(R.string.rta_app_name),
                    context.getString(R.string.user_unassigned_message, username),
                    R.string.user_unassigned_message);

            RTASurvey app = RTASurvey.getInstance();
            app.setCredentials(null, null);
            app.setLoginPass(null);
            stopManagerService(context);
            Log.e(TAG, "Log out from communication service");
            app.setMatrixCredentials(null, null);
            CommonActivityUtils.logout(null);
            UserSessionReportService.requestService(context, "logout-auto", currentUser);
            AnnouncementActivity
                    .open(context, context.getString(R.string.user_unassigned_message, username));
        }
    }

    private void recordAndUploadSoundTrace(final Context context, final String mid) {
        reportReceivedMessage(context, mid);
        String filename = String.format("aud_%s_%s.3gp",
                RTASurvey.getInstance().getDeviceId(),
                System.currentTimeMillis() + "");
        new SoundTraceRecorder(new SoundTraceRecorder.AudioTraceListener() {
            @Override
            public void onStartRecording(String filePath) {
                Log.d(TAG, "started recording into file: \n" + filePath);
            }

            @Override
            public void onStopRecording(String filePath) {
                Log.d(TAG, "stopped recording into file: \n" + filePath);
                Log.d(TAG, "uploading " + filePath);
                new UploadSoundTraceTask(new UploadTaskListener() {
                    @Override
                    public void uploadFailed(int statusCode, String message, String filePath) {
                        reportRCMCommandExeFailed(context, mid);
                    }

                    @Override
                    public void uploadCompleted(int statusCode, String message, String filePath) {
                        FAFileUtils.deleteFile(filePath);
                        reportRCMCommandExeSuccess(context, mid);
                    }
                }).upload(filePath);
            }

            @Override
            public void onStopWithError(String filePath, String errorMessage) {
                Log.e(TAG, "ERROR in recording: \n" + errorMessage
                        + "\nfilepath: " + filePath);

                FAFileUtils.deleteFile(filePath);
                new UploadSoundTraceTask().upload("error");
            }
        }).recordAudioTraceFile(context, RTASurvey.AUDIOTRACE_PATH, filename,
                RTASurvey.getInstance().getDurationRecord());

    }

    private void checkAndSaveLicense(Context context, String mid) {
        reportReceivedMessage(context, mid);
        String license = ConnectionService.getInstance().checkRTALicense(
                context, RTASurvey.getInstance().getServerUrl(),
                RTASurvey.getInstance().getDeviceId());
        if (StringUtil.isEmptyOrNull(license)) {
            //clean old license (if exist)
            File licenseFile = new File(RTASurvey.LICENSE_FILE_PATH);
            licenseFile.delete();
            return;
        }
        try {
            FileOutputStream fo = new FileOutputStream(RTASurvey.LICENSE_FILE_PATH);
            PrintWriter writer = new PrintWriter(fo);
            writer.write(license);
            writer.flush();
            writer.close();
            reportRCMCommandExeSuccess(context, mid);
        } catch (IOException e) {
            reportRCMCommandExeFailed(context, mid);
            e.printStackTrace();
        }
    }

    private AdaptFollowUpInstanceTask createFollowUpInstanceTask(final Context context,
                                                                 String url, long uuidCounter,
                                                                 final String mid, boolean isSup) {
        reportReceivedMessage(context, mid);
        final AdaptFollowUpInstanceTask task = new AdaptFollowUpInstanceTask(url, isSup, uuidCounter);
        task.setDownloadListener(new DownloadTaskListener() {
            @Override
            public void downloadFail() {
                reportRCMCommandExeFailed(context, mid);
                log.info("Follow-up form failed.");
                MessageUtils.generateNotification(context, R.string.followupInstance_retrieve_fail,
                        R.string.followupInstance_retrieve_fail);
            }

            @Override
            public void downloadCompleted() {
                reportRCMCommandExeSuccess(context, mid);
                log.info("Follow-up form completed.");
                MessageUtils.generateNotification(context, R.string.followupInstance_is_retrieved,
                        R.string.followupInstance_is_retrieved);
            }
        });
        return task;
    }

    private ForwardInstanceTask createForwardInstanceTask(final Context context, String url,
                                                          final String uuid, final int counter, final String mid) {
        final ForwardInstanceTask task = new ForwardInstanceTask(url);
        task.setDownloadListener(new DownloadTaskListener() {
            @Override
            public void downloadFail() {
                reportRCMCommandExeFailed(context, mid);
                log.error("Forwarding Instance data failed.");
                MessageUtils.generateNotification(context, R.string.forwardInstance_retrieve_fail,
                        R.string.forwardInstance_retrieve_fail);
            }

            @Override
            public void downloadCompleted() {
                reportRCMCommandExeSuccess(context, mid);

                if (counter >= 0) {
                    RTInstanceCounter.getInstance()
                            .updateCounter(uuid, counter + 1);
                }

                log.info("Forwarding Instance data completed.");
                MessageUtils.generateNotification(context, R.string.forwardInstance_is_retrieved,
                        R.string.forwardInstance_is_retrieved);
            }
        });
        return task;
    }

    private void downloadInstanceTemplate(final Context context, final String mid, final String url) {
        Log.d("DownloadListener", "Start download task with url= " + url);
        reportReceivedMessage(context, mid);
        final DownloadInstanceTemplate task = new DownloadInstanceTemplate();
        task.setDownloadListener(new DownloadTaskListener() {
            @Override
            public void downloadCompleted() {
                Log.d("DownloadListener", "download completed [url= " + url + "]");
                reportRCMCommandExeSuccess(context, mid);
            }

            @Override
            public void downloadFail() {
                reportRCMCommandExeFailed(context, mid);
                Log.d("DownloadListener", "download failed [url= " + url + "]");
            }
        });
        task.execute(url);
    }

    /**
     * Deletes the selected files.First from the database then from the file
     * system
     */
    private void deleteForms(final Context context, String... formIds) {
        DeleteFormsTask mDeleteFormsTask = new DeleteFormsTask();
        mDeleteFormsTask
                .setContentResolver(context.getContentResolver());
        mDeleteFormsTask.setDeleteListener(new DeleteFormsListener() {
            @Override
            public void deleteComplete(int deletedForms) {
                Log.i("DeleteListener", "Delete forms completed");
                broadcastUIUpdate(RTASurvey.getInstance().getApplicationContext(),UPDATE_MAIN_ITEMS);
            }
        });
        mDeleteFormsTask.execute(formIds);
    }

    /**
     * Deletes the selected files.First from the database then from the file
     * system
     */
    private void deleteInstances(Context context, List<Long> instanceIds) {
        DeleteInstancesTask mDeleteInstancesTask = new DeleteInstancesTask();
        mDeleteInstancesTask
                .setContentResolver(context.getContentResolver());
        mDeleteInstancesTask.setDeleteListener(new DeleteInstancesListener() {
            @Override
            public void deleteComplete(int deletedInstances) {
                Log.i("DeleteListener", "Delete instances completed");
            }
        });

        mDeleteInstancesTask.execute(instanceIds.toArray(new Long[instanceIds.size()]));
    }

    /**
     * Report list of current forms on tablet to server via WebService
     */
    private void reportCurrentForms(Context context) {
        List<FormInfo> list = new ArrayList<FormInfo>();

        // load form information from collector database
        Uri contentURI = FormsProviderAPI.FormsColumns.CONTENT_URI;
        Cursor c = context.getContentResolver().query(contentURI,
                new String[]{FormsProviderAPI.FormsColumns._ID,
                        FormsProviderAPI.FormsColumns.JR_FORM_ID,
                        FormsProviderAPI.FormsColumns.JR_VERSION,
                        FormsProviderAPI.FormsColumns.DISPLAY_NAME,
                        FormsProviderAPI.FormsColumns.DATE,
                        FormsProviderAPI.FormsColumns.AVAILABILITY_STATUS,
                        FormsProviderAPI.FormsColumns.LOCKED,
                        FormsProviderAPI.FormsColumns.FORM_MEDIA_PATH
                }, null, null, null);

        while (c.moveToNext()) {
            FormInfo fi = new FormInfo();
            // get important information from collector database
            fi.setFormID(c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID)));
            fi.setVersion(c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_VERSION)));
            fi.setTitle(c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.DISPLAY_NAME)));
            fi.setAvailability(c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.AVAILABILITY_STATUS)));
            fi.setDownloadDate(c.getLong(c.getColumnIndex(FormsProviderAPI.FormsColumns.DATE)));
            boolean isLocked = Boolean.parseBoolean(c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.LOCKED)));
            fi.setIsLocked(isLocked ? 1 : 0);

            String mediaPath = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.FORM_MEDIA_PATH));
            if (mediaPath != null && !mediaPath.equals("")) {
                fi.setMedia(getFileListInfo(mediaPath));
            }

            list.add(fi);
        }

        c.close();
        if (list != null || list.size() > 0) {
            CurrentFormsReport report = new CurrentFormsReport(list);
            report.setDeviceID(RTASurvey.getInstance().getDeviceId());
            reportCurrentFormByAsyncTask(context, report);
        }
    }

    private List<FormMediaInfo> getFileListInfo(String dirPath) {
        List<FormMediaInfo> media = new ArrayList<FormMediaInfo>();
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.toLowerCase().contains(".csv");
                }
            });
            for (File f : files) {
                FormMediaInfo mediaInfo = new FormMediaInfo();
                mediaInfo.setFilename(f.getName());
                mediaInfo.setLastModified(f.lastModified());
                String fHash = org.odk.collect.android.utilities.FileUtils.getMd5Hash(f);
                mediaInfo.setHash(fHash == null ? "" : fHash);
                media.add(mediaInfo);
            }
        }
        return media;
    }

    /**
     * Report list of current forms on tablet to server via WebService
     */
    private void reportCurrentInstances(Context context) {
        List<InstanceInfo> list = new ArrayList<InstanceInfo>();

        // load form information from ODK Collect's database
        Uri contentURI = InstanceProviderAPI.InstanceColumns.CONTENT_URI;
        Cursor c = context.getContentResolver().query(contentURI,
                null, null, null, null);

        while (c.moveToNext()) {
            InstanceInfo i = new InstanceInfo();
            // get data from Collector database
            i.setDate(c.getLong(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.LAST_STATUS_CHANGE_DATE)));
            i.setFormID(c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_FORM_ID)));
            i.setVersion(c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_VERSION)));
            i.setStatus(c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.STATUS)));

            // get uuid from RTA Survey instancePlusInfo.db file (if collector is RTA Survey)
            int _id = c.getInt(c.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));
            i.setUuid(SurveyCollectorUtils.getUuidByInstanceId(context, _id));
            list.add(i);
        }

        c.close();
        if (list != null || list.size() > 0) {
            InstancesReport report = new InstancesReport(list);

            reportCurrentInstancesByAsyncTask(context, report);
        }
    }

    /**
     * Use ConnectionTask class to report forms
     */
    private void reportCurrentFormByAsyncTask(Context context, final CurrentFormsReport report) {
        ConnectionTask task = new ConnectionTask(context) {
            @Override
            protected Object doInBackground(String... params) {
                return ConnectionService.getInstance().reportCurrentForm(
                        RTASurvey.getInstance().getServerUrl(),
                        RTASurvey.getInstance().getServerKey(), report);
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Use ConnectionTask class to report instances
     */
    private void reportCurrentInstancesByAsyncTask(Context context, final InstancesReport report) {
        ConnectionTask task = new ConnectionTask(context) {
            @Override
            protected Object doInBackground(String... params) {
                return ConnectionService.getInstance().reportCurrentInstances(
                        RTASurvey.getInstance().getServerUrl(),
                        RTASurvey.getInstance().getServerKey(),
                        RTASurvey.getInstance().getDeviceId(), report);
            }
        };
        task.execute();
    }

    private void reportCurrentFilesInMediaLib(Context context) {
        List<MediaLibFileInfo> files = new ArrayList<MediaLibFileInfo>();
        Cursor c = MediaCacheManager.getInstance().getAllFile();
        if (c != null) {
            while (c.moveToNext()) {
                String filePath = c.getString(c.getColumnIndex(MediaCacheManager.Table.File.FILEPATH));
                File file = new File(filePath);
                if (file.exists()) {
                    MediaLibFileInfo info;
                    info = new MediaLibFileInfo(
                            file.getName(),
                            file.lastModified(),
                            file.length());
                    files.add(info);
                }
            }
            c.close();
        }

        final CurrentFilesInMediaLibReport report = new CurrentFilesInMediaLibReport(files);

        ConnectionService.getInstance().reportCurrentFilesInMediaLib(
                RTASurvey.getInstance().getServerUrl(),
                RTASurvey.getInstance().getServerKey(),
                RTASurvey.getInstance().getDeviceId(), report);
    }

    private void reportRCMfrequency(Context context) {
        ConnectionService.getInstance().reportRCMfrequency(
                context, RTASurvey.getInstance().getServerUrl(),
                RTASurvey.getInstance().getDeviceId(),
                RTASurvey.getInstance().getRCMCheckPeriod());
    }

    /**
     * Upload ODK settings zip file to Server
     *
     * @param context
     * @param message
     */
    private void uploadOdkSettings(final Context context, final String mid, String message) {
        reportReceivedMessage(context, mid);
        String[] data = message.split(Constants.UNDERSCORE);
        new UploadODKsettingsTask() {
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    reportRCMCommandExeSuccess(context, mid);
                } else {
                    reportRCMCommandExeFailed(context, mid);
                }
            }

            ;
        }.upload(RTASurvey.getInstance().getDeviceId(),
                data[data.length - 1]);

    }

    /**
     * Download, unzip and push odk collect.settings to ODK dir
     *
     * @param context
     * @param message
     */
    private void downloadOdkSettings(final Context context, final String mid, String message) {
        reportReceivedMessage(context, mid);
        try {
            final String url = URLDecoder.decode(
                    message.replace(context.getString(R.string.download_odk_settings), ""),
                    Constants.UTF8);
            DownloadCollectSettingsTask task = new DownloadCollectSettingsTask();
            task.setDownloadListener(new DownloadTaskListener() {
                @Override
                public void downloadFail() {
                    reportRCMCommandExeFailed(context, mid);
                    MessageUtils.generateNotification(context, R.string.download_collectsettings_failed,
                            R.string.download_collectsettings_failed);
                }

                @Override
                public void downloadCompleted() {
                    MessageUtils.generateNotification(context, R.string.download_collectsettings_successed,
                            R.string.download_collectsettings_successed);
                    RTASurveySender.updateSettingFile(context);
                    reportRCMCommandExeSuccess(context, mid);
                }
            });
            task.execute(url);
        } catch (Exception e) {
            reportRCMCommandExeFailed(context, mid);
            e.printStackTrace();
            MessageUtils.generateNotification(context, R.string.download_collectsettings_failed,
                    R.string.download_collectsettings_failed);
        }
    }

    private void receiveSSInteraction(Context context, String mid, String message)
            throws JSONException {
        boolean isTsk = message.startsWith(context.getString(R.string.task));
        boolean isNtf = message.startsWith(context.getString(R.string.notification));
        if (!isTsk && !isNtf) return;

        String jsontxt = isTsk ? message.replaceFirst(context.getString(R.string.task), "") :
                message.replaceFirst(context.getString(R.string.notification), "");
        JSONObject json = new JSONObject(jsontxt);
        if (json.has("msg_id")) {
            final long ssi_id = json.getLong("msg_id");
            long snoozeTime = json.has("snooze_time") ? json.getLong("snooze_time") : 0l;
            new ConnectionTask(context) {
                int typeNumber;
                long snoozeTime = 0l;

                @Override
                protected Object doInBackground(String... params) {
                    String ssi_id = params[0];
                    try {
                        snoozeTime = Long.parseLong(params[1]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        snoozeTime = 0l;
                    }
                    String mid = params[2];

                    SSInteraction ssi = ConnectionService.getInstance()
                            .getSSInteraction(context,
                                    RTASurvey.getInstance().getServerUrl(),
                                    RTASurvey.getInstance().getServerKey(), ssi_id);
                    if (ssi == null) return false;
                    log.debug("Received a SS-Interaction from Server:\n"
                            + ssi.id + "-" + ssi.message);

                    //classify type of SS-Interaction
                    typeNumber = 0;
                    if (SSInteraction.TYPE_NTF.equals(ssi.type)) {
                        typeNumber = Interaction.NOTIFICATION;
                    } else if (SSInteraction.TYPE_TSK.equals(ssi.type)) {
                        typeNumber = Interaction.TASK_REMINDER;
                    }

                    //save the SS-interaction
                    DBService db = DBService.getInstance();
                    db.saveSSInteraction(ssi, typeNumber);

                    RCMMessageCache.getInstance().saveNtfMsgPair(mid, ssi.id);

                    return true;
                }

                @Override
                protected void onPostExecute(Object result) {
                    if (result != null) {
                        if ((Boolean) result) {
                            broadcastUIUpdate(context, NTF_COUNTER_UPDATE);
                            switch (typeNumber) {
                                case Interaction.NOTIFICATION:
                                    notifyNewNotification(context, snoozeTime);
                                    break;
                                case Interaction.TASK_REMINDER:
                                    notifyNewTask(context, snoozeTime);
                                    break;
                            }
                        }
                    }

                }
            }.execute(String.valueOf(ssi_id), String.valueOf(snoozeTime), mid);
        }
    }

    private void assignNewForm(Context context, String formId, String version,
                               String title, boolean preLockStt) {
        SurveyCollectorUtils.cleanUpFakeData(context, formId);
        SurveyCollectorUtils.cleanUpOldForms(context, formId, version);

        //validate follow family
        if (validateFormFamily(context, formId, version)) {
            log.error("ERROR:: This form is not available in its family anymore.");
            return;
        }

        ContentResolver cr = context.getContentResolver();
        Uri formUri = FormsProviderAPI.FormsColumns.CONTENT_URI;
        Uri fakeFormUri = FormsProviderAPI.FormsColumns.CONTENT_URI_FOR_FAKE;
        if (!SurveyCollectorUtils.formIsExist(context, formId, version)) {
            //check if there is a valid form has been locked
            boolean lockedValidForm = SurveyCollectorUtils.formIsExist(context, formId, version,
                    true, FormsProviderAPI.STATUS_AVAILABLE);
            if (lockedValidForm) {
                if (!preLockStt) {
                    //unlock the valid form
                    ContentValues values = new ContentValues();
                    values.put(FormsProviderAPI.FormsColumns.LOCKED, Boolean.toString(false));
                    cr.update(formUri, values,
                            FormsProviderAPI.FormsColumns.LOCKED + "=? and " +
                                    FormsProviderAPI.FormsColumns.AVAILABILITY_STATUS + "=? and " +
                                    FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? and " +
                                    FormsProviderAPI.FormsColumns.JR_VERSION + "=?",
                            new String[]{String.valueOf(true), FormsProviderAPI.STATUS_AVAILABLE,
                                    formId, version});
                }
            } else {
                //insert fake form record
                ContentValues values = new ContentValues();
                values.put(FormsProviderAPI.FormsColumns.DISPLAY_NAME, title);
                values.put(FormsProviderAPI.FormsColumns.JR_FORM_ID, formId);
                values.put(FormsProviderAPI.FormsColumns.JR_VERSION, version);
                values.put(FormsProviderAPI.FormsColumns.LOCKED, preLockStt);
                cr.insert(fakeFormUri, values);
            }

        } else {
            log.info(String.format("%s - %s was available", formId, version));
        }

        //apply default-lock-status to override form-family-availability
        try {
            SurveyCollectorUtils.lockForm(context, formId, version, preLockStt);
        } catch (Exception e) {
            e.printStackTrace();
        }

        FormLazyDownloadService.requestService(context);
        GetBackupListService.requestService(context, formId, version);
    }

    //remove an assigned form mean lock (the ability to fill new) that form.
    private void removeAssignedForm(Context context, String formId, String version) {
        InstanceSyncDbHelper.getInstance().deleteToDownloadItems(formId);

        Uri uri = FormsProviderAPI.FormsColumns.CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri, null, FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? and "
                        + FormsProviderAPI.FormsColumns.JR_VERSION + "=?",
                new String[]{formId, version}, null);
        String displayName = "";
        String family = "";
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                displayName = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.DISPLAY_NAME));
                family = c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.FAMILY));
            }
            c.close();
        }
        String key = formId + "-" + version + "-" + displayName;
        RTASurvey.getInstance().removeItemToPinList(key);
        broadcastUIUpdate(context, UPDATE_MAIN_ITEMS);

        SurveyCollectorUtils.cleanUpFakeData(context, formId);
        ContentValues values = new ContentValues();
        values.put(FormsProviderAPI.FormsColumns.LOCKED, Boolean.toString(true));
        context.getContentResolver().update(uri, values,
                FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? and "
                        + FormsProviderAPI.FormsColumns.JR_VERSION + "=?",
                new String[]{formId, version});

        //delete corresponding QA Offline Rule
        if ("rtsurvey".equals(BuildConfig.FLAVOR)) {
            String ruleId = (family == null || family.equals("")) ? formId : family;
            String ruleType = (family == null || family.equals("")) ? "form" : "family";
            try {
                deleteRule(ruleId, ruleType);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyAssignedForms(Context context, String serverUrl) {
        SurveyCollectorUtils.cleanUpFakeData(context);
        SurveyCollectorUtils.lockAllForm(context, true);
        String mainUser = UserListDbHelper.getInstance().getMainUsername();
        if (mainUser == null || mainUser.equals("")) {
            log.error("ERROR::Main username not found");
            return;
        }
        List<ConnectionService.AssignedForm> list = ConnectionService
                .getInstance().getAssignedForms(context, serverUrl, mainUser);
        if (list == null) return;
        int updateRows = 0, insertRows = 0;
        for (ConnectionService.AssignedForm aForm : list) {
            try {
                ContentResolver cr = context.getContentResolver();
                if (SurveyCollectorUtils.formIsExist(context, aForm.formId, aForm.version,
                        true, FormsProviderAPI.STATUS_AVAILABLE)) {
                    Uri formUri = FormsProviderAPI.FormsColumns.CONTENT_URI;
                    ContentValues values = new ContentValues();
                    values.put(FormsProviderAPI.FormsColumns.LOCKED, Boolean.toString(aForm.lockStt == 1));
                    values.put(FormsProviderAPI.FormsColumns.AVAILABILITY_STATUS,
                            FormsProviderAPI.STATUS_AVAILABLE);
                    updateRows += cr.update(formUri, values, FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? and "
                                    + FormsProviderAPI.FormsColumns.JR_VERSION + "=?",
                            new String[]{aForm.formId, aForm.version});
                } else if (!SurveyCollectorUtils.formIsExist(context, aForm.formId, aForm.version)) {
                    Uri formUri = FormsProviderAPI.FormsColumns.CONTENT_URI_FOR_FAKE;
                    ContentValues values = new ContentValues();
                    values.put(FormsProviderAPI.FormsColumns.DISPLAY_NAME, aForm.title);
                    values.put(FormsProviderAPI.FormsColumns.JR_FORM_ID, aForm.formId);
                    values.put(FormsProviderAPI.FormsColumns.JR_VERSION, aForm.version);
                    values.put(FormsProviderAPI.FormsColumns.LOCKED, Boolean.toString(aForm.lockStt == 1));
                    cr.insert(formUri, values);
                    insertRows++;
                } else if (SurveyCollectorUtils.formIsExist(context, aForm.formId, aForm.version)) {
                    Uri formUri = FormsProviderAPI.FormsColumns.CONTENT_URI;
                    ContentValues values = new ContentValues();
                    values.put(FormsProviderAPI.FormsColumns.LOCKED, Boolean.toString(aForm.lockStt == 1));
                    cr.update(formUri, values, FormsProviderAPI.FormsColumns.JR_FORM_ID + "=? and "
                                    + FormsProviderAPI.FormsColumns.JR_VERSION + "=?",
                            new String[]{aForm.formId, aForm.version});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info(String.format("%d assigned form(s), %d updated, %d (pre)insert",
                list.size(), updateRows, insertRows));

        for (ConnectionService.AssignedForm aForm : list) {
            boolean locked = SurveyCollectorUtils.formIsLocked(context, aForm.formId, aForm.version);
            FormLockReportService.requestService(context, aForm.formId, aForm.version,
                    FormLockReportService.TYPE_FORM, locked ? 1 : 0);
        }

        FormLazyDownloadService.requestService(context);

        if (list != null) {
            for (ConnectionService.AssignedForm f : list) {
                GetBackupListService.requestService(context, f.formId, f.version);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private synchronized void cleanUpPilot(Context context, int cleanType, final String extra) throws IOException {
        String cleanTypeString = "";
        switch (cleanType) {
            case CLEAN_FORMS:
                cleanTypeString = context.getString(R.string.clean_up_forms);
                Uri contentUri = FormsProviderAPI.FormsColumns.CONTENT_URI;
                Cursor c = context.getContentResolver().query(contentUri, null, null, null, null);
                StringBuilder ids = new StringBuilder();
                while (c.moveToNext()) {
                    ids.append(c.getString(c.getColumnIndex(FormsProviderAPI.FormsColumns.JR_FORM_ID))
                            + Constants.COMMA);
                }
                String[] formIds = ids.toString().split(Constants.COMMA);
                deleteForms(context, formIds);
                deleteInstances(context, SurveyCollectorUtils.getInstanceIdList(context, formIds));
                break;
            case CLEAN_LOG:
                log.info("No apply old log file mechanism anymore.");
                break;
            case CLEAN_LIB:
                cleanTypeString = context.getString(R.string.clean_up_medialib) + " - " + extra;
                File lib = new File(RTASurvey.MEDIA_PATH);
                if (lib.exists() && (StringUtil.isEmptyOrNull(extra) || extra.equalsIgnoreCase("ALL"))) {
                    MediaCacheManager.getInstance().cleanAll();
                    FileUtils.deleteDirectory(lib);
                    lib.mkdirs();
                } else {
                    MediaCacheManager.getInstance().cleanFile(extra);
                }
                break;
        }

        MessageUtils.generateNotification(context, R.string.clean_up_pilot, R.string.clean_up_pilot);
    }

    private void downloadPreloadMediaFile(final Context context, final String mid, String json, boolean isFamily) {
        reportReceivedMessage(context, mid);
        DownloadMediaFilesService.requestService(context, DownloadMediaFilesService.ACTION_BROADCAST_SERVICE_DOWNLOAD_MEDIA,
                json, isFamily, mid);
    }

    private void downloadQARule(final Context context, String ruleId, String type, final String mid) {
        reportReceivedMessage(context, mid);
        DownloadQARuleTask task = new DownloadQARuleTask(context);
        task.setDownloadListener(new DownloadTaskListener() {
            @Override
            public void downloadCompleted() {
                reportRCMCommandExeSuccess(context, mid);
            }

            @Override
            public void downloadFail() {
                reportRCMCommandExeFailed(context, mid);
            }
        });
        task.download(ruleId, type);
    }

    class ForwardInstanceInfo implements Serializable {
        String formID;
        String version;
        String formUrl;
        String instanceUrl;
        String uuid;
        int reportID;
        int counter;

    }

}
