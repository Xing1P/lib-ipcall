package vn.rta.cpms.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.multidex.MultiDex;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphoneService;
import com.rta.ipcall.ui.UpdateUIListener;

import org.apache.log4j.Logger;
import org.linphone.core.LinphoneAddress;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.listeners.TaskListener;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import vn.rta.cpms.application.Params;
import vn.rta.cpms.communication.datamodel.media.UriImageRequestDescriptor;
import vn.rta.cpms.communication.ui.AsyncImageView;
import vn.rta.cpms.communication.util.ContentType;
import vn.rta.cpms.fragments.ProcessFragment;
import vn.rta.cpms.fragments.StaffInfoFragment;
import vn.rta.cpms.preference.AppSettingSharedPreferences;
import vn.rta.cpms.services.ConnectionService;
import vn.rta.cpms.services.DBService;
import vn.rta.cpms.services.ManagerService;
import vn.rta.cpms.services.ProcessDbHelper;
import vn.rta.cpms.services.SwitchAppService;
import vn.rta.cpms.services.UserListDbHelper;
import vn.rta.cpms.services.model.Environment;
import vn.rta.cpms.services.model.Form;
import vn.rta.cpms.services.model.IpCallAccount;
import vn.rta.cpms.services.model.SSConfig;
import vn.rta.cpms.services.model.Schedule;
import vn.rta.cpms.services.model.ValidLocation;
import vn.rta.cpms.tasks.ConnectionTask;
import vn.rta.cpms.ui.MainViewInterface;
import vn.rta.cpms.ui.TabbedFragment;
import vn.rta.cpms.ui.calendar.agenda.AgendaFragment;
import vn.rta.cpms.ui.rtmessaging.ConversationListFragment;
import vn.rta.cpms.ui.search.SearchActivity;
import vn.rta.cpms.ui.subscribedmodulelist.ModuleListFragment;
import vn.rta.cpms.utils.Common;
import vn.rta.cpms.utils.MessageUtils;
import vn.rta.cpms.utils.NetworkMediaFileManager;
import vn.rta.cpms.utils.StringUtil;
import vn.rta.cpms.utils.SurveyCollectorUtils;
import vn.rta.ipcall.ui.IncomingIPCallActivity;
import vn.rta.piwik.Contacts;
import vn.rta.piwik.PiwikTrackerManager;
import vn.rta.rtmessaging.RtMessaging;
import vn.rta.rtmessaging.data.RtMessagingConfig;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.IPremiumLicense;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.manager.NotificationCreator;
import vn.rta.survey.android.preferences._AdminPreferencesActivity;
import vn.rta.survey.android.services.SaveSettingService;
import vn.rta.survey.android.tasks.LicenceChecker;
import vn.rta.survey.android.utilities.Utils;


/**
 * @author DungVu (dungvu@rta.vn) & Vu Hoang (vuhoang@rta.vn)
 */
