/**
 *
 */
package vn.rta.cpms.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.rta.ipcall.LinphoneService;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.provider.InstanceProviderAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import im.vector.activity.CommonActivityUtils;
import vn.rta.cpms.activities.NoteActivity;
import vn.rta.cpms.providers.ReportProviderAPI;
import vn.rta.cpms.services.ConnectionService;
import vn.rta.cpms.services.DBService;
import vn.rta.cpms.services.FailedReportManager;
import vn.rta.cpms.services.ManagerService;
import vn.rta.cpms.services.QATaskDbHelper;
import vn.rta.cpms.services.RCMCmdCache;
import vn.rta.cpms.services.StaffInfoDbHelper;
import vn.rta.cpms.services.SurveyConnectionService;
import vn.rta.cpms.services.SwitchAppService;
import vn.rta.cpms.services.UserHistoryDbHelper;
import vn.rta.cpms.services.UserListDbHelper;
import vn.rta.cpms.services.UserSessionReportService;
import vn.rta.cpms.services.model.Response;
import vn.rta.cpms.services.model.UserCredential;
import vn.rta.cpms.services.model.UserInfo;
import vn.rta.cpms.services.model.VersionInfo;
import vn.rta.cpms.services.sync.doc.DocumentSyncDBHelper;
import vn.rta.cpms.services.sync.instance.InstanceSyncDbHelper;
import vn.rta.cpms.services.sync.localdb.LogFile;
import vn.rta.cpms.services.sync.note.UserNoteSyncDbHelper;
import vn.rta.cpms.tasks.ConnectionTask;
import vn.rta.piwik.Contacts;
import vn.rta.piwik.PiwikTrackerManager;
import vn.rta.rtcenter.data.ModuleDbHelper;
import vn.rta.survey.android.BuildConfig;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.helper.WorkingDataHelper;
import vn.rta.survey.android.provider.NtfActionHelper;
import vn.rta.survey.android.provider.SamplingDbHelper;

/**
 * @author VietDung <dungvu@rta.vn>
 */
public class Common {
    private static final String TAG = "Common";
    private static final String VIETNAMESE_DIACRITIC_CHARACTERS
            = "ẮẰẲẴẶĂẤẦẨẪẬÂÁÀÃẢẠĐẾỀỂỄỆÊÉÈẺẼẸÍÌỈĨỊỐỒỔỖỘÔỚỜỞỠỢƠÓÒÕỎỌỨỪỬỮỰƯÚÙỦŨỤÝỲỶỸỴ";
    private static final String TOREPLACE_CHARACTERS
            = "AAAAAAAAAAAAAAAAADEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYY";

    /**
     * Determine the network connection
     *
     * @param context the Context
     * @return TRUE if network connect is establishing
     */
    public static boolean isConnect(Context context) {
        // Checking network configuration
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static String replaceVietnameseChars(String str) {
        StringBuilder builder = new StringBuilder(str);
        /*for (int i=0; i<VIETNAMESE_DIACRITIC_CHARACTERS.length(); i++) {
            str = str.replaceAll(VIETNAMESE_DIACRITIC_CHARACTERS.charAt(i)+"",
                    TOREPLACE_CHARACTERS.charAt(i)+"");
        }*/
        for (int i = 0; i < builder.length(); i++) {
            String c = builder.charAt(i) + "";
            for (int j = 0; j < VIETNAMESE_DIACRITIC_CHARACTERS.length(); j++) {
                if (c.equalsIgnoreCase(VIETNAMESE_DIACRITIC_CHARACTERS.charAt(j) + "")) {
                    builder.setCharAt(i, TOREPLACE_CHARACTERS.charAt(j));
                    break;
                }
            }
        }
        return builder.toString();
    }

    /**
     * Check GPS provider
     *
     * @param context
     * @return true if enable or false if not
     */
    public static boolean isGPSProviderEnabled(Context context) {
        LocationManager locManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        boolean value = locManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!value) {
            value = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }

        return value;
    }

