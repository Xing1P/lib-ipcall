package vn.rta.cpms.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.MyUser;
import org.matrix.androidsdk.listeners.MXMediaUploadListener;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.login.Credentials;
import org.matrix.androidsdk.util.ImageUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import im.vector.Matrix;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import vn.rta.cpms.activities.PasscodeActivity;
import vn.rta.cpms.activities.UserDataSyncStatus;
import vn.rta.cpms.communication.ui.AvatarView;
import vn.rta.cpms.communication.util.AvatarUriUtil;
import vn.rta.cpms.fragments.dialog.SelectDialogFragment;
import vn.rta.cpms.preference.AppSettingSharedPreferences;
import vn.rta.cpms.services.model.UserInfo;
import vn.rta.cpms.tasks.SyncAndDeactivateTask;
import vn.rta.cpms.utils.Common;
import vn.rta.cpms.utils.MessageUtils;
import vn.rta.cpms.utils.PhotoSelectionHandler;
import vn.rta.cpms.utils.qrscanner.ZxingUtils;
import vn.rta.piwik.Contacts;
import vn.rta.piwik.PiwikTrackerManager;
import vn.rta.rtmessaging.RtMessaging;
import vn.rta.rtmessaging.utils.CredentialManager;
import vn.rta.survey.android.BuildConfig;
import vn.rta.survey.android.R;
import vn.rta.survey.android.activities._MainMenuActivity;
import vn.rta.survey.android.application.RTASurvey;

import static vn.rta.cpms.communication.util.AvatarPersistTask.Callback;
import static vn.rta.cpms.communication.util.ResourceUtils.Resource;
import static vn.rta.cpms.communication.util.ResourceUtils.openResource;

/**
 * @author VietDung <dungvu@rta.vn>
 */