@RuntimePermissions
public class UserScreen extends RtDimmableActivity
        implements TaskListener, MainViewInterface {
    private final Logger log = Logger.getLogger(UserScreen.class);
    private static final String TAG = UserScreen.class.getSimpleName();

    // Alarm action for midnight (so we can update the date display).
    private static final String KEY_SELECTED_TAB = "selected_tab";
    public static final String SELECT_TAB_INTENT_EXTRA = "userscreen.select.tab";

    private static final boolean EXIT = true;
    public static final String FRAGMENT_ID = "fragment_id";
    public static final int FRAGMENT_NOTIFICATION = 1;
    public static final String IS_RELOAD_SCHEDULE = "reload_schedule";
    public static final String[] NECESSARY_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static final int TAB_HOME = 0;
    public static final int TAB_COMMUNICATION = 1;
    public static final int TAB_NOTIFICATION = 2;
    public static final int TAB_NOTE = 3;
    public static final int TAB_STAFF_INFO = 4;
    public static final int TAB_COUNT = TAB_STAFF_INFO + 1;

    private View mRootContainer;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private SearchAdapter mSearchAdapter;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private int mSelectedTab;

    private TextView mHeaderBrandText;
    private TextView mHeaderStaffText;
    private AsyncImageView mHeaderImage;
    private FloatingActionButton mFab;

    private AppBarLayout mTabAppBarLayout;
    private CollapsingToolbarLayout mTabCollapsingToolbarLayout;
    private Toolbar mTabToolbar;
    private ViewGroup mTabExpandingContainer;
//    private View mSecondTitleContainerView;
//    private TextView mTabTitleTextView;
//    private ImageView mTabTitleIcon;
//    private ActionMenuView mTabActionMenu;
    private boolean mSecondAppBarExpanded = false;

    private CompactCalendarView mCalendarView;

    private AlertDialog mAlertDialog;
    private Dialog continueEditDialog;
    private LicenceChecker mLicenseChecker = null;
    private SharedPreferences mAdminPreferences;
    private boolean isReloadSchedule = false;
    private ConnectionTask taskUpdateSchedule;

    /**
     * We're tracking the conversation list for handling
     * rt-messaging module login service behaviors.
     */
    private ConversationListFragment mConversationListFragment;

    Fragment getSelectedFragment() {
        return mTabsAdapter.getItem(mSelectedTab);
    }

    private void createTabs() {
        final TabLayout.Tab homeTab = mTabLayout.newTab();
        homeTab.setIcon(R.drawable.ic_tab_home);
        mTabsAdapter.addTab(homeTab, ModuleListFragment.class, TAB_HOME);

        final TabLayout.Tab communicationTab = mTabLayout.newTab();
        communicationTab.setIcon(R.drawable.ic_tab_communication);
        mTabsAdapter.addTab(communicationTab, ConversationListFragment.class, TAB_COMMUNICATION);

        final TabLayout.Tab notificationTab = mTabLayout.newTab();
        notificationTab.setIcon(R.drawable.ic_tab_notifications);
//        mTabsAdapter.addTab(notificationTab, NotificationListFragment.class, TAB_NOTIFICATION);
        mTabsAdapter.addTab(notificationTab, AgendaFragment.class, TAB_NOTIFICATION);

        final TabLayout.Tab noteTab = mTabLayout.newTab();
        noteTab.setIcon(R.drawable.ic_tab_document);
        mTabsAdapter.addTab(noteTab, vn.rta.cpms.fragments.NoteListFragment.class, TAB_NOTE);

        final TabLayout.Tab staffTab = mTabLayout.newTab();
        staffTab.setIcon(R.drawable.ic_tab_profile);
        mTabsAdapter.addTab(staffTab, StaffInfoFragment.class, TAB_STAFF_INFO);

        //TODO finds the way to fix tab icon not show
        mTabLayout.getTabAt(TAB_HOME).setIcon(R.drawable.ic_tab_home);
        mTabLayout.getTabAt(TAB_COMMUNICATION).setIcon(R.drawable.ic_tab_communication);
        mTabLayout.getTabAt(TAB_NOTIFICATION).setIcon(R.drawable.ic_tab_notifications);
        mTabLayout.getTabAt(TAB_NOTE).setIcon(R.drawable.ic_tab_document);
        mTabLayout.getTabAt(TAB_STAFF_INFO).setIcon(R.drawable.ic_tab_profile);

        mTabLayout.getTabAt(mSelectedTab).select();
        mViewPager.setCurrentItem(mSelectedTab);
        mTabsAdapter.notifySelectedPage(mSelectedTab);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.cpms_user_screen;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MultiDex.install(this);
        super.onCreate(savedInstanceState);
        PiwikTrackerManager.newInstance().trackOpenScreenWithNewSession(UserScreen.class.getSimpleName(), 3, Contacts.MAIN_SCREEN, null);

        if (savedInstanceState != null) {
            mSelectedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, TAB_HOME);
        } else {
            mSelectedTab = TAB_HOME;
        }

        // Honor the tab requested by the intent, if any.
        final Intent intent = getIntent();
        if (intent != null) {
            int tab = intent.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
            if (tab != -1) {
                mSelectedTab = tab;
            }
        }

        mRootContainer = findViewById(R.id.home_container);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mTabLayout = (TabLayout) findViewById(R.id.supervisor_tabLayout);
        ViewCompat.setElevation(mTabLayout, 4);

        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        mHeaderBrandText = (TextView) findViewById(R.id.registered_site);
        mHeaderStaffText = (TextView) findViewById(R.id.username);
        mHeaderImage = (AsyncImageView) findViewById(R.id.toolbar_image);
        mHeaderImage.setImageResourceId(new UriImageRequestDescriptor(
                NetworkMediaFileManager.get().fetchMediaWithoutCallback(
                        RTASurvey.getInstance().getBackgroundImgUrl(), ContentType.IMAGE_JPG)));
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Fragment selected = getSelectedFragment();
                if (selected instanceof TabbedFragment) {
                    ((TabbedFragment) selected).onFabClick(v);
                }
            }
        });

        if (mTabsAdapter == null) {
            mViewPager = (ViewPager) findViewById(R.id.supervisor_paper);
            // Keep all four tabs to minimize jank.
            mViewPager.setOffscreenPageLimit(4);
            // Set Accessibility Delegate to null so ViewPager doesn't intercept movements and
            // prevent the fab from being selected.
            mViewPager.setAccessibilityDelegate(null);
            mTabsAdapter = new TabsAdapter(mViewPager);
            mTabLayout.setupWithViewPager(mViewPager);
            createTabs();
        }


        mHeaderBrandText.setText(RTASurvey.getInstance().getOrgBrand());
        String staff = Common.getStaffInfo(this, R.string.staffInfo_format_short);
        if (TextUtils.isEmpty(staff)) {
            staff = getString(R.string.msg_staffInfo_notfound);
        }
        mHeaderStaffText.setText(staff);

        if (RTASurvey.getInstance().getDevIsApproved()) {
            UserScreenPermissionsDispatcher.doInitJobWithCheck(this);
        } else {
            finish();
        }
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                String title;
                switch (tab.getPosition()) {
                    case TAB_HOME:
                        title = getString(R.string.tracking_tab_home);
                        break;
                    case TAB_COMMUNICATION:
                        title = getString(R.string.tracking_tab_communication);
                        break;
                    case TAB_NOTIFICATION:
                        title = getString(R.string.tracking_tab_notifications);
                        break;
                    case TAB_NOTE:
                        title = getString(R.string.tracking_tab_documents);
                        break;
                    case TAB_STAFF_INFO:
                        title = getString(R.string.tracking_tab_staff_info);
                        break;
                    default:
                        title = "null";
                }
                PiwikTrackerManager.newInstance().trackTabChange(UserScreen.class.getSimpleName(), 4, Contacts.HOME, tab.getPosition(), title);
            }
        });

        mTabAppBarLayout = (AppBarLayout) findViewById(R.id.tab_appBarLayout);
        mTabCollapsingToolbarLayout = (CollapsingToolbarLayout) mTabAppBarLayout.findViewById(R.id.tab_collapsing_toolbar);
        mTabToolbar = (Toolbar) mTabCollapsingToolbarLayout.findViewById(R.id.tab_toolbar);
        mTabExpandingContainer = (ViewGroup) mTabCollapsingToolbarLayout.findViewById(R.id.tab_expanding_container);