    /**
     * Check Blue tooth status
     *
     * @return
     */
    public static boolean isBluetoothOn() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        // Check device supporting blue tooth or not
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.isEnabled();
        }

        return false;
    }

    /**
     * using WSG84 using the Metric system
     *
     * @return meters distance
     */
    public static float getDistance(double startLati, double startLongi,
                                    double goalLati, double goalLongi) {
        float[] resultArray = new float[5]; // meters
        Location.distanceBetween(startLati, startLongi, goalLati, goalLongi,
                resultArray);
        return resultArray[0];
    }

    /**
     * Get file content by filename from asset/
     *
     * @param c
     * @param filename
     * @return content String
     */
    public static String getFileContentFromAsset(Context c, String filename) {
        try {
            InputStream fin = c.getAssets().open(filename);
            byte[] buffer = new byte[fin.available()];
            fin.read(buffer);
            fin.close();
            return new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("inspector", e.getLocalizedMessage());
        }
        return "";
    }

    /**
     * Get file content by file path
     *
     * @param c
     * @param filePath
     * @return content String
     */
    public static String getFileContent(Context c, String filePath) {
        try {
            InputStream fin = new FileInputStream(filePath);
            byte[] buffer = new byte[fin.available()];
            fin.read(buffer);
            fin.close();
            return new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("inspector", e.getLocalizedMessage());
        }
        return "";
    }

    /**
     * Hide keyboard
     *
     * @param act  Activity
     * @param view
     */
    public static void hideKeyboard(Activity act, View view) {
        if (act != null && view != null) {
            InputMethodManager imm = (InputMethodManager) act
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static int getFormCount(Context context) {
        Cursor c = context.getContentResolver()
                .query(FormsProviderAPI.FormsColumns.CONTENT_URI,
                        new String[]{FormsColumns._ID},
                        null, null, null);
        if (c != null) {
            int count = c.getCount();
            c.close();
            return count;
        }
        return 0;
    }

    public static String getAppVersionName(Context context, String packageName) {
        String appversion = "N/A";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            appversion = (BuildConfig.DEBUG ? "debug-" : "") + pInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appversion;
    }

    public static String getAppVersionName() {
        return (BuildConfig.DEBUG ? "debug-" : "") + BuildConfig.VERSION_NAME;
    }

    public static int getAppVersionCode(Context context, String packageName) {
        int appversion = -1;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            appversion = pInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appversion;
    }

    public static int getAppVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public static void adjustBrightness(ContentResolver contentResolver, int brightness) {
        // To handle the auto
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        //Set the system brightness using the brightness variable value
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    /**
     * @return text which contain user information, follow the format resource string (formatId)
     */
    public static String getStaffInfo(Context context, int formatId) {
        UserInfo u = getUserInfo(context);
        return context.getString(formatId, u == null ?
                "n/a" : u.getName(), u == null ? "n/a" : u.getStaffCode());
    }

    /**
     * @return current logged in user information in object
     */
    public static UserInfo getUserInfo(Context context) {
        return getUserInfo(context, null);
    }

    /**
     * @return user information in object
     */
    public static UserInfo getUserInfo(Context context, String username) {
        // get the current logged in username if username arg is not determined
        if (username == null || username.equals("")) {
            username = RTASurvey.getInstance().getCredential(RTASurvey.KEY_CREDENTIAL_USERNAME);
        }

        File uFile = new File(context.getFilesDir() + File.separator + "user_info", username);
        ObjectInputStream input = null;
        UserInfo u = null;
        try {
            input = new ObjectInputStream(new FileInputStream(uFile));
            u = (UserInfo) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (u != null && u.getUsername() == null)
            u.setUsername(username);
        return u;
    }

    public static void updateStaffInfo(Context context, String host, String key, String username,
                                      Logger log) {
        // correct service URL
        String servicePath = null;
        if (context.getString(R.string.rta_platform_name).equals(SurveyConnectionService.SURVEY_PLATFORM)) {
            servicePath = SurveyConnectionService.SERVICE_GET_STAFF_INFO_BY_USER + username;
        } else {
            servicePath = ConnectionService.SERVICE_GET_STAFF_INFO_BY_USER + username;
        }
        String url = host + servicePath;
        String json = ConnectionService.getInstance().doGet(context, url, key);

        // write object to file
        if (!TextUtils.isEmpty(json)) {
            UserInfo uInfo = (UserInfo) StringUtil.json2Object(json, UserInfo.class);
            if (uInfo != null) {
                uInfo.setUsername(username);
                if (updateUserInfoFile(context, uInfo)) {

                    // BACKWARD COMPATIBLE -- BEGIN
                    // still support the external resource for old usage
                    // will be removed soon
                    StaffInfoDbHelper.getInstance()
                            .updateStaffInfo(uInfo.getStaffCode(), uInfo.getName(), username,
                                    RTASurvey.getInstance().getDeviceId(), uInfo.getOrganization_id(),
                                    uInfo.getTeam_id(), uInfo.getSupervisor_id(), uInfo.getUser_role());
                    // BACKWARD COMPATIBLE -- END

                    log.error("Update user info - success");
                } else {
                    log.error("Update staff info - failed");
                }
            }
        } else {
            log.error("Update staff info - failed");
        }
    }

    public static boolean updateUserInfoFile(Context context, UserInfo uInfo) {
        boolean result = false;
        if (uInfo != null) {
            // create directory and file
            File uDir = new File(context.getFilesDir(), "user_info");
            uDir.mkdirs();
            File uFile = new File(uDir, uInfo.getUsername());

            // write user object into file
            ObjectOutputStream output = null;
            try {
                output = new ObjectOutputStream(new FileOutputStream(uFile));
                output.writeObject(uInfo);
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }

    public static void switchUserCredential(final Context context, final String username) {
        Cursor c = UserListDbHelper.getInstance().getUser(username);
        if (c == null || !c.moveToFirst())
            return;

        //set current credentials
        RTASurvey pre = RTASurvey.getInstance();
        pre.setCredentials(username, c.getString(c.getColumnIndex(UserListDbHelper.User.PASSWORD)));
        pre.setMatrixCredentials(
                c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_USERNAME)),
                c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_PASSWORD)));
        pre.setLoginPass(c.getString(c.getColumnIndex(UserListDbHelper.User.PIN)));
        c.close();

        updateCurrentStaffToExternalResource(context, username);
    }

    public static boolean switchUserCredential(Context context, String username, String iPIN) {
        if (UserListDbHelper.getInstance().userLoginIsValid(username, iPIN)) {
            Cursor c = UserListDbHelper.getInstance().getUser(username);
            if (c == null) return false;
            c.moveToNext();

            //set current credentials
            RTASurvey pre = RTASurvey.getInstance();
            pre.setCredentials(username, c.getString(c.getColumnIndex(UserListDbHelper.User.PASSWORD)));
            pre.setMatrixCredentials(
                    c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_USERNAME)),
                    c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_PASSWORD)));
            pre.setLoginPass(iPIN);
            c.close();

            updateCurrentStaffToExternalResource(context, username);

            //...

            return true;
        }
        return false;
    }

    public static void setToUser(Context context, String username) {
        Cursor c = UserListDbHelper.getInstance().getUser(username);
        if (c != null) {
            c.moveToFirst();

            //set current credentials
            RTASurvey pre = RTASurvey.getInstance();
            pre.setCredentials(username, "");
            pre.setMatrixCredentials(
                    c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_USERNAME)),
                    c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_PASSWORD)));
            c.close();

            updateCurrentStaffToExternalResource(context, username);
        }
    }

    //update current staff info for external resources
    public static void updateCurrentStaffToExternalResource(Context context, String username) {
        UserInfo u = getUserInfo(context, username);
        if (u != null) {
            StaffInfoDbHelper.getInstance().updateStaffInfo(u.getStaffCode(), u.getName(),
                    username, RTASurvey.getInstance().getDeviceId(), u.getOrganization_id(),
                    u.getTeam_id(), u.getSupervisor_id(), u.getUser_role());
        }
    }

    public static void safeExit(Context context) {
        Common.safeExit(context, null);
    }

    public static void safeExit(Context context, Activity activity) {
        //set current working user to empty
        RTASurvey app = RTASurvey.getInstance();
        String currentUser = app.getCredential(RTASurvey.KEY_CREDENTIAL_USERNAME);
        app.setCredentials(null, null);
        app.setLoginPass(null);

        //stop main service
        context.stopService(new Intent(context, ManagerService.class));
        context.stopService(new Intent(Intent.ACTION_MAIN).setClass(context, LinphoneService.class));

        int userCount = UserListDbHelper.getInstance().getUserCount();
        if (activity != null) {
            if (userCount > 1 || userCount == 0) {
                Log.e(TAG, "Log out from communication service");
                app.setMatrixCredentials(null, null);
                CommonActivityUtils.logout(null);
            }
            UserSessionReportService.requestService(context, "logout", currentUser);
            activity.finish();
        } else {
            Log.e(TAG, "Log out from communication service");
            app.setMatrixCredentials(null, null);
            CommonActivityUtils.logout(null);
            UserSessionReportService.requestService(context, "logout-auto", currentUser);
            System.exit(0);
        }
    }

    public static void safeExitWithConfirmDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.rta_app_name);
        builder.setMessage(R.string.staff_exit_dialog_confirm_msg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.staff_exit,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PiwikTrackerManager.newInstance().trackClickAction(Contacts.EXIT, 12, Contacts.EXIT, Contacts.ACTION_EXIT);
                        Common.safeExit(RTASurvey.getInstance().getApplicationContext(), activity);
                    }
                });
        builder.setNegativeButton(R.string.btn_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static UserCredential getMainUser() {
        Cursor c = UserListDbHelper.getInstance().getMainUser();
        if (c == null) return null;
        UserCredential user = new UserCredential();

        if (c.getCount() >= 1) {
            c.moveToFirst();
            String username = c.getString(c.getColumnIndex(UserListDbHelper.User.USERNAME)),
                    password = c.getString(c.getColumnIndex(UserListDbHelper.User.PASSWORD)),
                    matrixUser = c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_USERNAME)),
                    matrixPass = c.getString(c.getColumnIndex(UserListDbHelper.User.MATRIX_PASSWORD)),
                    pin = c.getString(c.getColumnIndex(UserListDbHelper.User.PIN));

            user.setUsername(username);
            user.setPassword(password);
            user.setMatrix_username(matrixUser);
            user.setMatrix_password(matrixPass);
            user.setDefaultPIN(pin);

            if (!TextUtils.isEmpty(user.getUsername()) && !TextUtils.isEmpty(user.getPassword())) {
                c.close();
                return user;
            }
        }
        c.close();
        return null;
    }

    public static void cleanUpPersonalData() {
        RTASurvey app = RTASurvey.getInstance();

        UserListDbHelper.getInstance().clearUserList();

        //clean up all user's notes data
        try {
            File noteDir = new File(RTASurvey.NOTES_PATH);
            File noteDir2 = new File(NoteActivity.FOR_UPLOAD_DIR_PATH);
            if (noteDir.exists())
                FileUtils.cleanDirectory(noteDir);
            if (noteDir2.exists())
                FileUtils.cleanDirectory(noteDir2);
            UserNoteSyncDbHelper.getInstance().cleanUpDbContent();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //clean up all user's R-Reports
        app.getContentResolver().delete(ReportProviderAPI.RReport.CONTENT_URI, null, null);
        app.getContentResolver().delete(ReportProviderAPI.OnlineReport.CONTENT_URI, null, null);
        app.getContentResolver().delete(ReportProviderAPI.ToDownload.CONTENT_URI, null, null);
        File reportDir = new File(RTASurvey.REPORT_DIRECTORY_DIRPATH);
        if (reportDir.exists()) {
            for (File rdir : reportDir.listFiles()) {
                if (rdir.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(rdir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //clean up user's SSInteraction (Notifications/Tasks)
        DBService.getInstance().cleanAllInteractionData();

        //clean up new RCM command cache
        RCMCmdCache.getInstance().cleanCmdCache();

        //clean up temporary directories
        try {
            File tempDir = new File(RTASurvey.TEMP_ROOT_PATH);
            if (tempDir.exists() && tempDir.isDirectory()) {
                FileUtils.deleteDirectory(tempDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //reset necessary flag
        app.setGCMRegisteredFlag(false);
        app.setUpOnOffSwitchingApp(false);
        app.getApplicationContext().stopService(new Intent(app.getApplicationContext(), SwitchAppService.class));

        //clean up user registration history (Profile screen/History tab)
        UserHistoryDbHelper.getInstance().cleanHistory();

        //clean up module data
        ModuleDbHelper.getInstance().cleanAllData(app.getApplicationContext());

        //Clean up add item to home
        app.cleanUpPinItemOfHome();
        app.updateVisibleItemHome(new HashSet<String>());
        List<String> menuItems = Arrays
                .asList(app.getApplicationContext().getResources().getStringArray(R.array.user_main_menu));
        for (int i = 0; i < menuItems.size(); i++) {
            app.updateMenuItemPosition(menuItems.get(i), i);
        }

        //clean up sampling area data
        SamplingDbHelper.getInstance().cleanUpAreas(RTASurvey.getInstance().getApplicationContext());
        app.setSamplingForm(RTASurvey.KEY_SAMPLING_FORM_AREA_ID, null, null, 0, null, null);
        app.setSamplingForm(RTASurvey.KEY_SAMPLING_FORM_LISTING_ID, null, null, 0, null, null);
        File kmlDir = new File(RTASurvey.KML_DIR_PATH);
        if (kmlDir.exists() && kmlDir.isDirectory()) {
            try {
                FileUtils.cleanDirectory(kmlDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //clean up QA Offline data
        File ruleDir = new File(RTASurvey.R_SCRIPT_DIRPATH);
        if (ruleDir.exists() && ruleDir.isDirectory()) {
            try {
                FileUtils.cleanDirectory(ruleDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        QATaskDbHelper.getInstance().cleanUp(app.getApplicationContext());

        //clean ntf-Action
        NtfActionHelper.getInstance().cleanUpActions(app.getApplicationContext());

        //clean Document
        DocumentSyncDBHelper.getInstance().removeAllDocumentToDownload();
        File documentDir = new File(RTASurvey.DOCUMENTS_PATH);
        if (documentDir.exists()) {
            try {
                FileUtils.cleanDirectory(documentDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //clean instance data
        SurveyCollectorUtils.cleanLatestInstanceConfig();
        app.getContentResolver().delete(FormsProviderAPI.FormsColumns.CONTENT_URI, null, null);
        app.getContentResolver().delete(InstanceProviderAPI.InstanceColumns.CONTENT_URI, null, null);
        InstanceSyncDbHelper.getInstance().cleanUpDbContent();
        File logDir = new File(LogFile.ROOT_DIRECTORY_PATH);
        try {
            if (logDir.exists())
                FileUtils.deleteDirectory(logDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        WorkingDataHelper.getInstance().deleteAllData();
        //clean instance data (local database files)
        File localDbResDir = new File(RTASurvey.LOCALDATA_PATH);
        if (localDbResDir.exists() && localDbResDir.isDirectory()) {
            try {
                FileUtils.cleanDirectory(localDbResDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //clean cache for resource uploading
        File cacheDir = new File(RTASurvey.CACHE_RESOURCE_PATH);
        if (cacheDir.exists()) {
            try {
                FileUtils.cleanDirectory(cacheDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getMd5Hash(String plainText) {
        try {
            MessageDigest md = null;

            md = MessageDigest.getInstance("MD5");

            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static float dpToPx(Context context, float dp) {
        Resources r = context.getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }

    public static void checkAppUpdate(Context context) {
        if (!Common.isConnect(context)) {
            Log.e(TAG, "ERROR::No network connection, ignore new app checking.");
            return;
        }
        new ConnectionTask(context) {
            @Override
            protected void onPreExecute() {
            }

            @Override
            protected Object doInBackground(String... params) {
                try {
                    String newVersion = Jsoup
                            .connect(
                                    "https://play.google.com/store/apps/details?id="
                                            + BuildConfig.APPLICATION_ID + "&hl=en")
                            .timeout(30000)
                            .userAgent(
                                    "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com").get()
                            .select("div[itemprop=softwareVersion]").first()
                            .ownText();
                    return newVersion;
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                try {
                    if (result == null)
                        return;
                    String version = (String) result;
                    if (version != null && !version.equals("")) {
                        String[] lastNumber = version.split("\\.");
                        int mVersion = Integer.parseInt(lastNumber[2]);
                        int mCurrentVersion = BuildConfig.VERSION_CODE;
                        if (mVersion > mCurrentVersion) {
                            Log.e(TAG, "Please Update!");
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(R.string.app_update);
                            builder.setMessage(R.string.app_update_title);
                            builder.setCancelable(false);
                            builder.setPositiveButton(R.string.app_update,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            startUpdateAppActivity();
                                        }
                                    });
                            builder.setNegativeButton(R.string.app_cancel,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    } else {
                        Log.e(TAG, "ERROR:: PlayStore - Application not found - " +
                                BuildConfig.APPLICATION_ID);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            private void startUpdateAppActivity() {
                try {
                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
                    playStoreIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    context.startActivity(playStoreIntent);
                } catch (android.content.ActivityNotFoundException anfe) {
                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id="
                                    + BuildConfig.APPLICATION_ID));
                    playStoreIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    context.startActivity(playStoreIntent);
                }
            }
        }.execute();
    }

    public static String getStaffInfoRthome(Context context, int formatId) {
        UserInfo u = getUserInfo(context);
        return context.getString(formatId,
                RTASurvey.getInstance().getCredential(RTASurvey.KEY_CREDENTIAL_USERNAME),
                u == null ? "n/a" : u.getStaffCode());
    }

    public static boolean userIsSupervisor(Context context, String username) {
        UserInfo u = getUserInfo(context, username);
        return u != null && "1".equals(u.getIs_supervisor());
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean checkPlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.showErrorNotification(context, resultCode);
            } else {
                Log.e("GooglePlayService", "This device is not supported.");
            }
            return false;
        }
        return true;
    }

    /**
     * Gather app version information and update to server,
     * report content is backup in failed_report.db, in case of no network
     * @param context App Context
     */
    public static void reportCurrentVersionName(Context context) {
        String host = RTASurvey.getInstance().getServerUrl();
        if (host == null || host.equals("")) {
            Log.e(TAG, "ERROR::Application is inactive (User didn't register yet)");
            return;
        }

        // app platform == app flavor name
        String platform = context.getString(R.string.rta_platform_name);

        // app version
        String appVersionName = Common.getAppVersionName();

        // current profile (not exactly yet)
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bundle restrictions = um.getUserRestrictions();
        String userRestriction = "Owner";
        if (restrictions.containsKey(UserManager.DISALLOW_MODIFY_ACCOUNTS)
                && restrictions.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
            userRestriction = "Restricted";
        }

        // network activity
        String url = host + "/cpms/cpmsServices/refreshVersion";
        VersionInfo versionInfo = new VersionInfo(RTASurvey.getInstance().getDeviceId(),
                "NA", appVersionName, "", userRestriction, platform);
        String data = StringUtil.object2JSON(versionInfo);
        long reportId = FailedReportManager.getInstance().saveReport(host, url, data, false);

        if (Common.isConnect(context)) {
            String result = ConnectionService.getInstance().doPost(url, null, data); //no encrypt
            Response res = (Response) StringUtil.json2Object(result, Response.class);
            if (res != null && ConnectionService.SUCCESS.equals(res.getStatus())) {
                FailedReportManager.getInstance().deleteReport(reportId);
            }
        }
    }

}