@RuntimePermissions
public class StaffInfoFragment extends Fragment
        implements /*CommunicationLoginHelper.MatrixSessionListener, */OnClickListener,
        OnMenuItemClickListener, CredentialManager.CredentialManagerListener {
    private static final String TAG = StaffInfoFragment.class.getName();
    public static final String ITEM_MENU = StaffInfoFragment.class.getSimpleName() + ".ITEM_MENU";
    public static final String ITEM_POSITION = StaffInfoFragment.class.getSimpleName() + ".ITEM_POSITION";

    private ImageView popupBtn, staffQACode;
    private TextView staffInfo;
    private TextView txtIPCallInfo;

    private PopupMenu menu;
    private static final int MENUITEM_CHANGEPASS = 0;
    private static final int MENUITEM_SYNC_STT = 1;
    private static final int MENUITEM_CONF_HOME_MENU = 2;
    private static final int MENUITEM_EXIT = 4;
    private static final int MENUITEM_SIGN_OUT = 3;

    public static final int TAB_SYSTEM = 0;
    public static final int TAB_PROCESS = 1;
    public static final int TAB_HISTORY = 2;

    private AvatarView mAvatarView;
    private View mAvatarLoading;

    private PhotoHandler mPhotoHandler;
    private Uri mPhotoUri;

    //private ViewGroup mDataSyncing;

    private List<Fragment> mPagerData;
    protected SelectionsPagerAdapterAdapter mSelectionsPaperAdapter;
    protected ViewPager mViewPager;
    protected TabLayout mTabLayout;

    private View.OnTouchListener textviewTouchListener = new View.OnTouchListener() {
        private static final int MAX_CLICK_COUNT = 5;
        private static final int MIN_INTERVAL = 500;// millisecond
        private long lastClick = 0;
        private int count = 0;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.d("Click", "Clicked " + count);
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClick <= MIN_INTERVAL) {
                    count++;
                    if (count == MAX_CLICK_COUNT) {
                        createAdminControlAuthDialog();
                        count = 0;
                    }
                } else {
                    count = 1;
                }
                lastClick = clickTime;
            }
            return true;
        }

    };

    private PhotoHandler createPhotoHandler() {
        return new PhotoHandler(getActivity());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //registerUIReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpms_fragment_staff_info, container, false);
        mPagerData = new ArrayList<>();
        mPagerData.add(new SystemInformationFragment());
        mPagerData.add(new ProcessFragment());
        mPagerData.add(new HistoryFragment());
        staffQACode = (ImageView) view.findViewById(R.id.staff_qa_code);
        staffInfo = (TextView) view.findViewById(R.id.txt_staff_info);
        txtIPCallInfo = (TextView) view.findViewById(R.id.txt_ipcall_info);
        popupBtn = (ImageView) view.findViewById(R.id.overflow_menu);
        popupBtn.setOnClickListener(this);

        AppSettingSharedPreferences appSetting = AppSettingSharedPreferences.getInstance(getActivity());
        menu = new PopupMenu(getActivity(), popupBtn);
        if (appSetting.getKey(AppSettingSharedPreferences.KEY_USER_CHANGE_PIN))
            menu.getMenu().add(Menu.NONE, MENUITEM_CHANGEPASS, MENUITEM_CHANGEPASS,
                    getString(R.string.staffInfo_menuitem_passcode));
        if (appSetting.getKey(AppSettingSharedPreferences.KEY_USER_SYNC)) {
            menu.getMenu().add(Menu.NONE, MENUITEM_SYNC_STT, MENUITEM_SYNC_STT,
                    getString(R.string.data_syncing_status_title));
        }

        if (BuildConfig.FLAVOR.equals("rtsurvey") &&
                appSetting.getKey(AppSettingSharedPreferences.KEY_USER_MENU_CONFIG)) {
            menu.getMenu().add(Menu.NONE, MENUITEM_CONF_HOME_MENU, MENUITEM_CONF_HOME_MENU,
                    getString(R.string.home_menu_item_config_title));
        }

        if (BuildConfig.FLAVOR.equals("rtcpms")) {
            menu.getMenu().add(Menu.NONE, MENUITEM_EXIT, MENUITEM_EXIT,
                    getString(R.string.staffInfo_menuitem_exit));
            menu.getMenu().add(Menu.NONE, MENUITEM_SIGN_OUT, MENUITEM_SIGN_OUT,
                    getString(R.string.staffInfo_menuitem_signout));
        }

        popupBtn.setVisibility(menu.getMenu().size() > 0 ? View.VISIBLE : View.GONE);
        menu.setOnMenuItemClickListener(this);

        txtIPCallInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mAvatarView = (AvatarView) view.findViewById(R.id.staff_avatar);
        mAvatarLoading = view.findViewById(R.id.avatar_loading);

        //display device ID or app-version name
        TextView tvFooter = (TextView) view.findViewById(R.id.footer_text);
        tvFooter.setText("rtcpms".equals(BuildConfig.FLAVOR) ?
                getString(R.string.version_text, Common.getAppVersionName()) :
                getString(R.string.label_device_id, RTASurvey.getInstance().getDeviceId()));
        tvFooter.setOnTouchListener(textviewTouchListener);

        //display brand name
        TextView brand = (TextView) view.findViewById(R.id.txt_brand);
        brand.setText(RTASurvey.getInstance().getOrgBrand());
        String brandColor = RTASurvey.getInstance().getBrandColor();
        if (!TextUtils.isEmpty(brandColor)
                && brandColor.matches("^#(?:[0-9a-fA-F]{3}){1,2}$"))
            brand.setTextColor(Color.parseColor(brandColor));
        mViewPager = (ViewPager) view.findViewById(R.id.device_viewpager);
        mTabLayout = (TabLayout) view.findViewById(R.id.device_info_tab_layout);
        staffQACode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                staffQACode.setVisibility(View.GONE);
                showDiablogQaCode();
            }
        });
        mSelectionsPaperAdapter = new SelectionsPagerAdapterAdapter(getChildFragmentManager());
        mSelectionsPaperAdapter.setFragmentList(mPagerData);
        mViewPager.setAdapter(mSelectionsPaperAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                super.onTabUnselected(tab);
                if (tab.getPosition() == TAB_SYSTEM) {
                    SystemInformationFragment.stopCheckStatus();
                }
                if (tab.getPosition() == TAB_PROCESS) {
                    ProcessFragment.stopProcess();
                }
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                PiwikTrackerManager.newInstance().trackTabChange(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, tab.getPosition(), tab.getText().toString());
                if (tab.getPosition() == TAB_SYSTEM) {
                    SystemInformationFragment f = (SystemInformationFragment) getFragmentOfAdapter(TAB_SYSTEM);
                    if (f != null && f.isAdded()) {
                        f.updateRuntimeInformation();
                    }
                } else if (tab.getPosition() == TAB_PROCESS) {
                    ProcessFragment f = (ProcessFragment) getFragmentOfAdapter(TAB_PROCESS);
                    if (f != null && f.isAdded()) {
                        f.startProcess();
                    }
                } else {
                    HistoryFragment f = (HistoryFragment) getFragmentOfAdapter(TAB_HISTORY);
                    if (f != null && f.isAdded()) {
                        f.setUnlock(true);
                    }
                }
            }
        });
        staffInfo.setText(Common.getStaffInfo(getActivity(), R.string.staffInfo_format));

        //Listen matrix startup complete