//        mSecondTitleContainerView = mTabToolbar.findViewById(R.id.tab_title_container);
//        mTabTitleTextView = (TextView) mSecondTitleContainerView.findViewById(R.id.tab_title);
//        mTabTitleIcon = (ImageView) mSecondTitleContainerView.findViewById(R.id.tab_title_icon);
//        mTabActionMenu = (ActionMenuView) mTabToolbar.findViewById(R.id.tab_action_menu);
//        mCalendarView = (CompactCalendarView) findViewById(R.id.calendar_view);
    }

    @NeedsPermission({
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    })
    void doInitJob() {
        if (getIntent() != null) {
            isReloadSchedule = getIntent().getBooleanExtra(IS_RELOAD_SCHEDULE, false);
        }
        if (isReloadSchedule) {
            isReloadSchedule = false;
            taskUpdateSchedule = new ConnectionTask(getApplicationContext()) {
                @Override
                protected void onPreExecute() {
                    LocalBroadcastManager.getInstance(context)
                            .sendBroadcast(new Intent(ProcessFragment.DOWNLOAD_STAFF_INFORMATION));
                }

                @Override
                protected Object doInBackground(String... params) {
                    if (!Common.isConnect(context)) {
                        log.error("Offline mode - ignore schedule and config updating");
                        return null;
                    }
                    try {
                        ConnectionService connection = ConnectionService.getInstance();
                        RTASurvey pre = RTASurvey.getInstance();

                        // Get synchronize schedule (interval) in server
                        if (RTASurvey.getInstance().getNewestScheduleId() == -1) {
                            String formIdForUpdate = RTASurvey.getInstance().getFormId();
                            String res;
                            if (formIdForUpdate.equals("")) {
                                res = connection.synchronize(context, pre.getServerUrl(),
                                        pre.getServerKey(), pre.getDeviceId());
                            } else {
                                res = connection.updateSchedule(
                                        activity, pre.getServerUrl(), pre.getServerKey(),
                                        pre.getDeviceId(), formIdForUpdate);
                                if (res == null) {
                                    res = connection.synchronize(context, pre.getServerUrl(),
                                            pre.getServerKey(), pre.getDeviceId());
                                }
                            }

                            // Parse JSON result to Schedule
                            Schedule schedule = (Schedule) StringUtil.json2Object(res,
                                    Schedule.class);

                            // Default value for status
                            if (schedule != null) {
                                schedule.setStatus(Params.DETECT_ACTIVE);
                                log.info("" + schedule);

                                // Add sample data for Reliable rate checking (temporary)
                                List<Environment> envs = new ArrayList<Environment>();
                                List<ValidLocation> fields = new ArrayList<ValidLocation>();
                                List<Form> forms = new ArrayList<Form>();

                                schedule.setEnvironments(envs);
                                schedule.setFields(fields);
                                schedule.setForms(forms);

                                // save to fadata.db
                                DBService.getInstance().saveSchedule(schedule);
                            }
                        }

                        //get server config information
                        String mainUser = UserListDbHelper.getInstance().getMainUsername();
                        SSConfig config = connection.getConfiguration(context,
                                pre.getServerUrl(), pre.getServerKey(), mainUser);
                        pre.saveSSConfig(config, false);
                        ProcessDbHelper.getInstance().cleanUpData();
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Object result) {
                    LocalBroadcastManager.getInstance(RTASurvey.getInstance().getApplicationContext()).sendBroadcast(new Intent(ProcessFragment.DOWNLOAD_STAFF_INFORMATION_STOP));
                }
            };
            taskUpdateSchedule.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        try {
            RTASurvey.createRTASurveyDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }
        mAdminPreferences = this.getSharedPreferences(
                _AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                Common.hideKeyboard(UserScreen.this, mTabLayout);

                /*if (tab.getPosition() != TAB_HOME) {
                    if (MainMenuFragment.isRunning) {
                        MainMenuFragment.stopCheckNetwork();
                    }
                    mAppBarLayout.setExpanded(false);
                } else {
                    Fragment homeFragment = getFragmentOfAdapter(TAB_HOME);
                    if (homeFragment != null && homeFragment instanceof MainMenuFragment) {
                        MainMenuFragment f = (MainMenuFragment) homeFragment;
                        if (f.isAdded()) {
                            f.startCheckNetwork();
                        }
                    }
                }*/
                if (tab.getPosition() != TAB_HOME) {
                    mAppBarLayout.setExpanded(false);
                }
                if (tab.getPosition() == TAB_STAFF_INFO) {
                    StaffInfoFragment f = (StaffInfoFragment) getFragmentOfAdapter(TAB_STAFF_INFO);
                    if (f != null && f.isAdded())
                        f.startUpdateTask();
                }
                if (tab.getPosition() != TAB_STAFF_INFO) {
                    StaffInfoFragment.stopAllTask();
                }
//                if (tab.getPosition() != TAB_NOTE) {
//                    NoteListFragment f = (NoteListFragment) getFragmentOfAdapter(TAB_NOTE);
//                    if (f != null && f.isAdded())
//                        f.updateLayout();
//                }
            }
        });

        // start a foreground service
        Intent i = new Intent(this, ManagerService.class);
        i.setAction(ManagerService.ACTION_START_ALLSERVICE);
        startService(i);
        final IpCallAccount ipCallAccount = Common.getUserInfo(this).getIpcall();
        if (!LinphoneService.isReady() && ipCallAccount != null) {
            startService(new Intent(Intent.ACTION_MAIN).setClass(this, LinphoneService.class));
            LinphoneManager.setAllowIncomingCall(true);
            LinphoneService.setOnUpdateUIListener(new UpdateUIListener() {
                @Override
                public void updateUIByServiceStatus(boolean serviceConnected) {
                   if (serviceConnected) {
                       LinphoneManager.getInstance().prepareLogIn();
                       LinphoneManager.getInstance().genericLogIn(ipCallAccount.getUser(), ipCallAccount.getPassword(), null, ipCallAccount.getUrl(), LinphoneAddress.TransportType.LinphoneTransportUdp);

                       LinphoneService.instance().setForegroundNotif(NotificationCreator.getNotification(LinphoneService.instance()));
                       LinphoneService.instance().setMainNotifId(NotificationCreator.getNotificationId());
                       LinphoneService.instance().startForegroundNotification();
                   }

                    //Enable call status in user information fragment
                    ((StaffInfoFragment) mTabsAdapter.getItem(TAB_STAFF_INFO)).setEnableIPCallStatus(serviceConnected);
                }

                @Override
                public void updateToCallWidget(boolean isCalled) {

                }

                @Override
                public void launchIncomingCallActivity() {
                    Intent intent = new Intent(getApplicationContext(), IncomingIPCallActivity.class);
                    startActivity(intent);
                }

                @Override
                public void dismissCallActivity() {

                }
            });
        }

        RTASurvey pre = RTASurvey.getInstance();
        String hsUrl = pre.getChatServerURL();
        //Matrix credentials
        String username = pre.getCredential(RTASurvey.KEY_CREDENTIAL_MATRIX_USERNAME);
        String password = pre.getCredential(RTASurvey.KEY_CREDENTIAL_MATRIX_PASSWORD);

        // Current we not deploy any identity server.
        // So, we always use https://vector.im for identity url.
        // We're going to work out it later.
        RtMessaging.get().bindConfig(new RtMessagingConfig(this, hsUrl,
                "https://vector.im", username, password));

        //license checker job
        Object data = getLastNonConfigurationInstance();
        if (data instanceof LicenceChecker) {
            mLicenseChecker = (LicenceChecker) data;
        } else if (data == null) {
            mLicenseChecker = new LicenceChecker();
            mLicenseChecker.setLicenseCheckerListener(this);
            mLicenseChecker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (RTASurvey.getInstance().getOnOffSwitchingApp()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (RTASurvey.isHavePermissionUsage()) {
                    Intent intent = new Intent(this, SwitchAppService.class);
                    startService(intent);
                } else {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, 200);
                }
            } else {
                Intent intent = new Intent(this, SwitchAppService.class);
                startService(intent);
            }
        }

    }

    private void setSecondAppBarExpand(boolean expand, boolean animate) {
        mSecondAppBarExpanded = expand;
        mAppBarLayout.setExpanded(expand, animate);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Common.hideKeyboard(this, mTabLayout);
        UserScreenPermissionsDispatcher.doResumeJobWithCheck(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, mTabLayout.getSelectedTabPosition());
    }

    public FloatingActionButton getFab() {
        return mFab;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_search: {
                onSearchRequested();
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, SearchActivity.class);
        startActivity(i);
        return true;
    }

    private Fragment getFragmentOfAdapter(int position) {
        Fragment f = getFragmentManager()
                .findFragmentByTag("android:switcher:"
                        + mViewPager.getId() + ":" + position);
        return f;
    }

    public int getCurrentTab() {
        return mViewPager.getCurrentItem();
    }

    @NeedsPermission({
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    })
    void doResumeJob() {
        RTASurvey app = RTASurvey.getInstance();

        //license checker job
        if (mLicenseChecker != null) {
            mLicenseChecker.setLicenseCheckerListener(this);
            if (mLicenseChecker.getStatus() == AsyncTask.Status.FINISHED) {
                HashMap<String, Object> results = mLicenseChecker.getResults();
                if (results != null) {
                    taskComplete(results);
                    LicenceChecker t = mLicenseChecker;
                    mLicenseChecker = null;
                    t.cancel(true);
                }
            }
        }

        //update setting file
        if (app.hasPendingSettingChange()) {
            new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.new_setting_received_msg)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RTASurvey.getInstance().notifySettingChanged();
                            RTASurvey.restartApplication();
                        }
                    })
                    .create()
                    .show();
        }

        //load preferences file (collect.settings)
        File pf = new File(Collect.ODK_ROOT + "/collect.settings");
        if (pf.exists()) {
            boolean success = loadSharedPreferencesFromFile(pf);
            if (success) {
                Toast.makeText(this, R.string.collect_setting_is_loadded,
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, SaveSettingService.class);
                intent.putExtra("haveToSave", true);
                startService(intent);
                pf.delete();
            } else {
                Toast.makeText(
                        this, R.string.collect_setting_loading_failed,
                        Toast.LENGTH_LONG).show();
            }
        }
        //hack to settings file to always use MAC-Address
        app.applyDefaultDeviceIDType();

        //check config and show dialog is continued to edit form;
        File config = new File(RTASurvey.FORM_LOGGER_PATH + File.separator + FormEntryActivity.CONFIG_FILE_NAME);
        if (config.exists()) {
            String path = Utils.readFromFile(config.getPath(), getApplicationContext());
            long id = SurveyCollectorUtils.validateLatestInstanceConfig(this, path);
            if (id != -1) {
                if (mAdminPreferences.getBoolean(AdminPreferencesActivity.KEY_SHOW_CONFIRM_CONTINUED_WORK, true)) {
                    if (continueEditDialog == null || !continueEditDialog.isShowing()) {
                        continueEditDialog = createConfirmContinueDialog(id);
                        continueEditDialog.show();
                    }
                } else {
                    openInstanceEdit(id);
                }
            }
        }

        if (app.isGCMRegisteredFlag()) {
            Intent intent = new Intent(this, ManagerService.class);
            if (!Common.checkPlayServices(this)) {
                intent.setAction(Common.isConnect(this) ? ManagerService.ACTION_START_RCM : ManagerService.ACTION_STOP_RCM);
            } else {
                intent.setAction(ManagerService.ACTION_STOP_RCM);
            }
            startService(intent);
        }
        if (AppSettingSharedPreferences.getInstance(this).getKey(AppSettingSharedPreferences.KEY_APP_UPDATE))
            Common.checkAppUpdate(this);
        if (mViewPager.getCurrentItem() == TAB_STAFF_INFO) {
            ((StaffInfoFragment) mTabsAdapter.getItem(TAB_STAFF_INFO)).startUpdateTask();
        }
    }

    @SuppressWarnings("deprecation")
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger()
                .logAction(this, "createErrorDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Collect.getInstance().getActivityLogger()
                                .logAction(this, "createErrorDialog", "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }

    @Override
    public void taskComplete(HashMap<String, Object> results) {
        //license checker job
        mLicenseChecker.setLicenseCheckerListener(null);
        LicenceChecker t = mLicenseChecker;
        mLicenseChecker = null;
        t.cancel(true);

        Object license = results.get("license");
        RTASurvey.getInstance().setLicense((IPremiumLicense) license);
        String errMsg = (String) results.get("errMsg");
        if (!TextUtils.isEmpty(errMsg)) {
            Log.e(TAG, errMsg);
        } else {
            if (null != license) {
                Log.i(TAG, getString(R.string.license_checking_exist));
            } else {
                Log.i(TAG, getString(R.string.license_checking_not_exist));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        //license checker job
        if (mLicenseChecker != null) {
            mLicenseChecker.setLicenseCheckerListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            // but only if it's done, otherwise the thread never returns
            if (mLicenseChecker.getStatus() == AsyncTask.Status.FINISHED) {
                LicenceChecker t = mLicenseChecker;
                mLicenseChecker = null;
                t.cancel(true);
            }
        }
        if (taskUpdateSchedule != null) {
            taskUpdateSchedule.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (taskUpdateSchedule != null) {
            taskUpdateSchedule.cancel(true);
        }
        super.onStop();
    }

    private Dialog createConfirmContinueDialog(final long instanceId) {
        AlertDialog.Builder continuedDialog = new AlertDialog.Builder(this);
        final AlertDialog confirmContinueDialog = continuedDialog.create();
        continuedDialog.setCancelable(false);
        confirmContinueDialog.setTitle(getString(R.string.continued));
        confirmContinueDialog.setMessage(getString(R.string.continued_message));
        confirmContinueDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        openInstanceEdit(instanceId);
                    }
                });

        confirmContinueDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                getString(R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SurveyCollectorUtils.cleanLatestInstanceConfig();
                    }
                });
        return confirmContinueDialog;
    }

    private void openInstanceEdit(long id) {
        SurveyCollectorUtils.cleanLatestInstanceConfig();
        final Uri instanceUri = ContentUris.withAppendedId(
                InstanceProviderAPI.InstanceColumns.CONTENT_URI, id);
        Intent mIntent = new Intent(Intent.ACTION_EDIT, instanceUri);
        startActivity(mIntent);
    }

    private boolean loadSharedPreferencesFromFile(File src) {
        // this should probably be in a thread if it ever gets big
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefEdit = mSharedPreferences.edit();

            //maintain some entry which cannot be changed from external file
            long lastVersion = mSharedPreferences.getLong(PreferencesActivity.KEY_LAST_VERSION, 0);
            boolean firstRun = mSharedPreferences.getBoolean(PreferencesActivity.KEY_FIRST_RUN, true);

            prefEdit.clear();
            // first object is preferences
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                //retrieve the maintained entry values
                if (PreferencesActivity.KEY_FIRST_RUN.equals(key)) {
                    v = firstRun;
                }
                if (PreferencesActivity.KEY_LAST_VERSION.equals(key)) {
                    v = lastVersion;
                }

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    prefEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    prefEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
            }
            prefEdit.commit();

            // second object is admin options
            SharedPreferences.Editor adminEdit = getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0).edit();
            adminEdit.clear();
            // first object is preferences
            Map<String, ?> adminEntries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : adminEntries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    adminEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    adminEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    adminEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    adminEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    adminEdit.putString(key, ((String) v));
            }
            adminEdit.commit();

            res = true;
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        } catch (ClassNotFoundException e) {

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
        return res;
    }

    @OnPermissionDenied({
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    })
    void showPermissionDenied() {
        MessageUtils.showToastInfo(this, R.string.cpms_permission_not_allowed);
        Common.safeExit(this.getApplicationContext(), this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        UserScreenPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);

        if (mSelectedTab == TAB_NOTIFICATION) {
            mTabsAdapter.getItem(mSelectedTab).onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public int getSelectedTab() {
        return mSelectedTab;
    }

    public void registerPageChangedListener(TabbedFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.registerPageChangedListener(frag);
        }
    }

    public void unregisterPageChangedListener(TabbedFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.unregisterPageChangedListener(frag);
        }
    }

    /**
     * Cleans tab's appbar last added children.
     */
    private void cleanTabAppBarChildren() {
        for (int i = 0; i < mTabAppBarLayout.getChildCount(); i++) {
            final View child = mTabAppBarLayout.getChildAt(i);
            if (child.equals(mTabCollapsingToolbarLayout)) {
                continue;
            }
            mTabAppBarLayout.removeView(child);
        }

        for (int i = 0; i < mTabCollapsingToolbarLayout.getChildCount(); i++) {
            final View child = mTabCollapsingToolbarLayout.getChildAt(i);
            if (child.equals(mTabToolbar) || child.equals(mTabExpandingContainer)) {
                continue;
            }
            mTabCollapsingToolbarLayout.removeView(child);
        }

        mTabToolbar.removeAllViews();
        mTabToolbar.setOnMenuItemClickListener(null);
        mTabExpandingContainer.removeAllViews();

        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams)
                mTabCollapsingToolbarLayout.getLayoutParams();
        params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL |
                AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        mTabCollapsingToolbarLayout.setLayoutParams(params);
        // collapse the appbar to default state
        mTabAppBarLayout.setExpanded(false, false);
    }

    private class TabsAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {
        private static final String KEY_TAB_POSITION = "tab_position";

        private final Object mScrollingLock = new Object();

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, int position) {
                clss = _class;
                args = new Bundle();
                args.putInt(KEY_TAB_POSITION, position);
            }

            public int getPosition() {
                return args.getInt(KEY_TAB_POSITION, 0);
            }
        }

        private final ViewPager mPager;

        private final List<TabInfo> mTabs = new ArrayList<>(TAB_COUNT /* number of fragments */);
        private final Set<String> mFragmentTags = new HashSet<>(TAB_COUNT /* number of fragments */);

        public TabsAdapter(ViewPager pager) {
            super(getFragmentManager());
            mPager = pager;
            mPager.setAdapter(this);
            mPager.addOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            // Because this public method is called outside many times,
            // check if it exits first before creating a new one.
            final String name = makeFragmentName(mPager.getId(), position);
            Fragment fragment = getFragmentManager().findFragmentByTag(name);
            if (fragment == null) {
                TabInfo info = mTabs.get(position);
                fragment = Fragment.instantiate(UserScreen.this, info.clss.getName(), info.args);
            }

            return fragment;
        }

        public void addTab(TabLayout.Tab tab, Class<?> clss, int position) {
            TabInfo info = new TabInfo(clss, position);
            mTabs.add(info);
            mTabLayout.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        private String makeFragmentName(int viewId, long id) {
            return "android:switcher:" + viewId + ":" + id;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            notifySelectedPage(position);
        }

        @Override
        public void onPageSelected(int position) {
            mTabLayout.getTabAt(position).select();
            notifyPageChanged(position);

            mSelectedTab = position;

            final Fragment f = getItem(position);
            if (f instanceof TabbedFragment) {
                final TabbedFragment tab = (TabbedFragment) f;
                // cleaning children of tab's appbar, except toolbar, collapsingToolbarLayout
                cleanTabAppBarChildren();
                if (tab.onTabAppBarLayoutAppearance(mTabAppBarLayout,
                        mTabCollapsingToolbarLayout, mTabToolbar, mTabExpandingContainer)) {
                    AppBarLayout.LayoutParams params =
                            (AppBarLayout.LayoutParams) mTabCollapsingToolbarLayout.getLayoutParams();
                    params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL |
                            AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED |
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);
                    mTabCollapsingToolbarLayout.setLayoutParams(params);
                } else {
                    // re-cleaning appbar children in case of setting appearance causes change
                    // something in appbar
                    cleanTabAppBarChildren();
                    mTabAppBarLayout.setExpanded(false, false);
                }
                tab.setFabAppearance(mFab);
            } else {
                cleanTabAppBarChildren();
                mTabAppBarLayout.setExpanded(false, false);
                mFab.hide();
            }

            if (position != TAB_HOME) {
                mAppBarLayout.setExpanded(false);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        public void notifySelectedPage(int page) {
            notifyPageChanged(page);
        }

        private void notifyPageChanged(int newPage) {
            for (String tag : mFragmentTags) {
                final FragmentManager fm = getFragmentManager();
                TabbedFragment f = (TabbedFragment) fm.findFragmentByTag(tag);
                if (f != null) {
                    f.onPageChanged(newPage);
                }
            }
        }

        public void registerPageChangedListener(TabbedFragment frag) {
            String tag = frag.getTag();
            if (mFragmentTags.contains(tag)) {
                Log.wtf(TAG, "Trying to add an existing fragment " + tag);
            } else {
                mFragmentTags.add(frag.getTag());
            }
            // Since registering a listener by the fragment is done sometimes after the page
            // was already changed, make sure the fragment gets the current page
            frag.onPageChanged(mTabLayout.getSelectedTabPosition());
        }

        public void unregisterPageChangedListener(TabbedFragment frag) {
            mFragmentTags.remove(frag.getTag());
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, SwitchAppService.class);
                startService(intent);
            }
        }
    }
}