//        RtMessaging.get().getCredentialManager().addListener(this);

        return view;
    }


    public void setEnableIPCallStatus(boolean enabled) {
        if (enabled)
        {
            Drawable d = ContextCompat.getDrawable(getActivity(), R.drawable.ic_phone_voip_enabled);
            txtIPCallInfo.setCompoundDrawables(d, null, null, null);
            txtIPCallInfo.setTextColor(Color.parseColor("#9E9E9E"));
        }
        else {
            Drawable d = ContextCompat.getDrawable(getActivity(), R.drawable.ic_phone_voip);
            txtIPCallInfo.setCompoundDrawables(d, null, null, null);
            txtIPCallInfo.setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showDiablogQaCode() {
        PiwikTrackerManager.newInstance().trackClickAction(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, Contacts.ACTION_SHOW_STAFF_CODE);
        AlertDialog.Builder mDialog = new AlertDialog.Builder(getActivity());
        final AlertDialog confirmContinueDialog = mDialog.create();
        View view = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_qr_code_layout, null);
        confirmContinueDialog.setView(view);
        ImageView qrCode = (ImageView) view.findViewById(R.id.qr_code);
        confirmContinueDialog.setCancelable(true);
        confirmContinueDialog.show();
        try {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            qrCode.setImageBitmap(ZxingUtils.encodeAsBitmap(getStaffCode(), displaymetrics.widthPixels));
            staffQACode.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
            confirmContinueDialog.dismiss();
            staffQACode.setVisibility(View.VISIBLE);
        }
    }

    private String getStaffCode() {
        UserInfo u = Common.getUserInfo(getActivity());
        return (u == null || u.getStaffCode() == null) ? "n/a" : u.getStaffCode();
    }

    public static void stopAllTask() {
        SystemInformationFragment.stopCheckStatus();
        ProcessFragment.stopProcess();
    }

    @Override
    public void onResume() {
        super.onResume();
        RtMessaging.get().getCredentialManager().addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        RtMessaging.get().getCredentialManager().removeListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mPhotoHandler != null
                && mPhotoHandler.handlePhotoActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    public void startUpdateTask() {
        if (mViewPager != null) {
            switch (mViewPager.getCurrentItem()) {
                case TAB_SYSTEM:
                    SystemInformationFragment s = (SystemInformationFragment) getFragmentOfAdapter(TAB_SYSTEM);
                    if (s != null && s.isAdded()) {
                        s.updateRuntimeInformation();
                    }
                    break;
                case TAB_PROCESS:
                    ProcessFragment p = (ProcessFragment) getFragmentOfAdapter(TAB_PROCESS);
                    if (p != null && p.isAdded()) {
                        p.startProcess();
                    }
                    break;
                case TAB_HISTORY:
                    HistoryFragment h = (HistoryFragment) getFragmentOfAdapter(TAB_HISTORY);
                    if (h != null && h.isAdded()) {
                        h.setUnlock(true);
                    }
                    break;
            }
        } else {
            Log.e(TAG, "mView pager null roi");
        }
    }

    protected Fragment getFragmentOfAdapter(int position) {
        Fragment f = getChildFragmentManager()
                .findFragmentByTag("android:switcher:"
                        + mViewPager.getId() + ":" + position);
        return f;
    }

    private void updateAvatarContent() {
        final MXSession mxSession = Matrix.getInstance(getActivity()).getDefaultSession();
        AvatarUriUtil.createAvatarUri(mxSession.getMyUser(), new Callback() {
            @Override
            public void onImageReady(Uri uriToImage, String contentType, int width, int height) {
                mPhotoHandler = createPhotoHandler();
                mAvatarView.setImageResourceUri(uriToImage);
                mAvatarView.setOnClickListener(mPhotoHandler);
                mAvatarLoading.setVisibility(View.GONE);
            }

            @Override
            public void onImageFailed(Exception exception) {
                mAvatarLoading.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.equals(popupBtn) && menu != null) {
            menu.show();
        } else if (v.equals(mAvatarView)) {
            final int[] texts = new int[]{
                    R.string.change_avatar_take_photo,
                    R.string.change_avatar_choose_photo,
            };

            SelectDialogFragment.show(getFragmentManager(), texts, new SelectDialogFragment.CallBack() {
                @Override
                public boolean onClick(DialogInterface dialog, int textResId) {
                    boolean dismissDialog = false;
                    switch (textResId) {
                        case R.string.change_avatar_take_photo:
                            dismissDialog = true;
                            break;
                        case R.string.change_avatar_choose_photo:
                            dismissDialog = true;
                            break;
                        default:
                            Log.e(TAG, "Unexpected resource: "
                                    + getActivity().getResources().getResourceEntryName(textResId));
                    }

                    return dismissDialog;
                }
            });

        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (menu == null) {
            return false;
        }

        switch (item.getItemId()) {
            case MENUITEM_CHANGEPASS:
                PiwikTrackerManager.newInstance().trackClickAction(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, Contacts.ACTION_MENU_SYC);
                getActivity().startActivity(new Intent(getActivity(), PasscodeActivity.class));
                break;
            case MENUITEM_SYNC_STT:
                getActivity().startActivity(new Intent(getActivity(), UserDataSyncStatus.class));
                break;
            case MENUITEM_CONF_HOME_MENU:
                PiwikTrackerManager.newInstance().trackClickAction(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, Contacts.ACTION_CONFIG_HOME_MENU);
                showDialogConfigureHomeMenu();
                break;
            case MENUITEM_EXIT:
                PiwikTrackerManager.newInstance().trackClickAction(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, Contacts.ACTION_EXIT);
                Common.safeExitWithConfirmDialog(getActivity());
                break;
            case MENUITEM_SIGN_OUT:
                if (Common.isConnect(getActivity())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.rta_app_name);
                    builder.setMessage(R.string.deactivate_confirm_msg);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.staffInfo_menuitem_signout,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    deactivateDevice();
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
                } else {
                    MessageUtils.showNetworkInfo(getActivity());
                }
                break;
        }

        return false;
    }

    private void deactivateDevice() {
        PiwikTrackerManager.newInstance().trackClickAction(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, Contacts.ACTION_DEACTIVE);
        new SyncAndDeactivateTask(getActivity()).execute();
    }

    private void showDialogConfigureHomeMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.home_menu_item_config_title);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_home_menu_config_layout, null);
        ListView mList = (ListView) view.findViewById(R.id.home_menu_list);
        List<HomeMenuItem> listData = getListItem();
        mList.setAdapter(new MenuItemAdapter(getActivity(), R.layout.dialog_home_menu_config_item, listData));
        builder.setView(view);
        AlertDialog alert = builder.create();
        alert.show();
    }


    private List<HomeMenuItem> getListItem() {
        List<String> menuItems = Arrays
                .asList(getActivity().getResources().getStringArray(R.array.user_main_menu));
        List<HomeMenuItem> returnList = new ArrayList<>();
        Set<String> mSet = RTASurvey.getInstance().getVisibleItemHome();
        for (int i = 0; i < menuItems.size(); i++) {
            HomeMenuItem item = new HomeMenuItem();
            item.setTile(menuItems.get(i));
            item.setDesc(menuItems.get(i));
            if (mSet == null) {
                item.setOn(true);
            } else {
                item.setOn(!mSet.contains(item.getTile()));
            }
            returnList.add(item);
        }
        return returnList;
    }

    @SuppressLint("InflateParams")
    private void createAdminControlAuthDialog() {
        //create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.secret_key_is_activated);
        builder.setCancelable(false);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.cpms_admin_setting_auth, null);
        final EditText input = (EditText) view.findViewById(R.id.admin_pass);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String pass = v.getText().toString();
                    authenticate(pass);
                }
                return true;
            }
        });
        builder.setView(view);
        builder.setPositiveButton(R.string.btn_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Common.hideKeyboard(getActivity(), input);
                        String pass = input.getText().toString();
                        authenticate(pass);
                    }
                });
        builder.setNegativeButton(R.string.btn_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Common.hideKeyboard(getActivity(), input);
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Authenticate administrator password
     */
    public void authenticate(String pass) {
        String adminPass = RTASurvey.getInstance().getAdminPass();
        if (adminPass != null && (pass == null || !pass.equals(adminPass))) {
            MessageUtils.showDialogInfo(getActivity(), R.string.password_incorrect);
        } else {
            //start RS-module setting screen
            Intent ig = new Intent(getActivity(), _MainMenuActivity.class);
            startActivity(ig);
        }
    }

    private void processAvatar(final Uri imageUri) {
        mAvatarView.setOnClickListener(null);
        mAvatarLoading.setVisibility(View.VISIBLE);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Uri scaledImageUri = null;
                MXSession session = Matrix.getInstance(getActivity()).getDefaultSession();
                try {
                    Resource resource = openResource(getActivity(), imageUri);

                    // with jpg files
                    // check exif parameter and reduce image size
                    if ("image/jpg".equals(resource.mimeType) || "image/jpeg".equals(resource.mimeType)) {
                        InputStream stream = resource.contentStream;
                        int rotationAngle = ImageUtils
                                .getRotationAngleForBitmap(getActivity(), imageUri);

                        String mediaUrl = ImageUtils.scaleAndRotateImage(
                                getActivity(), stream, resource.mimeType, 1024,
                                rotationAngle, session.getMediasCache());
                        scaledImageUri = Uri.parse(mediaUrl);
                    }
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put(Contacts.FILE_NAME, imageUri.toString());
                    PiwikTrackerManager.newInstance().trackClickAction(StaffInfoFragment.class.getSimpleName(), 20, Contacts.STAFF_INFO, Contacts.ACTION_CHANGE_AVATAR);
                    resource.contentStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "MediaStore.Images.Thumbnails.getThumbnail " + e.getMessage());
                }
                persistAvatarToServer(session, scaledImageUri != null ? scaledImageUri : imageUri);
            }
        });
    }

    private void persistAvatarToServer(final MXSession session, final Uri uri) {
        if (uri == null) {
            return;
        }

        final MyUser myUser = session.getMyUser();
        Resource resource = openResource(getActivity(), uri);

        if (resource == null) {
            Toast.makeText(getActivity(),
                    getString(R.string.failed_to_upload_avatar),
                    Toast.LENGTH_LONG).show();
            mAvatarView.setOnClickListener(mPhotoHandler);
            return;
        }

        final InputStream inputStream = resource.contentStream;
        final String contentType = resource.mimeType;
        final MXMediaUploadListener uploadCallback = new MXMediaUploadListener() {
            @Override
            public void onUploadError(String uploadId, int serverResponseCode, String serverErrorMessage) {
                Toast.makeText(getActivity(),
                        (null != serverErrorMessage) ? serverErrorMessage : getString(R.string.failed_to_upload_avatar),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onUploadComplete(String uploadId, String contentUri) {
                Log.d(TAG, "Uploaded to " + contentUri);
                myUser.updateAvatarUrl(contentUri, new SimpleApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void info) {
                        super.onSuccess(info);
                        mAvatarView.setImageResourceUri(uri);
                        mAvatarLoading.setVisibility(View.GONE);
                    }
                });
                mAvatarView.setOnClickListener(mPhotoHandler);
            }
        };
        session.getMediasCache().uploadContent(inputStream, null, contentType, null, uploadCallback);
    }

    @Override
    public void onCredentialsTaken(Credentials credentials) {

    }

    @Override
    public void onStartupComplete(MXSession defaultSession) {
        updateAvatarContent();
    }

    @Override
    public void onStartLoading() {

    }

    private final class PhotoHandler extends PhotoSelectionHandler {

        private final PhotoListener mPhotoListener;

        public PhotoHandler(Context context) {
            super(context);
            mPhotoListener = new PhotoListener();
        }

        @Override
        public PhotoActionListener getListener() {
            return mPhotoListener;
        }

        @Override
        protected void startPhotoActivity(Intent intent, int requestCode, Uri photoUri) {
            mPhotoUri = photoUri;
            StaffInfoFragmentPermissionsDispatcher
                    .openCameraWithCheck(StaffInfoFragment.this, intent, requestCode);
        }

        private final class PhotoListener extends PhotoActionListener {

            @Override
            public void onPhotoSelected(Uri uri) throws FileNotFoundException {
                processAvatar(uri);

                // Re-create the photo handler so that any additional photo selections create a
                // new temp file (and don't hit the one that was just added to the cache).
                mPhotoHandler = createPhotoHandler();
            }

            @Override
            public Uri getCurrentPhotoUri() {
                return mPhotoUri;
            }

            @Override
            public void onPhotoSelectionDismissed() {

            }
        }
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    void openCamera(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void showDeniedForCamera() {
        MessageUtils.showToastInfo(getActivity(), R.string.cpms_permission_not_allowed);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        StaffInfoFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public class SelectionsPagerAdapterAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragmentList = new ArrayList<>();

        public SelectionsPagerAdapterAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setFragmentList(List<Fragment> fragmentList) {
            this.fragmentList = fragmentList;
            notifyDataSetChanged();
        }

        public Fragment getFragment(int position) {
            return fragmentList.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment mFragment = null;
            mFragment = fragmentList.get(position);
            FragmentManager mgr = getChildFragmentManager();
            //String name = makeFragmentName(mViewPager.getId(),getItemId(position));
            String name = null;
            if (mViewPager.getId() == -1) {
                name = makeFragmentName(R.layout.fragment_system_information, TAB_SYSTEM);
            } else {
                name = makeFragmentName(mViewPager.getId(), position);
            }
            FragmentManager.BackStackEntry entry = null;
            for (int i = 0; i < mgr.getBackStackEntryCount(); i++) {
                FragmentManager.BackStackEntry e = mgr.getBackStackEntryAt(i);
                if (e.getName().equals(name)) {
                    entry = e;
                    break;
                }
            }
            if (entry != null) {
                mgr.popBackStackImmediate(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            FragmentTransaction currentTrans = mgr.beginTransaction();
            currentTrans.addToBackStack(name);
            currentTrans.commit();
            return mFragment;
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public String makeFragmentName(int viewId, long id) {
            return "android:switcher:" + viewId + ":" + id;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_SYSTEM:
                    return getString(R.string.device_info_system_title).toUpperCase();
                case TAB_PROCESS:
                    return getString(R.string.device_info_process_title).toUpperCase();
                case TAB_HISTORY:
                    return getString(R.string.device_info_history_title).toUpperCase();
                default:
                    return "".toLowerCase();
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    private class MenuItemAdapter extends ArrayAdapter<HomeMenuItem> {
        private List<HomeMenuItem> mList;
        private LayoutInflater inflater;
        private Context mContext;

        public MenuItemAdapter(Context context, int resource, List<HomeMenuItem> list) {
            super(context, resource);
            mContext = context;
            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public HomeMenuItem getItem(int position) {
            return mList.get(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            convertView = inflater.inflate(R.layout.dialog_home_menu_config_item, parent, false);
            final TextView title, desc;
            final Switch mSwitch;
            title = (TextView) convertView.findViewById(R.id.home_menu_item_name);
            desc = (TextView) convertView.findViewById(R.id.home_menu_item_desc);
            mSwitch = (Switch) convertView.findViewById(R.id.home_menu_item_on_off);
            title.setText(getItem(position).getTile());
            mSwitch.setChecked(getItem(position).isOn());
            if (getItem(position).isOn()) {
                desc.setText(getResources().getString(R.string.home_menu_item_visible, getItem(position).getDesc()));
            } else {
                desc.setText(getResources().getString(R.string.home_menu_item_invisible, getItem(position).getDesc()));
            }

            mSwitch.setOnClickListener(new OnClickListener() {
                private long mLastClickTime = 0;

                @Override
                public void onClick(View view) {
                    boolean isChecked = ((Switch) view).isChecked();
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        ((Switch) view).setChecked(!isChecked);
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();

                    Set<String> itemList = RTASurvey.getInstance().getVisibleItemHome();
                    if (itemList == null)
                        itemList = new HashSet<String>();
                    if (!isChecked) {
                        if (!itemList.contains(getItem(position).getTile())) {
                            itemList.add(getItem(position).getTile());
                            desc.setText(getResources().getString(R.string.home_menu_item_invisible, getItem(position).getDesc()));
                            RTASurvey.getInstance().updateMenuItemPosition(getItem(position).getTile(), -2);
                        }
                    } else if (itemList.contains(getItem(position).getTile())) {
                        itemList.remove(getItem(position).getTile());
                        desc.setText(getResources().getString(R.string.home_menu_item_visible, getItem(position).getDesc()));
                    }

                    RTASurvey.getInstance().updateVisibleItemHome(itemList);
                    Intent intent = new Intent(MainMenuFragment.MSG_UPDATE_MENU_ADAPTER);
                    intent.putExtra(ITEM_MENU, getItem(position).getTile());
                    intent.putExtra(ITEM_POSITION, position);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                }
            });

            return convertView;
        }
    }


    private class HomeMenuItem {
        private String tile;
        private String desc;
        private boolean isOn;

        public HomeMenuItem() {
        }

        public String getTile() {
            return tile;
        }

        public void setTile(String tile) {
            this.tile = tile;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public boolean isOn() {
            return isOn;
        }

        public void setOn(boolean on) {
            isOn = on;
        }
    }
}
