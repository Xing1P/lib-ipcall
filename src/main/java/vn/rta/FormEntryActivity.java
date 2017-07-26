/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.odk.collect.android.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.ListPreference;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.OpenableColumns;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rta.ipcall.LinphoneService;
import com.rta.ipcall.ui.OnUpdateUIListener;
import com.zj.btsdk.BluetoothService;

import net.sqlcipher.database.SQLiteDatabase;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.IAnswerDataSerializer;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.xform.util.XFormAnswerDataSerializer;
import org.linphone.core.LinphoneAccountCreator;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.exception.JavaRosaException;
import org.odk.collect.android.external.ExternalDataUtil;
import org.odk.collect.android.listeners.AdvanceToNextListener;
import org.odk.collect.android.listeners.FormLoaderListener;
import org.odk.collect.android.listeners.FormSavedListener;
import org.odk.collect.android.listeners.SavePointListener;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.logic.FormController.FailedConstraint;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.FormsProviderAPI;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;
import org.odk.collect.android.tasks.FormLoaderTask;
import org.odk.collect.android.tasks.SavePointTask;
import org.odk.collect.android.tasks.SaveResult;
import org.odk.collect.android.tasks.SaveToDiskTask;
import org.odk.collect.android.utilities.CompatibilityUtils;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.widgets.QuestionWidget;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;
import vn.rta.cpms.activities.FormSelectionActivity;
import vn.rta.cpms.activities.RtDimmableActivity;
import vn.rta.cpms.keyboard.RTAInputManager;
import vn.rta.cpms.listener.PermissionRequestListener;
import vn.rta.cpms.preference.AppSettingSharedPreferences;
import vn.rta.cpms.providers.QATaskProviderAPI;
import vn.rta.cpms.services.ConnectionService;
import vn.rta.cpms.services.FailedReportManager;
import vn.rta.cpms.services.ProcessDbHelper;
import vn.rta.cpms.services.UserListDbHelper;
import vn.rta.cpms.services.model.Response;
import vn.rta.cpms.services.model.UserInfo;
import vn.rta.cpms.tasks.ConnectionTask;
import vn.rta.cpms.ui.ntfaction.FloatingNtfActionService;
import vn.rta.cpms.ui.ntfaction.NtfActionListOverlayService;
import vn.rta.cpms.utils.Common;
import vn.rta.cpms.utils.MessageUtils;
import vn.rta.cpms.utils.SimpleCrypto;
import vn.rta.cpms.utils.StdCodeDBHelper;
import vn.rta.cpms.utils.StringUtil;
import vn.rta.cpms.utils.SurveyCollectorUtils;
import vn.rta.javarosa.model.ElementExport;
import vn.rta.media.android.callrecord.CallBroadcastReceiver;
import vn.rta.media.android.callrecord.RecordService;
import vn.rta.piwik.PiwikTrackerManager;
import vn.rta.survey.android.R;
import vn.rta.survey.android.activities.DeviceListActivity;
import vn.rta.survey.android.application.Constants;
import vn.rta.survey.android.application.IPremiumLicense;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.audiorecord.manager.RecordManager;
import vn.rta.survey.android.audiorecord.service.BackGroundAudioRecordService;
import vn.rta.survey.android.data.AnswerOfListSelectionOne;
import vn.rta.survey.android.data.InstanceLog;
import vn.rta.survey.android.data.QACheckPointData;
import vn.rta.survey.android.data.RTAActivityLogger;
import vn.rta.survey.android.data.StatusOfAnswer;
import vn.rta.survey.android.entities.MiniLogEntity;
import vn.rta.survey.android.entities.RFormEntryPrompt;
import vn.rta.survey.android.entities.TimeTampEntity;
import vn.rta.survey.android.google.BarcodeCaptureService;
import vn.rta.survey.android.listeners.BluetoothServiceCallback;
import vn.rta.survey.android.listeners.ChooseFormLanguageListener;
import vn.rta.survey.android.listeners.EndSurveyListener;
import vn.rta.survey.android.listeners.ExportToCsvTaskListener;
import vn.rta.survey.android.listeners.OnCreateRepeatRecord;
import vn.rta.survey.android.listeners.OnFocusListener;
import vn.rta.survey.android.listeners.OnNotifyComboWidget;
import vn.rta.survey.android.listeners.OnRefreshScreenListener;
import vn.rta.survey.android.listeners.OnSpecialTouchListener;
import vn.rta.survey.android.listeners.RemoveResponeListener;
import vn.rta.survey.android.listeners.SaveCurrentAnswerListener;
import vn.rta.survey.android.localDatabase.AggregateLocal;
import vn.rta.survey.android.logic.RandomViewController;
import vn.rta.survey.android.manager.ActivityLogManager;
import vn.rta.survey.android.manager.IPCallManager;
import vn.rta.survey.android.manager.InstancePoolManager;
import vn.rta.survey.android.manager.MiniLogFormAction;
import vn.rta.survey.android.manager.QACheckPointImp;
import vn.rta.survey.android.manager.QACheckPointManager;
import vn.rta.survey.android.manager.QACheckPointViewManager;
import vn.rta.survey.android.manager.QuickNoteManager;
import vn.rta.survey.android.preference.PreferencesManager;
import vn.rta.survey.android.preferences._PreferencesActivity;
import vn.rta.survey.android.services.CheckNoteService;
import vn.rta.survey.android.services.InIpCallService;
import vn.rta.survey.android.services.TraceLocationService;
import vn.rta.survey.android.tasks.ErrorReportTask;
import vn.rta.survey.android.tasks.ExportToCsvTask;
import vn.rta.survey.android.tasks.ReFormLoaderTask;
import vn.rta.survey.android.tasks.uploadworking.UploadWorkingTask;
import vn.rta.survey.android.ui.SurveyUiIntents;
import vn.rta.survey.android.utilities.CheckingSelectionOne;
import vn.rta.survey.android.utilities.FASender;
import vn.rta.survey.android.utilities.IRSAction;
import vn.rta.survey.android.utilities.RTALog;
import vn.rta.survey.android.utilities.ScreenUtils;
import vn.rta.survey.android.utilities.StringFormatUtils;
import vn.rta.survey.android.utilities.Utils;
import vn.rta.survey.android.views.CheckNoteView;
import vn.rta.survey.android.views.CheckNoteView.UpdateStatusMenuQA;
import vn.rta.survey.android.views.RTAView;
import vn.rta.survey.android.views.ScreenCaptureReportDialog;
import vn.rta.survey.android.views.ScreenRequestDialog;
import vn.rta.survey.android.views.googleprogessbar.ProgressBarHandler;
import vn.rta.survey.android.views.roundcornerprogress.TextRoundCornerProgressBar;
import vn.rta.survey.android.widgets.CallRecordWidgets;
import vn.rta.survey.android.widgets.QACheckPointWidget;
import vn.rta.survey.android.widgets.alternatives.AlbumControlView;
import vn.rta.survey.android.widgets.alternatives.WidgetAlternative;
import vn.rta.survey.android.widgets.handledata.SelectMultiWidgetDataHandler;
import vn.rta.survey.android.widgets.timeinstance.CountDownTimerInstance;

/**
 * FormEntryActivity is responsible for displaying questions, animating
 * transitions between questions, and allowing the user to enter data.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Thomas Smyth, Sassafras Tech Collective (tom@sassafrastech.com;
 *         constraint behavior option)
 * @author Thi Nguyen
 */
public class FormEntryActivity extends RtDimmableActivity implements
        AnimationListener, FormLoaderListener, FormSavedListener,
        AdvanceToNextListener, OnGestureListener, SavePointListener,
        UpdateStatusMenuQA, EndSurveyListener, OnCreateRepeatRecord, ChooseFormLanguageListener,
        SaveCurrentAnswerListener, OnRefreshScreenListener,
        OnSpecialTouchListener, RemoveResponeListener, OnFocusListener, RTAView.ScrollChangeListener, UploadWorkingTask.InstanceUploadListener,
        OnNotifyComboWidget, LinphoneAccountCreator.LinphoneAccountCreatorListener, BluetoothServiceCallback, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final boolean EVALUATE_CONSTRAINTS = true;
    public static final boolean DO_NOT_EVALUATE_CONSTRAINTS = false;
    public static final String EXTRA_FROM_NEW_INTENT = "FROM_NEW_INTENT";
    public static final String PREFIX_SHOW_DIALOG = "dialog(";
    public static final String IS_NEXT_INSTANCE = "is_next_instance";
    public static final String IS_SENT_INSTANCE = "is_sent_instance";
    public static final String INSTANCE_ID = "instance_id";
    public static final String FORM_ID = "form_id";
    public static final String FORM_VERSION = "form_version";
    // Request codes for returning data from specified intent.
    public static final int IMAGE_CAPTURE = 1;
    public static final int BARCODE_CAPTURE = 2;
    public static final int AUDIO_CAPTURE = 3;
    public static final int VIDEO_CAPTURE = 4;
    public static final int LOCATION_CAPTURE = 5;
    public static final int HIERARCHY_ACTIVITY = 6;
    public static final int IMAGE_CHOOSER = 7;
    public static final int AUDIO_CHOOSER = 8;
    public static final int VIDEO_CHOOSER = 9;
    public static final int EX_STRING_CAPTURE = 10;
    public static final int EX_INT_CAPTURE = 11;
    public static final int EX_DECIMAL_CAPTURE = 12;
    public static final int DRAW_IMAGE = 13;
    public static final int SIGNATURE_CAPTURE = 14;
    public static final int ANNOTATE_IMAGE = 15;
    public static final int ALIGNED_IMAGE = 16;
    public static final int BEARING_CAPTURE = 17;
    public static final int EX_GROUP_CAPTURE = 18;
    public static final int FACE_IMAGE = 19;
    public static final int IMAGE_TO_TEXT = 20;
    public static final int INLINEIMAGE_CAPTURE = 21;
    public static final int INLINEBARCODE_CAPTURE = 22;
    public static final int INLINEAUDIO_CAPTURE = 23;
    public static final int INLINEVIDEO_CAPTURE = 24;
    public static final int INLINELOCATION_CAPTURE = 25;
    public static final int OSM_CAPTURE = 26;
    public static final int INLINEIMAGE_ALBUM_CAPTURE = 27;
    public static final int TAGGING_SELECT_ONE_SEARCH = 30;
    public static final int TAGGING_SELECT_MULTIPLE_SEARCH = 31;
    public static final int SEARCH_AUTO_COMPLETE_NOEDIT = 32;
    public static final int GEOSHAPE_CAPTURE = 33;
    public static final int GEOTRACE_CAPTURE = 34;
    public static final int INLINEVIDEO_ALBUMM_CAPTURE = 35;
    public static final int VERIFY_FACEBOOK = 36;
    public static final int ALBUM_CAPTURE = 37;
    public static final int ALBUM_UPDATE = 38;
    public static final int VIDEO_PLAYER = 39;
    public static final String GEOSHAPE_RESULTS = "GEOSHAPE_RESULTS";
    public static final String GEOTRACE_RESULTS = "GEOTRACE_RESULTS";
    // Extra returned from gp activity
    public static final String LOCATION_RESULT = "LOCATION_RESULT";
    public static final String BEARING_RESULT = "BEARING_RESULT";
    //The name of file which contain latest instance path which wasn't safely saved.
    public static final String CONFIG_FILE_NAME = "config";
    public static final String KEY_INSTANCES = "instances";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_ERROR = "error";
    public static final String ACTION_FLOATING_BARCODE = FormEntryActivity.class.getName() + ".ACTION_FLOATING_BARCODE";
    public static final String ACTION_FLOATING_BARCODE_ACTION = FormEntryActivity.class.getName() + ".ACTION_FLOATING_BARCODE_ACTION";
    public static final String ACTION_FLOATING_AUDIO = FormEntryActivity.class.getName() + ".ACTION_FLOATING_AUDIO";
    public static final String ACTION_RELOAD_LAYOUT = FormEntryActivity.class.getName() + ".ACTION_RELOAD_LAYOUT";
    public static final String ACTION_FLOATING_SIP_CALL = FormEntryActivity.class.getName() + ".ACTION_FLOATING_SIP_CALL";
    public static final String ACTION_AUDIO_CAPTURE = FormEntryActivity.class.getName() + ".ACTION_AUDIO_CAPTURE";
    // Identifies the gp of the form used to launch form entry
    public static final String KEY_FORMPATH = "formpath";
    // these are only processed if we shut down and are restoring after an
    // external intent fires
    public static final String KEY_INSTANCEPATH = "instancepath";
    public static final String KEY_QAPATH = "qaPath";
    public static final String KEY_EXTERNALDATAPATH = "externalDataPath";
    public static final String KEY_XPATH = "xpath";
    public static final String KEY_XPATH_WAITING_FOR_DATA = "xpathwaiting";
    // Tracks whether we are autosaving
    public static final String KEY_AUTO_SAVED = "autosaved";
    public static final String KEY_FONT_SIZE = "font_size";
    public static final int PERMISSIONS_REQUEST = 1;
    private static final String t = "FormEntryActivity";
    // save with every swipe forward or back. Timings indicate this takes .25
    // seconds.
    // if it ever becomes an issue, this value can be changed to save every n'th
    // screen.
    private static final int SAVEPOINT_INTERVAL = 1;
    // Defines for FormEntryActivity+
    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    // add by rta
    private static final String STARTFORM = "Beginform";
    private static final String ENDFORMS = "SaveAndExit_S";
    private static final String ENDFORMF = "SaveAndExit_F";
    // Identifies whether this is a new form, or reloading a form after a screen
    // rotation (or similar)
    private static final String NEWFORM = "newform";
    private static final int REQUEST_ENABLE_BT = 28;
    private static final int REQUEST_CONNECT_DEVICE = 29;
    private static final int MENU_LANGUAGES = Menu.FIRST;
    private static final int MENU_HIERARCHY_VIEW = Menu.FIRST + 1;
    private static final int MENU_SAVE = Menu.FIRST + 2;
    private static final int MENU_PREFERENCES = Menu.FIRST + 3;
    private static final int MENU_QUICK_NOTE = Menu.FIRST + 4;
    private static final int MENU_QA_CHECKING = Menu.FIRST + 5;
    private static final int MENU_IMAGE_REPORT = Menu.FIRST + 6;
    private static final int MENU_REQUEST_EDIT = Menu.FIRST + 7;
    private static final int MENU_FONT_SIZE = Menu.FIRST + 8;
    private static final int PROGRESS_DIALOG = 1;
    private static final int SAVING_DIALOG = 2;
    private static final int CONSTRAIN_DIALOG = 3;
    // Random ID
    private static final int DELETE_REPEAT = 654321;
    private static final int OPEN_IN_MUSIC = 1;
    private static final int COUNTER_UPDATE_MSG_ID = 123;
    // check relevant in question
    public static boolean fisrtShow = true;
    //check show promt list
    public static boolean isShowFromtList = false;
    public static boolean currentIsRepeat = false;
    public static String currentRepeatName = "";
    public static HashMap<String, TimeTampEntity> tempTimeMaps = new HashMap<>();
    public static boolean isFloating;
    //flag is sending working data
    private static boolean isSendingWorking = false;
    //flag is data has changed in xml
    private static boolean isSaveConfig = false;
    private static ArrayList<Integer> mPointCountsList;
    private static String keyQuestion;
    private final Object saveDialogLock = new Object();
    public boolean isImmersiveFullscreen = false;
    public boolean ispause = false;
    public boolean isViewOnly = false;
    public ArrayList<FailedConstraint> listConfirmRead;
    public boolean isLoading = false;
    protected boolean isOnActivityResult = false;
    boolean isresume = false;
    int screenWidth = 0;
    int screenHeight = 0;
    //For receive phone call event
    CallBroadcastReceiver callReceiver;
    IntentFilter callIntentFilter;
    Toolbar mToolbar;
    int countTimer;
    int positionConfirmRead = 0;
    ExecutorService mThreadPool;
    boolean started = true;
    int duration = 0;
    /**
     * Creates a view given the View type and an event
     *
     * @param event
     * @param advancingPage -- true if this results from advancing through the form
     * @return newly created View
     */
    boolean movingNext = true;   //true: moving next. false: moving back
    int progressWaitChecking;
    OnUpdateUIListener updateUIListener = null;
    private int countAction = 0;
    private String action_qrapi = "";
    private Map<String, HashMap<String, List<String>>> question;
    private BluetoothService bluetoothService = null;
    private boolean isNextInstance = false;
    private boolean isSentInstance = false;
    private boolean isShowPercentProgress = false;
    //qa checking
    private boolean havePopupWidget = false;
    private boolean mAutoSaved;
    private boolean existEndSurvey = false;
    private String mFormPath;
    private GestureDetector mGestureDetector;
    private Animation mInAnimation;
    private Animation mOutAnimation;
    private View mStaleView = null;
    private LinearLayout mQuestionHolder;
    private View mCurrentView;
    private File infLegacy = null;
    private AlertDialog mAlertDialog;
    private Dialog error_dialog, warnig_dialog, exit_dialog, add_new_group, add_one_more_group;
    private Dialog loading_dialog, saving_dialog, changes_language;
    private ProgressDialog mProgressDialog;
    private String mErrorMessage;
    private String instacePathforDatabase;
    private String timeStart = "";
    private String total = "";
    private long timeStartmilis = 0;
    private long IdInstanceSend = -1;
    // used to limit forward/backward swipes to one per question
    private boolean mBeenSwiped = false;
    private String nameofInstance;
    private int viewCount = 0;
    //number error of QA checking reciver form server
    private long numberError = 0;
    private FormController mFormController;
    private FormLoaderTask mFormLoaderTask;
    private ReFormLoaderTask mReFormLoaderTask;
    private SaveToDiskTask mSaveToDiskTask;
    private ExportToCsvTask mExportToCsvTask;
    private UploadWorkingTask mUploadWorkingTask;
    private ImageButton mNextButton;
    private ImageButton mBackButton;
    private ImageButton mNext;
    private ImageButton mPre;
    private ImageButton mUp;
    private ImageButton mDown;
    private boolean isScrollable;
    private Button mShowPercentProgress;
    private RelativeLayout out;
    private RelativeLayout content;
    //mdeia player
    private PreviewPlayer mPlayer;
    private TextView mTextLine1;
    private TextView mTextTimer;
    private ImageButton closePlayer;
    private TextView mTextLine2;
    private TextView mLoadingText;
    private TextView mTextSize;
    private SeekBar mSeekBar;
    private Handler mProgressRefresher;
    private boolean mSeeking = false;
    private int mDuration;
    private Uri mUri;
    private long mMediaId = -1;
    private AudioManager mAudioManager;
    private boolean mPausedByTransientLossOfFocus;
    private int countSave = 0;
    private String stepMessage = "";
    private boolean isComfrimAudioRecore = false;
    private Handler mHandler;
    private Bundle preloadBundle = null;
    private FormIndex currentQaCheckPoint = null;
    private boolean waitingCheckpoint;
    private String currentCommand;
    private Dialog qaCheckPointDialog;
    private CountDownTimer waitingQAResultCountDown;
    private Dialog waitingQAResultDialog;
    private boolean isReatpeatDelete = false;
    private String formFamilyNext;
    private AlertDialog constraintMessageDialog;
    private ListPreference mFontSizePreference;
    //skip validate form
    private boolean skipValidate = false;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mPlayer == null) {
                // this activity has handed its MediaPlayer off to the next activity
                // (e.g. portrait/landscape switch) and should abandon its focus
                mAudioManager.abandonAudioFocus(this);
                return;
            }
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    mPausedByTransientLossOfFocus = false;
                    mPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mPlayer.isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        mPlayer.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        start();
                    }
                    break;
            }
            updatePlayPause();
        }
    };
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mSeeking = true;
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            // Protection for case of simultaneously tapping on seek bar and exit
            if (mPlayer == null) {
                return;
            }
            mPlayer.seekTo(progress);
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mSeeking = false;
        }
    };
    private FormIndex formIndexClear = null;
    private InstancePoolManager instancePoolManager = InstancePoolManager.getManager();
    private AggregateLocal aggregateLocal;
    private boolean safeExitForm = false;
    private int mAnimationCompletionSet = 0;
    private ProgressDialog dialogProcessQAchecking;
    private boolean isBreakThreadQA = false;
    private BottomSheetBehavior mBottomSheetBehavior;
    private PermissionRequestListener listener;
    private TextRoundCornerProgressBar progressFillForm;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_FLOATING_BARCODE.equals(action)) {
                boolean isCombo = intent.getBooleanExtra(RTAView.NOTIFY_COMBO, false);
                if (mFormController != null) {
                    String sb = intent.getStringExtra("SCAN_RESULT");
                    String viewIndex = intent.getStringExtra("VIEW_INDEX");
                    if (sb == null || sb.equals("")) {
                        return;
                    }
                    if (viewIndex == null || viewIndex.equals("")) {
                        viewIndex = getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                    }
                    if (viewIndex == null || viewIndex.equals("")) {
                        return;
                    }
                    if (isCombo) {
                        String reference = intent.getStringExtra(RTAView.REFER_QUESTION_COMBO);
                        ((RTAView) mCurrentView).setComboData(sb, reference);
                    }
                    if (mCurrentView instanceof RTAView)
                        ((RTAView) mCurrentView).setBinaryData(sb, viewIndex);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    refreshCurrentView(viewIndex);
                    mFormController.setIndexWaitingForData(null);
                }
            } else if (ACTION_FLOATING_BARCODE_ACTION.equals(action)) {
                action_qrapi = intent.getStringExtra("SCAN_ACTION");
                notifyActionAnswer(question);

            } else if (ACTION_RELOAD_LAYOUT.equals(action)) {
                if (isFloating) {
                    if (ScreenUtils.isDimmed()) {
                        refreshCurrentView();
                    }
                }
            } else if (ACTION_FLOATING_AUDIO.equals(action)) {
                String audioData = intent.getStringExtra(BackGroundAudioRecordService.EXTRA_AUDIO_PATH);
                if (audioData == null) {
                    return;
                }
                String viewIndex = intent.getStringExtra("VIEW_INDEX");
                if (viewIndex == null || viewIndex.equals("")) {
                    viewIndex = getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                }

                if (viewIndex == null || viewIndex.equals("")) {
                    return;
                }

                if (mCurrentView instanceof RTAView)
                    ((RTAView) mCurrentView).setBinaryDataWithoutCheck(new StringData(audioData), viewIndex);

                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                stopService(new Intent(FormEntryActivity.this, BackGroundAudioRecordService.class));
            } else if (ACTION_FLOATING_SIP_CALL.equals(action)) {
                FormController formController = RTASurvey.getInstance().getFormController();
                if (formController != null) {
                    String sb = intent.getStringExtra("SCAN_RESULT");
                    FormIndex viewIndex = formController.getSipCallIndexs();
                    if (sb == null || sb.equals("")) {
                        return;
                    }
                    if (viewIndex == null || viewIndex.equals("")) {
                        return;
                    }
                    try {
                        String oldAnser = formController.getQuestionPrompt(viewIndex).getAnswerText();
                        if (oldAnser != null)
                            sb = oldAnser + " " + sb;
                        formController.saveAnswer(viewIndex, new StringData(sb));
                    } catch (JavaRosaException e) {
                        e.printStackTrace();
                    }
                    refreshCurrentView();
                }
            } else if (ACTION_AUDIO_CAPTURE.equals(action)) {
                String index = null;
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                    if (index == null)
                        index = getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // For audio/video capture/chooser, we get the URI from the content
                // provider
                // then the widget copies the file and makes a new entry in the
                // content provider.
                Uri capturedVideo = intent.getParcelableExtra("DATA");
                if (capturedVideo != null) {
                    String pathSrcVideo = capturedVideo.getPath();
                    File srcVideoFile = new File(pathSrcVideo);
                    String instanceFolderVideo = mFormController.getInstancePath()
                            .getParent();
                    String pathDestVideo = instanceFolderVideo + File.separator
                            + System.currentTimeMillis();

                    File destVideoFile = new File(pathDestVideo);
                    if (!destVideoFile.exists()) {
                        Log.e(t, destVideoFile.getAbsolutePath() + " this is not exists");
                    }

                    try {
                        org.apache.commons.io.FileUtils.moveFile(srcVideoFile, destVideoFile);
                    } catch (IOException e) {
                        Log.e(t, "Failed to move file to " + destVideoFile.getAbsolutePath());
                        e.printStackTrace();
                    }
                    ((RTAView) mCurrentView).setBinaryData(destVideoFile, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                } else {
                    ((RTAView) mCurrentView).setBinaryData(null, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }

            }
        }
    };

    public static boolean isSendingWorking() {
        return isSendingWorking;
    }

    public static void setIsSendingWorking(boolean isSendingWorking) {
        FormEntryActivity.isSendingWorking = isSendingWorking;
    }

    @Deprecated
    public static void setIsChangeDataEdit(boolean isChangeDataEdit) {
    }

    public static void openFormEntry(Context context, long formId,
                                     Bundle preloadBundle, String keyQuestion) {
        Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, formId);
        Intent fillForm = new Intent(Intent.ACTION_EDIT, formUri);
        if (preloadBundle != null) {
            fillForm.putExtras(preloadBundle);
        }
        FormEntryActivity.keyQuestion = keyQuestion;
        context.startActivity(fillForm);
    }

    public static void openFormEntry(Context context, String jrFormId, String jrVersion,
                                     Bundle preloadBundle, String keyQuestion) {
        Cursor c = context.getContentResolver().query(FormsColumns.CONTENT_URI,
                new String[]{FormsColumns._ID},
                FormsColumns.JR_FORM_ID + "=? and " +
                        FormsColumns.JR_VERSION + "=? and " +
                        FormsColumns.AVAILABILITY_STATUS + "=?",
                new String[]{jrFormId, jrVersion, FormsProviderAPI.STATUS_AVAILABLE}, null);
        long id = -1;
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getLong(0);
            }
            c.close();
        }
        if (id > -1) {
            openFormEntry(context, id, preloadBundle, keyQuestion);
        } else {
            Log.e(t, "ERROR:: Cannot connect to Form's data provider");
        }
    }

    public void setCountSave(int countSave) {
        this.countSave = countSave;
    }

    @Override
    public void onSpecialTouch(int left, int top, int height, int width) {

    }

    @Override
    public void onFocus(FormIndex index, boolean isQaCheckPoint, String command) {
        if (isViewOnly)
            return;
        if (mFormController != null) {
            QACheckPointImp.getInstance(mFormController.getUuid()).setFlagDataChange();
        }
        if (currentQaCheckPoint == null) {
            if (isQaCheckPoint) {
                currentQaCheckPoint = index;
                waitingCheckpoint = true;
                currentCommand = command;
            }
        } else {
            if (index.equals(currentQaCheckPoint)) {
                // focusing in one question
                //nothing to do
            } else {
                if (waitingCheckpoint) {
                    //send qa check point
                    if (currentCommand.contains(QACheckPointImp.COMMAND_INVISIBLE)) {
                        String uuid = mFormController.getUuid();
                        saveAnswersForCurrentScreen(false);
                        QACheckPointManager qaCheckPointManager = QACheckPointImp.getInstance(uuid);
                        qaCheckPointManager.setQACheckPointMode(QACheckPointImp.INVISIBLE);
                        qaCheckPointManager.runQACheckPoint(currentCommand);
                    }
                }
                if (isQaCheckPoint) {
                    currentQaCheckPoint = index;
                    waitingCheckpoint = true;
                    currentCommand = command;
                } else {
                    currentQaCheckPoint = null;
                    waitingCheckpoint = false;
                    currentCommand = "";
                }
            }
        }
    }

    @Override
    public void onScrollChanged() {
        mDown.setVisibility(mCurrentView instanceof RTAView && isScrollable && !((RTAView) mCurrentView).isBottom() &&
                PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_NAVIGATION_UP_DOWN_LEFT_RIGHT_BUTTONS, false) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onUploadCompleted(String instancePath) {


    }

    @Override
    public void onUploadFailed(String result, String instancePath) {


    }

    @Override
    public void notifyComboWidget(String index, boolean isBarcodeOnly) {
        for (QuestionWidget questionWidget : ((RTAView) mCurrentView).getWidgets()) {
            if (index.equals(mFormController.getQuestionName(questionWidget.getPrompt().getIndex()))) {
                questionWidget.startCombo(isBarcodeOnly);
                break;
            }
        }
    }

    @Override
    public void onAccountCreatorIsAccountUsed(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorAccountCreated(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorAccountActivated(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorAccountLinkedWithPhoneNumber(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorPhoneNumberLinkActivated(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorIsAccountActivated(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorPhoneAccountRecovered(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorIsAccountLinked(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorIsPhoneNumberUsed(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onAccountCreatorPasswordUpdated(LinphoneAccountCreator linphoneAccountCreator, LinphoneAccountCreator.RequestStatus requestStatus) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mSeekBar.setProgress(mDuration);
        updatePlayPause();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (isFinishing()) return;
        mPlayer = (PreviewPlayer) mp;
        setNames();
        mPlayer.start();
        showPostPrepareUI();

    }

    public void playPauseClicked(View v) {
        // Protection for case of simultaneously tapping on play/pause and exitex

        if (mPlayer == null) {
            return;
        }
        if (mPlayer.isPlaying()) {
            ispause = true;
            isresume = true;
            mPlayer.pause();
        } else {
            ispause = false;
            if (!isresume) {
                timer();
            }
            isresume = false;
            start();


        }
        updatePlayPause();
    }

    @Override
    protected int getContentViewId() {
        return (R.layout.form_entry);
    }
    //refreh current view when remove answer

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PiwikTrackerManager.newInstance().startNewSession();
        RTAInputManager.getInstance().setmActivity(this);
        safeExitForm = false;
        isFloating = false;
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        System.loadLibrary("sqliteX");
        SQLiteDatabase.loadLibs(this);

        Intent intentSetting = getIntent();

        listConfirmRead = new ArrayList<>();

        if (intentSetting != null) {
            Uri uri = intentSetting.getData();
            if (getContentResolver().getType(uri).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
                // get the formId and version for this instance...
                Cursor c = null;
                try {
                    c = getContentResolver().query(uri, null, null, null, null);
                    c.moveToFirst();
                    String jrFormId = c.getString(c.getColumnIndex(InstanceColumns.JR_FORM_ID));
                    String jrVersion = c.getString(c.getColumnIndex(InstanceColumns.JR_VERSION));
                    String formFamilyId = null;
                    String formMediaPath = "";
                    Cursor mCursor = null;
                    try {
                        mCursor = getContentResolver().query(FormsColumns.CONTENT_URI,
                                new String[]{FormsColumns.FORM_MEDIA_PATH, FormsColumns.JR_FORM_ID, FormsColumns.FAMILY},
                                FormsColumns.JR_FORM_ID + "=? AND " + FormsColumns.JR_VERSION + "=?",
                                new String[]{jrFormId, jrVersion}, null);
                        mCursor.moveToFirst();
                        formFamilyId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.FAMILY));
                        formMediaPath = mCursor.getString(mCursor.getColumnIndex(FormsColumns.FORM_MEDIA_PATH));
                        String formFamilyPath = RTASurvey.FAMILY_MEDIA_PATH + File.separator + formFamilyId;
                        PreferencesManager.getPreferencesManager(this).buildFormSetting(formMediaPath, formFamilyPath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (mCursor != null) {
                            mCursor.close();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null)
                        c.close();
                }
            } else if (getContentResolver().getType(uri).equals(FormsColumns.CONTENT_ITEM_TYPE)) {
                Cursor c = null;
                try {
                    c = getContentResolver().query(uri, null, null, null,
                            null);
                    c.moveToFirst();
                    String mFormMediaPath = c.getString(c.getColumnIndex(FormsColumns.FORM_MEDIA_PATH));
                    String formFamilyPath = c.getString(c.getColumnIndex(FormsColumns.FAMILY));
                    PreferencesManager.getPreferencesManager(this).buildFormSetting(mFormMediaPath, formFamilyPath);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null)
                        c.close();
                }
            }
        }

        //override 'disable screen off' config from super class
        boolean disableScreenoff = PreferencesManager.getPreferencesManager(this)
                .getBoolean(_PreferencesActivity.KEY_DISABLE_SCREEN_OFF, true);
        ScreenUtils.setAllowScreenOff(this, !disableScreenoff);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_FLOATING_BARCODE);
        intentFilter.addAction(ACTION_FLOATING_BARCODE_ACTION);
        intentFilter.addAction(ACTION_FLOATING_AUDIO);
        intentFilter.addAction(ACTION_RELOAD_LAYOUT);
        intentFilter.addAction(ACTION_FLOATING_SIP_CALL);
        intentFilter.addAction(ACTION_AUDIO_CAPTURE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
        RTASurvey.getInstance().setActivity(this);
        // must be at the beginning of any activity that can be called from an
        // external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            ErrorReportTask.exportError(this, getClass().getName(), e.getMessage() + "Full log: " + Log.getStackTraceString(e));
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        out = (RelativeLayout) findViewById(R.id.rl);
        content = (RelativeLayout) findViewById(R.id.rl_content);

        //Use the new, powerful toolbar to replace the old actionbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // Display icon in the toolbar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RTAInputManager.getInstance().setView(out);
        // RTAInputManager.getInstance().setButtonHolderView(findViewById(R.id.buttonholder).getId());

        setTitle(getString(R.string.loading_form));

        mErrorMessage = null;

        mBeenSwiped = false;
        mAlertDialog = null;
        mCurrentView = null;
        mInAnimation = null;
        mOutAnimation = null;
        mGestureDetector = new GestureDetector(this, this);
        mQuestionHolder = (LinearLayout) findViewById(R.id.questionholder);
        RTAInputManager.getInstance().setQuestionId(mQuestionHolder.getId());
        mPointCountsList = new ArrayList<Integer>();
        // get admin preference settings
        mShowPercentProgress = (Button) findViewById(R.id.show_percent);
        mShowPercentProgress.setOnClickListener(new OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                mShowPercentProgress.setVisibility(View.GONE);
                isShowPercentProgress = false;
            }
        });

        progressFillForm = (TextRoundCornerProgressBar) findViewById(R.id.progress_bar);
        progressFillForm.setProgressBackgroundColor(getResources().getColor(R.color.custom_progress_background));
        progressFillForm.setOnClickListener(new OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if (isShowPercentProgress) {
                    isShowPercentProgress = false;
                } else {
                    isShowPercentProgress = true;
                    if (mFormController != null) {
                        mShowPercentProgress.setText("Answered:" + mFormController.getQuestionsHaveFill().size() + "/" + mFormController.getQuestionsList().size());
                        mShowPercentProgress.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        setProgressColor();

        mNextButton = (ImageButton) findViewById(R.id.form_forward_button);
        mNextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeenSwiped = true;
                checkAlertAndShowNextView();
            }
        });

        mBackButton = (ImageButton) findViewById(R.id.form_back_button);
        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeenSwiped = true;
                checkAlertAndShowPreviousView();
            }
        });

        mNext = (ImageButton) findViewById(R.id.form_entry_button_forward);
        mPre = (ImageButton) findViewById(R.id.form_entry_button_backward);
        mDown = (ImageButton) findViewById(R.id.form_entry_button_down);
        setUpPosition(mNext);
        setUpPosition(mPre);

        String startingXPath = null;
        String waitingXPath = null;
        String instancePath = null;
        String qaPath = null;
        String externalDataPath = null;
        Boolean newForm = true;
        mAutoSaved = false;
        if (!getIntent().getBooleanExtra(EXTRA_FROM_NEW_INTENT, false)
                && savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_FORMPATH)) {
                mFormPath = savedInstanceState.getString(KEY_FORMPATH);
            }
            if (savedInstanceState.containsKey(KEY_INSTANCEPATH)) {
                instancePath = savedInstanceState.getString(KEY_INSTANCEPATH);
            }
            if (savedInstanceState.containsKey(KEY_QAPATH)) {
                qaPath = savedInstanceState.getString(KEY_QAPATH);
            }
            if (savedInstanceState.containsKey(KEY_EXTERNALDATAPATH)) {
                externalDataPath = savedInstanceState.getString(KEY_EXTERNALDATAPATH);
            }
            if (savedInstanceState.containsKey(KEY_XPATH)) {
                startingXPath = savedInstanceState.getString(KEY_XPATH);
            }
            if (savedInstanceState.containsKey(KEY_XPATH_WAITING_FOR_DATA)) {
                waitingXPath = savedInstanceState
                        .getString(KEY_XPATH_WAITING_FOR_DATA);
            }
            if (savedInstanceState.containsKey(NEWFORM)) {
                newForm = savedInstanceState.getBoolean(NEWFORM, true);
            }
            if (savedInstanceState.containsKey(KEY_ERROR)) {
                mErrorMessage = savedInstanceState.getString(KEY_ERROR);
            }
            if (savedInstanceState.containsKey(KEY_AUTO_SAVED)) {
                mAutoSaved = savedInstanceState.getBoolean(KEY_AUTO_SAVED);
            }
        }

        // If a parse error message is showing then nothing else is loaded
        // Dialogs mid form just disappear on rotation.
        if (mErrorMessage != null) {
            createErrorDialog(mErrorMessage, EXIT);
            return;
        }

        //get preload values from intent's extras
        //may null
        preloadBundle = getIntent() == null ? null : getIntent().getExtras();

        // Check to see if this is a screen flip or a new form load.
        Object data = getLastCustomNonConfigurationInstance();
        if (data instanceof FormLoaderTask) {
            mFormLoaderTask = (FormLoaderTask) data;
        } else if (data instanceof SaveToDiskTask) {
            mSaveToDiskTask = (SaveToDiskTask) data;
        } else if (data == null) {
            if (!newForm) {
                if (mFormController != null) {
                    refreshCurrentView();
                } else {
                    // we need to launch the form loader to load the form
                    // controller...
                    mFormLoaderTask = new FormLoaderTask(instancePath, qaPath,
                            startingXPath, waitingXPath);
                    mFormLoaderTask.execute(mFormPath);
                }
                return;
            }

            // Not a restart from a screen orientation change (or other).
            Collect.getInstance().setFormController(null);
            CompatibilityUtils.invalidateOptionsMenu(this);
            CheckNoteView.setOnupdateMenuQA(this);

            Intent intent = getIntent();
            if (intent != null) {
                Uri uri = intent.getData();

                if (getContentResolver().getType(uri).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
                    // get the formId and version for this instance...
                    String jrFormId = null;
                    String jrVersion = null;
                    {
                        Cursor instanceCursor = null;
                        try {
                            instanceCursor = getContentResolver().query(uri,
                                    null, null, null, null);
                            if (instanceCursor.getCount() != 1) {
                                this.createErrorDialog("Bad URI: " + uri, EXIT);
                                return;
                            } else {
                                instanceCursor.moveToFirst();
                                String status = instanceCursor.getString(instanceCursor.getColumnIndex(InstanceColumns.STATUS));
                                // we ignore check exist instancePlusInfo of template instance
                                if (instanceCursor.getCount() != 1 && !status.equals("pending")) {
                                    this.createErrorDialog("Bad URI: " + uri, EXIT);
                                    return;
                                } else {
                                    instancePath = instanceCursor.getString(instanceCursor
                                            .getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));
                                    if (instanceCursor.getCount() == 1) {
                                        qaPath = instanceCursor.getString(instanceCursor
                                                .getColumnIndex(InstanceColumns.INSTANCE_QA_PATH));
                                    }

                                    //lost qaPath (in case baseline, returned, pending instance)
                                    if (TextUtils.isEmpty(qaPath)) {
                                        File _f = new File(instancePath);
                                        qaPath = RTASurvey.QA_INSTANCE_PATH
                                                + File.separator
                                                + _f.getParentFile().getName()
                                                + RTASurvey.QA_INSTANCE_FILE_POSFIX;
                                    }

                                    if (TextUtils.isEmpty(externalDataPath)) {
                                        File _f = new File(instancePath);
                                        externalDataPath = RTASurvey.EXTERNALDATA_INSTANCE_PATH
                                                + File.separator
                                                + _f.getParentFile().getName();
                                    }

                                    jrFormId = instanceCursor.getString(instanceCursor
                                            .getColumnIndex(InstanceColumns.JR_FORM_ID));
                                    int idxJrVersion = instanceCursor
                                            .getColumnIndex(InstanceColumns.JR_VERSION);

                                    jrVersion = instanceCursor.isNull(idxJrVersion) ? null
                                            : instanceCursor.getString(idxJrVersion);
                                }

                                //avoid editing complete instance flows
                                isViewOnly = getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false);
                                if (!isViewOnly) {
                                    boolean canEditAfterFinal = Boolean
                                            .parseBoolean(instanceCursor
                                                    .getString(instanceCursor
                                                            .getColumnIndex(InstanceColumns.CAN_EDIT_WHEN_COMPLETE)));
                                    isViewOnly = !SurveyCollectorUtils.canBeEdited(status) && !canEditAfterFinal;
                                }
                            }
                        } finally {
                            if (instanceCursor != null) {
                                instanceCursor.close();
                            }
                        }
                    }

                    String[] selectionArgs;
                    String selection;

                    if (jrVersion == null) {
                        selectionArgs = new String[]{jrFormId};
                        selection = FormsColumns.JR_FORM_ID + "=? AND "
                                + FormsColumns.JR_VERSION + " IS NULL";
                    } else {
                        selectionArgs = new String[]{jrFormId, jrVersion};
                        selection = FormsColumns.JR_FORM_ID + "=? AND "
                                + FormsColumns.JR_VERSION + "=?";
                    }

                    {
                        Cursor formCursor = null;
                        try {
                            formCursor = getContentResolver().query(
                                    FormsColumns.CONTENT_URI, null, selection,
                                    selectionArgs, null);
                            if (formCursor == null || formCursor.getCount() < 1) {
                                this.createErrorDialog(
                                        getString(R.string.parent_form_not_present, jrFormId)
                                                + ((jrVersion == null) ? ""
                                                : "\n" + getString(R.string.version) + " " + jrVersion),
                                        EXIT);
                                return;
                            } else if (formCursor.getCount() == 1) {
                                formCursor.moveToFirst();
                                String availability = formCursor
                                        .getString(formCursor
                                                .getColumnIndex(FormsColumns.AVAILABILITY_STATUS));
                                if (FormsProviderAPI.STATUS_UNAVAILABLE.equals(availability)) {
                                    this.createErrorDialog(
                                            getString(R.string.parent_form_not_present, jrFormId)
                                                    + ((jrVersion == null) ? ""
                                                    : "\n" + getString(R.string.version) + " " + jrVersion),
                                            EXIT);
                                    return;
                                }
                                mFormPath = formCursor
                                        .getString(formCursor
                                                .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                            } else if (formCursor.getCount() > 1) {
                                // still take the first entry, but warn that
                                // there are multiple rows.
                                // user will need to hand-edit the SQLite
                                // database to fix it.
                                formCursor.moveToFirst();
                                mFormPath = formCursor
                                        .getString(formCursor
                                                .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                                this.createErrorDialog(
                                        getString(R.string.survey_multiple_forms_error),
                                        EXIT);
                                return;
                            }
                        } finally {
                            if (formCursor != null) {
                                formCursor.close();
                            }
                        }
                    }
                } else if (getContentResolver().getType(uri).equals(
                        FormsColumns.CONTENT_ITEM_TYPE)) {
                    Cursor c = null;
                    try {
                        c = getContentResolver().query(uri, null, null, null,
                                null);
                        if (c.getCount() != 1) {
                            this.createErrorDialog("Bad URI: " + uri, EXIT);
                            return;
                        } else {
                            c.moveToFirst();
                            mFormPath = c
                                    .getString(c
                                            .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                            // This is the fill-blank-form code path.
                            // See if there is a savepoint for this form that
                            // has never been
                            // explicitly saved
                            // by the user. If there is, open this savepoint
                            // (resume this filled-in
                            // form).
                            // Savepoints for forms that were explicitly saved
                            // will be recovered
                            // when that
                            // explicitly saved instance is edited via
                            // edit-saved-form.
                            final String filePrefix = mFormPath.substring(
                                    mFormPath.lastIndexOf('/') + 1,
                                    mFormPath.lastIndexOf('.'))
                                    + "_";
                            final String fileSuffix = ".xml.save";
                            File cacheDir = new File(Collect.CACHE_PATH);

                            File[] files = cacheDir.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File pathname) {
                                    String name = pathname.getName();
                                    return name.startsWith(filePrefix)
                                            && name.endsWith(fileSuffix);
                                }
                            });
                            // see if any of these savepoints are for a
                            // filled-in form that has never been
                            // explicitly saved by the user...
                            for (int i = 0; i < files.length; ++i) {
                                File candidate = files[i];
                                String instanceDirName = candidate.getName()
                                        .substring(
                                                0,
                                                candidate.getName().length()
                                                        - fileSuffix.length());
                                File instanceDir = new File(
                                        Collect.INSTANCES_PATH + File.separator
                                                + instanceDirName);
                                File instanceFile = new File(instanceDir,
                                        instanceDirName + ".xml");
                                if (instanceDir.exists()
                                        && instanceDir.isDirectory()
                                        && !instanceFile.exists()) {
                                    // yes! -- use this savepoint file
                                    instancePath = instanceFile
                                            .getAbsolutePath();
                                    qaPath = RTASurvey.QA_INSTANCE_PATH
                                            + File.separator
                                            + instanceFile.getParentFile()
                                            .getName()
                                            + RTASurvey.QA_INSTANCE_FILE_POSFIX;
                                    break;
                                }
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                } else {
                    Log.e(t, "unrecognized URI");
                    this.createErrorDialog("Unrecognized URI: " + uri, EXIT);
                    return;
                }
                mFormLoaderTask = new FormLoaderTask(instancePath, qaPath, null, null);
                showDialog(PROGRESS_DIALOG);
                // show dialog before we execute...
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mFormLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFormPath);
                } else {
                    mFormLoaderTask.execute(mFormPath);
                }
            }
        }

        Display display =
                ((WindowManager) getSystemService(
                        Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();

        /*
        if (!PreferencesManager.getPreferencesManager(RTASurvey.getInstance())
                .getBoolean(_PreferencesActivity.KEY_DISABLE_FULL_SCREEN, false)) {
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0 && isImmersiveFullscreen) {
                        ScreenUtils.enterFullscreen();
                    }
                }
            });
        }
        */

        callReceiver = new CallBroadcastReceiver(this);
        callIntentFilter = new IntentFilter(CallRecordWidgets.INTENT_CALL_RECORD);
    }

    private void setUpPosition(View view) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        params.setMargins(0, height / 4, 0, 0);
        view.setLayoutParams(params);
    }

    //Audio Media Player
    public void showMediaPlayer(Uri uri) {
        mThreadPool = Executors.newCachedThreadPool();
        Animation SnackbarShow = AnimationUtils.loadAnimation(this, R.anim.snackbar_show_animation);
        final Animation SnackbarHide = AnimationUtils.loadAnimation(this, R.anim.snackbar_hide_animation);
        final RelativeLayout layout_media = (RelativeLayout) findViewById(R.id.snacbar_player);
        if (mNextButton.getVisibility() == View.GONE || mBackButton.getVisibility() == View.GONE) {
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            p.addRule(RelativeLayout.ABOVE, layout_media.getId());
            mQuestionHolder.setLayoutParams(p);

        }
        layout_media.setVisibility(View.VISIBLE);
        layout_media.setAnimation(SnackbarShow);
        mUri = uri;
        String scheme = mUri.getScheme();
        File filePlayer = new File(mUri.getPath());
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        double size = filePlayer.length() / 1024;
        String length = "" + decimalFormat.format(size) + "kb";
        final MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(filePlayer.getAbsolutePath());
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mTextLine1 = (TextView) findViewById(R.id.line1);
        mTextLine2 = (TextView) findViewById(R.id.line2);
        mTextTimer = (TextView) findViewById(R.id.txt_timer);
        mLoadingText = (TextView) findViewById(R.id.loading);
        mTextSize = (TextView) findViewById(R.id.txtsize);
        closePlayer = (ImageButton) findViewById(R.id.close_player);
        closePlayer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayback();
                layout_media.setAnimation(SnackbarHide);
                layout_media.setVisibility(View.GONE);

            }
        });
        mTextLine1.setText(filePlayer.getName());
        mTextSize.setText("Size: " + "" + length);

        if (scheme.equals("http")) {
            String msg = getString(R.string.streamloadingtext, mUri.getHost());
            mLoadingText.setText(msg);
        } else {
            mLoadingText.setVisibility(View.GONE);
        }
        mSeekBar = (SeekBar) findViewById(R.id.progress);
        mProgressRefresher = new Handler();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        PreviewPlayer player = (PreviewPlayer) getLastNonConfigurationInstance();
        if (player == null) {
            mPlayer = new PreviewPlayer();
            mPlayer.setActivity(this);
            try {
                mPlayer.setDataSourceAndPrepare(uri);
            } catch (Exception ex) {
                Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            mPlayer = player;
            mPlayer.setActivity(this);
            if (mPlayer.isPrepared()) {
                showPostPrepareUI();
            }
        }

        timer();


        AsyncQueryHandler mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {

                    int titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int displaynameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (idIdx >= 0) {
                        mMediaId = cursor.getLong(idIdx);
                    }

                    if (titleIdx >= 0) {
                        String title = cursor.getString(titleIdx);
                        mTextLine1.setText(title);
                        if (artistIdx >= 0) {
                            String artist = cursor.getString(artistIdx);
                            mTextLine2.setText(artist);
                        }
                    } else if (displaynameIdx >= 0) {
                        String name = cursor.getString(displaynameIdx);
                        mTextLine1.setText(name);
                    } else {
                        // Couldn't find anything to display, what to do now?
                    }
                } else {
                }

                if (cursor != null) {
                    cursor.close();
                }
                setNames();
            }
        };

        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (uri.getAuthority() == MediaStore.AUTHORITY) {
                // try to get title and artist from the media content provider
                mAsyncQueryHandler.startQuery(0, null, uri, new String[]{
                                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                        null, null, null);
            } else {
                // Try to get the display name from another content provider.
                // Don't specifically ask for the display name though, since the
                // provider might not actually support that column.
                mAsyncQueryHandler.startQuery(0, null, uri, null, null, null, null);
            }
        } else if (scheme.equals("file")) {
            // check if this file is in the media database (clicking on a download
            // in the download manager might follow this path
            String path = uri.getPath();
            mAsyncQueryHandler.startQuery(0, null, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DATA + "=?", new String[]{path}, null);
        } else {
            // We can't get metadata from the file/stream itself yet, because
            // that API is hidden, so instead we display the URI being played
            if (mPlayer.isPrepared()) {
                setNames();
            }
        }
    }

    private void showPostPrepareUI() {
        mDuration = mPlayer.getDuration();
        if (mDuration != 0) {
            mSeekBar.setMax(mDuration);
            mSeekBar.setVisibility(View.VISIBLE);
        }
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        mLoadingText.setVisibility(View.GONE);
        View v = findViewById(R.id.titleandbuttons);
        v.setVisibility(View.VISIBLE);
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        updatePlayPause();
    }

    private void timer() {
        final Timer timer = new Timer();
        countTimer = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (countTimer <= duration / 1000) {
                            String s_time = String.format("%02d:%02d:%02d",
                                    countTimer / 3600,
                                    (countTimer % 3600) / 60,
                                    countTimer % 60);
                            mTextTimer.setText(s_time + "/" + StringFormatUtils.formatDuration(duration));
                            if (!ispause) {
                                countTimer++;
                            }
                            if (countTimer == duration / 1000 + 1) {
                                timer.cancel();
                            }
                        }

                    }
                });
            }
        }, 0, 1000);
    }

    private void start() {
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mPlayer.start();
        mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
    }

    public void setNames() {
        if (TextUtils.isEmpty(mTextLine1.getText())) {
            mTextLine1.setText(mUri.getLastPathSegment());
        }
        if (TextUtils.isEmpty(mTextLine2.getText())) {
            mTextLine2.setVisibility(View.GONE);
        } else {
            mTextLine2.setVisibility(View.VISIBLE);
        }
    }

    private void updatePlayPause() {
        ImageButton b = (ImageButton) findViewById(R.id.playpause);
        if (b != null) {
            if (mPlayer.isPlaying()) {
                b.setBackgroundResource(R.drawable.ic_pause_player);
                //b.setImageResource(R.drawable.ic_action_pause);
            } else {
                b.setBackgroundResource(R.drawable.ic_play_player);
                //b.setImageResource(R.drawable.ic_action_play);
                mProgressRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    /**
     * Create save-points asynchronously in order to not affect swiping
     * performance on larger forms.
     */
    private void nonblockingCreateSavePointData() {
        Intent intent = getIntent();
        if (null != intent) {
            Uri uri = intent.getData();
            if (InstanceColumns.CONTENT_ITEM_TYPE.equals(getContentResolver().getType(uri))) {
                String status = null;
                Cursor instanceCursor = null;
                try {
                    instanceCursor = getContentResolver().query(uri, null,
                            null, null, null);
                    if (instanceCursor != null) {
                        if (instanceCursor.getCount() != 1) {
                            this.createErrorDialog("Bad URI: " + uri, EXIT);
                            instanceCursor.close();
                            return;
                        } else {
                            instanceCursor.moveToFirst();
                            status = instanceCursor.getString(instanceCursor
                                    .getColumnIndex(InstanceColumns.STATUS));
                        }
                    } else {
                        this.createErrorDialog("Bad URI: " + uri, EXIT);
                        return;
                    }

                    if (status != null
                            && (status.equals(InstanceProviderAPI.STATUS_RETURNED)
                            || status.equals(InstanceProviderAPI.STATUS_INCOMPLETE_RETURNED)
                            || status.equals(InstanceProviderAPI.STATUS_BASELINE)
                            || status.equals(InstanceProviderAPI.STATUS_INCOMPLETE_BASELINE)
                            || status.equals(InstanceProviderAPI.STATUS_INCOMPLETE_NEW_CREATE)
                            || status.equals(InstanceProviderAPI.STATUS_INCOMPLETE)
                            || status.equals(InstanceProviderAPI.STATUS_COMPLETE))) {
                        String instanceUUID = instanceCursor.getString(instanceCursor
                                .getColumnIndex(InstanceColumns.INSTANCE_UUID));

                        if (instanceUUID != null) {
                            mFormController.setSubmissionInstanceId(instanceUUID);
                            mFormController.setUuid(instanceUUID);
                        } else {
                            Log.e(t, "Cannot find uuid of this instance");
                        }
                    } else {
                        Log.e(t, "Error: instance status not found");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (instanceCursor != null) {
                        instanceCursor.close();
                    }
                }
            }
        }

        try {
            SavePointTask savePointTask = new SavePointTask(mFormController, this);
            savePointTask.execute();
        } catch (Exception e) {
            Log.e(t, "Could not schedule SavePointTask. Perhaps a lot of swiping is taking place?");
        }
    }

    public void reInit(Uri uri, boolean newFamily) {
        mBeenSwiped = false;
        Collect.getInstance().setFormController(null);
        CompatibilityUtils.invalidateOptionsMenu(this);
        CheckNoteView.setOnupdateMenuQA(this);
        if (getContentResolver().getType(uri).equals(
                FormsColumns.CONTENT_ITEM_TYPE)) {
            Cursor c = null;
            try {
                c = getContentResolver().query(uri, null, null, null,
                        null);
                if (c.getCount() != 1) {
                    this.createErrorDialog("Bad URI: " + uri, EXIT);
                    return;
                } else {
                    c.moveToFirst();
                    mFormPath = c
                            .getString(c
                                    .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null)
                    c.close();
            }
        }

        if (!newFamily) {
            mReFormLoaderTask = new ReFormLoaderTask();
            mReFormLoaderTask.setFormLoaderListener(this);
            mReFormLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFormPath);

            SurveyCollectorUtils.cleanLatestInstanceConfig();

            if (mSaveToDiskTask != null) {
                mSaveToDiskTask.setFormSavedListener(this);
            }
        } else {
            if (getContentResolver().getType(uri).equals(
                    FormsColumns.CONTENT_ITEM_TYPE)) {
                Cursor c = null;
                String instancePath = null;
                String qaPath = null;
                try {
                    c = getContentResolver().query(uri, null, null, null,
                            null);
                    if (c.getCount() != 1) {
                        this.createErrorDialog("Bad URI: " + uri, EXIT);
                        return;
                    } else {
                        c.moveToFirst();
                        mFormPath = c
                                .getString(c
                                        .getColumnIndex(FormsColumns.FORM_FILE_PATH));
                        // This is the fill-blank-form code path.
                        // See if there is a savepoint for this form that
                        // has never been
                        // explicitly saved
                        // by the user. If there is, open this savepoint
                        // (resume this filled-in
                        // form).
                        // Savepoints for forms that were explicitly saved
                        // will be recovered
                        // when that
                        // explicitly saved instance is edited via
                        // edit-saved-form.
                        final String filePrefix = mFormPath.substring(
                                mFormPath.lastIndexOf('/') + 1,
                                mFormPath.lastIndexOf('.'))
                                + "_";
                        final String fileSuffix = ".xml.save";
                        File cacheDir = new File(Collect.CACHE_PATH);

                        File[] files = cacheDir.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                String name = pathname.getName();
                                return name.startsWith(filePrefix)
                                        && name.endsWith(fileSuffix);
                            }
                        });
                        // see if any of these savepoints are for a
                        // filled-in form that has never been
                        // explicitly saved by the user...
                        for (int i = 0; i < files.length; ++i) {
                            File candidate = files[i];
                            String instanceDirName = candidate.getName()
                                    .substring(
                                            0,
                                            candidate.getName().length()
                                                    - fileSuffix.length());
                            File instanceDir = new File(
                                    Collect.INSTANCES_PATH + File.separator
                                            + instanceDirName);
                            File instanceFile = new File(instanceDir,
                                    instanceDirName + ".xml");
                            if (instanceDir.exists()
                                    && instanceDir.isDirectory()
                                    && !instanceFile.exists()) {
                                instancePath = instanceFile
                                        .getAbsolutePath();
                                qaPath = RTASurvey.QA_INSTANCE_PATH
                                        + File.separator
                                        + instanceFile.getParentFile()
                                        .getName()
                                        + RTASurvey.QA_INSTANCE_FILE_POSFIX;
                                break;
                            }
                        }
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                //Log.d(t, "load data of form, begin form");
                mFormLoaderTask = new FormLoaderTask(instancePath, qaPath, null, null);
                Collect.getInstance().getActivityLogger().logAction(this, "formLoaded", mFormPath);
                //showDialog(PROGRESS_DIALOG);
                // show dialog before we execute...
                mFormLoaderTask.setFormLoaderListener(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mFormLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFormPath);
                } else {
                    mFormLoaderTask.execute(mFormPath);
                }

                if (!isViewOnly && isSentInstance) {
                    sendSavedBroadcast(IRSAction.ACTION_SEND_FINALIZE_INSTANCE, IdInstanceSend);
                }


            } else {
                Log.e(t, "unrecognized URI");
                this.createErrorDialog("Unrecognized URI: " + uri, EXIT);
                return;
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        RecordManager.stopAll(this);

        String instancePath = "";
        outState.putString(KEY_FORMPATH, mFormPath);
        outState.putString(KEY_ERROR, mErrorMessage);
        outState.putBoolean(NEWFORM, false);
        outState.putBoolean(KEY_AUTO_SAVED, mAutoSaved);
        if (mFormController != null) {
            instancePath = mFormController.getInstancePath().getAbsolutePath();
            ProcessDbHelper.getInstance().removeProcess(mFormController.getUuid());
            outState.putString(KEY_INSTANCEPATH, mFormController.getInstancePath().getAbsolutePath());
            try {
                outState.putString(KEY_QAPATH, mFormController.getQaFolder().getAbsolutePath());
                outState.putString(KEY_EXTERNALDATAPATH, mFormController.getmExternalInstancesPath().getAbsolutePath());
                outState.putString(KEY_XPATH, mFormController.getXPath(mFormController.getFormIndex()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            FormIndex waiting = mFormController.getIndexWaitingForData();
            if (waiting != null) {
                outState.putString(KEY_XPATH_WAITING_FOR_DATA, mFormController.getXPath(waiting));
            }
        }

        if (!isViewOnly) {
            //save the instance to a temp path...
            nonblockingCreateSavePointData();
            updateInstanceConfigFile(instancePath);
            updateInstanceLastIndex();
        }

    }

    private void updateInstanceConfigFile(String instancePath) {
        if (!getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false)
                && instancePath != null && !instancePath.equals("")) {
            SurveyCollectorUtils.cleanLatestInstanceConfig();
            Utils.writeToFile(RTASurvey.FORM_LOGGER_PATH, CONFIG_FILE_NAME, instancePath);
        }
        isSaveConfig = true;
    }

    //Update instance latest indexg
    private void updateInstanceLastIndex() {
        if (mFormController != null) {
            String lastUpdatedIndex = getLastUpdateIndex();
            ContentValues values = new ContentValues();
            if (lastUpdatedIndex == null)
                values.putNull(InstanceColumns.LAST_UPDATED_INDEX);
            else
                values.put(InstanceColumns.LAST_UPDATED_INDEX, lastUpdatedIndex);
            getContentResolver().update(InstanceColumns.CONTENT_URI,
                    values, InstanceColumns.INSTANCE_UUID + " = ?",
                    new String[]{mFormController.getUuid()});
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        isOnActivityResult = true;

        if (mFormController == null) {
            // we must be in the midst of a reload of the FormController.
            // try to save this callback data to the FormLoaderTask
            if (mFormLoaderTask != null
                    && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
                mFormLoaderTask.setActivityResult(requestCode, resultCode,
                        intent);
            } else {
                Log.e(t,
                        "Got an activityResult without any pending form loader");
            }
            return;
        }

        if (resultCode == RESULT_CANCELED) {
            // request was canceled...
            if (requestCode != HIERARCHY_ACTIVITY) {
                ((RTAView) mCurrentView).cancelWaitingForBinaryData();
            } else {
                isShowFromtList = false;
                if (vn.rta.survey.android.ui.formhierarchy.FormHierarchyActivity.isShowFirst) {
                    String uuid = mFormController.getUuid();
                    if (!TextUtils.isEmpty(uuid)) {
                        numberError = QACheckPointData.getNoErrorOfInstance(uuid);
                    }

                    if (numberError > 0 && !isViewOnly) {
                        QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                        checkNoteManager.showCheckNote();
                    }
                    invalidateOptionsMenu();
                }

                //refreshCurrentView();
            }
            return;
        }

        String index = "";
        boolean isCombo = getIntent().getBooleanExtra(RTAView.NOTIFY_COMBO, false);
        getIntent().putExtra(RTAView.NOTIFY_COMBO, false);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Intent serverIntent = new Intent(this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    return;
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = intent.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    if (bluetoothService != null && bluetoothService.isAvailable()) {
                        try {
                            BluetoothDevice devByMac = bluetoothService.getDevByMac(address);
                            bluetoothService.connect(devByMac);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Choose Device to connect!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                return;
            case BARCODE_CAPTURE:
                try {
                    index = this.getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String sb = intent.getStringExtra("SCAN_RESULT");
                action_qrapi = intent.getStringExtra("SCAN_ACTION");
                ((RTAView) mCurrentView).setBinaryData(sb, index);
                if (isCombo) {
                    String questionName = intent.getStringExtra(RTAView.REFER_QUESTION_COMBO);
                    ((RTAView) mCurrentView).setComboData(sb, questionName);
                }
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);

                break;
            case OSM_CAPTURE:

                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String osmFileName = intent.getStringExtra("OSM_FILE_NAME");
                ((RTAView) mCurrentView).setBinaryData(osmFileName, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case INLINEBARCODE_CAPTURE:
                try {
                    index = this.getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String isb = intent.getStringExtra("SCAN_RESULT");
                ((RTAView) mCurrentView).setBinaryData(isb, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case EX_STRING_CAPTURE:
            case EX_INT_CAPTURE:
            case EX_DECIMAL_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String key = "value";
                boolean exists = intent.getExtras().containsKey(key);
                if (exists) {
                    Object externalValue = intent.getExtras().get(key);
                    ((RTAView) mCurrentView).setBinaryData(externalValue, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
                break;
            case EX_GROUP_CAPTURE:
                try {
                    Bundle extras = intent.getExtras();
                    ((RTAView) mCurrentView).setDataForFields(extras);
                } catch (JavaRosaException e) {
                    Log.e(t, e.getMessage(), e);
                    createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
                }
                break;
            case DRAW_IMAGE:
            case ANNOTATE_IMAGE:
            case SIGNATURE_CAPTURE:
            case IMAGE_CAPTURE:
                // //////////
            case FACE_IMAGE:
                // //////////
            /*
             * We saved the image to the tempfile_path, but we really want it to
			 * be in: /sdcard/odk/instances/[current instance]/something.jpg so
			 * we move it there before inserting it into the content provider.
			 * Once the android image capture bug gets fixed, (read, we move on
			 * from Android 1.6) we want to handle images the audio and video
			 */
                // The intent is empty, but we know we saved the image to the temp
                // file
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Uri imageURI = intent.getData();
                if (imageURI != null) {
                    String pathImageLegacy = imageURI.getPath();
                    //RTALog.d("test image", " test path is = " + pathImageLegacy);
                    File ifiLegacy = new File(pathImageLegacy);
                    String miInstanceFolderLegacy = mFormController.getInstancePath().getParent();
//                String isLegacy = miInstanceFolderLegacy + File.separator
//                        + System.currentTimeMillis() + ".jpg";
                    String isLegacy = miInstanceFolderLegacy + File.separator
                            + System.currentTimeMillis();

                    File infLegacy = new File(isLegacy);
                    if (!infLegacy.exists()) {
                        Log.e(t, ifiLegacy.getAbsolutePath() + " this is not exists");
                    }
               /* if (!ifiLegacy.renameTo(infLegacy)) {
                    Log.e(t, "Failed to rename " + ifiLegacy.getAbsolutePath());
                } else {
                    Log.i(t,
                            "renamed " + ifiLegacy.getAbsolutePath() + " to "
                                    + infLegacy.getAbsolutePath());
                }
                ifiLegacy.delete();*/
                    try {
                        org.apache.commons.io.FileUtils.moveFile(ifiLegacy, infLegacy);
                    } catch (IOException e) {
                        Log.e(t, "Failed to move file to " + infLegacy.getAbsolutePath());
                        e.printStackTrace();
                        break;
                    }
                    ((RTAView) mCurrentView).setBinaryData(infLegacy, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                } else {
                    ((RTAView) mCurrentView).setBinaryData(null, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                }

            case INLINEIMAGE_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Uri inline_imageURI = intent.getData();
                if (inline_imageURI != null) {
                    String pathImage = inline_imageURI.getPath();
//                RTALog.d("test image", " test paht is = " + pathImage);
                    File ifi = new File(pathImage);
                    String miInstanceFolder = mFormController.getInstancePath()
                            .getParent();
//                String is = miInstanceFolder + File.separator
//                        + System.currentTimeMillis() + ".jpg";
                    String is = miInstanceFolder + File.separator
                            + System.currentTimeMillis();

                    File inf = new File(is);
                    if (!ifi.renameTo(inf)) {
                        Log.e(t, "Failed to rename " + ifi.getAbsolutePath());
                    } else {
                        Log.i(t,
                                "renamed " + ifi.getAbsolutePath() + " to "
                                        + inf.getAbsolutePath());
                    }
                    ifi.delete();
                    ((RTAView) mCurrentView).setBinaryData(inf, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                } else {
                    ((RTAView) mCurrentView).setBinaryData(null, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                }
            case ALIGNED_IMAGE:
            /*
             * We saved the image to the tempfile_path; the app returns the full
			 * path to the saved file in the EXTRA_OUTPUT extra. Take that file
			 * and move it into the instance folder.
			 */
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String path = intent
                        .getStringExtra(MediaStore.EXTRA_OUTPUT);
                File fi = new File(path);
                String mInstanceFolder = mFormController.getInstancePath().getParent();
//                String s = mInstanceFolder + File.separator + System.currentTimeMillis()
//                        + ".jpg";

                String s = mInstanceFolder + File.separator + System.currentTimeMillis();

                File nf = new File(s);
                if (!fi.renameTo(nf)) {
                    Log.e(t, "Failed to rename " + fi.getAbsolutePath());
                } else {
                    Log.i(t,
                            "renamed " + fi.getAbsolutePath() + " to "
                                    + nf.getAbsolutePath());
                }

                ((RTAView) mCurrentView).setBinaryData(nf, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case IMAGE_CHOOSER:
            /*
             * We have a saved image somewhere, but we really want it to be in:
			 * /sdcard/odk/instances/[current instnace]/something.jpg so we move
			 * it there before inserting it into the content provider. Once the
			 * android image capture bug gets fixed, (read, we move on from
			 * Android 1.6) we want to handle images the audio and video
			 */
//                try {
//                    //index = intent.getExtras().getString(RTAView.BINARY_QUESTION_INDEX);
//                    index = mFormController.getFormIndex().toString();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                    if (index == null)
                        index = this.getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // get gp of chosen file
                Uri selectedImage = intent.getData();
                try {
                    String sourceImagePath = MediaUtils.getPathFromUri(this,
                            selectedImage, Images.Media.DATA);
                    // Copy file to sdcard
                    String mInstanceFolder1 = mFormController.getInstancePath()
                            .getParent();
//                    String destImagePath = mInstanceFolder1 + File.separator
//                            + System.currentTimeMillis() + ".jpg";
                    String destImagePath = mInstanceFolder1 + File.separator + System.currentTimeMillis();

                    File source = new File(sourceImagePath);
                    File newImage = new File(destImagePath);
                    Log.d("DestFile", newImage.toString());
                    //FileUtils.copyFile(source, newImage);
                    try {
                        org.apache.commons.io.FileUtils.copyFile(source, newImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ((RTAView) mCurrentView).setBinaryData(newImage, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                } catch (NullPointerException e) {
                    createErrorDialog("Can't load image form gallery", false);
                }

                break;

            case IMAGE_TO_TEXT:
            /*
             * We have a saved image somewhere, but we really want it to be in:
			 * /sdcard/odk/instances/[current instnace]/something.jpg so we move
			 * it there before inserting it into the content provider. Once the
			 * android image capture bug gets fixed, (read, we move on from
			 * Android 1.6) we want to handle images the audio and video
			 */
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // get gp of chosen file
                File fi_i2t = new File(Collect.TMPFILE_PATH);
                String mInstanceFolder_i2t = mFormController.getInstancePath()
                        .getParent();

//                String s_i2t = mInstanceFolder_i2t + File.separator
//                        + System.currentTimeMillis() + ".jpg";

                String s_i2t = mInstanceFolder_i2t + File.separator + System.currentTimeMillis();

                File nf_i2t = new File(s_i2t);
                if (!fi_i2t.renameTo(nf_i2t)) {
                    Log.e(t, "Failed to rename " + fi_i2t.getAbsolutePath());
                } else {
                    Log.i(t,
                            "renamed " + fi_i2t.getAbsolutePath() + " to "
                                    + nf_i2t.getAbsolutePath());
                }

                String textResult = intent.getStringExtra("answer");
                int screenwidth = intent.getIntExtra("imgewidth", -1);
                int screenheight = intent.getIntExtra("imgeheight", -1);
                int width = intent.getIntExtra("width", -1);
                int height = intent.getIntExtra("height", -1);
                ((RTAView) mCurrentView).setBinaryImagetoTextData(nf_i2t);
                ((RTAView) mCurrentView).setRectData(screenwidth, screenheight, width, height);
                ((RTAView) mCurrentView).setTextAnswer(textResult);
                ((RTAView) mCurrentView).setTextResult(textResult);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case AUDIO_CAPTURE:


            case VIDEO_CAPTURE:
                //For capture not copy, move file
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                    if (index == null)
                        index = this.getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // For audio/video capture/chooser, we get the URI from the content
                // provider
                // then the widget copies the file and makes a new entry in the
                // content provider.
                Uri capturedVideo = intent.getData();
                if (capturedVideo != null) {
                    String pathSrcVideo = capturedVideo.getPath();
                    File srcVideoFile = new File(pathSrcVideo);
                    String instanceFolderVideo = mFormController.getInstancePath()
                            .getParent();
                    String pathDestVideo = instanceFolderVideo + File.separator
                            + System.currentTimeMillis();

                    File destVideoFile = new File(pathDestVideo);
                    if (!destVideoFile.exists()) {
                        Log.e(t, destVideoFile.getAbsolutePath() + " this is not exists");
                    }

                    try {
                        org.apache.commons.io.FileUtils.moveFile(srcVideoFile, destVideoFile);
                    } catch (IOException e) {
                        Log.e(t, "Failed to move file to " + destVideoFile.getAbsolutePath());
                        e.printStackTrace();
                        break;
                    }
                    ((RTAView) mCurrentView).setBinaryData(destVideoFile, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                }
                break;
            case AUDIO_CHOOSER:
            case VIDEO_CHOOSER:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                    if (index == null)
                        index = this.getIntent().getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // For audio/video capture/chooser, we get the URI from the content
                // provider
                // then the widget copies the file and makes a new entry in the
                // content provider.
                Uri media = intent.getData();
                String pathVideoLegacy = MediaUtils.getPathFromUri(this, media, MediaStore.Video.Media.DATA);
                //RTALog.d("test image", " test path is = " + pathImageLegacy);
                File ifiLegacys = new File(pathVideoLegacy);
                String miInstanceFolderLegacys = mFormController.getInstancePath()
                        .getParent();
//                String isLegacys = miInstanceFolderLegacys + File.separator
//                        + System.currentTimeMillis() + ".mp4";
                String isLegacys = miInstanceFolderLegacys + File.separator
                        + System.currentTimeMillis();

                File infLegacys = new File(isLegacys);
                if (!ifiLegacys.exists()) {
                    Log.e(t, ifiLegacys.getAbsolutePath() + " this is not exists");
                }

                try {
                    //org.apache.commons.io.FileUtils.moveFile(ifiLegacys, infLegacys);
                    //copy file used for both choose and capture
                    org.apache.commons.io.FileUtils.copyFile(ifiLegacys, infLegacys);
                } catch (IOException e) {
                    Log.e(t, "Failed to move file to " + infLegacys.getAbsolutePath());
                    e.printStackTrace();
                    break;
                }
                ((RTAView) mCurrentView).setBinaryData(infLegacys, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case INLINEAUDIO_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Uri imedia_a = intent.getData();
                ((RTAView) mCurrentView).setBinaryData(imedia_a, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case INLINEVIDEO_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Uri imedia_v = intent.getData();
                if (imedia_v != null) {
                    String pathVideo = imedia_v.getPath();
                    File srcFile = new File(pathVideo);
                    String videoInstancePath = mFormController.getInstancePath().getParent() + File.separator + System.currentTimeMillis();

                    File destFile = new File(videoInstancePath);

                    if (!srcFile.renameTo(destFile)) {
                        Log.e(t, "Failed to rename " + srcFile.getAbsolutePath());
                    } else {
                        Log.i(t,
                                "renamed " + srcFile.getAbsolutePath() + " to "
                                        + destFile.getAbsolutePath());
                    }
                    if (srcFile.exists()) {
                        srcFile.delete();
                    }

                    ((RTAView) mCurrentView).setBinaryData(destFile, index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                } else {
                    ((RTAView) mCurrentView).setBinaryData(" ", index);
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                    break;
                }

            case LOCATION_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String sl = intent.getStringExtra(LOCATION_RESULT);
                ((RTAView) mCurrentView).setBinaryData(sl, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case INLINELOCATION_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String isl = intent.getStringExtra(LOCATION_RESULT);
                ((RTAView) mCurrentView).setBinaryData(isl, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case BEARING_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String bearing = intent.getStringExtra(BEARING_RESULT);
                ((RTAView) mCurrentView).setBinaryData(bearing, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            /*case INLINEIMAGE_ALBUM_CAPTURE: {
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                String imageList = intent.getStringExtra("result");
                //cut tag and copy to file no tag
                String[] arrayName = cutTagImageName(imageList);

                imageList = Arrays.toString(arrayName).replace(",", "");
                imageList = imageList.replace("[", "");
                imageList = imageList.replace("]", "");
                ((RTAView) mCurrentView).setBinaryData(imageList, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                //mFormController.saveAlbumImage();
                break;
            }*/
            case ALBUM_CAPTURE: {
                FormIndex albumPromptIndex =
                        (FormIndex) intent.getSerializableExtra(RTAView.BINARY_QUESTION_INDEX);
                FormIndex albumIndex = albumPromptIndex;
                if (albumPromptIndex != null) {
                    String result = intent.getStringExtra("result");
                    String[] imgNameArray = cutTagImageName(result);

                    FormIndex beforeIndex = mFormController.getFormIndex();
                    for (String img : imgNameArray) {
                        mFormController.newRepeat(albumIndex);
                        mFormController.jumpToIndex(albumIndex);
                        int event = mFormController.stepToNextEvent(true);
                        if (event == FormEntryController.EVENT_QUESTION) {
                            FormIndex dataIndex = mFormController.getFormIndex();
                            IAnswerData data = new StringData(img);
                            try {
                                mFormController.saveAnswer(dataIndex, data);
                            } catch (JavaRosaException e) {
                                e.printStackTrace();
                            }
                        }
                        mFormController.stepToNextEvent(true);
                        albumIndex = mFormController.getFormIndex();
                    }

                    for (WidgetAlternative wa : ((RTAView) mCurrentView).getWidgetAlternatives()) {
                        if (wa.getPrompt().getFormIndex().equals(albumPromptIndex)) {
                            wa.getPrompt().setFormIndex(albumIndex);
                            if (wa instanceof AlbumControlView) {
                                ((AlbumControlView) wa).updateThumbnailView();
                            }
                            break;
                        }
                    }
                    mFormController.jumpToIndex(beforeIndex);
                }

                break;
            }
            case ALBUM_UPDATE: {
                FormIndex albumPromptIndex =
                        (FormIndex) intent.getSerializableExtra(RTAView.BINARY_QUESTION_INDEX);
                List<String> deleteImages = intent.getStringArrayListExtra("deleted_images");
                if (albumPromptIndex != null && deleteImages != null && deleteImages.size() > 0) {
                    FormIndex albumIndex = albumPromptIndex;
                    FormIndex beforeIndex = mFormController.getFormIndex();
                    mFormController.jumpToIndex(albumIndex);

                    // move back to repeat event
                    while (mFormController.stepToPreviousEvent() == FormEntryController.EVENT_REPEAT) {
                        // move forward to question data next to repeat event
                        int e = mFormController.getmFormEntryController().stepToNextEvent();
                        if (e == FormEntryController.EVENT_QUESTION) {
                            FormIndex i = mFormController.getFormIndex();
                            FormEntryPrompt p = mFormController.getQuestionPrompt(i);
                            if (p != null) {
                                String answer = p.getAnswerText();
                                if (answer != null && !answer.equals("") && deleteImages.contains(answer)) {
                                    mFormController.deleteRepeat();
                                    deleteImages.remove(answer);
                                    continue;
                                }
                            }
                        }
                        // to repeat event, in order to continue traversal
                        mFormController.stepToPreviousEvent();
                    }

                    // step next to find the new prompt_new_repeat index
                    while (mFormController.getmFormEntryController().stepToNextEvent() != FormEntryController.EVENT_PROMPT_NEW_REPEAT)
                        ;
                    albumIndex = mFormController.getFormIndex();

                    for (WidgetAlternative wa : ((RTAView) mCurrentView).getWidgetAlternatives()) {
                        if (wa.getPrompt().getFormIndex().equals(albumPromptIndex)) {
                            wa.getPrompt().setFormIndex(albumIndex);
                            if (wa instanceof AlbumControlView) {
                                ((AlbumControlView) wa).updateThumbnailView();
                            }
                            break;
                        }
                    }
                    mFormController.jumpToIndex(beforeIndex);
                }

                break;
            }
            case INLINEVIDEO_ALBUMM_CAPTURE:
                try {
                    index = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String videoList = intent.getStringExtra("result");
                ((RTAView) mCurrentView).setBinaryData(videoList, index);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;

            case HIERARCHY_ACTIVITY:
                refreshScreenAfterJump();
                return;
            case TAGGING_SELECT_ONE_SEARCH:
                ((RTAView) mCurrentView).setBinaryData(intent);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case TAGGING_SELECT_MULTIPLE_SEARCH:
                ((RTAView) mCurrentView).setBinaryData(intent);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case VERIFY_FACEBOOK:
                ((RTAView) mCurrentView).setBinaryData(intent);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case GEOSHAPE_CAPTURE:
                //String ls = intent.getStringExtra(GEOSHAPE_RESULTS);
                String gshr = intent.getStringExtra(GEOSHAPE_RESULTS);
                String gshrIndex = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                ((RTAView) mCurrentView).setBinaryData(gshr, gshrIndex);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case GEOTRACE_CAPTURE:
                String traceExtra = intent.getStringExtra(GEOTRACE_RESULTS);
                String traceIndex = intent.getStringExtra(RTAView.BINARY_QUESTION_INDEX);
                ((RTAView) mCurrentView).setBinaryData(traceExtra, traceIndex);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
            case VIDEO_PLAYER:
                boolean DeletePlayer = intent.getBooleanExtra("IsDelete", false);
                ((RTAView) mCurrentView).setBinaryData(DeletePlayer);
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                break;
        }
        //refresh screen set answer and show new question if have relevant
        refreshCurrentView(index);

    }

    private String[] cutTagImageName(String imageList) {
        String[] arrayList = imageList.split(" ");
        String mInstanceFolder = mFormController.getInstancePath().getParent();
        for (int i = 0; i < arrayList.length; i++) {
            String beginName = arrayList[i];
            if (arrayList[i].contains(".")) {
                arrayList[i] = arrayList[i].substring(0, arrayList[i].indexOf("."));
            }

            //Copy here
            String selectedImage = mInstanceFolder + File.separator + beginName;
            Uri uriImage = Uri.fromFile(new File(selectedImage));

            if (!beginName.equals(arrayList[i])) {
                try {
                    String sourceImagePath = MediaUtils.getPathFromUri(this, uriImage, Images.Media.DATA);

                    String destImagePath = mInstanceFolder + File.separator + arrayList[i];

                    File source = new File(sourceImagePath);
                    File newImage = new File(destImagePath);

                    FileUtils.copyFile(source, newImage);

                    if (source.exists()) {
                        source.delete();
                    }
                } catch (NullPointerException e) {

                }
            }
        }


        return arrayList;
    }

    public void refreshCurrentView(String index) throws RuntimeException {
        FormEntryCaption[] groups = mFormController
                .getGroupsForCurrentIndex();

        boolean isGripView = false;
        if (groups.length > 0) {
            for (FormEntryCaption g : groups) {
                String appearance = g.getAppearanceHint();
                if (appearance != null && appearance.contains(RTAView.GRID)) {
                    isGripView = true;
                    break;
                }
            }
        }

        for (String s : mFormController.getIndexRelevants()) {
            if (s.contains(index)) {
                saveAnswersForCurrentScreen(false);
                if (!isGripView) {
                    refreshCurrentView();
                    return;
                }
                try {
                    RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
                    ((RTAView) mCurrentView).refreshView(prompts);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                }
                //refreshCurrentView();
                return;
            }
        }
        //only refresh label
        saveAnswersForCurrentScreen(false);
        updateLabelAndResetXpath();
    }

    /**
     * save and refresh current view
     */
    public void saveAndReFreshCurrentView(String refs, boolean mustRefesh, FormIndex index) throws RuntimeException {
        if (mustRefesh) {
            saveAnswersForCurrentScreen(false);
            refreshCurrentView();
            setupUIForNavigationButton();
        } else {

            saveAnswersForCurrentScreen(false);
            if (mFormController.getModel().isIndexRelevant(index)) {
                try {
                    RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
                    ((RTAView) mCurrentView).refreshView(prompts);
                    setupUIForNavigationButton();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                }
                return;
            }
            //only refresh label
            updateLabelAndResetXpath();
            setupUIForNavigationButton();
        }
    }

    public void showLotteryAnimation() {
        saveAnswersForCurrentScreen(false);
        refreshCurrentView();
    }

    public void saveAndrefeshWhenRemoveData() {
    }

    /**
     * Refreshes the current view. the controller and the displayed view can get
     * out of sync due to dialogs and restarts caused by screen orientation
     * changes, so they're resynchronized here.
     */
    public void refreshRepeatCurrentView() {
        if (RTASurvey.DEBUG) {
        }
        saveAnswersForCurrentScreen(false);
        int event = mFormController.getEvent();

        if (event == FormEntryController.EVENT_END_OF_FORM) {
            if (existEndSurvey) {
            }
        }
        View current = createView(event, false);
        showView(current, AnimationType.FADE);
        setProgress(mFormController.getPercentOfForm(), (mFormController.getQuestionsHaveFill().size() / (float) mFormController.getQuestionsList().size()) * 100);
    }

    /**
     * Refreshes the current view. the controller and the displayed view can get
     * out of sync due to dialogs and restarts caused by screen orientation
     * changes, so they're resynchronized here.
     */
    public void refreshCurrentView() {
        int event = mFormController.getEvent();

        // When we refresh, repeat dialog state isn't maintained, so step back
        // to the previous
        // question.
        // Also, if we're within a group labeled 'field list', step back to the
        // beginning of that
        // group.
        // That is, skip backwards over repeat prompts, groups that are not
        // field-lists,
        // repeat events, and indexes in field-lists that is not the containing
        // group.

        if (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
            createRepeatDialog();
        } else {
            View current = createView(event, false);
            showView(current, AnimationType.FADE);
        }
        setProgress(mFormController.getPercentOfForm(), (mFormController.getQuestionsHaveFill().size() / (float) mFormController.getQuestionsList().size()) * 100);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "onCreateOptionsMenu", "show");
        super.onCreateOptionsMenu(menu);
        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_FONT_SIZE, 0, R.string.font_size)
                        .setIcon(R.drawable.ic_action_226_icon),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);


        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_QUICK_NOTE, 0, R.string.quick_not).setIcon(
                        R.drawable.ic_menu_note),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);

        CompatibilityUtils.setShowAsAction(menu.add(0, MENU_QA_CHECKING, 0,
                R.string.qa_check).setIcon(getQADrawableId((int) numberError)),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);

        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_SAVE, 0, R.string.save_all_answers).setIcon(
                        R.drawable.ic_menu_save),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);

        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_HIERARCHY_VIEW, 0, R.string.view_hierarchy)
                        .setIcon(R.drawable.ic_menu_goto),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);

        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_LANGUAGES, 0, R.string.change_language)
                        .setIcon(R.drawable.ic_language_white_24dp),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);

        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_PREFERENCES, 0, R.string.general_preferences)
                        .setIcon(R.drawable.ic_menu_preferences),
                MenuItem.SHOW_AS_ACTION_NEVER);

        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_IMAGE_REPORT, 0, R.string.report_issue)
                        .setIcon(R.drawable.ic_menu_report_issue),
                MenuItem.SHOW_AS_ACTION_IF_ROOM);

        CompatibilityUtils.setShowAsAction(
                menu.add(0, MENU_REQUEST_EDIT, 0, R.string.btn_request_return_instance),
                MenuItem.SHOW_AS_ACTION_NEVER);


        return true;
    }

    private int getQADrawableId(int numberOfError) {
        switch (numberOfError) {
            case 0:
                return R.drawable.ic_menu_checking;
            case 1:
                return R.drawable.ic_menu_checking_1;
            case 2:
                return R.drawable.ic_menu_checking_2;
            case 3:
                return R.drawable.ic_menu_checking_3;
            case 4:
                return R.drawable.ic_menu_checking_4;
            case 5:
                return R.drawable.ic_menu_checking_5;
            case 6:
                return R.drawable.ic_menu_checking_6;
            case 7:
                return R.drawable.ic_menu_checking_7;
            case 8:
                return R.drawable.ic_menu_checking_8;
            case 9:
                return R.drawable.ic_menu_checking_9;
            case -1:
                return R.drawable.ic_menu_checking_not_ready;
        }
        return R.drawable.ic_menu_checking_10;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean changeTextFont = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_CHANGE_FONTSIZE, false);
        if (!changeTextFont) {
            menu.findItem(MENU_FONT_SIZE).setVisible(changeTextFont).setEnabled(changeTextFont);
        }

        boolean usability;

        usability = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_DISABLE_QUICK_NOTE, false);

        if (!usability) {
            usability = getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false);
        }

        menu.findItem(MENU_QUICK_NOTE).setVisible(!usability)
                .setEnabled(!usability);

        usability = PreferencesManager.getPreferencesManager(this).getBoolean(
                AdminPreferencesActivity.KEY_SAVE_MID, true);

        if (usability) {
            usability = !getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false);
        }
        menu.findItem(MENU_SAVE).setVisible(usability).setEnabled(usability);

        usability = PreferencesManager.getPreferencesManager(this).getBoolean(
                AdminPreferencesActivity.KEY_JUMP_TO, true);

        menu.findItem(MENU_HIERARCHY_VIEW).setVisible(usability)
                .setEnabled(usability);

        usability = PreferencesManager.getPreferencesManager(this).getBoolean(
                AdminPreferencesActivity.KEY_CHANGE_LANGUAGE, true)
                && (mFormController != null)
                && mFormController.getLanguages() != null
                && mFormController.getLanguages().length > 1;

        menu.findItem(MENU_LANGUAGES).setVisible(usability)
                .setEnabled(usability);

        usability = PreferencesManager.getPreferencesManager(this).getBooleanInAdmin(
                AdminPreferencesActivity.KEY_ACCESS_SETTINGS, false);

        menu.findItem(MENU_PREFERENCES).setVisible(usability)
                .setEnabled(usability);

        usability = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_DISABLE_REPORTISSUE, false);

        menu.findItem(MENU_IMAGE_REPORT).setVisible(!usability)
                .setEnabled(!usability);

        usability = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_DISABLE_QA_CHECK, false);
        if (!usability) {
            usability = getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false);
        }
        menu.findItem(MENU_QA_CHECKING).setVisible(!usability)
                .setEnabled(!usability);

        //hide "Request to edit" function whenever instance has been "request" before
        usability = canRequestToEdit();
        menu.findItem(MENU_REQUEST_EDIT).setVisible(usability).setEnabled(usability);

        return true;
    }

    private boolean canRequestToEdit() {
        boolean usability = getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false);
        if (usability) {
            ContentResolver cr = getContentResolver();
            Cursor ci = cr.query(getIntent().getData(),
                    new String[]{InstanceColumns.REQUEST_TO_EDIT_DATE, InstanceColumns.STATUS},
                    null, null, null);
            usability = ci != null && ci.moveToFirst()
                    && !ci.isNull(ci.getColumnIndex(InstanceColumns.STATUS))
                    && !ci.getString(ci.getColumnIndex(InstanceColumns.STATUS)).contains(InstanceProviderAPI.STATUS_INCOMPLETE)
                    && !ci.getString(ci.getColumnIndex(InstanceColumns.STATUS)).equals(InstanceProviderAPI.STATUS_SUP_TRANSFERRED)
                    && !ci.getString(ci.getColumnIndex(InstanceColumns.STATUS)).equals(InstanceProviderAPI.STATUS_ENUM_FEEDBACK)
                    && (ci.isNull(ci.getColumnIndex(InstanceColumns.REQUEST_TO_EDIT_DATE))
                    || ci.getLong(ci.getColumnIndex(InstanceColumns.REQUEST_TO_EDIT_DATE)) == 0);
            if (ci != null)
                ci.close();
        }
        return usability;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LANGUAGES:
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_LANGUAGES);
                createLanguageDialog();
                return true;
            case MENU_SAVE:
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_SAVE);

                //track mini log open form
                MiniLogFormAction miniLogFormAction = MiniLogFormAction.getMinilogAction(mFormController.getUuid());
                miniLogFormAction.writeLog(MiniLogEntity.BUTTON_SAVE_CODE, System.currentTimeMillis());

                nameofInstance = getInstanceDisplayName(mFormController);

                // don't exit
                //update save info for uuid;
                saveDataToDisk(DO_NOT_EXIT, isInstanceComplete(false), nameofInstance);
                mFormController.keepBackupInstanceTree();
                //not save finalize
                updateInstanceplushInfor();
                return true;
            case MENU_HIERARCHY_VIEW:
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_HIERARCHY_VIEW);

                if (mFormController.currentPromptIsQuestion()) {
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);

                    String[] questionName = mFormController.getFormIndex().getReference().toString().split("/");

                    if (!currentRepeatName.equals("")) {
                        currentRepeatName = currentRepeatName.trim();
                        boolean insert = false;
                        for (int i = 0; i < questionName.length; i++) {

                            String tmp = questionName[i];
                            if (tmp.length() > 0 && tmp.contains("["))
                                tmp = tmp.substring(0, tmp.indexOf("["));
                            if (tmp.equals(currentRepeatName)) {
                                insert = true;
                                break;
                            }
                        }
                        //out repeat
                        if (insert) {
                            saveRepeatDataToLocalDb(currentRepeatName);
                            currentRepeatName = "";
                            currentIsRepeat = false;
                        }

                    }
                }

                isShowFromtList = true;
                SurveyUiIntents.get().launchFormHierarchyActivity(this, mFormController, false, HIERARCHY_ACTIVITY, isViewOnly);
                return true;
            case MENU_PREFERENCES:
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_PREFERENCES);
                Intent pref = new Intent(this, PreferencesActivity.class);
                startActivity(pref);
                return true;
            case MENU_QUICK_NOTE:
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_QUICK_NOTE);
                if (ActivityCompat.checkSelfPermission(RTASurvey.getInstance().getActivity(),
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ((FormEntryActivity) RTASurvey.getInstance().getActivity()).setPermissionRequestListener(new PermissionRequestListener() {
                        @Override
                        public void onPermissionGranted() {
                            QuickNoteManager manager = QuickNoteManager.getInstance();
                            try {
                                manager.showQuickNote();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onPermissionDenied() {
                            Toast.makeText(RTASurvey.getInstance().getActivity(), R.string.cpms_permission_not_allowed,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    ActivityCompat.requestPermissions(RTASurvey.getInstance().getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            FormEntryActivity.PERMISSIONS_REQUEST);

                } else {
                    QuickNoteManager manager = QuickNoteManager.getInstance();
                    try {
                        manager.showQuickNote();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return true;
            case MENU_QA_CHECKING:
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_QA_CHECKING);
                if (!isSendingWorking()) {
                    QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                    checkNoteManager.showCheckNote();
                } else {
                    Toast.makeText(this, getString(R.string.qa_processing), Toast.LENGTH_SHORT).show();
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_QA_CHECKING, " working have been sent, can not show QA List");
                }
                return true;
            case MENU_IMAGE_REPORT:
                RTAInputManager.getInstance().hideRTAKeyboard();
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_IMAGE_REPORT);
                Bitmap bitmap = takeScreenshot();
                ScreenCaptureReportDialog screenCaptureReportDialog = new ScreenCaptureReportDialog(this, bitmap);
                screenCaptureReportDialog.show();
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_DIALOG_ADD_REPEAT_GROUP);
                return true;
            case MENU_FONT_SIZE:
                boolean changeTextFont = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_CHANGE_FONTSIZE, false);
                String question_font = PreferencesManager.getPreferencesManager(Collect.getInstance()).getString(PreferencesActivity.KEY_FONT_SIZE,
                        Collect.DEFAULT_FONTSIZE, changeTextFont);
                int questionFontsize = Integer.valueOf(question_font);
                final Dialog fontSize_dialog = new Dialog(this);
                DisplayMetrics displaymetrics = new DisplayMetrics();
                this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int width = (int) ((int) displaymetrics.widthPixels * 0.8);
                fontSize_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                fontSize_dialog.setCancelable(false);
                fontSize_dialog.setContentView(R.layout.dialog_font_size);
                fontSize_dialog.getWindow().setLayout(width, -1);
                fontSize_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                Button btnCancel = (Button) fontSize_dialog.findViewById(R.id.btn_cancel);
                final RadioButton radioButton1 = (RadioButton) fontSize_dialog.findViewById(R.id.rb_1);
                final RadioButton radioButton2 = (RadioButton) fontSize_dialog.findViewById(R.id.rb_2);
                final RadioButton radioButton3 = (RadioButton) fontSize_dialog.findViewById(R.id.rb_3);
                final RadioButton radioButton4 = (RadioButton) fontSize_dialog.findViewById(R.id.rb_4);
                final RadioButton radioButton5 = (RadioButton) fontSize_dialog.findViewById(R.id.rb_5);
                if (questionFontsize == 13) {
                    radioButton1.setChecked(true);
                } else if (questionFontsize == 17) {
                    radioButton2.setChecked(true);
                } else if (questionFontsize == 21) {
                    radioButton3.setChecked(true);
                } else if (questionFontsize == 25) {
                    radioButton4.setChecked(true);
                } else if (questionFontsize == 29) {
                    radioButton5.setChecked(true);
                }
                radioButton1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (radioButton1.isChecked()) {
                            radioButton1.setChecked(true);
                            PreferencesManager.getPreferencesManager(fontSize_dialog.getContext()).setInt(KEY_FONT_SIZE, "13");
                            refreshCurrentView();
                            fontSize_dialog.dismiss();

                        }

                    }
                });
                radioButton2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (radioButton2.isChecked()) {
                            radioButton2.setChecked(true);
                            PreferencesManager.getPreferencesManager(fontSize_dialog.getContext()).setInt(KEY_FONT_SIZE, "17");
                            refreshCurrentView();
                            fontSize_dialog.dismiss();

                        }

                    }
                });
                radioButton3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (radioButton3.isChecked()) {
                            radioButton3.setChecked(true);
                            PreferencesManager.getPreferencesManager(fontSize_dialog.getContext()).setInt(KEY_FONT_SIZE, "21");
                            refreshCurrentView();
                            fontSize_dialog.dismiss();

                        }

                    }
                });
                radioButton4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (radioButton4.isChecked()) {
                            radioButton4.setChecked(true);
                            PreferencesManager.getPreferencesManager(fontSize_dialog.getContext()).setInt(KEY_FONT_SIZE, "25");
                            refreshCurrentView();
                            fontSize_dialog.dismiss();
                        }

                    }
                });
                radioButton5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (radioButton5.isChecked()) {
                            radioButton5.setChecked(true);
                            PreferencesManager.getPreferencesManager(fontSize_dialog.getContext()).setInt(KEY_FONT_SIZE, "29");
                            refreshCurrentView();
                            fontSize_dialog.dismiss();
                        }

                    }
                });
                btnCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fontSize_dialog.dismiss();
                    }
                });
                try {
                    fontSize_dialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            case MENU_REQUEST_EDIT:
                requestEditInstance();
                return true;
            case android.R.id.home:
                if (RTAInputManager.getInstance().hideRTAKeyboard())
                    return true;
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_BACK, "");
                createQuitDialog();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void requestEditInstance() {
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE,
                Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_DISCARD_AND_EXIT);

        // close all open databases of external data.
        Collect.getInstance().getExternalDataManager().close();
        RTASurvey.getInstance().setExternalDataPullResultManager(null);
        removeTempInstance();
        removePendingInstance();

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(InstanceColumns.CONTENT_URI,
                null, InstanceColumns.INSTANCE_UUID + "=?",
                new String[]{mFormController.getUuid()}, null);
        if (c != null) {
            if (c.moveToFirst()) {
                String stt = c.getString(c.getColumnIndex(InstanceColumns.STATUS));
                String name = c.getString(c.getColumnIndex(InstanceColumns.DISPLAY_NAME));
                String formid = c.getString(c.getColumnIndex(InstanceColumns.JR_FORM_ID));
                String formv = c.getString(c.getColumnIndex(InstanceColumns.JR_VERSION));
                c.close();
                if (stt != null && (stt.contains(InstanceProviderAPI.STATUS_SUBMITTED)
                        || stt.startsWith(InstanceProviderAPI.STATUS_COMPLETE))) {
                    String type = stt.startsWith(InstanceProviderAPI.STATUS_COMPLETE)
                            ? ConnectionService.RequestReturn.TYPE_FINALIZED
                            : ConnectionService.RequestReturn.TYPE_SUBMITTED;
                    createRequestEditDialog(mFormController.getUuid(), name, formid, formv, type);
                } else if (stt != null && stt.startsWith(InstanceProviderAPI.STATUS_SUBMISSION_FAILED)) {
                    MessageUtils.showDialogInfo(this, R.string.request_return_instance_err_ist_submit_failed);
                } else {
                    MessageUtils.showDialogInfo(this, R.string.request_return_instance_err_ist_stt);
                }
            } else {
                c.close();
                MessageUtils.showToastInfo(this, R.string.request_return_instance_err_ist_not_found);
                finishReturnInstance(false);
            }
        } else {
            MessageUtils.showToastInfo(this, R.string.request_return_instance_err_ist_not_found);
            finishReturnInstance(false);
        }
    }

    /**
     * Attempt to save the answer(s) in the current screen to into the data
     * model.
     *
     * @param evaluateConstraints
     * @return false if any error occurs while saving (constraint violated,
     * etc...), true otherwise.
     */
    public boolean saveAnswersForCurrentScreen(boolean evaluateConstraints) {
        if (isViewOnly) {
            return true;
        }
        FormController formController = RTASurvey.getInstance().getFormController();
        if (formController == null) {
            finish();
        } else if (formController.currentPromptIsQuestion()) {
            try {
                if (mCurrentView instanceof RTAView) {
                    LinkedHashMap<FormIndex, IAnswerData> answers = ((RTAView) mCurrentView)
                            .getAnswers();
                    //set question answer
                    if (answers != null) {
                        SimpleDateFormat f = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
                        for (FormIndex index : answers.keySet()) {
                            if (answers.get(index) != null) {
                                formController.setQuestionsHaveFill(index);
                                try {
                                    String questionName = StringFormatUtils.getQuestionName(index);
                                    boolean exists = tempTimeMaps.containsKey(questionName);
                                    if (!exists) {
                                        f.setTimeZone(TimeZone.getTimeZone("Asia/Saigon"));
                                        tempTimeMaps.put(questionName, new TimeTampEntity(f.format(new Date()), answers));
                                    } else {
                                        TimeTampEntity value = FormEntryActivity.tempTimeMaps.get(questionName);
                                        LinkedHashMap<FormIndex, IAnswerData> answers2 = value.getmAnswer();
                                        if (answers2 != null && !checkAnswer(answers, answers2)) {
                                            tempTimeMaps.remove(questionName);
                                            f.setTimeZone(TimeZone.getTimeZone("Asia/Saigon"));
                                            tempTimeMaps.put(questionName, new TimeTampEntity(f.format(new Date()), answers));
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                mFormController.removeQuestionHaveFill(index);
                            }
                        }
                    }

                    List<FailedConstraint> constraint = mFormController.saveAllScreenAnswers(answers, evaluateConstraints, true);
                    if (constraint != null && constraint.size() > 0) {
                        resetQuestion();
                        listConfirmRead.clear();
                        String tooltip = PreferencesManager.getPreferencesManager(Collect.getInstance()).getString(PreferencesActivity.KEY_CHANGE_TOOLTIP,
                                Collect.DEFAULT_TOOLTIP);
                        if (tooltip.equals("1")) {
                            if (countSave == 0) {
                                createConstraintToast(constraint.get(0).index, constraint.get(0).status);
                            }
                            if (countSave == 1) {
                                setCountSave(0);
                            }

                        } else if (tooltip.equals("2")) {
                            for (int i = 0; i < constraint.size(); i++) {
                                createConstraint(constraint.get(i).index, constraint.get(i).status);
                            }
                            if (listConfirmRead.size() > 0) {
                                if (countSave == 0) {
                                    createContraintDialog(listConfirmRead.get(0));
                                }
                                if (countSave == 1) {
                                    setCountSave(0);
                                }

                            }
                        }
                        return false;
                    }

                    return true;
                } else {
                    Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
                }
            } catch (JavaRosaException e) {
                e.printStackTrace();
                return false;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            setProgress(mFormController.getPercentOfForm(), (mFormController.getQuestionsHaveFill().size() / (float) mFormController.getQuestionsList().size()) * 100);
        }
        return true;
    }

    private boolean checkAnswer(LinkedHashMap<FormIndex, IAnswerData> answers, LinkedHashMap<FormIndex, IAnswerData> answers2) {
        boolean check = true;
        String s1 = null;
        String s2 = null;
        int i1 = 0, i2 = 0;
        if (answers.size() != answers2.size()) check = false;
        else {
            Iterator<FormIndex> iterator = answers.keySet().iterator();
            while (iterator.hasNext()) {
                FormIndex index = iterator.next();
                IAnswerData t1 = answers.get(index);
                if (t1 != null)
                    s1 = t1.getDisplayText();
                i1++;

            }

            Iterator<FormIndex> iterator2 = answers2.keySet().iterator();
            while (iterator2.hasNext()) {
                FormIndex index2 = iterator2.next();
                IAnswerData t2 = answers2.get(index2);
                if (t2 != null)
                    s2 = t2.getDisplayText();
                i2++;

            }
            if (s1 != null && !s1.equalsIgnoreCase(s2)) {
                check = false;
            }
        }
        return check;
    }

    /**
     * Clears the answer on the screen.
     */
    private void clearAnswerAndBind(QuestionWidget qw, ArrayList<QuestionWidget> widgets) {
        if (qw.getAnswer() != null) {
            if (mFormController.getQuestionsHaveFill().contains(StringFormatUtils.getQuestionName(qw.getPrompt().getIndex()))) {
                mFormController.getQuestionsHaveFill().remove(StringFormatUtils.getQuestionName(qw.getPrompt().getIndex()));
                setProgress(mFormController.getPercentOfForm(), (mFormController.getQuestionsHaveFill().size() / (float) mFormController.getQuestionsList().size()) * 100);
            }
            //this is just for test, in current time is not use.
            qw.clearAnswer();
            List<String> sourceCombo = qw.sourceCombo();
            if (sourceCombo != null) {
                List<FormIndex> indexs = new ArrayList<>();
                for (String s : sourceCombo) {
                    List<FormIndex> appendIndex = RTASurvey.getInstance().getComboWaitingIndex(s);
                    if (appendIndex != null && appendIndex.size() > 0) {
                        indexs.addAll(appendIndex);
                    }
                }
                if (indexs != null && indexs.size() > 0) {
                    for (QuestionWidget widget : widgets) {
                        if (indexs.contains(widget.getIndex()))
                            widget.clearAnswer();
                    }
                }
            }
            refreshCurrentView(qw.getPrompt().getIndex().toString());
        }
    }

    @Override
    public void removeAnswer(int id) {
        if (mCurrentView instanceof RTAView) {
            for (QuestionWidget qw : ((RTAView) mCurrentView).getWidgets()) {
                if (id == qw.getId()) {
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_CLEAR_DIALOG, " question : " + qw.getPrompt().getIndex());
                    clearAnswerAndBind(qw, ((RTAView) mCurrentView).getWidgets());
                    break;
                }
            }
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }
    }

    //use for instance newly created
    private void updateInstanceplushInfor() {
        ContentValues values2 = new ContentValues();
        values2.put(InstanceColumns.INSTANCE_SAVE_STATUS, "1");
        RTASurvey
                .getInstance()
                .getContentResolver()
                .update(InstanceColumns.CONTENT_URI,
                        values2,
                        InstanceColumns.INSTANCE_UUID + " = \""
                                + mFormController.getUuid()
                                + "\"", null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (isViewOnly)
            return;
        FormController formController = Collect.getInstance()
                .getFormController();
        if (PreferencesManager.getPreferencesManager(this).getBoolean(PreferencesActivity.KEY_LONG_PRESS_REMOVE_ANSWER, true))
            menu.add(0, v.getId(), 0, getString(R.string.clear_answer));

        FormIndex index = null;
        boolean isrepeat = false;
        if ((v instanceof QuestionWidget) && ((QuestionWidget) v).getPrompt() != null && ((QuestionWidget) v).getPrompt().getIndex() != null) {
            index = ((QuestionWidget) v).getPrompt().getIndex();
            formIndexClear = index;
            if (((QuestionWidget) v).isRepeat())
                isrepeat = true;
        }
        if (formController.indexContainsRepeatableGroup() || isrepeat) {
            if (PreferencesManager.getPreferencesManager(this).getBoolean(PreferencesActivity.KEY_LONG_PRESS_REMOVE_REPEAT, true))
                menu.add(0, DELETE_REPEAT, 0, getString(R.string.delete_repeat));
        }
        menu.setHeaderTitle(getString(R.string.edit_prompt));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /*
         * We don't have the right view here, so we store the View's ID as the
		 * item ID and loop through the possible views to find the one the user
		 * clicked on.
		 */

        if (mCurrentView instanceof RTAView) {
            for (QuestionWidget qw : ((RTAView) mCurrentView).getWidgets()) {
                if (item.getItemId() == qw.getId()) {
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_CLEAR_DIALOG, " question : " + qw.getPrompt().getIndex());
                    createClearDialog(qw);
                    break;
                }
            }
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }

        if (item.getItemId() == DELETE_REPEAT) {
            createDeleteRepeatConfirmDialog(formIndexClear);
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_DELETE_REPEAT_DIALOG, "");
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // if a form is loading, pass the loader task
        if (mFormLoaderTask != null
                && mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED)
            return mFormLoaderTask;

        // if a form is writing to disk, pass the save to disk task
        if (mSaveToDiskTask != null
                && mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED)
            return mSaveToDiskTask;

        if (mExportToCsvTask != null
                && mExportToCsvTask.getStatus() != AsyncTask.Status.FINISHED) {
            return mExportToCsvTask;
        }

        // mFormEntryController is static so we don't need to pass it.
        if (mFormController != null && mFormController.currentPromptIsQuestion()) {
            saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
        }
        return null;
    }

    private View createView(int event, boolean advancingPage) {
        PreferencesManager pre = PreferencesManager.getPreferencesManager(this);
        if (getLocalDatabase() != null && event != FormEntryController.EVENT_BEGINNING_OF_FORM) {
            instancePoolManager.insertLocalData(getLocalDatabase(), mFormController, false);
        }
        setupUIForNavigationButton();
        if (timeStart.equals("")) {
            timeStart = StringFormatUtils.getDate(System.currentTimeMillis(),
                    "dd/MM/yyyy hh:mm:ss");
            timeStartmilis = System.currentTimeMillis();
        }
        boolean welcome_screen = pre.getBoolean(_PreferencesActivity.KEY_WELCOME_SCREEN, true);
        switch (event) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                String uuid = StringFormatUtils.getUUIDInstancePath(this,
                        mFormController.getInstancePath().getAbsolutePath());
                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_BEGIN_SCREEN);
                View startView = View.inflate(this, R.layout.form_entry_start, null);
                if (welcome_screen) {   //display welcome screen and save form
                    Drawable image = null;
                    File mediaFolder = mFormController.getMediaFolder();
                    String mediaDir = mediaFolder.getAbsolutePath();
                    BitmapDrawable bitImage = null;
                    // attempt to load the form-specific logo...
                    // this is arbitrarily silly
                    bitImage = new BitmapDrawable(getResources(), mediaDir
                            + File.separator + "form_logo.png");

                    if (bitImage.getBitmap() != null && bitImage.getIntrinsicHeight() > 0
                            && bitImage.getIntrinsicWidth() > 0) {
                        image = bitImage;
                    }

                    if (image == null) {
                        // show the opendatakit zig...
                        // image =
                        // getResources().getDrawable(R.drawable.opendatakit_zig);
//                        ((ImageView) startView.findViewById(R.id.form_start_bling))
//                                .setVisibility(View.INVISIBLE);
                    } else {
                        ImageView v = ((ImageView) startView
                                .findViewById(R.id.form_start_bling));
                        v.setImageDrawable(image);
                        v.setImageDrawable(image);
                        v.setContentDescription(mFormController.getFormTitle());
                    }

                    // change start screen based on navigation prefs
                    String navigationChoice = pre.getString(PreferencesActivity.KEY_NAVIGATION,
                            PreferencesActivity.NAVIGATION_SWIPE_ONE);
                    Boolean useSwipe = false;
                    Boolean useButtons = pre.getBoolean(_PreferencesActivity.KEY_NAVIGATION_BUTTONS, false);
                    ImageView is = ((ImageView) startView
                            .findViewById(R.id.form_start_bling));
                    ImageView ia = ((ImageView) startView
                            .findViewById(R.id.image_2));
                    ImageView ib = ((ImageView) startView
                            .findViewById(R.id.image_3));
//                    TextView ta = ((TextView) startView.findViewById(R.id.text_advance));
//                    TextView tb = ((TextView) startView.findViewById(R.id.text_backup));
                    TextView d = ((TextView) startView.findViewById(R.id.description));
                    if (navigationChoice != null
                            && navigationChoice.contains(PreferencesActivity.NAVIGATION_SWIPE)) {
                        useSwipe = true;
                    }
                    if (useSwipe && !useButtons) {
                        d.setText(getString(R.string.swipe_instructions,
                                mFormController.getFormTitle()));
                    } else if (useButtons && !useSwipe) {
                        is.setVisibility(View.GONE);
                        ib.setVisibility(View.GONE);
                        ia.setVisibility(View.GONE);
                        d.setText(getString(R.string.buttons_instructions,
                                mFormController.getFormTitle()));
                    } else {
                        d.setText(getString(R.string.swipe_buttons_instructions,
                                mFormController.getFormTitle()));
                    }

                    skipValidate = true;
                    if (uuid == null && Intent.ACTION_EDIT.equals(getIntent().getAction())
                            && getIntent().getData() != null
                            && getIntent().getData().toString().contains(FormsProviderAPI.AUTHORITY)) {
                        saveDataToDisk(false, false, null);
                    }
                    if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
                        saveDataToDisk(false, false, null);
                    }
                    skipValidate = false;
                    return startView;
                } else {    //save new form and move to next screen
                    if (uuid == null && Intent.ACTION_EDIT.equals(getIntent().getAction())
                            && getIntent().getData() != null
                            && getIntent().getData().toString().contains(FormsProviderAPI.AUTHORITY)) {
                        saveDataToDisk(false, false, null);
                    }
                    if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
                        saveDataToDisk(false, false, null);
                    }
                    try {
                        event = mFormController.stepToNextScreenEvent();
                    } catch (JavaRosaException e) {
                        e.printStackTrace();
                    }

                    if (mFormController.currentPromptIsQuestion())
                        return createView(event, true);
                    else {
                        try {
                            event = mFormController.stepToNextScreenEvent();
                        } catch (JavaRosaException e) {
                            e.printStackTrace();
                        }
                        return createView(event, true);
                    }

                }
            case FormEntryController.EVENT_END_OF_FORM:
                // if form has (some) 'end-survey widget' (button),
                // then they cannot go to 'end-form screen', just step back
                boolean final_screen = pre.getBoolean(_PreferencesActivity.KEY_FINAL_SCREEN, true);
                if (existEndSurvey || !final_screen) {
                    try {
                        event = mFormController.stepToPreviousScreenEvent();
                    } catch (JavaRosaException ex) {
                        ex.printStackTrace();
                    }
                    return createView(event, advancingPage);
                }

                ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_END_SCREEN);

                // if user is in read-only mode,
                // then 'end-form screen' will be different
                if (isViewOnly) {
                    View endView = View.inflate(this, R.layout.exit_view_data, null);
                    Button exit = ((Button) endView.findViewById(R.id.exit_button));
                    exit.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            removeTempInstance();
                            removePendingInstance();
                            finishReturnInstance(false);
                        }
                    });
                    return endView;
                }

                // standard 'end-form screen'
                View endView = View.inflate(this, R.layout.form_entry_end, null);

                // checkbox for if finished or ready to send
                final CheckBox instanceComplete = ((CheckBox) endView
                        .findViewById(R.id.mark_finished));
                instanceComplete.setChecked(isInstanceComplete(true));
                instanceComplete.setVisibility(View.GONE);

                if (!PreferencesManager.getPreferencesManager(this).getBooleanInAdmin(
                        AdminPreferencesActivity.KEY_MARK_AS_FINALIZED, true)) {
                    instanceComplete.setVisibility(View.GONE);
                }

                // edittext to change the displayed name of the instance
                final EditText saveAs = (EditText) endView
                        .findViewById(R.id.save_name);

                // disallow carriage returns in the name
                InputFilter returnFilter = new InputFilter() {
                    public CharSequence filter(CharSequence source, int start,
                                               int end, Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (Character.getType((source.charAt(i))) == Character.CONTROL) {
                                return "";
                            }
                        }
                        return null;
                    }
                };
                saveAs.setFilters(new InputFilter[]{returnFilter});

                String saveName = mFormController.getSubmissionMetadata().instanceName;
                if (saveName == null) {
                    // present the prompt to allow user to name the form
                    nameofInstance = getInstanceDisplayName(mFormController);
                    saveAs.setText(saveName);
                    saveAs.setEnabled(true);
                    saveAs.setVisibility(View.VISIBLE);
                } else {
                    // if instanceName is defined in form, this is the name -- no
                    // revisions
                    // display only the name, not the prompt, and disable edits
                    // get name of instance;
                    saveAs.setText(saveName);
                    nameofInstance = saveName;
                    saveAs.setEnabled(false);
                    saveAs.setBackgroundColor(Color.WHITE);
                    saveAs.setVisibility(View.VISIBLE);
                }

                // override the visibility settings based upon admin preferences
                if (!PreferencesManager.getPreferencesManager(this).getBooleanInAdmin(
                        AdminPreferencesActivity.KEY_SAVE_AS, true)) {
                    saveAs.setVisibility(View.GONE);
                }
                instacePathforDatabase = mFormController.getInstancePath().getParent();

                final boolean send_working = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_ALLOW_SEND_WORKING_DATA, true);

                Button saveButtonIncomplete = ((Button) endView.findViewById(R.id.save_exit_button_incompleted));
                boolean hideButtonIncomplete = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_HIDE_INCOMPLETE_EXIT_BUTTON, false);

                if (hideButtonIncomplete) {
                    saveButtonIncomplete.setVisibility(View.GONE);
                    TextView textView = ((TextView) endView.findViewById(R.id.description_save_exit_button_incompleted));
                    textView.setVisibility(View.GONE);
                }

                saveButtonIncomplete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProcessDbHelper.getInstance().removeProcess(mFormController.getUuid());
                        ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_SAVE_INCOMPLETE);
                        total = StringFormatUtils.getTime(
                                System.currentTimeMillis() - timeStartmilis);
                        // Form is marked as 'saved' here.
                        if (saveAs.getText().length() < 1) {
                            Toast.makeText(FormEntryActivity.this, R.string.save_as_error,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            if (!send_working) {
                                saveDataToDisk(EXIT, false/*is check completed checkbox*/, saveAs.getText()
                                        .toString(), RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                                //not save finalize
                                updateInstanceplushInfor();
                            } else {
                                String uuid = mFormController.getUuid();
                                int status = RTASurvey.getInstance().getWorkingDataHelper().getStatusOfFileName(uuid);

                                if (status == 1) {
                                    saveDataToDisk(EXIT, false/*is check completed checkbox*/, saveAs.getText()
                                            .toString(), RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                                } else {
                                    saveDataToDisk(EXIT, false/*is check completed checkbox*/, saveAs.getText()
                                            .toString(), RTASurvey.STATUS_PENDING_SEND_FINALIZE);
                                }
                                //not save finalize
                                updateInstanceplushInfor();
                                RTASurvey.getInstance().getWorkingDataHelper().closeDatabase();
                            }
                        }
                    }
                });

                Button saveButtonFinalize = ((Button) endView.findViewById(R.id.save_exit_button_finalize));
                saveButtonFinalize.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProcessDbHelper.getInstance().removeProcess(mFormController.getUuid());

                        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_SAVE_COMPLETE, "");
                        String uuid = mFormController.getUuid();//StringFormatUtils.getUUIDInstancePath(RTASurvey.getInstance().getActivity(), RTASurvey.getInstance().getFormController().getInstancePath().getAbsolutePath());
                        updateInstanceplushInfor();

                        if (QACheckPointData.getNoErrorOfInstance(uuid) > 0 && !isViewOnly) {
                            QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                            checkNoteManager.showCheckNote();
                        } else {
                            total = StringFormatUtils.getTime(System.currentTimeMillis() - timeStartmilis);
                            // Form is marked as 'saved' here.
                            if (saveAs.getText().length() < 1) {
                                Toast.makeText(FormEntryActivity.this, R.string.save_as_error,
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                if (!send_working) {
                                    saveDataToDisk(EXIT, true/*is check completed checkbox*/, saveAs.getText()
                                            .toString(), RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                                } else {

                                    int status = RTASurvey.getInstance().getWorkingDataHelper().getStatusOfFileName(uuid);
                                    if (status == 1) {
                                        saveDataToDisk(EXIT, true/*is check completed checkbox*/, saveAs.getText()
                                                .toString(), RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                                    } else {
                                        saveDataToDisk(EXIT, true/*is check completed checkbox*/, saveAs.getText()
                                                .toString(), RTASurvey.STATUS_PENDING_SEND_FINALIZE);

                                    }
                                }
                            }

                            RTASurvey.getInstance().getWorkingDataHelper().closeDatabase();
                        }
                    }
                });

                if (!isViewOnly && mFormController != null && mFormController.getUuid() != null
                        && QACheckPointData.getNoErrorOfInstance(mFormController.getUuid()) > 0) {
                    saveButtonFinalize.setClickable(false);
                    saveButtonFinalize.setEnabled(false);
                    QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                    checkNoteManager.showCheckNote();
                } else {
                    //send data
                    saveButtonFinalize.setEnabled(true);
                    saveButtonFinalize.setClickable(true);
                }
                return endView;
            case FormEntryController.EVENT_QUESTION:
            case FormEntryController.EVENT_GROUP:
            case FormEntryController.EVENT_REPEAT:
                RTAView rtaView = null;
                // should only be a group here if the event_group is a field-list
                try {
                    RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
                    FormEntryCaption[] groups = mFormController.getGroupsForCurrentIndex();

                    if (MiniLogFormAction.isInMiniLogGroup(mFormController)) {
                        try {
                            try {
                                event = welcome_screen ? (movingNext ? mFormController.stepToNextScreenEvent()
                                        : mFormController.stepToPreviousEvent()) : mFormController.stepToNextScreenEvent();
                            } catch (JavaRosaException e) {
                                e.printStackTrace();
                            }

                            if (mFormController.currentPromptIsQuestion())
                                return createView(event, true);
                            else {
                                try {
                                    event = welcome_screen ? (movingNext ? mFormController.stepToNextScreenEvent()
                                            : mFormController.stepToPreviousEvent()) : mFormController.stepToNextScreenEvent();
                                } catch (JavaRosaException e) {
                                    e.printStackTrace();
                                }
                                return createView(event, true);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    rtaView = new RTAView(FormEntryActivity.this,
                            this, prompts,
                            groups, advancingPage, isViewOnly);

                } catch (RuntimeException e) {
                    e.printStackTrace();
                    if (mFormController.isHaveFirstOrLastScreen(false)) {
                        createErrorDialog(e.getMessage(), true);
                        return new View(this);
                    }
                    try {
                        event = mFormController.stepToNextScreenEvent();
                        createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                    } catch (JavaRosaException e1) {
                        e.printStackTrace();
                        createErrorDialog(e.getMessage() + "\n\n"
                                + e1.getCause().getMessage(), DO_NOT_EXIT);
                        e1.printStackTrace();
                    }
                    return createView(event, advancingPage);
                }
                if (!isViewOnly) {
                    for (QuestionWidget qw : rtaView.getWidgets()) {
                        if (!qw.getPrompt().isReadOnly()) {
                            registerForContextMenu(qw);
                        }
                    }
                }
                return rtaView;
            default:
                // this is badness to avoid a crash.
                try {
                    event = mFormController.stepToNextScreenEvent();
                    createErrorDialog(getString(R.string.survey_internal_error),
                            EXIT);
                } catch (JavaRosaException e) {
                    e.printStackTrace();
                    if (mFormController.isHaveFirstOrLastScreen(false)) {
                        createErrorDialog(e.getMessage(), true);
                        return new View(this);
                    }
                }
                return createView(event, advancingPage);
        }
    }

    private void setupUIForNavigationButton() {
        if (PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_NAVIGATION_BUTTONS, false)) {
            boolean isHaveFirst = mFormController.isHaveFirstOrLastScreen(true);
            boolean isHaveLast = mFormController.isHaveFirstOrLastScreen(false);
            mBackButton.setVisibility(isHaveFirst ? View.GONE : View.VISIBLE);
            mNextButton.setVisibility(isHaveLast ? View.GONE : View.VISIBLE);
            if (isHaveFirst && isHaveLast) {
                mBackButton.setVisibility(View.GONE);
                mNextButton.setVisibility(View.GONE);
            }
        } else {
            mBackButton.setVisibility(View.GONE);
            mNextButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent mv) {
        mPointCountsList.add(mv.getPointerCount());
        mShowPercentProgress.setVisibility(View.GONE);
        isShowPercentProgress = false;
        boolean handled = super.dispatchTouchEvent(mv);
        handled = mGestureDetector.onTouchEvent(mv);
        return handled;
    }

    // Hopefully someday we can use managed dialogs when the bugs are fixed
    /*
     * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
	 * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
	 * (cupcake). We do use managed dialogs for our static loading
	 * ProgressDialog. The main issue we noticed and are waiting to see fixed
	 * is: onPrepareDialog() is not called after a screen orientation change.
	 * http://code.google.com/p/android/issues/detail?id=1639
	 */

    //

    public int getActionbarHeight() {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getNavigationBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public void checkAlertAndShowNextView() {
        // only try to save if the current event is a question or a field-list
        // group
        if (mFormController != null && mFormController.currentPromptIsQuestion()) {
            if (mCurrentView instanceof RTAView) {
                String alertMessage = ((RTAView) mCurrentView).getAlertMessage();
                if (!alertMessage.equals("") && !alertMessage.equals("final")) {
                    createWarningDialog(alertMessage, "showNext");
                }
                String mess = "";
                if (CheckingSelectionOne.isexpr(StringFormatUtils.getAppearanceOfcurrentGroup(mFormController.getGroupsForCurrentIndex()))) {
                    CheckingSelectionOne checkingSelectionOne = new CheckingSelectionOne();
                    AnswerOfListSelectionOne answerOfListSelectionOne = AnswerOfListSelectionOne.getInstance();
                    mess = checkingSelectionOne.checkingGroup(answerOfListSelectionOne.getAllAnswer(), true);
                    if (!mess.equals("")) {
                        //ScreenUtils.alertboxError(this, getString(R.string.error), mess, false);
                        createErrorDialog(mess, false);
                        mBeenSwiped = false;
                    }
                }
                if (alertMessage.equals("final") && mess.equals("")) {
                    showNextView();
                    //mBeenSwiped = false;
                } else if (alertMessage.equals("")) {
                    checkAlertAndShowNextView();
                }
            } else {
                showNextView();
            }
        } else {
            showNextView();
        }
    }

    private void createWarningDialog(String message, String action) {
        final String ac = action;
        final String answer = message.substring(message.indexOf(":::") + 3);
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_WARNING_DIALOG, "message: " + message);
        message = message.substring(0, message.indexOf(":::"));
//        if (mAlertDialog != null && mAlertDialog.isShowing()) {
//            message = mErrorMessage + "\nWarning: " + message;
//            mErrorMessage = message;
//        } else {
//            mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
//            mErrorMessage = message;
//        }
//
//        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);//       mAlertDialog.setTitle(getString(R.string.warning_occured));
//        mAlertDialog.setMessage(message);
//        final StatusOfAnswer statusOfAnswer = StatusOfAnswer.getInstance();
//        DialogInterface.OnClickListener warningListener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int i) {
//                switch (i) {
//                    case DialogInterface.BUTTON_POSITIVE:
//                        //showPreviousView();
//                        mErrorMessage = null;
//                        dismissDialogs();
//                        statusOfAnswer.addAnswer(answer, true);
//                        mBeenSwiped = false;
//                        if (ac.equals("showNext")) {
//                            checkAlertAndShowNextView();
//
//                        } else {
//                            checkAlertAndShowPreviousView();
//                        }
//                        break;
//                    case DialogInterface.BUTTON_NEGATIVE:
//                        mErrorMessage = null;
//                        mBeenSwiped = false;
//                        statusOfAnswer.addAnswer(answer, false);
//                        dismissDialogs();
//                        break;
//                }
//
//            }
//        };
//        mAlertDialog.setCancelable(false);
//        mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", warningListener);
//        mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", warningListener);
//        mAlertDialog.show();
        if (warnig_dialog != null && warnig_dialog.isShowing()) {
            message = mErrorMessage + "\n\n" + message;
            mErrorMessage = message;
        } else {
            // mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
            warnig_dialog = new Dialog(this);
            mErrorMessage = message;
            DisplayMetrics displaymetrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = (int) ((int) displaymetrics.widthPixels * 0.8);
            warnig_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            warnig_dialog.setCancelable(false);
            warnig_dialog.setContentView(R.layout.warning_dialog_new);
            warnig_dialog.getWindow().setLayout(width, -1);
            warnig_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            TextView text = (TextView) warnig_dialog.findViewById(R.id.text_warning);
            text.setText(message);
            final StatusOfAnswer statusOfAnswer = StatusOfAnswer.getInstance();
            Button dialogOkButton = (Button) warnig_dialog.findViewById(R.id.btn_ok);
            dialogOkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mErrorMessage = null;
                    dismissDialogs();
                    statusOfAnswer.addAnswer(answer, true);
                    mBeenSwiped = false;
                    if (ac.equals("showNext")) {
                        checkAlertAndShowNextView();

                    } else {
                        checkAlertAndShowPreviousView();
                    }
                }
            });
            Button dialogCancelButton = (Button) warnig_dialog.findViewById(R.id.btn_cancel);
            dialogCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mErrorMessage = null;
                    mBeenSwiped = false;
                    statusOfAnswer.addAnswer(answer, false);
                    dismissDialogs();
                }
            });
        }
        try {
            //some time it occur error "that was originally added here"?????
            warnig_dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Determines what should be displayed on the screen. Possible options are:
     * a question, an ask repeat dialog, or the submit screen. Also saves
     * answers to the data model after checking constraints.
     */
    public void showNextView() {
        movingNext = true;
        try {
            //adding for call widget.
            if (CallRecordWidgets.COUNT_CALL != 0) {
                CallRecordWidgets.COUNT_CALL = 0;
            }
            // get constraint behavior preference value with appropriate default
            String constraint_behavior = PreferencesManager.getPreferencesManager(this).getString(
                    PreferencesActivity.KEY_CONSTRAINT_BEHAVIOR,
                    PreferencesActivity.CONSTRAINT_BEHAVIOR_DEFAULT);
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_NEXT_SCREEN, "");
            if (mFormController.currentPromptIsQuestion()) {
                // if constraint behavior says we should validate on swipe, do
                // so
                if (constraint_behavior
                        .equals(PreferencesActivity.CONSTRAINT_BEHAVIOR_ON_SWIPE)) {
                    if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS)) {
                        // A constraint was violated so a dialog should be
                        // showing.
                        mBeenSwiped = false;
                        return;
                    }
                    // otherwise, just save without validating (constraints will
                    // be validated on finalize)
                } else
                    saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }

            if (mFormController.getEvent() == FormEntryController.EVENT_END_OF_FORM) {
                mBeenSwiped = false;
                return;
            }

            if (mFormController.currentPromptIsQuestion()) {
                RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
                for (RFormEntryPrompt p : prompts) {
                    if (p.getFormEntryPrompt() != null && p.getFormEntryPrompt().getAppearanceHint() != null
                            && (p.getFormEntryPrompt().getAppearanceHint().toLowerCase().startsWith("saveincomplete") || p.getFormEntryPrompt().getAppearanceHint().toLowerCase().startsWith("savefinalized"))) {
                        mBeenSwiped = false;
                        refreshCurrentView();
                        return;
                    }
                }
            }

            View next;
            int event = mFormController.stepToNextScreenEvent();

            switch (event) {
                case FormEntryController.EVENT_QUESTION:
                case FormEntryController.EVENT_GROUP:
                    // create a savepoint
                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                        nonblockingCreateSavePointData();
                    }
                    next = createView(event, true);
                    showView(next, AnimationType.RIGHT);
                    break;
                case FormEntryController.EVENT_END_OF_FORM:
                    next = createView(event, true);
                    showView(next, AnimationType.RIGHT);
                    //send data
                    final boolean send_working = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_ALLOW_SEND_WORKING_DATA, false);
                    sendWorkingToServer(send_working);
                    break;
                case FormEntryController.EVENT_REPEAT:
                    next = createView(event, true);
                    showView(next, AnimationType.RIGHT);
                    break;
                case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                    if (MiniLogFormAction.isInMiniLogGroup(mFormController)
                            || mFormController.indexIsInOneScreen()) {
                        //manually move next
                        mBeenSwiped = false;
                        next();
                    } else {
                        createRepeatDialog();
                    }
                    break;
                case FormEntryController.EVENT_REPEAT_JUNCTURE:
                    Log.i(t, "repeat juncture: " + mFormController.getFormIndex().getReference());
                    // skip repeat junctures until we implement them
                    break;
                default:
                    Log.w(t,
                            "JavaRosa added a new EVENT type and didn't tell us... shame on them.");
                    break;
            }
            setProgress(mFormController.getPercentOfForm(), (mFormController.getQuestionsHaveFill().size() / (float) mFormController.getQuestionsList().size()) * 100);
        } catch (JavaRosaException e) {
            e.printStackTrace();
            createErrorDialog(e.getCause().getMessage(), DO_NOT_EXIT);
        }
    }

    public AggregateLocal getLocalDatabase() {
        if (isViewOnly) {
            return null;    // skip local database update when in View-Only mode
        }
        if (mFormController != null) {
            String formFamily = mFormController.getFormFamilyID();
            if (aggregateLocal == null && formFamily != null && !formFamily.equals("")) {
                File localDataFile = new File(RTASurvey.LOCALDATA_PATH + File.separator
                        + formFamily + File.separator + formFamily + ".db");
                aggregateLocal = new AggregateLocal(localDataFile.getParent(), formFamily);
            }
        }
        return aggregateLocal;
    }

    public void checkAlertAndShowPreviousView() {
        showPreviousView();
    }

    /**
     * Determines what should be displayed between a question, or the start
     * screen and displays the appropriate view. Also saves answers to the data
     * model without checking constraints.
     */
    public void showPreviousView() {
        movingNext = false;
        if (PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_DISABLE_MOVE_TO_PREV_SCREEN, false) && !isViewOnly) {
            mBeenSwiped = false;
            return;
        }
        try {
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_PREVIOUS, "");
            // The answer is saved on a back swipe, but question constraints are
            // ignored.
            if (mFormController.currentPromptIsQuestion() && !isReatpeatDelete) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            } else {
                isReatpeatDelete = false;
            }

            if (mFormController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
                int event = mFormController.stepToPreviousScreenEvent();
                // add by RTA
                try {
                    if (mFormController.getQuestionPrompt(
                            mFormController.getFormIndex()) != null) {
                        String appearance = mFormController.getQuestionPrompt(
                                mFormController.getFormIndex()).getAppearanceHint();
                        if (appearance != null && appearance.contains("qacheckpoint-visible")) {
                            event = mFormController.stepToPreviousScreenEvent();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // /////////////
                if (event == FormEntryController.EVENT_BEGINNING_OF_FORM
                        || event == FormEntryController.EVENT_GROUP
                        || event == FormEntryController.EVENT_QUESTION) {
                    // create savepoint
                    if ((++viewCount) % SAVEPOINT_INTERVAL == 0) {
                        nonblockingCreateSavePointData();
                    }
                }
                View next = createView(event, false);
                showView(next, AnimationType.LEFT);

            } else {
                mBeenSwiped = false;
            }
        } catch (JavaRosaException e) {
            e.printStackTrace();
            createErrorDialog(e.getCause().getMessage() + "\n", DO_NOT_EXIT);
        }
    }

    /**
     * Displays the View specified by the parameter 'next', animating both the
     * current view and next appropriately given the AnimationType. Also updates
     * the progress bar.
     */
    public void showView(View next, AnimationType from) {
        // disable notifications...
        // hide keyboard
        RTAInputManager.getInstance().hideRTAKeyboard();
        if (next instanceof RTAView) {
            isScrollable = isScrollable(mQuestionHolder, (RTAView) next);
            mDown.setVisibility(isScrollable &&
                    PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_NAVIGATION_UP_DOWN_LEFT_RIGHT_BUTTONS, false) ? View.VISIBLE : View.GONE);
            setColorForScrollView((RTAView) next);
        }
        if (mInAnimation != null) {
            mInAnimation.setAnimationListener(null);
        }
        if (mOutAnimation != null) {
            mOutAnimation.setAnimationListener(null);
        }

        try {
            if (bluetoothService != null) {
                bluetoothService.stop();
                bluetoothService = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // logging of the view being shown is already done, as this was handled
        // by createView()
        String logString = "";
        switch (from) {
            case RIGHT:
                // left -> right
                mInAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_left_in);
                // if animation is left or right then it was a swipe, and we want to
                // re-save on entry
                mOutAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_left_out);
                mAutoSaved = false;
                logString = "next";
                break;
            case LEFT:
                // right -> left
                mInAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_right_in);
                mOutAnimation = AnimationUtils.loadAnimation(this,
                        R.anim.push_right_out);
                mAutoSaved = false;
                logString = "previous";
                break;
            case FADE:
                mInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                mOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
                logString = "refresh";
                break;
        }

        // complete setup for animations...
        mInAnimation.setAnimationListener(this);
        mOutAnimation.setAnimationListener(this);

        // drop keyboard before transition...
        if (mCurrentView != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mCurrentView.getWindowToken(),
                    0);
        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // adjust which view is in the layout container...
        mStaleView = mCurrentView;
        mCurrentView = next;
        mQuestionHolder.addView(mCurrentView, lp);
        mQuestionHolder.invalidate();
        mAnimationCompletionSet = 0;

        if (mStaleView != null) {
            // start OutAnimation for transition...
            mStaleView.startAnimation(mOutAnimation);
            // and raddViewaddViewemove the old view (MUST occur after start of animation!!!)
            mQuestionHolder.removeView(mStaleView);
        } else {
            mAnimationCompletionSet = 2;
        }
        // start InAnimation for transition...
        mCurrentView.startAnimation(mInAnimation);
        Collect.getInstance().getActivityLogger()
                .logInstanceAction(this, "showView", logString);

        String repeatName = "";
        String questionName = "";
        boolean is1Screen = false;
        if (mFormController.getEvent() == FormEntryController.EVENT_QUESTION
                || mFormController.getEvent() == FormEntryController.EVENT_GROUP
                || mFormController.getEvent() == FormEntryController.EVENT_REPEAT) {
            RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
            for (RFormEntryPrompt p : prompts) {
                if (p.getFormEntryPrompt() != null) {
                    List<TreeElement> attrs = p.getFormEntryPrompt().getBindAttributes();
                    for (int i = 0; i < attrs.size(); i++) {
                        if (!mAutoSaved
                                && "saveIncomplete".equals(attrs.get(i).getName())) {
                            saveDataToDisk(false, false, null, false, null);
                            mAutoSaved = true;
                            updateInstanceplushInfor();
                        }
                    }
                    if (p.getFormEntryPrompt().isReadOnly() || !mFormController.getModel().isIndexRelevant(p.getFormEntryPrompt().getIndex()))
                        mFormController.setQuestionsReadonly(StringFormatUtils.getQuestionName(p.getFormEntryPrompt().getIndex()));
                    else {
                        if (mFormController.getQuestionsReadonlys().contains(StringFormatUtils.getQuestionName(p.getFormEntryPrompt().getIndex())))
                            mFormController.getQuestionsReadonlys().remove(StringFormatUtils.getQuestionName(p.getFormEntryPrompt().getIndex()));
                    }
                }

                if (p.getmType() == FormEntryController.EVENT_PROMPT_NEW_REPEAT && !is1Screen) {
                    FormIndex formIndex = p.getFormIndex();
                    questionName = formIndex.getReference().toShortString();
                    if (questionName.contains("["))
                        repeatName = questionName.substring(0, questionName.indexOf("["));
                    else repeatName = questionName;
                    is1Screen = true;
                } else if (p.getmType() == FormEntryController.EVENT_REPEAT && p.getFormEntryPrompt() != null && !is1Screen) {
                    is1Screen = true;
                    FormIndex formIndex = p.getFormEntryPrompt().getIndex();
                    questionName = formIndex.getReference().getParentRef().toShortString();
                    if (questionName.contains("["))
                        repeatName = questionName.substring(0, questionName.indexOf("["));
                    else repeatName = questionName;
                }

            }
            if (!is1Screen)
                repeatName = StringFormatUtils.getLastRepeatGroupName(mFormController.getCaptionHierarchy());
        }
        mPre.setVisibility(mFormController.isHaveFirstOrLastScreen(true) || mFormController.getEvent() == FormEntryController.EVENT_BEGINNING_OF_FORM || !PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_NAVIGATION_UP_DOWN_LEFT_RIGHT_BUTTONS, false) ? View.GONE : View.VISIBLE);
        mNext.setVisibility(mFormController.isHaveFirstOrLastScreen(false) || mFormController.getEvent() == FormEntryController.EVENT_END_OF_FORM || !PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_NAVIGATION_UP_DOWN_LEFT_RIGHT_BUTTONS, false) ? View.GONE : View.VISIBLE);

        if (mFormController.getEvent() != FormEntryController.EVENT_END_OF_FORM && mFormController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
            if (mFormController.currentPromptIsQuestion()) {
                String[] questionNames;
                if (!is1Screen)
                    questionNames = mFormController.getFormIndex().getReference().toString().split("/");
                else questionNames = questionName.split("/");

                //refresh screen, and in a repeat group, then save repeat data
                if (from == AnimationType.FADE && !currentRepeatName.equals("")) {
                    saveRepeatDataToLocalDb(currentRepeatName);
                } else if (!currentRepeatName.equals("")) {
                    boolean moveOutFromRepeat = true;
                    currentRepeatName = currentRepeatName.trim();

                    //check if still in repeat group or not
                    for (String name : questionNames) {
                        String tmp = name;
                        if (tmp.length() > 0 && tmp.contains("["))
                            tmp = tmp.substring(0, name.indexOf("["));
                        if (tmp.equals(currentRepeatName)) {
                            moveOutFromRepeat = false;
                            break;
                        }
                    }

                    //out repeat, save data to local database
                    if (moveOutFromRepeat) {
                        saveRepeatDataToLocalDb(currentRepeatName);
                        currentRepeatName = "";
                        currentIsRepeat = false;
                    }

                } else {
                    if (repeatName != null && !repeatName.equals("")) {
                        currentRepeatName = repeatName;
                    }
                }
            }
        }

        if (mCurrentView instanceof RTAView) {
            for (QuestionWidget question : ((RTAView) mCurrentView).getWidgets()) {
                if (question.isSetPreload()) {
                    FormController formController = RTASurvey.getInstance().getFormController();
                    saveAnswersForCurrentScreen(false);
                    try {
                        RFormEntryPrompt[] prompts = formController.getQuestionPrompts();
                        ((RTAView) mCurrentView).refreshView(prompts);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                    }
                    return;
                }
            }
        }
    }

    @Deprecated
    public void saveRepeatDataToLocalDb(String repeatName) {

        //no need to save repeat data invidually
        //all changes will be updated to local database file immediatetly

        /*try {
            if (getLocalDatabaseFile() != null) {
                instancePoolManager.insertRepeatData(localDataFile, repeatName, mFormController);
                instancePoolManager.insertRepeatData(mFormController,
                        repeatName, localData, mFormController.getFormFamilyID(),
                        mFormController.getUuid());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    /**
     * Creates and displays a dialog displaying the violated constraint.
     */
    public void createConstraintToast(FormIndex index, int saveStatus) {
        String constraintText;
        boolean shouldShowToast = true;
        String appearance = mFormController.getQuestionPromptApearance(index) + "";
        if (appearance.contains("confirm_read")) {
            FailedConstraint a = new FailedConstraint(index, saveStatus);
            createContraintDialog(a);

        } else if (!appearance.contains("confirm_read")) {
            switch (saveStatus) {
                case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
                    constraintText = mFormController
                            .getQuestionPromptConstraintText(index);
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.ANSWER_CONSTRAINT_VIOLATED, constraintText);

                    if (constraintText == null) {
                        constraintText = getString(R.string.invalid_answer_error);
                    } else {
                        shouldShowToast = constraintText.contains(PREFIX_SHOW_DIALOG) ? false : true;
                    }
                    break;
                case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                    constraintText = mFormController
                            .getQuestionPromptRequiredText(index);
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.ANSWER_REQUIRED_BUT_EMPTY, constraintText);

                    if (constraintText == null) {
                        constraintText = getString(R.string.required_answer_error);
                    } else {
                        shouldShowToast = constraintText.contains(PREFIX_SHOW_DIALOG) ? false : true;
                    }
                    break;
                default:
                    return;
            }
            if (shouldShowToast) {
                showCustomToast(constraintText, Toast.LENGTH_LONG);
            } else
                showCustomDialog(constraintText);
        }
    }

    public void resetQuestion() {
        if (mCurrentView instanceof RTAView) {
            List<QuestionWidget> qt = ((RTAView) mCurrentView).getWidgets();

            for (int i = 0; i < qt.size(); i++) {
                qt.get(i).setRefesh();
            }
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }
    }

    public void createConstraint(FormIndex index, int saveStatus) {

        List<QuestionWidget> qt = ((RTAView) mCurrentView).getWidgets();
        boolean isShowRequiredAsterisk = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_SHOW_REQUIRED_ASTERISK, false);
        for (final QuestionWidget q : qt) {
            if (q.getIndex() == index) {
                if (q.getAppearance() != null) {
                    if (q.getAppearance().contains("confirm_read")) {
                        FailedConstraint failedConstraint = new FailedConstraint(index, saveStatus);
                        listConfirmRead.add(failedConstraint);
                    }
                }
                String violatedText = null;
                switch (saveStatus) {
                    case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
                        violatedText = mFormController.getQuestionPromptConstraintText(index);
                        q.setRefreshQuestion(isShowRequiredAsterisk, violatedText, true);
                        q.setShowingIconRequierOrContranint(true);
                        break;
                    case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                        violatedText = mFormController.getQuestionPromptRequiredText(index);
                        q.setRefreshQuestion(isShowRequiredAsterisk, violatedText, false);
                        q.setShowingIconRequierOrContranint(true);
                        break;
                    default:
                        violatedText =  this.getResources().getString(R.string.invalid_answer_error);
                        break;
                }
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.ANSWER_CONSTRAINT_VIOLATED, violatedText == null ? "" : violatedText);
            }
        }

    }

    public void createContraintDialog(FailedConstraint constraint) {
        String constraintMessage = "";
        if (constraint.status == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
            constraintMessage = mFormController.getQuestionPromptConstraintText(constraint.getFormIndex()) + "";
        } else if (constraint.status == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
            constraintMessage = mFormController.getQuestionPromptRequiredText(constraint.getFormIndex()) + "";
        }

        ArrayList<String> listMessage = cutConstraint(constraintMessage);
        String Messages = "";
        String confirmMessage = " I have read and understand this message.";
        if (listMessage != null && listMessage.size() > 0) {
            constraintMessage = listMessage.get(0);
            confirmMessage = listMessage.get(1);
        }
        final Dialog confirm_dialog = new Dialog(this);
        DisplayMetrics displaymetrics = new DisplayMetrics();

        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = (int) ((int) displaymetrics.widthPixels * 0.8);
        confirm_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        confirm_dialog.setCancelable(false);
        confirm_dialog.setContentView(R.layout.confirm_require_dialog_new);
        confirm_dialog.getWindow().setLayout(width, -1);
        confirm_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView title = (TextView) confirm_dialog.findViewById(R.id.txt_title_confirm);
        TextView txt_constranit = (TextView) confirm_dialog.findViewById(R.id.txt_constraint);
        txt_constranit.setText(confirmMessage);
        title.setText(constraintMessage);
        CheckBox checkBox_confirm = (CheckBox) confirm_dialog.findViewById(R.id.cb_dialog_required);

        ImageView icon_dialog = (ImageView) confirm_dialog.findViewById(R.id.ic_dialog_required);
        if (constraint.status == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
            icon_dialog.setImageResource(R.drawable.ic_constraint_dialog);
        } else if (constraint.status == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
            icon_dialog.setImageResource(R.drawable.ic_requied_dialog);
        }
        final Button btnOk = (Button) confirm_dialog.findViewById(R.id.btOk);
        btnOk.setEnabled(false);
        checkBox_confirm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    btnOk.setTextColor(getResources().getColor(R.color.colorPrimary));
                    btnOk.setEnabled(true);
                } else {
                    btnOk.setEnabled(false);
                    btnOk.setTextColor(getResources().getColor(R.color.gray_light));
                }
            }
        });
        btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm_dialog.dismiss();
                positionConfirmRead++;
                if (positionConfirmRead >= listConfirmRead.size()) {
                    positionConfirmRead = 0;
                    return;
                }
                createContraintDialog(listConfirmRead.get(positionConfirmRead));

            }
        });
        try {
            confirm_dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
            confirm_dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> cutConstraint(String constraint) {
        ArrayList<String> parameters = new ArrayList<>();
        Pattern thumbnailPattern = Pattern.compile("message\\(([^()]*|\\([^()]*\\))*\\)");
        Pattern thumbnailPattern1 = Pattern.compile("confirm\\(([^()]*|\\([^()]*\\))*\\)");
        Matcher matcher = thumbnailPattern.matcher(constraint);
        Matcher matcher1 = thumbnailPattern1.matcher(constraint);
        if (matcher.find()) {
            String message = matcher.group();
            message = new String(message.substring(8, message.length() - 1));
            parameters.add(message);
        }
        if (matcher1.find()) {
            String confirm = matcher1.group();
            confirm = new String(confirm.substring(8, confirm.length() - 1));
            parameters.add(confirm);
        }
        return parameters;
    }

    private void showCustomDialog(String constraintText) {
        String qName = "";
        String message = "";
        if (constraintText.contains(PREFIX_SHOW_DIALOG)) {
            constraintText = constraintText.substring(7, constraintText.length() - 2);
            int index = constraintText.indexOf(",");
            if (index > 0) {
                qName = constraintText.substring(0, index);
                message = constraintText.substring(index + 1, constraintText.length());
            } else {
                return;
            }
        }
        if (constraintMessageDialog == null) {
            final String finalQName = qName;
            constraintMessageDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog))
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RTALog.d(t, "Try to save data to question by name" + finalQName);
                            saveDataToQuestionByName(finalQName);
                        }
                    })
                    .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            RTALog.d(t, "Try to save data to question by name" + finalQName);
                            saveDataToQuestionByName(finalQName);
                        }
                    })
                    .setMessage(message)
                    .create();
            constraintMessageDialog.setIcon(android.R.drawable.ic_dialog_info);
            constraintMessageDialog.setTitle(R.string.warning);

        } else {
            constraintMessageDialog.setMessage(message);
        }
        if (constraintMessageDialog != null)
            constraintMessageDialog.show();
    }

    private void saveDataToQuestionByName(String finalQName) {
        if (mFormController == null)
            return;
        try {
            //Save data by type of qeuestion widget.
            FormIndex saveIndex = mFormController.getFormIndexByName(finalQName);
            RTALog.d(t, "Successfully save answer for question name" + finalQName);
            int type = mFormController.getQuestionPrompt(saveIndex).getDataType();
            switch (type) {
                case org.javarosa.core.model.Constants.DATATYPE_DATE:
                    mFormController.saveAnswer(saveIndex, new DateData(new Date(System.currentTimeMillis())));
                    break;
                case org.javarosa.core.model.Constants.DATATYPE_TIME:
                    mFormController.saveAnswer(saveIndex, new TimeData(new Date(System.currentTimeMillis())));
                    break;
                case org.javarosa.core.model.Constants.DATATYPE_DATE_TIME:
                default:
                    mFormController.saveAnswer(saveIndex, new DateTimeData(new Date(System.currentTimeMillis())));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a toast with the specified message.
     *
     * @param message
     */
    private void showCustomToast(String message, int duration) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.toast_view, null);

        // set the text in the view
        TextView tv = (TextView) view.findViewById(R.id.message);
        tv.setText(message);

        Toast t = new Toast(this);
        t.setView(view);
        t.setDuration(duration);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    /**
     * Creates and displays a dialog asking the user if they'd like to create a
     * repeat of the current group.
     */
    private void createRepeatDialog() {
        if (isViewOnly) {
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_REPEAT_DIALOG, Constants.SHOW_NEXT_VIEW);
            //
            // Make sure the error dialog will not disappear.
            //
            // When showNextView() popups an error dialog (because of a
            // JavaRosaException)
            // the issue is that the "add new repeat dialog" is
            // referenced by mAlertDialog
            // like the error dialog. When the "no repeat" is clicked,
            // the error dialog
            // is shown. Android by default dismisses the dialogs when a
            // button is clicked,
            // so instead of closing the first dialog, it closes the
            // second.
            new Thread() {

                @Override
                public void run() {
                    FormEntryActivity.this
                            .runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    checkAlertAndShowNextView();
                                }
                            });
                }
            }.start();
        } else {
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_REPEAT_DIALOG, "");
//            mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
//            mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
//            DialogInterface.OnClickListener repeatListener = new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int i) {
//                    switch (i) {
//                        case DialogInterface.BUTTON_POSITIVE: // yes, repeat
//                            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_REPEAT_DIALOG, "add repeat");
//                            try {
//                                mFormController.newRepeat();
//                                currentIsRepeat = true;
//                                //check repeat in here.
//                                String repeatName = "";
//                                FormIndex formIndex = mFormController.getFormIndex();
//                                repeatName = formIndex.getReference().toShortString();
//                                currentRepeatName = repeatName.substring(0, repeatName.indexOf("["));
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                FormEntryActivity.this.createErrorDialog(
//                                        e.getMessage(), DO_NOT_EXIT);
//                                return;
//                            }
//                            if (!mFormController.indexIsInFieldList()) {
//                                // we are at a REPEAT event that does not have a
//                                // field-list appearance
//                                // step to the next visible field...
//                                // which could be the start of a new repeat group...
//                                //showNextView();
//                                checkAlertAndShowNextView();
//                            } else {
//                                // we are at a REPEAT event that has a field-list
//                                // appearance
//                                // just display this REPEAT event's group.
//                                refreshCurrentView();
//                            }
//                            ScreenUtils.enterFullscreen();
//                            break;
//                        case DialogInterface.BUTTON_NEGATIVE: // no, no repeat
//                            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_REPEAT_DIALOG, Constants.SHOW_NEXT_VIEW);
//
//                            currentIsRepeat = false;
//                            saveRepeatDataToLocalDb(currentRepeatName);
//                            currentRepeatName = "";
//
//                            //
//                            // Make sure the error dialog will not disappear.x
//                            //
//                            // When showNextView() popups an error dialog (because of a
//                            // JavaRosaException)
//                            // the issue is that the "add new repeat dialog" is
//                            // referenced by mAlertDialog
//                            // like the error dialog. When the "no repeat" is clicked,
//                            // the error dialog
//                            // is shown. Android by default dismisses the dialogs when a
//                            // button is clicked,
//                            // so instead of closing the first dialog, it closes the
//                            // second.
//                            new Thread() {
//
//                                @Override
//                                public void run() {
//                                    FormEntryActivity.this
//                                            .runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    try {
//                                                        Thread.sleep(500);
//                                                    } catch (InterruptedException e) {
//                                                        e.printStackTrace();
//                                                    }
//                                                    checkAlertAndShowNextView();
//                                                }
//                                            });
//                                }
//                            }.start();
//                            ScreenUtils.enterFullscreen();
//                            break;
//                    }
//                }
//            };
            if (mFormController.getLastRepeatCount() > 0) {
//                mAlertDialog.setTitle(getString(R.string.leaving_repeat_ask));
//                mAlertDialog.setMessage(getString(R.string.add_another_repeat,
//                        mFormController.getLastGroupText()));
//                mAlertDialog.setButton(getString(R.string.add_another),
//                        repeatListener);
//                mAlertDialog.setButton2(getString(R.string.leave_repeat_yes),
//                        repeatListener);
                add_one_more_group = new Dialog(this);
                DisplayMetrics displaymetrics = new DisplayMetrics();
                this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int width = (int) ((int) displaymetrics.widthPixels * 0.8);
                add_one_more_group.requestWindowFeature(Window.FEATURE_NO_TITLE);
                add_one_more_group.setCancelable(false);
                add_one_more_group.setContentView(R.layout.dialog_add_one_more_group);
                add_one_more_group.getWindow().setLayout(width, -1);
                add_one_more_group.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                TextView txt_messges = (TextView) add_one_more_group.findViewById(R.id.text_message);
                txt_messges.setText(getString(R.string.add_another_repeat,
                        mFormController.getLastGroupText()));
                Button btnadd = (Button) add_one_more_group.findViewById(R.id.button_add);
                btnadd.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            mFormController.newRepeat();
                            currentIsRepeat = true;
                            //check repeat in here.
                            String repeatName = "";
                            FormIndex formIndex = mFormController.getFormIndex();
                            repeatName = formIndex.getReference().toShortString();
                            currentRepeatName = repeatName.substring(0, repeatName.indexOf("["));
                        } catch (Exception e) {
                            e.printStackTrace();
                            FormEntryActivity.this.createErrorDialog(
                                    e.getMessage(), DO_NOT_EXIT);
                            return;
                        }
                        if (!mFormController.indexIsInFieldList()) {
                            // we are at a REPEAT event that does not have a
                            // field-list appearance
                            // step to the next visible field...
                            // which could be the start of a new repeat group...
                            //showNextView();
                            checkAlertAndShowNextView();
                        } else {
                            // we are at a REPEAT event that has a field-list
                            // appearance
                            // just display this REPEAT event's group.
                            refreshCurrentView();
                        }
                        ScreenUtils.enterFullscreen();
                        add_one_more_group.dismiss();
                    }
                });
                Button btnnotadd = (Button) add_one_more_group.findViewById(R.id.button_add_not);
                btnnotadd.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_REPEAT_DIALOG, Constants.SHOW_NEXT_VIEW);

                        currentIsRepeat = false;
                        saveRepeatDataToLocalDb(currentRepeatName);
                        currentRepeatName = "";

                        //
                        // Make sure the error dialog will not disappear.x
                        //
                        // When showNextView() popups an error dialog (because of a
                        // JavaRosaException)
                        // the issue is that the "add new repeat dialog" is
                        // referenced by mAlertDialog
                        // like the error dialog. When the "no repeat" is clicked,
                        // the error dialog
                        // is shown. Android by default dismisses the dialogs when a
                        // button is clicked,
                        // so instead of closing the first dialog, it closes the
                        // second.
                        new Thread() {

                            @Override
                            public void run() {
                                FormEntryActivity.this
                                        .runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Thread.sleep(500);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                checkAlertAndShowNextView();
                                            }
                                        });
                            }
                        }.start();
                        ScreenUtils.enterFullscreen();
                        add_one_more_group.dismiss();

                    }
                });


            } else {
//                mAlertDialog.setTitle(getString(R.string.entering_repeat_ask));
//                mAlertDialog.setMessage(getString(R.string.add_repeat,
//                        mFormController.getLastGroupText()));
//                mAlertDialog.setButton(getString(R.string.entering_repeat),
//                        repeatListener);
//                mAlertDialog.setButton2(getString(R.string.add_repeat_no),
//                        repeatListener);
                add_one_more_group = new Dialog(this);
                DisplayMetrics displaymetrics = new DisplayMetrics();
                this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int width = (int) ((int) displaymetrics.widthPixels * 0.8);
                add_one_more_group.requestWindowFeature(Window.FEATURE_NO_TITLE);
                add_one_more_group.setCancelable(false);
                add_one_more_group.setContentView(R.layout.dialog_add_one_more_group);
                add_one_more_group.getWindow().setLayout(width, -1);
                add_one_more_group.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                ImageView imgView = (ImageView) add_one_more_group.findViewById(R.id.icon_title);
                imgView.setImageResource(R.drawable.ic_add_new_group);
                TextView txt_messges = (TextView) add_one_more_group.findViewById(R.id.text_message);
                txt_messges.setText(getString(R.string.add_another_repeat,
                        mFormController.getLastGroupText()));
                TextView txt_Title = (TextView) add_one_more_group.findViewById(R.id.txt_title_group);
                txt_Title.setText(getString(R.string.entering_repeat_ask));
                Button btnadd = (Button) add_one_more_group.findViewById(R.id.button_add);
                btnadd.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            mFormController.newRepeat();
                            currentIsRepeat = true;
                            //check repeat in here.
                            String repeatName = "";
                            FormIndex formIndex = mFormController.getFormIndex();
                            repeatName = formIndex.getReference().toShortString();
                            currentRepeatName = repeatName.substring(0, repeatName.indexOf("["));
                        } catch (Exception e) {
                            e.printStackTrace();
                            FormEntryActivity.this.createErrorDialog(
                                    e.getMessage(), DO_NOT_EXIT);
                            return;
                        }
                        if (!mFormController.indexIsInFieldList()) {
                            // we are at a REPEAT event that does not have a
                            // field-list appearance
                            // step to the next visible field...
                            // which could be the start of a new repeat group...
                            //showNextView();
                            checkAlertAndShowNextView();
                        } else {
                            // we are at a REPEAT event that has a field-list
                            // appearance
                            // just display this REPEAT event's group.
                            refreshCurrentView();
                        }
                        ScreenUtils.enterFullscreen();
                        add_one_more_group.dismiss();
                    }
                });
                Button btnnotadd = (Button) add_one_more_group.findViewById(R.id.button_add_not);
                btnnotadd.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_REPEAT_DIALOG, Constants.SHOW_NEXT_VIEW);

                        currentIsRepeat = false;
                        saveRepeatDataToLocalDb(currentRepeatName);
                        currentRepeatName = "";

                        //
                        // Make sure the error dialog will not disappear.x
                        //
                        // When showNextView() popups an error dialog (because of a
                        // JavaRosaException)
                        // the issue is that the "add new repeat dialog" is
                        // referenced by mAlertDialog
                        // like the error dialog. When the "no repeat" is clicked,
                        // the error dialog
                        // is shown. Android by default dismisses the dialogs when a
                        // button is clicked,
                        // so instead of closing the first dialog, it closes the
                        // second.
                        new Thread() {

                            @Override
                            public void run() {
                                FormEntryActivity.this
                                        .runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Thread.sleep(500);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                checkAlertAndShowNextView();
                                            }
                                        });
                            }
                        }.start();
                        ScreenUtils.enterFullscreen();
                        add_one_more_group.dismiss();

                    }
                });
            }
            add_one_more_group.setCancelable(false);
            mBeenSwiped = false;
            add_one_more_group.show();
        }

    }

    /**
     * Creates and displays dialog with the given errorMsg.
     */
    public void createErrorDialog(String errorMsg, final boolean shouldExit) {
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_ERROR_DIALOG, "Shout exit: " + shouldExit);

        if (error_dialog != null && error_dialog.isShowing()) {
            errorMsg = mErrorMessage + "\n\n" + errorMsg;
            mErrorMessage = errorMsg;
        } else {
            // mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
            error_dialog = new Dialog(this);
            mErrorMessage = errorMsg;
            DisplayMetrics displaymetrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = (int) ((int) displaymetrics.widthPixels * 0.8);
            error_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            error_dialog.setCancelable(false);
            error_dialog.setContentView(R.layout.error_dialog);
            error_dialog.getWindow().setLayout(width, -1);
            error_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            TextView text = (TextView) error_dialog.findViewById(R.id.text_dialog);
            text.setText(errorMsg);

            Button dialogButton = (Button) error_dialog.findViewById(R.id.btn_dialog);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (shouldExit) {
                        mErrorMessage = null;
                        finish();
                    } else {
                        error_dialog.dismiss();
                    }
                }
            });
        }
        try {
            //some time it occur error "that was originally added here"?????
            error_dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates and displays dialog with the given errorMsg.
     */
    private void createErrorLinkFormDialog(String errorMsg) {
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_ERROR_DIALOG, "");

        if (error_dialog != null && error_dialog.isShowing()) {
            mErrorMessage = errorMsg;
        } else {
            // mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
            error_dialog = new Dialog(this);
            mErrorMessage = errorMsg;
            DisplayMetrics displaymetrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = (int) ((int) displaymetrics.widthPixels * 0.8);
            error_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            error_dialog.setCancelable(false);
            error_dialog.setContentView(R.layout.error_dialog);
            error_dialog.getWindow().setLayout(width, -1);
            error_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            TextView text = (TextView) error_dialog.findViewById(R.id.text_dialog);
            text.setText(errorMsg);

            Button dialogButton = (Button) error_dialog.findViewById(R.id.btn_dialog);
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mErrorMessage = null;
                    mFormController.numberQuestions = 0;

                    if (!isViewOnly)
                        if (isSentInstance && !isNextInstance) {
                            sendSavedBroadcast(IRSAction.ACTION_SEND_FINALIZE_INSTANCE, IdInstanceSend);
                        }
                    finish();
                }
            });
        }
        try {
            //some time it occur error "that was originally added here"?????
            error_dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a confirm/cancel dialog for deleting repeats.
     */
    public void createDeleteRepeatConfirmDialog(FormIndex formIndex) {
        if (isViewOnly)
            return;
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_DELETE_REPEAT_DIALOG, "Show");
        FormController formController = Collect.getInstance()
                .getFormController();
        final FormIndex currentIndex = formIndex == null ? formController.getFormIndex() : formIndex;

        int repeatcount = formController.getLastRepeatedGroupRepeatCount(currentIndex);
        int totalRepeats = formController.getLastRepeatedGroupNumRepetitions(currentIndex);

        boolean isLastRepeat = ((repeatcount + 1) ^ totalRepeats) == 0x0;
        if (formController.indexIsInLastGroupRemoveOnly(currentIndex) && !isLastRepeat) {
            mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
            mAlertDialog.setTitle(getString(R.string.delete_repeat));
            mAlertDialog.setMessage(getString(R.string.delete_repeat_err));
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_DELETE_REPEAT_DIALOG, "Delete err: " + getString(R.string.delete_repeat_err));
            mAlertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ScreenUtils.enterFullscreen();
                }
            });
        } else {
            mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
            mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
            String name = formController.getLastRepeatedGroupName(currentIndex);
            if (repeatcount != -1) {
                name += " (" + (repeatcount + 1) + ")";
            }

            final String nameRepeatDeleted = formController.getLastRepeatedGroupShortName(currentIndex);

            //RTALog.d(t, "appearance of group will delete " + databaseName);
            mAlertDialog.setTitle(getString(R.string.delete_repeat_ask));
            mAlertDialog
                    .setMessage(getString(R.string.delete_repeat_confirm, name));
            DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    switch (i) {
                        case DialogInterface.BUTTON_POSITIVE: // yes
                            //deleted repeat in here
                            //repeat name and repeat index was deleted
                            FormController formController = Collect.getInstance()
                                    .getFormController();
                            if (formController.currentPromptIsQuestion()) {
                                saveAnswersForCurrentScreen(false);
                            }
                            String formFamilyPath = RTASurvey.FAMILY_MEDIA_PATH + File.separator + mFormController.getFormFamilyID();
                            String formMediaPath = mFormController.getMediaFolder().getAbsolutePath();
                            String[] repeatLinks = ExternalDataUtil.getReaptLinks(nameRepeatDeleted, formMediaPath, formFamilyPath);
                            formController.increaseOrDecreaseRepeatCount(formController.getRepeatOneScreenIndex(currentIndex), false);
                            if (repeatLinks != null) {
                                formController.deleteRepeat(currentIndex, repeatLinks);
                            } else {
                                formController.deleteRepeat(currentIndex, nameRepeatDeleted);
                            }
                            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_DELETE_REPEAT_DIALOG, "Delete: Name" + nameRepeatDeleted);
                            ScreenUtils.enterFullscreen();
                            isReatpeatDelete = true;
                            showPreviousView();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE: // no
                            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_DELETE_REPEAT_DIALOG, "Delete: cancel");
                            break;
                    }
                }
            };
            mAlertDialog.setCancelable(false);
            mAlertDialog.setButton(getString(R.string.discard_group), quitListener);
            mAlertDialog.setButton2(getString(R.string.delete_repeat_no),
                    quitListener);
        }
        mAlertDialog.show();
    }

    /**
     * Saves data and writes it to disk. If exit is set, program will exit after
     * save completes. Complete indicates whether the user has marked the
     * isntancs as complete. If updatedSaveName is non-null, the instances
     * content provider is updated with the new name
     */
    // by default, save the current screen
    private boolean saveDataToDisk(boolean exit, boolean complete,
                                   String updatedSaveName) {
        return saveDataToDisk(exit, complete, updatedSaveName, true, null);
    }

    // by default, save the current screen
    private boolean saveDataToDisk(boolean exit, boolean complete,
                                   String updatedSaveName, String statusSendFinalize) {
        RTASurvey.uuidOFInstance = "";
        return saveDataToDisk(exit, complete, updatedSaveName, true, statusSendFinalize);
    }

    private void sendWorkingToServer(boolean isSendWorking) {
        if (isSendWorking) {
            FormController formController = RTASurvey.getInstance().getFormController();
            if (!QACheckPointImp.getInstance(formController.getUuid()).isWaitingQAResult() && !QACheckPointImp.getInstance(formController.getUuid()).isSending()) {
                File fzip = new File(RTASurvey.ZIP_PATH);
                if (fzip.exists()) {
                    fzip.delete();
                }
                mUploadWorkingTask = new UploadWorkingTask(this, RTASurvey.ZIP_PATH, RTASurvey.getInstance().getPremiumRegAddress());
                mUploadWorkingTask.upload(UserListDbHelper.getInstance().getMainUsername());
            }
        }
    }

    // but if you want save in the background, can't be current screen
    private boolean saveDataToDisk(boolean exit, boolean complete,
                                   String updatedSaveName, boolean current, String statusSaveFinalize) {
        // save current answer
        if (current) {
            if (!saveAnswersForCurrentScreen(complete)) {
                Toast.makeText(this, getString(R.string.data_saved_error),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        synchronized (saveDialogLock) {
            String keyAnswer = getKeyAnswerValue();
            String uuid_ssname = getSSName();
            String lastUpdatedIndex = getLastUpdateIndex();
            mSaveToDiskTask = new SaveToDiskTask(getIntent().getData(), exit,
                    complete, updatedSaveName, statusSaveFinalize, keyAnswer, uuid_ssname, lastUpdatedIndex);
            mSaveToDiskTask.setSkipValidate(skipValidate);
            mSaveToDiskTask.setFormSavedListener(this);
            mAutoSaved = true;
            if (!isNextInstance) {
                showDialog(SAVING_DIALOG);
            } else {
                ProgressBarHandler.getInstance(this).show();
            }
            // show dialog before we execute...

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mSaveToDiskTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                mSaveToDiskTask.execute();
            }
        }

        return true;
    }

    /**
     * Create a dialog with options to save and exit, save, or quit without
     * saving
     */
    private void createQuitDialog() {
        String title;
        title = (mFormController == null) ? null : mFormController.getFormTitle();
        if (title == null) {
            title = "<no form loaded>";
        }

        if (isViewOnly) {
            ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, "show");
//            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog))
//                    .setIcon(android.R.drawable.ic_dialog_info)
//                    .setTitle(getString(R.string.quit_application, title))
//                    .setNeutralButton(getString(R.string.do_not_exit),
//                            new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int id) {
//                                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, "cancel");
//                                    dialog.cancel();
//
//                                }
//                            })
            //                   .setPositiveButton(getString(R.string.exit_view_data),
//                            new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_DISCARD_AND_EXIT);
//                                    RTASurvey.getInstance().setExternalDataPullResultManager(null);
//                                    removeTempInstance();
//                                    removePendingInstance();
//                                    //check save info
//                                    //get status;
//                                    String st = getSavingStatus();
//                                    if (st == null) {
//                                        //delete data
//                                        getContentResolver().delete(InstanceColumns.CONTENT_URI,
//                                                InstanceColumns.INSTANCE_FILE_PATH + " = \"" +
//                                                        mFormController.getInstancePath().getAbsolutePath()
//                                                        + "\"", null);
//                                    }
//                                    finishReturnInstance(false);
//                                }
//                            }
//                    );
            exit_dialog = new Dialog(this);
            DisplayMetrics displaymetrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = (int) ((int) displaymetrics.widthPixels * 0.8);
            exit_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            exit_dialog.setCancelable(false);
            exit_dialog.setContentView(R.layout.exit_dialog);
            exit_dialog.getWindow().setLayout(width, -1);
            exit_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            TextView text = (TextView) exit_dialog.findViewById(R.id.txt_title);
            text.setText(getString(R.string.quit_application, title));
            Button dialogIgnoreButton = (Button) exit_dialog.findViewById(R.id.btn_ignore);
            dialogIgnoreButton.setText(R.string.exit_view_data);
            dialogIgnoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_DISCARD_AND_EXIT);
                    RTASurvey.getInstance().setExternalDataPullResultManager(null);
                    removeTempInstance();
                    removePendingInstance();
                    //check save info
                    //get status;
                    String st = getSavingStatus();
                    if (st == null) {
                        //delete data
                        getContentResolver().delete(InstanceColumns.CONTENT_URI,
                                InstanceColumns.INSTANCE_FILE_PATH + " = \"" +
                                        mFormController.getInstancePath().getAbsolutePath()
                                        + "\"", null);
                    }
                    finishReturnInstance(false);
                }
            });
            Button dialogCancelButton = (Button) exit_dialog.findViewById(R.id.btn_cancel);
            dialogCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE,
                            Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_CANCEL);
                    exit_dialog.dismiss();
                }
            });

            //... Add "request to edit" button here
            if (canRequestToEdit()) {
//                dialogBuilder.setNegativeButton(R.string.btn_request_return_instance,
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                requestEditInstance();
//                            }
//                        });
                Button dialogSaveButton = (Button) exit_dialog.findViewById(R.id.btn_save);
                dialogSaveButton.setText(R.string.btn_request_return_instance);
                dialogSaveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        exit_dialog.dismiss();
                        requestEditInstance();
                    }
                });
            }

            try {
                exit_dialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        //track mini log open form
        if (mFormController != null) {
            MiniLogFormAction miniLogFormAction = MiniLogFormAction.getMinilogAction(mFormController.getUuid());
            miniLogFormAction.writeLog(MiniLogEntity.BUTTON_BACK_CODE, System.currentTimeMillis());
        }

        String[] items;
        if (PreferencesManager.getPreferencesManager(this).getBooleanInAdmin(
                AdminPreferencesActivity.KEY_SAVE_MID, true)) {
            items = new String[]{getString(R.string.keep_changes), getString(R.string.do_not_save)};
        } else {
            items = new String[]{getString(R.string.do_not_save)};
        }
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, "show");
        exit_dialog = new Dialog(this);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = (int) ((int) displaymetrics.widthPixels * 0.8);
        exit_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        exit_dialog.setCancelable(false);
        exit_dialog.setContentView(R.layout.exit_dialog);
        exit_dialog.getWindow().setLayout(width, -1);
        exit_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView text = (TextView) exit_dialog.findViewById(R.id.txt_title);
        text.setText(getString(R.string.quit_application, title));

        Button dialogSaveButton = (Button) exit_dialog.findViewById(R.id.btn_save);
        dialogSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PreferencesManager.getPreferencesManager(RTASurvey.getInstance()).getBooleanInAdmin(AdminPreferencesActivity.KEY_SAVE_MID, true)) {
                    ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE,
                            Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_SAVE_AND_EXIT);

                    nameofInstance = getInstanceDisplayName(mFormController);
                    saveDataToDisk(EXIT, isInstanceComplete(false), nameofInstance);
                    ProcessDbHelper.getInstance().removeProcess(mFormController.getSubmissionMetadata().instanceId);
                    exit_dialog.dismiss();
                    updateInstanceplushInfor();
                } else { // discard changes and exit
                    exit_dialog.dismiss();
                    discardChangesAndExit();
                }
            }
        });
        Button dialogIgnoreButton = (Button) exit_dialog.findViewById(R.id.btn_ignore);
        dialogIgnoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discardChangesAndExit();
            }
        });
        Button dialogCancelButton = (Button) exit_dialog.findViewById(R.id.btn_cancel);
        dialogCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE,
                        Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_CANCEL);
                exit_dialog.dismiss();

                //Make empty list media file to delete
                RTASurvey.getInstance().clearListNameFileToDelete();
            }
        });
        try {
            exit_dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void discardChangesAndExit() {
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE,
                Constants.SHOW_QUIT_DIALOG, Constants.BUTTON_DISCARD_AND_EXIT);
        // close all open databases of external data.
        Collect.getInstance().getExternalDataManager().close();
        RTASurvey pre = RTASurvey.getInstance();
        pre.setExternalDataPullResultManager(null);
        removeTempInstance();
        removePendingInstance();
        //get status;
        String st = getSavingStatus();
        if (st == null) { //delete instance which is never saved before
            //delete data
            getContentResolver().delete(InstanceColumns.CONTENT_URI,
                    InstanceColumns.INSTANCE_FILE_PATH + " = \"" +
                            mFormController.getInstancePath().getAbsolutePath()
                            + "\"", null);
            getContentResolver().delete(InstanceColumns.CONTENT_URI,
                    InstanceColumns.INSTANCE_QA_PATH + " = \"" +
                            mFormController.getQaFolder().getAbsolutePath()
                            + "\"", null);
        } else if (getLocalDatabase() != null) {
            // revert local database data to the latest saving
            TreeElement bakRoot = mFormController.getBackupInstanceTree();
            if (bakRoot != null) {
                instancePoolManager.insertLocalData(getLocalDatabase(),
                        mFormController, bakRoot, false, true);

            }
        }
        ProcessDbHelper.getInstance().removeProcess(mFormController.getSubmissionMetadata().instanceId);

        safeExitForm = true;
        finishReturnInstance(false);
    }

    private void createRequestEditDialog(final String uuid, final String name,
                                         final String formId, final String version, final String type) {
        final ScreenRequestDialog screenRequestDialog = new ScreenRequestDialog(this);
        screenRequestDialog.show();
        Button btn_cancel = (Button) screenRequestDialog.findViewById(R.id.cancel_request);
        final Button btn_submit = (Button) screenRequestDialog.findViewById(R.id.submit_request);
        final EditText et_input = (EditText) screenRequestDialog.findViewById(R.id.request_comment);
        if (et_input.length() == 0) {
            btn_submit.setEnabled(false);
        }
        et_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


            }

            @Override
            public void afterTextChanged(Editable s) {
                if (et_input.getText().length() > 0) {
                    btn_submit.setEnabled(true);
                    btn_submit.setBackground(getResources().getDrawable(R.drawable.btn_backgroud_private));
                    btn_submit.setTextColor(getResources().getColor(R.color.text_color_white));
                } else {
                    btn_submit.setEnabled(false);
                    btn_submit.setBackground(getResources().getDrawable(R.drawable.btn_backgroud_white));
                    btn_submit.setTextColor(getResources().getColor(R.color.text_color_grey));
                }
            }
        });
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Common.hideKeyboard(FormEntryActivity.this, et_input);
                screenRequestDialog.cancel();
            }
        });
        btn_submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.isConnect(FormEntryActivity.this)) {
                    Common.hideKeyboard(FormEntryActivity.this, et_input);
                    String comment = et_input.getText().toString();
                    screenRequestDialog.cancel();
                    startRequestReturnTask(uuid, name, formId, version, comment, type);
                } else if (!Common.isConnect(FormEntryActivity.this)) {
                    MessageUtils.showNetworkInfo(FormEntryActivity.this);
                } else if (ConnectionService.RequestReturn.TYPE_FINALIZED.equals(type)) {
                    UserInfo u = Common.getUserInfo(getApplicationContext());
                    if (u != null && "1".equals(u.getAuto_approve_edit_request())) {
                        Common.hideKeyboard(FormEntryActivity.this, et_input);
                        String comment = et_input.getText().toString();
                        screenRequestDialog.cancel();
                        SurveyCollectorUtils.forceIncomplete(getApplicationContext(), uuid);
                        MessageUtils.showToastInfo(getApplicationContext(),
                                getString(R.string.request_return_instance_successful_local, name));
                        saveOfflineRequest(uuid, name, formId, version, comment, type);
                        finishReturnInstance(false);
                    }
                }
            }

        });
    }

    void saveOfflineRequest(String uuid, String name, String formId, String version,
                            String comment, String type) {
        try {
            String host = RTASurvey.getInstance().getServerUrl();
            String key = RTASurvey.getInstance().getServerKey();
            String username = RTASurvey.getInstance().getCredential(RTASurvey.KEY_CREDENTIAL_USERNAME);
            String serviceUrl = host + ConnectionService.SERVICE_REQUEST_RETURN_INSTANCE;
            ConnectionService.RequestReturn obj = new ConnectionService.RequestReturn(username,
                    name, uuid, formId, version, comment, type, "1", System.currentTimeMillis());
            String data = StringUtil.object2JSON(obj);
            data = SimpleCrypto.encrypt(key, data);
            FailedReportManager.getInstance().saveReport(host, serviceUrl, data, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRequestReturnTask(String uuid, String name, String formId, String version,
                                        String comment, String type) {
        if (Common.isConnect(this)) {
            new ConnectionTask(this) {
                String uuid;

                @Override
                protected Object doInBackground(String... params) {
                    uuid = params[0];
                    String name = params[1];
                    String formId = params[2];
                    String version = params[3];
                    String comment = params[4];
                    String type = params[5];
                    return ConnectionService.getInstance()
                            .requestReturnInstance(RTASurvey.getInstance().getServerUrl(),
                                    RTASurvey.getInstance().getServerKey(),
                                    UserListDbHelper.getInstance().getMainUsername(),
                                    uuid, name, formId, version, comment, type, "0");
                }

                @Override
                protected void onPostExecute(Object result) {
                    super.onPostExecute(result);
                    Response response = (Response) result;
                    if (response != null) {
                        if (StdCodeDBHelper.STT_S1001.equals(response.getSttCode())) {
                            ContentValues v = new ContentValues();
                            v.put(InstanceColumns.REQUEST_TO_EDIT_DATE, System.currentTimeMillis());
                            activity.getContentResolver().update(InstanceColumns.CONTENT_URI, v,
                                    InstanceColumns.INSTANCE_UUID + "=?", new String[]{uuid});
                            MessageUtils.showToastInfo(activity, R.string.request_return_instance_successful);

                            //create instance package for sync to server
                            SurveyCollectorUtils.addNewBackupPackageToUploadQueue(activity, uuid);
                        } else {
                            String failedMsg = !TextUtils.isEmpty(response.getMsgCode()) ?
                                    StdCodeDBHelper.getInstance().getCodeDesc(response.getMsgCode()) : "";
                            showFailedDialog(R.string.request_return_instance_err_failed, failedMsg);
                        }
                    } else {
                        showFailedDialog(R.string.request_return_instance_err_failed, null);
                    }
                    finishReturnInstance(false);
                }

                private void showFailedDialog(int msgId, String extra) {
                    String error = activity.getString(msgId)
                            + (extra == null ? "" : ("\n" + extra));
                    MessageUtils.showToastInfo(activity, error);
                }
            }.execute(uuid, name, formId, version, comment, type);
        } else {
            MessageUtils.showNetworkInfo(this);
        }
    }

    /**
     * this method cleans up unneeded files when the user selects 'discard and
     * exit'
     */
    private void removeTempInstance() {
        // attempt to remove any scratch file
        File temp = SaveToDiskTask.getSavePointFile(mFormController.getInstancePath());
        if (temp.exists()) {
            temp.delete();
        }

        String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] selectionArgs = {mFormController.getInstancePath()
                .getAbsolutePath()};

        boolean erase = false;
        {
            Cursor c = null;
            try {
                c = getContentResolver().query(InstanceColumns.CONTENT_URI,
                        null, selection, selectionArgs, null);
                erase = (c.getCount() < 1);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        // if it's not already saved, erase everything
        if (erase) {
            // delete media first
            String instanceFolder = mFormController.getInstancePath()
                    .getParent();
            int images = MediaUtils
                    .deleteImagesInFolderFromMediaProvider(mFormController
                            .getInstancePath().getParentFile());
            int audio = MediaUtils
                    .deleteAudioInFolderFromMediaProvider(mFormController
                            .getInstancePath().getParentFile());
            int video = MediaUtils
                    .deleteVideoInFolderFromMediaProvider(mFormController
                            .getInstancePath().getParentFile());

            File f = new File(instanceFolder);
            if (f.exists() && f.isDirectory()) {
                for (File del : f.listFiles()) {
                    del.delete();
                }
                f.delete();
            }

            // seconds, delete qa files
            File qaFolder = mFormController.getQaFolder();
            if (qaFolder.exists() && qaFolder.isDirectory()) {
                for (File del : qaFolder.listFiles()) {
                    Log.i(t, "deleting file: " + del.getAbsolutePath());
                    del.delete();
                }
                qaFolder.delete();
            }
        }
    }

    /**
     * Confirm clear answer dialog
     */
    private void createClearDialog(final QuestionWidget qw) {
        if (isViewOnly)
            return;

        RTASurvey
                .getInstance()
                .getRTAActivityLogger()
                .logInstanceAction(this, "createClearDialog", "show",
                        qw.getPrompt());

        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_CLEAR_DIALOG, "show");

        mAlertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.full_screen_dialog)).create();
        mAlertDialog.setTitle(getString(R.string.clear_answer_ask));

        String question = qw.getPrompt().getLongText();
        if (question != null) {
            if (question.contains("<hint>")) {
                question = removeMediaHint(question);
            }
            if (question.contains("<img")) {
                question = removeMediaImg(question);
            }
        } else
            question = "";

        if (question.length() > 50) {
            question = question.substring(0, 50) + "...";
        }

        //TODO handle string before get Spanned

        mAlertDialog.setMessage(question == null ? "" : org.odk.collect.android.utilities.TextUtils.textToHtml(getString(R.string.clearanswer_confirm,
                question)));

        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // yes
                        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, "clear answer: confirm " + qw.getPrompt().getIndex());
                        clearAnswerAndBind(qw, ((RTAView) mCurrentView).getWidgets());
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // no
                        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_QUIT_DIALOG, "clear answer : cancel" + qw.getPrompt().getIndex());
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog
                .setButton(getString(R.string.discard_answer), quitListener);
        mAlertDialog.setButton2(getString(R.string.clear_answer_no),
                quitListener);
        mAlertDialog.show();
    }

    private String removeMediaHint(String question) {
        question = question.substring(0, question.indexOf("<hint>")) + question.substring(question.indexOf("</hint>") + 7, question.length());
        return question;
    }

    private String removeMediaImg(String question) {
        question = question.substring(0, question.indexOf("<img")) + question.substring(question.indexOf(">") + 1, question.length());
        return question;
    }

    /**
     * Creates and displays a dialog allowing the user to set the language for
     * the form.
     */
    private void createLanguageDialog() {
        ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_LANGUAGE_DIALOG);
        final String[] languages = mFormController.getLanguages();
        int selected = -1;
        if (languages != null) {
            String language = mFormController.getLanguage();
            for (int i = 0; i < languages.length; i++) {
                if (language.equals(languages[i])) {
                    selected = i;
                }
            }
        }
        changes_language = new Dialog(this);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = (int) ((int) displaymetrics.widthPixels * 0.8);
        changes_language.requestWindowFeature(Window.FEATURE_NO_TITLE);
        changes_language.setCancelable(false);
        changes_language.setContentView(R.layout.dialog_changes_language);
        changes_language.getWindow().setLayout(width, -1);
        changes_language.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        Button btn_cancel = (Button) changes_language.findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changes_language.dismiss();
            }
        });
        RadioGroup ll = (RadioGroup) changes_language.findViewById(R.id.group_language);

        for (int i = 1; i <= languages.length; i++) {
            final RadioButton rdbtn = new RadioButton(this);
            rdbtn.setText(languages[i - 1]);
            rdbtn.setTextColor(Color.BLACK);
            if (selected == i - 1) {
                rdbtn.setChecked(true);
            }
            final int whichButton = i - 1;
            rdbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (rdbtn.isChecked()) {

                        ContentValues values = new ContentValues();
                        values.put(FormsColumns.LANGUAGE,
                                languages[whichButton]);
                        String selection = FormsColumns.FORM_FILE_PATH
                                + "=?";
                        String selectArgs[] = {mFormPath};
                        int updated = getContentResolver().update(
                                FormsColumns.CONTENT_URI, values,
                                selection, selectArgs);

                        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_LANGUAGE_DIALOG, "changeLanguage."
                                + languages[whichButton]);
                        mFormController
                                .setLanguage(languages[whichButton]);
                        changes_language.dismiss();
                        if (mFormController.currentPromptIsQuestion()) {
                            saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                        }

                        currentQaCheckPoint = null;
                        currentCommand = "";
                        waitingCheckpoint = false;
                        refreshCurrentView();
                    }
                }
            });
            ll.addView(rdbtn);
        }
        changes_language.show();


    }

    /**
     * We use Android's dialog management for loading/saving progress dialogs
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.CREATE_PROGRESS_DIALOG, "show");
                loading_dialog = new Dialog(this);
                DisplayMetrics displaymetrics = new DisplayMetrics();
                this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int width = (int) ((int) displaymetrics.widthPixels * 0.8);
                loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                loading_dialog.setCancelable(false);
                loading_dialog.setContentView(R.layout.loading_dialog);
                loading_dialog.getWindow().setLayout(width, -1);
                loading_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                TextView textTitle = (TextView) loading_dialog.findViewById(R.id.txt_title);
                textTitle.setText(getString(R.string.loading_form));
                TextView textMessage = (TextView) loading_dialog.findViewById(R.id.txt_message);
                textMessage.setText(getString(R.string.please_wait));
                ProgressBar proLoading = (ProgressBar) loading_dialog.findViewById(R.id.pro_loading);
                proLoading.setIndeterminate(true);
                return loading_dialog;
            case SAVING_DIALOG:
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.CREATE_PROGRESS_DIALOG, "SAVING_DIALOG");
                saving_dialog = new Dialog(this);
                DisplayMetrics displaymetrics1 = new DisplayMetrics();
                this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics1);
                int width1 = (int) ((int) displaymetrics1.widthPixels * 0.8);
                saving_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                saving_dialog.setCancelable(false);
                saving_dialog.setContentView(R.layout.saving_dialog);
                saving_dialog.getWindow().setLayout(width1, -1);
                saving_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                TextView textTitle1 = (TextView) saving_dialog.findViewById(R.id.txt_title);
                textTitle1.setText(getString(R.string.saving_form));
                TextView textMessage1 = (TextView) saving_dialog.findViewById(R.id.txt_message);
                textMessage1.setText(getString(R.string.please_wait));
                ProgressBar proLoading1 = (ProgressBar) saving_dialog.findViewById(R.id.pro_loading);
                proLoading1.setIndeterminate(true);
                saving_dialog
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.CREATE_PROGRESS_DIALOG, Constants.BUTTON_CANCEL);
                                cancelSaveToDiskTask();
                            }
                        });
                return saving_dialog;
            case CONSTRAIN_DIALOG:
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.CREATE_PROGRESS_DIALOG, "CONSTRAIN_DIALOG");
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setTitle(getString(R.string.check_constrain));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                return mProgressDialog;

        }
        return null;
    }

    private void cancelSaveToDiskTask() {
        synchronized (saveDialogLock) {
            if (mSaveToDiskTask != null) {
                try {
                    mSaveToDiskTask.setFormSavedListener(null);
                    boolean cancelled = mSaveToDiskTask.cancel(true);
                    Timber.w("Cancelled SaveToDiskTask! (%s)", cancelled);
                    mSaveToDiskTask = null;

                    if (mExportToCsvTask != null) {
                        boolean c = mExportToCsvTask.cancel(true);
                        mExportToCsvTask = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Dismiss any showing dialogs that we manually manage.
     */
    private void dismissDialogs() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CountDownTimerInstance.getInstance().stopCountDownTimer();
        BarcodeCaptureService.isShowChecked = false;
        stopService(new Intent(this, BarcodeCaptureService.class));
        dismissDialogs();
        if (waitingQAResultCountDown != null)
            waitingQAResultCountDown.cancel();

        try {
            unregisterReceiver(callReceiver);
        } catch (Exception e) {
            Log.e(t, "Error from unregisterReceiver(callReceiver)");
        }

        isBreakThreadQA = true;

        if (waitingQAResultDialog != null && waitingQAResultDialog.isShowing())
            waitingQAResultDialog.dismiss();

        //check and dismiss qa check point visible if it exite
        if (mCurrentView instanceof RTAView) {
            for (QuestionWidget q : ((RTAView) mCurrentView).getWidgets()) {
                if (q instanceof QACheckPointWidget) {
                    if ((((QACheckPointWidget) q).getDialogProcessQAchecking() != null && ((QACheckPointWidget) q).getDialogProcessQAchecking().isShowing()))
                        ((QACheckPointWidget) q).getDialogProcessQAchecking().dismiss();
                }
            }
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }

        if (mFormController != null)
            QACheckPointImp.getInstance(mFormController.getUuid()).release();

        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.PAUSE_APP, "");
        // make sure we're not already saving to disk. if we are, currentPrompt
        // is getting constantly updated
        if (mSaveToDiskTask == null
                || mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
            if (mCurrentView != null && mFormController != null
                    && mFormController.currentPromptIsQuestion()) {
                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
            }
        }

        if (mFormController != null) {
            String instanceFilePath = mFormController.getInstancePath().getAbsolutePath();
            String qaPath = mFormController.getQaFolder().getAbsolutePath();
            if (getContentResolver().getType(getIntent().getData()).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
                FASender.sendBroadcastPausedEditForm(this, instanceFilePath, qaPath);
            } else if (getContentResolver().getType(getIntent().getData()).equals(FormsColumns.CONTENT_ITEM_TYPE)) {
                FASender.sendBroadcastPausedFillForm(this, instanceFilePath, qaPath);
            }
        } else {
            Log.e(t, "unrecognize");
        }

        // hide quick note
        QuickNoteManager manager = QuickNoteManager.getInstance();
        if (!isShowFromtList) {
            manager.hideQuickNote();
        }

        RecordManager.stopAll(this);
        RTAInputManager.getInstance().hideRTAKeyboard();

        //safe saving
        if (!isViewOnly && !isSaveConfig && !safeExitForm) {
            nonblockingCreateSavePointData();
            updateInstanceConfigFile(mFormController == null ? "" : mFormController.getInstancePath().getAbsolutePath());
        }
        if (mCurrentView != null && mCurrentView instanceof RTAView) {
            View v = ((RTAView) mCurrentView).getFocusedChild();
            if (v != null) {
                v.clearFocus();
            }
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }
        Log.d("FORM_ENTRY", "onPause() - end");
    }

    @Override
    protected void onResume() {
        super.onResume();

        SurveyCollectorUtils.cleanLatestInstanceConfig();
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.RESUME_APP, "");
        isSaveConfig = false;

        //stop the floating ntf-action
        if (/*!isReadOnlyOverride && */AppSettingSharedPreferences.RT_HELPER_HIDE.equals(AppSettingSharedPreferences
                .getInstance(this).getPref(AppSettingSharedPreferences.KEY_RT_HELPER_DISPLAY,
                        AppSettingSharedPreferences.RT_HELPER_HIDE))) {
            stopService(new Intent(this, FloatingNtfActionService.class));
            stopService(new Intent(this, NtfActionListOverlayService.class));
        }

        RTASurvey.getInstance().setActivity(this);

        if (callReceiver != null && callIntentFilter != null) {
            registerReceiver(callReceiver, callIntentFilter);
        }

        if (isOnActivityResult) {
            isOnActivityResult = false;
            return;
        }
        if (mErrorMessage != null) {
            if (mAlertDialog != null && !mAlertDialog.isShowing()) {
                createErrorDialog(mErrorMessage, EXIT);
            } else {
                return;
            }
        }

        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(this);
            isLoading = true;
            if (mFormController == null
                    && mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormController fec = mFormLoaderTask.getFormController();
                if (fec != null) {
                    loadingComplete(mFormLoaderTask);
                } else {
                    dismissFormDialog(PROGRESS_DIALOG);
                    FormLoaderTask t = mFormLoaderTask;
                    mFormLoaderTask = null;
                    t.cancel(true);
                    t.destroy();
                    startActivity(new Intent(this, FormSelectionActivity.class));
                }
            }
        } else {
            if (mFormController == null) {
                // there is no mFormController -- fire MainMenu activity?
                startActivity(new Intent(this, FormSelectionActivity.class));
                return;
            } else {
                refreshCurrentView();
            }
        }

        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(this);
        }

        if (!isLoading) {
            if (mFormController != null) {
                String instanceFilePath = mFormController.getInstancePath().getAbsolutePath();
                String qaPath = mFormController.getQaFolder().getAbsolutePath();
                try {
                    if (getContentResolver().getType(getIntent().getData()).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
                        FASender.sendBroadcastResumedEditForm(this, instanceFilePath, qaPath);
                    } else if (getContentResolver().getType(getIntent().getData()).equals(FormsColumns.CONTENT_ITEM_TYPE)) {
                        FASender.sendBroadcastResumedFillForm(this, instanceFilePath, qaPath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ScreenUtils.enterFullscreen();
        }

        if (LinphoneService.isReady()) {
            final View miniView = LinphoneService.instance().getIncallMiniView();

            if (miniView != null) {
                if (miniView.isShown()) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
                    params.setMargins(0, miniView.getHeight(), 0, 0);
                    content.setLayoutParams(params);

                    updateUIListener = new OnUpdateUIListener() {
                        @Override
                        public void updateUIByServiceStatus(boolean serviceConnected) {

                        }

                        @Override
                        public void registrationState(boolean isConnected, String statusMessage) {

                        }

                        @Override
                        public void updateToCallWidget(boolean isCalled) {

                        }

                        @Override
                        public void launchIncomingCallActivity() {

                        }

                        @Override
                        public void dismissCallActivity() {
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) content.getLayoutParams();
                            params.setMargins(0, 0, 0, 0);
                            content.setLayoutParams(params);
                        }
                    };

                    LinphoneService.addOnUpdateUIListener(updateUIListener);
                }
            }
        }
    }

    private void stopPlayback() {
        if (mProgressRefresher != null) {
            mProgressRefresher.removeCallbacksAndMessages(null);
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                stopPlayback();
                if (RTAInputManager.getInstance().hideRTAKeyboard())
                    return true;
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_BACK, "");
                createQuitDialog();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (event.isAltPressed() && !mBeenSwiped) {
                    mBeenSwiped = true;

                    checkAlertAndShowNextView();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (event.isAltPressed() && !mBeenSwiped) {
                    mBeenSwiped = true;

                    checkAlertAndShowPreviousView();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                } else {
                    start();
                }
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                start();
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
                updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        stopPlayback();
        if (mReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
        //for call widgets
        if (CallRecordWidgets.COUNT_CALL != 0)
            CallRecordWidgets.COUNT_CALL = 0;
        if (RecordService.RECORD_NAME != null) {
            RecordService.RECORD_NAME = null;
        }

        if (!isViewOnly) {
            updateInstanceLastIndex();

            //clean QA task returned in Auto QA process, which aren't saved
            deleteDirtyQATasks();

            //clean complete status which cached in QA Review UI service
            CheckNoteService.cleanCache();
        }

        //clear sentinel objects (form controller, activity)
        FormController globalFc = RTASurvey.getInstance().getFormController();
        if (globalFc != null && globalFc.equals(mFormController)) {
            RTASurvey.getInstance().setFormController(null);
        }

        //Clear hashmap of user editted flags
        RTASurvey.getInstance().clearUserEditedFlag();

        //try to re-start floating ntfAction counter
        startService(new Intent(getApplicationContext(), FloatingNtfActionService.class));

        if (getLocalDatabase() != null) {
            getLocalDatabase().close();
        }

        //Exit ipcall
        stopService(new Intent(this, InIpCallService.class));
        cleanConfigs();
        RTASurvey.getInstance().clearValuePreloadHashMap();

        // clean mini-log flags
        MiniLogFormAction.clearMinilogIndexFlags();

        super.onDestroy();

    }

    private void cleanConfigs() {
        if (IPCallManager.isInstantiated())
            IPCallManager.getInstance().cleanConfigs(this);
        if (TraceLocationService.isRunning()) {
            stopService(new Intent(this, TraceLocationService.class));
        }
        RTASurvey.getInstance().getExternalDataManagerImplMultipleDbs().close();
        RTAInputManager.getInstance().reset(this);
        RTASurvey.getInstance().cleanCacheValues();
        RTASurvey.getInstance().cleanCacheAnswers();
        RTASurvey.getInstance().cleanCacheFlags();
        SelectMultiWidgetDataHandler.getInstance().clearValueMultiChoiceHashMap();
    }

    private void deleteDirtyQATasks() {
        ContentResolver cr = getContentResolver();
        if (mFormController != null) {
            String uuid = mFormController.getUuid();

            //delete redundant task (from last instance saving)
            int deleted = cr.delete(QATaskProviderAPI.TaskColumns.CONTENT_URI,
                    QATaskProviderAPI.TaskColumns.UUID + " like ? and "
                            + QATaskProviderAPI.TaskColumns.INSTANCE_SAVED + "=?",
                    new String[]{uuid + "_%", "0"});

            //delete redundant history log (from last instance saving)
            deleted += cr.delete(QATaskProviderAPI.HistoryColumns.CONTENT_URI,
                    QATaskProviderAPI.HistoryColumns.UUID + "=? and "
                            + QATaskProviderAPI.HistoryColumns.INSTANCE_SAVED + "=?",
                    new String[]{uuid, "0"});

            Log.d(t, "Delete Auto QA data which haven't valid saving status: " + deleted);
        }
    }

    private void afterAllAnimations() {
        if (mStaleView != null) {
            if (mStaleView instanceof RTAView) {
                ((RTAView) mStaleView).recycleDrawables();
            }
            mStaleView = null;
        }

        if (mCurrentView instanceof RTAView) {
            ((RTAView) mCurrentView).setFocus(this);
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }
        mBeenSwiped = false;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (mInAnimation == animation) {
            mAnimationCompletionSet |= 1;
        } else if (mOutAnimation == animation) {
            mAnimationCompletionSet |= 2;
        } else {
            Log.e(t, "Unexpected animation");
        }

        if (mAnimationCompletionSet == 3) {
            this.afterAllAnimations();
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // Added by AnimationListener interface.
        Log.i(t, "onAnimationRepeat "
                + ((animation == mInAnimation) ? "in"
                : ((animation == mOutAnimation) ? "out" : "other")));
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Added by AnimationListener interface.
        Log.i(t, "onAnimationStart "
                + ((animation == mInAnimation) ? "in"
                : ((animation == mOutAnimation) ? "out" : "other")));
    }

    @Override
    public void reLoadingComplete(ReFormLoaderTask task) {
        if (!isViewOnly && isSentInstance) {
            sendSavedBroadcast(IRSAction.ACTION_SEND_FINALIZE_INSTANCE, IdInstanceSend);
        }
        isSentInstance = false;
        isNextInstance = false;
        existEndSurvey = false;

        isLoading = false;

        mFormController = task.getFormController();
        mReFormLoaderTask.setFormLoaderListener(null);
        ReFormLoaderTask t = mReFormLoaderTask;
        mReFormLoaderTask = null;
        t.cancel(true);
        t.destroy();

        mFormController.setPreloadBundle(preloadBundle);

        Collect.getInstance().setFormController(mFormController);
        CompatibilityUtils.invalidateOptionsMenu(this);
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.LOADING_COMPLETE, "reloading...");

        isSendingWorking = false;
        // Set saved answer path
        if (mFormController.getInstancePath() == null) {
            //RTALog.d("test", "fill blank form");
            // Create new answer folder.
            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS",
                    Locale.ENGLISH).format(Calendar.getInstance().getTime());

            mFormController.setUuid(RTASurvey.uuidOFInstance);

            Cursor mCursor = null;
            try {
                mCursor = Collect.getInstance().getContentResolver().query(getIntent().getData(), new String[]{FormsColumns.JR_FORM_ID, FormsColumns.JR_VERSION, FormsColumns.FAMILY},
                        null, null, null);
                mCursor.moveToFirst();
                String jrFormId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_FORM_ID));
                String famliy = mCursor.getString(mCursor.getColumnIndex(FormsColumns.FAMILY));
                String jrFormVersion = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_VERSION));
                mFormController.setFormFamilyID(famliy);
                mFormController.setFormID(jrFormId);
                mFormController.setFormVersion(jrFormVersion);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mCursor != null) {
                    mCursor.close();
                }
            }


            String file = mFormController.getFormID();

            String path = Collect.INSTANCES_PATH + File.separator + file + "_"
                    + time;
            String qaPath = RTASurvey.QA_INSTANCE_PATH + File.separator + file
                    + "_" + time + "_SS";

            String externalDataPath = mFormController.getMediaFolder().getAbsolutePath();

            FileUtils.createFolder(qaPath);
            mFormController.setmExternalInstancesPath(new File(externalDataPath));

            if (FileUtils.createFolder(path)) {
                mFormController.setInstancePath(new File(path + File.separator
                        + file + "_" + time + ".xml"));

                mFormController.setQaFolder(new File(qaPath));

                FASender.sendBroadcastFillBlankForm(this, path, qaPath);
                RTAActivityLogger activityLogger = new RTAActivityLogger(qaPath);

                RTASurvey.getInstance().setRTAActivityLogger(activityLogger);

            }
            //init intance in export data csv
            numberError = 0;
            invalidateOptionsMenu();
        }

        //reset mini log flag for the instances
        MiniLogFormAction.findLastMiniLogIndex(mFormController);
        MiniLogFormAction miniLogFormAction = MiniLogFormAction.getMinilogAction(mFormController.getUuid());
        miniLogFormAction.writeLog(MiniLogEntity.FORM_OPEN_CODE, System.currentTimeMillis());

        //start audio record if it has turn on in setting
        boolean isRecordAudio = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_RECORD_AUDIO_FULL_TIME, false);
        if (isRecordAudio)
            RecordManager.getInstance().startRecordBeginForm(mFormController.getQaFolder().getPath(), "beginForm");
        refreshCurrentView();
        ProgressBarHandler.getInstance(this).hide();

        dismissFormDialog(PROGRESS_DIALOG);
        dismissFormDialog(SAVING_DIALOG);
    }

    /**
     * loadingComplete() is called by FormLoaderTask once it has finished
     * loading a form.
     */
    @Override
    public void loadingComplete(FormLoaderTask task) {
        //set necessary flags and references variables
        isLoading = false;
        isSentInstance = false;
        isNextInstance = false;
        existEndSurvey = false;
        mFormController = task.getFormController();
        boolean pendingActivityResult = task.hasPendingActivityResult();
        boolean hasUsedSavepoint = task.hasUsedSavepoint();
        int requestCode = task.getRequestCode(); // these are bogus if
        int resultCode = task.getResultCode();
        Intent intent = task.getIntent();
        mFormLoaderTask.setFormLoaderListener(null);
        FormLoaderTask t = mFormLoaderTask;
        mFormLoaderTask = null;
        t.cancel(true);
        t.destroy();
        HashMap<String, String> mapTitle = mFormController.getDisplayTitleQuestion();
        if (mapTitle.size() > 0) {
            if (isViewOnly || getIntent().getFlags() == Intent.FLAG_ACTIVITY_CLEAR_TOP) {
                setTitle(mapTitle.get(RTAView.EDIT_OR_REVIEW_TITLE) == null ? mFormController.getFormTitle() : mapTitle.get(RTAView.EDIT_OR_REVIEW_TITLE));
            } else {
                setTitle(mapTitle.get(RTAView.FILL_TITLE) == null ? mFormController.getFormTitle() : mapTitle.get(RTAView.FILL_TITLE));
            }
        } else {
            setTitle(mFormController.getFormTitle());
        }
        //set preload values bundle
        mFormController.setPreloadBundle(preloadBundle);

        //set global references
        Collect.getInstance().setFormController(mFormController);
        CompatibilityUtils.invalidateOptionsMenu(this);
        Collect.getInstance().setExternalDataManager(
                task.getExternalDataManager());

        //Set the language if one has already been set in the past
        String[] languageTest = mFormController.getLanguages();
        if (languageTest != null) {
            String defaultLanguage = mFormController.getLanguage();
            String newLanguage = "";
            String selection = FormsColumns.FORM_FILE_PATH + "=?";
            String selectArgs[] = {mFormPath};
            Cursor c = null;
            try {
                c = getContentResolver().query(FormsColumns.CONTENT_URI, null,
                        selection, selectArgs, null);
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    newLanguage = c.getString(c.getColumnIndex(FormsColumns.LANGUAGE));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            // if somehow we end up with a bad language, set it to the default
            try {
                mFormController.setLanguage(newLanguage);
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                e.printStackTrace();
            }
        }
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.LOADING_COMPLETE, "loading...");

        if (pendingActivityResult) {
            // set the current view to whatever group we were at...
            refreshCurrentView();
            // process the pending activity request...
            onActivityResult(requestCode, resultCode, intent);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissFormDialog(PROGRESS_DIALOG);
                }
            });
            return;
        }

        // it can be a normal flow for a pending activity result to restore from
        // a savepoint
        // (the call flow handled by the above if statement). For all other use
        // cases, the
        // user should be notified, as it means they wandered off doing other
        // things then
        // returned to ODK Collect  and chose Edit Saved Form, but that the
        // savepoint for that
        // form is newer than the last saved version of their form data.
        if (hasUsedSavepoint) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FormEntryActivity.this,
                            getString(R.string.savepoint_used),
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        isSendingWorking = false;
        // Set saved answer path
        if (mFormController.getInstancePath() == null) {
            // Create new answer folder.
            Cursor mCursor = null;
            String displayName = "";
            try {
                mCursor = getContentResolver().query(getIntent().getData(),
                        new String[]{FormsColumns.JR_FORM_ID, FormsColumns.JR_VERSION,
                                FormsColumns.FAMILY, FormsColumns.DISPLAY_NAME},
                        null, null, null);
                mCursor.moveToFirst();
                String jrFormId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_FORM_ID));
                String formFamilyId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.FAMILY));
                String jrFormVersion = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_VERSION));
                displayName = mCursor.getString(mCursor.getColumnIndex(FormsColumns.DISPLAY_NAME));
                mFormController.setFormFamilyID(formFamilyId);
                mFormController.setFormID(jrFormId);
                mFormController.setFormVersion(jrFormVersion);
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                e.printStackTrace();
            } finally {
                if (mCursor != null) {
                    mCursor.close();
                }
            }

            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS",
                    Locale.ENGLISH).format(Calendar.getInstance().getTime());
            String file = mFormController.getFormID();

            String path = Collect.INSTANCES_PATH + File.separator + file + "_"
                    + time;

            String qaPath = RTASurvey.QA_INSTANCE_PATH + File.separator + file
                    + "_" + time + "_SS";

            String externalDataPath = mFormController.getMediaFolder().getAbsolutePath();

            FileUtils.createFolder(qaPath);
            mFormController.setmExternalInstancesPath(new File(externalDataPath));

            if (FileUtils.createFolder(path)) {
                mFormController.setInstancePath(new File(path + File.separator
                        + file + "_" + time + ".xml"));

                mFormController.setQaFolder(new File(qaPath));

                FASender.sendBroadcastFillBlankForm(this, path, qaPath);
                RTAActivityLogger activityLogger = new RTAActivityLogger(qaPath);

                RTASurvey.getInstance().setRTAActivityLogger(activityLogger);
            }

            //init form id, form version, form family, and uuid for form controller
            //uuid
            mFormController.setUuid(RTASurvey.uuidOFInstance);
            ProcessDbHelper.getInstance().insertNewProcess(mFormController.getSubmissionMetadata().instanceId, ProcessDbHelper.ProcessStatus.FILL_FORM, displayName);

            FASender.sendBroadcastStartedEditForm(this, mFormController.getInstancePath().getAbsolutePath(), mFormController.getQaFolder().getAbsolutePath());
            RTAActivityLogger activityLogger = new RTAActivityLogger(qaPath);
            RTASurvey.getInstance().setRTAActivityLogger(activityLogger);

            numberError = 0;
            invalidateOptionsMenu();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissFormDialog(PROGRESS_DIALOG);
                }
            });

            //reset mini-log flag for the instances
            if (!isViewOnly) {
                MiniLogFormAction.findLastMiniLogIndex(mFormController);
                MiniLogFormAction miniLogFormAction = MiniLogFormAction.getMinilogAction(mFormController.getUuid());
                miniLogFormAction.writeLog(MiniLogEntity.FORM_OPEN_CODE, System.currentTimeMillis());
            }

            ScreenUtils.enterFullscreen();
            mFormController.keepBackupInstanceTree();
        } else {    // this is instance uri
            //check if the instance editing is request, but not valid to edit,
            //then show Toast to annouce to end-user
            boolean initByReadOnlyMode = getIntent()
                    .getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false);
            if (!initByReadOnlyMode && isViewOnly) {
                Toast.makeText(this, R.string.cannot_edit_completed_instance, Toast.LENGTH_LONG).show();
            }

            //delete dirty QA Task of last form editing
            if (!isViewOnly) {
                //clean QA task returned in Auto QA process, which aren't saved
                deleteDirtyQATasks();
                //clean complete status which cached in QA Review UI service
                CheckNoteService.cleanCache();
            }

            //get uuid, form id, form ver
            Cursor mCursor = null;
            try {
                Uri uri = getIntent().getData();
                switch (getContentResolver().getType(uri)) {
                    case InstanceColumns.CONTENT_ITEM_TYPE: {
                        mCursor = getContentResolver().query(uri,
                                new String[]{InstanceColumns.JR_FORM_ID, InstanceColumns.JR_VERSION,
                                        InstanceColumns.INSTANCE_UUID, InstanceColumns.DISPLAY_NAME},
                                null, null, null);
                        mCursor.moveToFirst();
                        String jrFormId = mCursor.getString(mCursor.getColumnIndex(InstanceColumns.JR_FORM_ID));
                        String uuid = mCursor.getString(mCursor.getColumnIndex(InstanceColumns.INSTANCE_UUID));
                        String jrFormVersion = mCursor.getString(mCursor.getColumnIndex(InstanceColumns.JR_VERSION));
                        mFormController.setUuid(uuid);
                        mFormController.setFormID(jrFormId);
                        mFormController.setFormVersion(jrFormVersion);
                        if (!getIntent().getBooleanExtra(SurveyUiIntents.UI_INTENT_EXTRA_IS_READ_ONLY, false)) {
                            ProcessDbHelper.getInstance().insertNewProcess(uuid, ProcessDbHelper.ProcessStatus.EDIT_INSTANCE, mCursor.getString(mCursor.getColumnIndex(InstanceColumns.DISPLAY_NAME)));
                        }
                        break;
                    }
                    case FormsColumns.CONTENT_ITEM_TYPE: {
                        mCursor = getContentResolver().query(uri,
                                new String[]{FormsColumns.JR_FORM_ID, FormsColumns.JR_VERSION,
                                        FormsColumns.FAMILY, FormsColumns.DISPLAY_NAME},
                                null, null, null);
                        mCursor.moveToFirst();
                        String jrFormId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_FORM_ID));
                        String formFamilyId = mCursor.getString(mCursor.getColumnIndex(FormsColumns.FAMILY));
                        String jrFormVersion = mCursor.getString(mCursor.getColumnIndex(FormsColumns.JR_VERSION));
                        mFormController.setFormFamilyID(formFamilyId);
                        mFormController.setFormID(jrFormId);
                        mFormController.setFormVersion(jrFormVersion);
                        break;
                    }
                }
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                e.printStackTrace();
            } finally {
                if (mCursor != null) {
                    mCursor.close();
                }
            }

            //get form Family
            Cursor mFormCursor = null;
            try {
                mFormCursor = Collect.getInstance().getContentResolver().query(FormsColumns.CONTENT_URI, new String[]{FormsColumns.FAMILY},
                        FormsColumns.JR_FORM_ID + " = \"" + mFormController.getFormID() + "\" AND " +
                                FormsColumns.JR_VERSION + " = \"" + mFormController.getFormVersion() + "\"", null, null);
                mFormCursor.moveToFirst();
                String formFamilyId = mFormCursor.getString(mFormCursor.getColumnIndex(FormsColumns.FAMILY));
                mFormController.setFormFamilyID(formFamilyId);

            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                e.printStackTrace();
            } finally {
                if (mFormCursor != null) {
                    mFormCursor.close();
                }
            }

            Intent reqIntent = getIntent();
            boolean showFirst = reqIntent.getBooleanExtra("start", false);

            String qaPath = mFormController.getQaFolder().getAbsolutePath();

            if (qaPath == null) {
                qaPath = RTASurvey.QA_INSTANCE_PATH + File.separator
                        + mFormController.getInstancePath().getParentFile().getName()
                        + RTASurvey.QA_INSTANCE_FILE_POSFIX;
                FileUtils.createFolder(qaPath);
            }
            String externalDataPath = mFormController.getMediaFolder().getAbsolutePath();

            mFormController.setmExternalInstancesPath(new File(externalDataPath));

            RTAActivityLogger activityLogger = new RTAActivityLogger(qaPath);
            RTASurvey.getInstance().setRTAActivityLogger(activityLogger);

            FASender.sendBroadcastStartedFillForm(this, mFormController.getInstancePath().getAbsolutePath(), mFormController.getQaFolder().getAbsolutePath());
            ScreenUtils.enterFullscreen();
            mFormController.keepBackupInstanceTree();

            if (task.hasPendingInstance()) {
                FASender.sendBroadcastFillBlankForm(this, mFormController.getInstancePath().getPath(), qaPath);
                //start audio record if it has turn on in setting
                boolean isRecordAudio = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_RECORD_AUDIO_FULL_TIME, false);
                if (isRecordAudio)
                    RecordManager.getInstance().startRecordBeginForm(mFormController.getQaFolder().getPath(), "beginForm");
                ProgressBarHandler.getInstance(this).hide();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                return;
            }

            //reset mini-log flag for the instances
            if (!isViewOnly) {
                MiniLogFormAction.findLastMiniLogIndex(mFormController);
                MiniLogFormAction miniLogFormAction = MiniLogFormAction.getMinilogAction(mFormController.getUuid());
                miniLogFormAction.writeLog(MiniLogEntity.FORM_OPEN_CODE, System.currentTimeMillis());
            }

            if (!showFirst) {
                // we've just loaded a saved form, so start in the hierarchy view
                // start audio record if it has turn on in setting
                boolean isRecordAudio = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_RECORD_AUDIO_FULL_TIME, false);
                if (isRecordAudio)
                    RecordManager.getInstance().startRecordBeginForm(mFormController.getQaFolder().getPath(), "beginForm");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                String uuid = mFormController.getUuid();
                if (!TextUtils.isEmpty(uuid)) {
                    numberError = QACheckPointData.getNoErrorOfInstance(uuid);
                }

                invalidateOptionsMenu();
                ProgressBarHandler.getInstance(this).hide();
                SurveyUiIntents.get().launchFormHierarchyActivity(this, mFormController, true, HIERARCHY_ACTIVITY, isViewOnly);

                return; // so we don't show the intro screen before jumping to
                // the hierarchy
            }
        }

        //start audio record if it has turn on in setting
        boolean isRecordAudio = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_RECORD_AUDIO_FULL_TIME, false);
        if (isRecordAudio)
            RecordManager.getInstance().startRecordBeginForm(mFormController.getQaFolder().getPath(), "beginForm");

        refreshCurrentView();
        ProgressBarHandler.getInstance(this).hide();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissFormDialog(PROGRESS_DIALOG);
            }
        });
    }

    /**
     * called by the FormLoaderTask if something goes wrong.
     */
    @Override
    public void loadingError(String errorMsg) {
        dismissFormDialog(PROGRESS_DIALOG);
        if (errorMsg != null) {
            createErrorDialog(errorMsg, EXIT);
        } else {
            createErrorDialog(getString(R.string.parse_error), EXIT);
        }
    }

    public String getLastUpdateIndex() {
        FormIndex index = mFormController.getFormIndex();
        if (index.isEndOfFormIndex())
            return null;
        else
            return mFormController.getXPath(index);
    }

    public FormController getFormController() {
        return mFormController;
    }

    public String getSSName() {
        return mFormController != null ? mFormController.getSSName() : null;
    }

    /*
     * support for getting a key answer from Form Entry after instance is saved.
     */
    private String getKeyAnswerValue() {
        if (FormEntryActivity.keyQuestion == null || FormEntryActivity.keyQuestion.equals("")) {
            return null;
        }

        if (FormEntryActivity.keyQuestion.equals(FormController.INSTANCE_ID)) {
            return mFormController.getSubmissionMetadata().instanceId;
        }
        if (FormEntryActivity.keyQuestion.equals(FormController.INSTANCE_NAME)) {
            return mFormController.getSubmissionMetadata().instanceName;
        }

        IAnswerDataSerializer answerSerializer = new XFormAnswerDataSerializer();
        HashMap<String, FormIndex> indexMap = mFormController.getQuestionIndexMap();

        FormIndex index = indexMap.get(FormEntryActivity.keyQuestion);
        if (index == null) {
            return null;
        }
        FormEntryPrompt qPrompt = mFormController.getQuestionPrompt(index);
        IAnswerData answer = qPrompt.getAnswerValue();
        String answerTxt = (String) (answer == null ? null :
                (answerSerializer.canSerialize(answer) ?
                        answerSerializer.serializeAnswerData(answer) : null));

        return answerTxt;
    }

    private void completeSaving(SaveResult saveResult) {
        int saveStatus = saveResult.getSaveResult();
        switch (saveStatus) {
            case SaveToDiskTask.SAVED:
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_SAVE, getString(R.string.data_saved_ok));
                Toast.makeText(FormEntryActivity.this, getString(R.string.data_saved_ok),
                        Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(SAVING_DIALOG);
                    }
                }).start();
                break;
            case SaveToDiskTask.SAVED_AND_EXIT:
                if (!isSentInstance)
                    sendSavedBroadcast(IRSAction.ACTION_FORM_SAVED);
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_MENU_SAVE, getString(R.string.data_saved_ok));
                boolean isShow = RTASurvey.getInstance().isReallyPremiumMode()
                        && RTASurvey.getInstance().isAvailableFeature(
                        IPremiumLicense.FEATURE_KEY_SHOW_X3_VALUE);

                Intent intent = getIntent();
                if (intent != null) {
                    Uri uri = intent.getData();
                    String instanceFilePath = mFormController.getInstancePath().getAbsolutePath();
                    String qaPath = mFormController.getQaFolder().getAbsolutePath();
                    if (getContentResolver().getType(uri).equals(InstanceColumns.CONTENT_ITEM_TYPE)) {
                        FASender.sendBroadcastCompletedEditForm(this, instanceFilePath, qaPath);
                    } else if (getContentResolver().getType(uri).equals(FormsColumns.CONTENT_ITEM_TYPE)) {
                        FASender.sendBroadcastCompletedFillForm(this, instanceFilePath, qaPath);
                    }
                } else {
                    Log.e(t, "unrecognized URI");
                }
                Toast.makeText(FormEntryActivity.this, getString(R.string.data_saved_ok),
                        Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(SAVING_DIALOG);
                    }
                }).start();
                finishReturnInstance(isShow);
                safeExitForm = true;
                break;
            case SaveToDiskTask.SAVE_ERROR:
                final String message;
                if (saveResult.getSaveErrorMessage() != null) {
                    message = getString(R.string.data_saved_error) + ": "
                            + saveResult.getSaveErrorMessage();
                } else {
                    message = getString(R.string.data_saved_error);
                }
                Toast.makeText(FormEntryActivity.this, message, Toast.LENGTH_LONG).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(PROGRESS_DIALOG);
                    }
                });
                break;
            case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
            case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
                refreshCurrentView();
                saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(SAVING_DIALOG);
                    }
                }).start();
                break;
            default:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dismissFormDialog(SAVING_DIALOG);
                    }
                }).start();
        }
    }

    /**
     * Called by SavetoDiskTask if everything saves correctly.
     */
    @Override
    public void savingComplete(final SaveResult saveResult) {
        ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SAVE_INSTANCE, "savingComplete");

        if (mFormController.isExportCsv()) {
            synchronized (saveDialogLock) {
                this.stepMessage = getString(R.string.exporting_csv);
                if (mProgressDialog != null) {
                    mProgressDialog.setMessage(getString(R.string.please_wait)
                            + "\n\n" + stepMessage);
                }
                ExportToCsvTaskListener exportToCsvTaskListener = new ExportToCsvTaskListener() {

                    @Override
                    public void onExportProgress(String msg) {

                    }

                    @Override
                    public void onExportError(String errMsg) {
                        SaveResult saveResult = new SaveResult();
                        saveResult.setSaveResult(SaveToDiskTask.SAVE_ERROR);
                        saveResult.setSaveErrorMessage(errMsg);
                        completeSaving(saveResult);
                    }

                    @Override
                    public void onExportComplete(File outputDir) {
                        completeSaving(saveResult);
                    }
                };

                try {
                    if (!new File(ExportToCsvTask.SUBMISSION_DIR).exists()) {
                        if (new File(ExportToCsvTask.SUBMISSION_DIR).mkdirs()) {
                            mFormController.setSubmissionInstanceId(mFormController.getUuid());
                            SaveToDiskTask.exportXmlFile(mFormController.getSubmissionXml(), ExportToCsvTask.SUBMISSION_FILE_PATH);
                        }
                    }
                    if (mExportToCsvTask != null) {
                        mExportToCsvTask.setExportToCsvListener(exportToCsvTaskListener);
                    } else {
                        mExportToCsvTask = new ExportToCsvTask(new File(RTASurvey.RESOURCES_PATH), new File(mFormPath));
                        mExportToCsvTask.setExportToCsvListener(exportToCsvTaskListener);
                    }
                    mExportToCsvTask.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            completeSaving(saveResult);
        }
    }

    @Override
    public void onProgressStep(String stepMessage) {
        if (isNextInstance)
            return;
        this.stepMessage = stepMessage;
        if (mProgressDialog != null) {
            mProgressDialog.setMessage(getString(R.string.please_wait) + "\n\n"
                    + stepMessage);
        }
    }

    /**
     * Checks the database to determine if the current instance being edited has
     * already been 'marked completed'. A form can be 'unmarked' complete and
     * then resaved.
     *
     * @return true if form has been marked completed, false otherwise.
     */
    private boolean isInstanceComplete(boolean end) {
        // default to false if we're mid form
        boolean complete = false;
        if (mFormController == null)
            return complete;

        // if we're at the end of the form, then check the preferences
        if (end) {
            // First get the value from the preferences
            complete = PreferencesManager.getPreferencesManager(this).getBoolean(
                    PreferencesActivity.KEY_COMPLETED_DEFAULT, true);
        }

        // Then see if we've already marked this form as complete before
        String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
        String[] selectionArgs = {mFormController.getInstancePath()
                .getAbsolutePath()};
        Cursor c = null;
        try {
            c = getContentResolver().query(InstanceColumns.CONTENT_URI, null,
                    selection, selectionArgs, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String status = c.getString(c
                        .getColumnIndex(InstanceColumns.STATUS));
                if (InstanceProviderAPI.STATUS_COMPLETE.compareTo(status) == 0) {
                    complete = true;
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return complete;
    }

    public void next() {
        if (!mBeenSwiped) {
            mBeenSwiped = true;
            //showNextView();
            checkAlertAndShowNextView();
        }
    }

    public void back() {
        if (!mBeenSwiped) {
            mBeenSwiped = true;
            showPreviousView();
        }
    }

    /**
     * Returns the instance that was just filled out to the calling activity, if
     * requested.
     */
    private void finishReturnInstance(boolean isShowSummary) {
        QACheckPointImp.getInstance(mFormController.getUuid()).release();
        String action = getIntent().getAction();
        RecordManager.stopAll(this);
        if (Intent.ACTION_PICK.equals(action)
                || Intent.ACTION_EDIT.equals(action)) {
            // caller is waiting on a picked form
            String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
            String[] selectionArgs = {mFormController.getInstancePath()
                    .getAbsolutePath()};
            Cursor c = null;
            try {
                c = getContentResolver().query(InstanceColumns.CONTENT_URI,
                        null, selection, selectionArgs, null);
                if (c.getCount() > 0) {
                    // should only be one...
                    c.moveToFirst();
                    String id = c.getString(c
                            .getColumnIndex(InstanceColumns._ID));
                    IdInstanceSend = c.getLong(c
                            .getColumnIndex(InstanceColumns._ID));
                    Uri instance = Uri.withAppendedPath(
                            InstanceColumns.CONTENT_URI, id);
                    String status = c.getString(c
                            .getColumnIndex(InstanceColumns.STATUS));
                    setResult(RESULT_OK, new Intent().setData(instance));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        if (!isNextInstance) {
            PreferencesManager.getPreferencesManager(this).release();
        }

        instacePathforDatabase = mFormController.getInstancePath()
                .getParent();

//        ActivityLogDatabase.getInstance().closeDb();
//        ActivityLogDatabase.getInstance().release();

        File f = new File(instacePathforDatabase);
        String instanceName = f.getName();
        instacePathforDatabase = RTASurvey.QA_INSTANCE_PATH
                + File.separator + instanceName + "_SS";
        vn.rta.survey.android.utilities.FileUtils
                .clearAllCrash(instacePathforDatabase);

        mFormController.numberQuestions = 0;
        currentIsRepeat = false;
        currentRepeatName = "";

        //clear cache
        RandomViewController.clean();
        try {
            if (bluetoothService != null) {
                bluetoothService.stop();
                bluetoothService = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
        checkNoteManager.hideCheckNote();
        ElementExport elementExport = ElementExport.getInstance();
        elementExport.refreshListRef();
        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            // but only if it's done, otherwise the thread never returns
            if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormLoaderTask t = mFormLoaderTask;
                mFormLoaderTask = null;
                t.cancel(true);
                t.destroy();
            }
        }
        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            if (mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSaveToDiskTask.cancel(true);
                mSaveToDiskTask = null;
            }
        }

        if (mExportToCsvTask != null) {
            mExportToCsvTask.setExportToCsvListener(null);
            if (mExportToCsvTask.getStatus() == AsyncTask.Status.FINISHED) {
                mExportToCsvTask.cancel(true);
                mExportToCsvTask = null;
            }
        }

        String errorNewForm = null;

        if (!isViewOnly)
            if (isNextInstance) {
                Cursor cForm = null;
                long _idForm;
                try {
                    if (formFamilyNext == null) {
                        cForm = getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI, null,
                                FormsProviderAPI.FormsColumns.JR_FORM_ID + " =\"" + mFormController.getFormID() + "\"" +
                                        " AND " + FormsProviderAPI.FormsColumns.JR_VERSION + " =\"" + mFormController.getFormVersion() + "\"", null, null, null);
                        if (cForm.getCount() == 1) {
                            cForm.moveToFirst();
                            _idForm = cForm.getLong(cForm.getColumnIndex(FormsProviderAPI.FormsColumns._ID));
                            final Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI,
                                    _idForm);
                            getIntent().setData(formUri);
                            reInit(formUri, false);
                        }
                    } else {
                        //////////////////////////
                        cForm = getContentResolver().query(FormsProviderAPI.FormsColumns.CONTENT_URI, new String[]{FormsProviderAPI.FormsColumns._ID},
                                FormsColumns.FAMILY + " =\"" + formFamilyNext + "\"" +
                                        " AND " + FormsColumns.AVAILABILITY_STATUS + " =\"" + FormsProviderAPI.STATUS_AVAILABLE + "\"" + " AND " + FormsColumns.LOCKED + "=\"false\"", null, FormsColumns.DATE + " DESC", null);

                        if (cForm != null && cForm.getCount() >= 1) {
                            mFormController.setFormFamilyID(formFamilyNext);
                            aggregateLocal = null;
                            cForm.moveToFirst();
                            _idForm = cForm.getLong(cForm.getColumnIndex(FormsProviderAPI.FormsColumns._ID));
                            final Uri formUri = ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI,
                                    _idForm);
                            getIntent().setData(formUri);
                            reInit(formUri, true);
                        } else {
                            errorNewForm = "Can't link to " + formFamilyNext + "!";
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cForm != null)
                        cForm.close();
                }
            }

        if (errorNewForm == null) {
            mFormController.numberQuestions = 0;

            if (!isViewOnly)
                if (isSentInstance && !isNextInstance) {
                    sendSavedBroadcast(IRSAction.ACTION_SEND_FINALIZE_INSTANCE, IdInstanceSend);
                }
            if (!isNextInstance) {
                finish();
            }
        } else {
            createErrorLinkFormDialog(errorNewForm);
        }

    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // only check the swipe if it's enabled in preferences
        String navigation = PreferencesManager.getPreferencesManager(this).getString(
                PreferencesActivity.KEY_NAVIGATION,
                PreferencesActivity.NAVIGATION_SWIPE_ONE);
        int windowHeight = getWindow().getDecorView().getMeasuredHeight();
        int qHeight = mQuestionHolder.getMeasuredHeight();
        int rawY = (int) e1.getRawY();
        int result = windowHeight - qHeight - getStatusBarHeight() - getActionbarHeight();
        int position = windowHeight - rawY - getStatusBarHeight();

        if (RTAInputManager.getInstance().isShownKeyboard() && position <= result)
            return false;
        Boolean doSwipe = false;
        if (navigation.contains(PreferencesActivity.NAVIGATION_SWIPE)) {
            doSwipe = true;
        }
        if (doSwipe) {
            fisrtShow = true;
            // Looks for user swipes. If the user has swiped, move to the
            // appropriate screen.

            // for all screens a swipe is left/right of at least
            // .25" and up/down of less than .25"
            // OR left/right of > .5"
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int xPixelLimit = (int) (dm.xdpi * .25);
            int yPixelLimit = (int) (dm.ydpi * .25);
            if (mCurrentView instanceof RTAView) {
                if (((RTAView) mCurrentView).suppressFlingGesture(e1, e2,
                        velocityX, velocityY)) {
                    return false;
                }
            } else {
                Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
            }

            if (mBeenSwiped) {
                return false;
            }

            //refesh activity
            if (mPointCountsList.contains(2) && ScreenUtils.isPinchOutAction(e1.getX(), e2.getX(), e1.getY(), e2.getY(), xPixelLimit, yPixelLimit)) {
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.FULL_SCREEN, "");
            }

            if (navigation.contains(PreferencesActivity.NAVIGATION_SWIPE_ONE)) {
                mPointCountsList.removeAll(mPointCountsList);
            }

            if (mPointCountsList.contains(2)
                    || navigation.contains(PreferencesActivity.NAVIGATION_SWIPE_ONE)) {
                //calculate angle swipe
                int angle = (int) getAngle(e1.getX(), e1.getY(), e2.getX(), e2.getY());
                int angleSetting = 45;
                try {
                    angleSetting = (int) Double.parseDouble(PreferencesManager.getPreferencesManager(this).getString(_PreferencesActivity.KEY_SET_ANGLE_SWIPE, "" + 45));
                } catch (Exception e) {
                    e.printStackTrace();
                    angleSetting = 45;
                }
                if (Math.abs(e1.getX() - e2.getX()) > xPixelLimit * 3 /*240*/ &&
                        (angle <= angleSetting || angle >= (360 - angleSetting) ||
                                (angle >= 180 - angleSetting && angle <= 180 + angleSetting))) {
                        /*|| Math.abs(e1.getX() - e2.getX()) > xPixelLimit * 2*///) {
                    mBeenSwiped = true;
                    if (velocityX > 0) {
                        if (e1.getX() > e2.getX()) {
                            checkAlertAndShowNextView();
                        } else {
                            checkAlertAndShowPreviousView();
                        }
                    } else {
                        if (e1.getX() < e2.getX()) {
                            checkAlertAndShowPreviousView();
                        } else {
                            checkAlertAndShowNextView();
                        }
                    }
                }
            }
            // add by van thi
            mPointCountsList.removeAll(mPointCountsList);
            return true;
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    /**
     * Finds the angle between two points in the plane (x1,y1) and (x2, y2)
     * The angle is measured with 0/360 being the X-axis to the right, angles
     * increase counter clockwise.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the angle between two points
     */
    public double getAngle(float x1, float y1, float x2, float y2) {

        double rad = Math.atan2(y1 - y2, x2 - x1) + Math.PI;
        return (rad * 180 / Math.PI + 180) % 360;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        // The onFling() captures the 'up' event so our view thinks it gets long
        // pressed.
        // We don't want that, so cancel it.
        // RTALog.d("test dispatch", " get dispath touch event on onScroll");
//        if (mCurrentView != null) {
//            mCurrentView.cancelLongPress();
//        }

        if (e1.getRawY() - e2.getRawY() > 20) {
            if (e1.getRawY() >= (screenHeight - getNavigationBarHeight())) {
                //Check to determine whether it scrolled from bottom of the screen
                if (isImmersiveFullscreen) {
                    //Now it certainly is in immersive fullscreen -> escape fullscreen
                    ScreenUtils.escapeFullscreen(this);
                }
            } else if (!isImmersiveFullscreen) {
                //Calculate the height of the screen and the layout to detect keyboard show or hide
                Rect r = new Rect();
                out.getWindowVisibleDisplayFrame(r);
                final int screenHeight = out.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    RTAInputManager.getInstance().hideRTAKeyboard();
                    isImmersiveFullscreen = true;

                    //Workaround, waiting for keyboard to close
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ScreenUtils.enterFullscreen();
                        }
                    }, 500);
                } else {
                    // keyboard is closed
                    ScreenUtils.enterFullscreen();
                }
            }
        }

        if (isImmersiveFullscreen && (e2.getRawY() - e1.getRawY() > 40) && (e1.getRawY() < screenHeight * 5 / 100)) {
            ScreenUtils.escapeFullscreen(this);
        }

        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        //RTALog.d("test dispatch", " get dispath touch event onSingleTapUp");

        return false;
    }

    @Override
    public void advance(boolean isNext) {
        if (isNext)
            next();
        else
            back();
    }

    @Override
    protected void onStop() {
        /*
        if (SipCallManager.getInstance().isRunning())
            SipCallManager.getInstance().cleanConfigs();
            */

        stopService(new Intent(this, BackGroundAudioRecordService.class));
        Collect.getInstance().getActivityLogger().logOnStop(this);
        RandomViewController.clean();
        try {
            if (bluetoothService != null) {
                bluetoothService.stop();
                bluetoothService = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
        checkNoteManager.hideCheckNote();
        ElementExport elementExport = ElementExport.getInstance();
        elementExport.refreshListRef();
        if (mFormLoaderTask != null) {
            mFormLoaderTask.setFormLoaderListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            // but only if it's done, otherwise the thread never returns
            if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                FormLoaderTask t = mFormLoaderTask;
                mFormLoaderTask = null;
                t.cancel(true);
                t.destroy();
            }
        }
        if (mSaveToDiskTask != null) {
            mSaveToDiskTask.setFormSavedListener(null);
            // We have to call cancel to terminate the thread, otherwise it
            // lives on and retains the FEC in memory.
            if (mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSaveToDiskTask.cancel(true);
                mSaveToDiskTask = null;
            }
        }

        if (mExportToCsvTask != null) {
            mExportToCsvTask.setExportToCsvListener(null);
            if (mExportToCsvTask.getStatus() == AsyncTask.Status.FINISHED) {
                mExportToCsvTask.cancel(true);
                mExportToCsvTask = null;
            }
        }

        if (!isSaveConfig) {
            SurveyCollectorUtils.cleanLatestInstanceConfig();
        }

        if (LinphoneService.isReady() && updateUIListener != null) {
            LinphoneService.removeOnUpdateUIListener(updateUIListener);
        }

        super.onStop();
    }

    private void sendSavedBroadcast(String action) {
        Intent i = new Intent();
        i.setAction(action);
        this.sendBroadcast(i);
    }

    private void sendSavedBroadcast(String action, long id) {
        Intent i = new Intent();
        i.putExtra("instanceID", id);
        i.setAction(action);
        this.sendBroadcast(i);
    }

    @Override
    public void onSavePointError(String errorMessage) {
        if (errorMessage != null && errorMessage.trim().length() > 0) {
            Toast.makeText(this,
                    getString(R.string.save_point_error, errorMessage),
                    Toast.LENGTH_LONG).show();
        }
    }

    /*
     * //TODO: Modified protected boolean _saveAnswersForCurrentScreen(boolean
	 * evaluateConstraints) { return
	 * saveAnswersForCurrentScreen(evaluateConstraints); }
	 */
    private void removePendingInstance() {
        Intent intent = getIntent();
        if (intent != null) {
            Uri uri = intent.getData();
            if (getContentResolver().getType(uri).equals(
                    InstanceColumns.CONTENT_ITEM_TYPE)) {
                Cursor c = null;
                try {
                    c = getContentResolver().query(uri, null, null, null, null);
                    if (c.getCount() != 1) {
                        Log.e(t, "Error: Has one more pending instance");
                    } else {
                        c.moveToFirst();
                        String status = c.getString(c
                                .getColumnIndex(InstanceColumns.STATUS));
                        if (status.equals("pending")) {
                            File file = new File(
                                    c.getString(c
                                            .getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH)));
                            if (file.isFile()) {
                                file.delete();
                            }
                            getContentResolver().delete(uri, null, null);
                        }
                    }

                } finally {
                    if (null != c) {
                        c.close();
                    }
                }
            }
        }
    }

    private String analyticsDuration() {
        long totalTime = 0;

        ArrayList<InstanceLog> durationInstances = RTASurvey.getInstance()
                .getRTAActivityLogger().getAllInstanceofContext("duration");
        if (durationInstances.size() == 0) {
            return "unknown";
        }
        String duation = "";
        long time = 0;
        for (InstanceLog l : durationInstances) {
            if (l.getAction().equals(STARTFORM)) {
                time = l.getTimeStamp();
            } else if (l.getAction().equals(ENDFORMS)) {
                if (duation.equals(""))
                    duation = "" + ((l.getTimeStamp() - time) / 1000) + "s";
                else
                    duation = duation + " + "
                            + ((l.getTimeStamp() - time) / 1000) + "s";
                totalTime = totalTime + (l.getTimeStamp() - time) / 1000;
            } else if (l.getAction().equals(ENDFORMF)) {
                if (duation.equals(""))
                    duation = "" + ((l.getTimeStamp() - time) / 1000) + "f";
                else
                    duation = duation + " + "
                            + ((l.getTimeStamp() - time) / 1000) + "f";
                totalTime = totalTime + (l.getTimeStamp() - time) / 1000;
            }
        }
        if (durationInstances.get(durationInstances.size() - 1).getAction()
                .equals(STARTFORM)) {
            if (duation.equals(""))
                duation = "" + ((System.currentTimeMillis() - time) / 1000)
                        + "f";
            else
                duation = duation + " + "
                        + ((System.currentTimeMillis() - time) / 1000) + "f";
            totalTime = totalTime + (System.currentTimeMillis() - time) / 1000;
        }
        if (duation.equals("")) {
            duation = "Unknown";
        }
        return totalTime + " = " + duation;
    }

    @Override
    public void onUpdateStatusMenuQA(long numError) {
        this.numberError = numError;
        invalidateOptionsMenu();

    }

    public void updateStatusMenuQA(long numError) {
        this.numberError = numError;
        invalidateOptionsMenu();

    }

    public boolean isHavePopupWidget() {
        return havePopupWidget;
    }

    public void setHavePopupWidget(boolean havePopupWidget) {
        this.havePopupWidget = havePopupWidget;
    }

    public boolean isComfrimAudioRecore() {
        return isComfrimAudioRecore;
    }

    public void setComfrimAudioRecore(boolean isComfrimAudioRecore) {
        this.isComfrimAudioRecore = isComfrimAudioRecore;
    }

    //capture screen shot and show popup
    public Bitmap takeScreenshot() {
        ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.TASK_TAKE_SCREEN_SHOT);
        View rootView = findViewById(android.R.id.content).getRootView();
        rootView.invalidate();
        rootView.setDrawingCacheEnabled(true);
        return rootView.getDrawingCache();
    }

    private void setProgressColor() {
        float progress = progressFillForm.getProgress();
        progressFillForm.setProgressColor(getResources().getColor(R.color.custom_progress_blue_progress));
        progressFillForm.setSecondaryProgressColor(getResources().getColor(R.color.custom_progress_orange_progress_half));
    }

    private void setProgress(float progress, float current) {
        progressFillForm.setProgress(progress);
        progressFillForm.setSecondaryProgress(current);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void endSurvey(boolean unsave, boolean isComplete, boolean isNew, boolean isSent, String formFamily) {
        //set status for saving complete
        CountDownTimerInstance.getInstance().stopCountDownTimer();
        cleanConfigs();
        isNextInstance = isNew;
        isSentInstance = isSent;
        if (formFamily != null) {
            formFamilyNext = formFamily.equals(".") ? getFormController().getFormFamilyID() : formFamily;
        }

        enableCurrentView(false);

        //call action save
        if (isViewOnly) {
            finishReturnInstance(false);
        } else if (unsave) {
            RTASurvey.getInstance().setExternalDataPullResultManager(null);
            removeTempInstance();
            removePendingInstance();

            String st = getSavingStatus();
            if (st == null) {
                //delete data
                getContentResolver().delete(InstanceColumns.CONTENT_URI,
                        InstanceColumns.INSTANCE_FILE_PATH + " = \"" +
                                mFormController.getInstancePath().getAbsolutePath()
                                + "\"", null);
                getContentResolver().delete(InstanceColumns.CONTENT_URI,
                        InstanceColumns.INSTANCE_QA_PATH + " = \"" +
                                mFormController.getQaFolder().getAbsolutePath()
                                + "\"", null);
            }
            ProcessDbHelper.getInstance().removeProcess(mFormController.getSubmissionMetadata().instanceId);
            finishReturnInstance(false);
        } else {
            //save instance incomplete
            if (!isComplete) {
                //send data
                //check number of qa error
                final FormController formController = RTASurvey.getInstance().getFormController();
                long nErrors = isViewOnly ? 0 : QACheckPointData.getNoErrorOfInstance(formController.getUuid());
                if (nErrors > 0) {
                    //show dialog
                    qaCheckPointDialog = new Dialog(this);
                    String errorMsg = nErrors == 1 ? getString(R.string.warning_qa_message)
                            : getString(R.string.warnings_qa_message, "" + nErrors);

                    qaCheckPointDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    qaCheckPointDialog.setCancelable(false);
                    qaCheckPointDialog.setContentView(R.layout.warning_dialog);
                    qaCheckPointDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    TextView text = (TextView) qaCheckPointDialog.findViewById(R.id.text_dialog);
                    text.setText(errorMsg);
                    Button btOk = (Button) qaCheckPointDialog.findViewById(R.id.btn_ok);
                    btOk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            qaCheckPointDialog.dismiss();
                            final boolean send_working = PreferencesManager.getPreferencesManager(RTASurvey.getInstance()).getBoolean(_PreferencesActivity.KEY_ALLOW_SEND_WORKING_DATA, false);
                            sendWorkingToServer(send_working);
                            ProcessDbHelper.getInstance().removeProcess(formController.getSubmissionMetadata().instanceId);
                            nameofInstance = getInstanceDisplayName(formController);

                            // override the visibility settings based upon admin preferences
                            instacePathforDatabase = formController.getInstancePath().getParent();

                            ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_SAVE_INCOMPLETE);

                            total = StringFormatUtils.getTime(System.currentTimeMillis() - timeStartmilis);

                            updateInstanceplushInfor();
                            saveDataToDisk(EXIT, false/*is check completed checkbox*/, nameofInstance, RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                        }
                    });

                    Button btCan = (Button) qaCheckPointDialog.findViewById(R.id.btn_cancel);
                    btCan.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            qaCheckPointDialog.dismiss();
                            QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                            checkNoteManager.showCheckNote();
                            enableCurrentView(true);

                        }
                    });
                    qaCheckPointDialog.show();

                } else {
                    ProcessDbHelper.getInstance().removeProcess(formController.getSubmissionMetadata().instanceId);

                    final boolean send_working = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_ALLOW_SEND_WORKING_DATA, false);
                    if (send_working) {
                        sendWorkingToServer(send_working);
                    }
                    nameofInstance = getInstanceDisplayName(formController);

                    // override the visibility settings based upon admin preferences
                    instacePathforDatabase = formController.getInstancePath()
                            .getParent();


                    ActivityLogManager.InsertChangeScreenLog(Constants.ENTER_DATA_INSTANCE, Constants.BUTTON_SAVE_INCOMPLETE);

                    total = StringFormatUtils.getTime(System.currentTimeMillis() - timeStartmilis);

                    Collect.getInstance().getActivityLogger().logInstanceAction(
                            this, "createView.saveAndExit", "saveIncomplete");

                    updateInstanceplushInfor();
                    saveDataToDisk(EXIT, false/*is check completed checkbox*/, nameofInstance, RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                }
            } else {
                //enable button save in screen
                ProcessDbHelper.getInstance().removeProcess(mFormController.getUuid());
                // get constraint behavior preference value with appropriate default
                String constraint_behavior = PreferencesManager.getPreferencesManager(this).getString(
                        PreferencesActivity.KEY_CONSTRAINT_BEHAVIOR,
                        PreferencesActivity.CONSTRAINT_BEHAVIOR_DEFAULT);
                ActivityLogManager.InsertActionWithContext(Constants.ENTER_DATA_INSTANCE, Constants.SHOW_NEXT_SCREEN, "");

                saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
                if (mFormController.currentPromptIsQuestion()) {
                    // if constraint behavior says we should validate on swipe, do
                    // so
                    if (constraint_behavior
                            .equals(PreferencesActivity.CONSTRAINT_BEHAVIOR_ON_SWIPE)) {
                        if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS)) {
                            isNextInstance = false;
                            isSentInstance = false;
                            // A constraint was violated so a dialog should be
                            CountDownTimer countDownTimer = new CountDownTimer(2000, 2000) {
                                @Override
                                public void onTick(long l) {
                                }

                                @Override
                                public void onFinish() {
                                    enableCurrentView(true);
                                }
                            };
                            countDownTimer.start();
                            return;
                        }
                    }
                }

                //get errors before send working --> uuid will change
                long nErrors = isViewOnly ? 0 : QACheckPointData.getNoErrorOfInstance(mFormController.getUuid());
                if (nErrors > 0) {
                    //show dialog
                    qaCheckPointDialog = new Dialog(this);
                    String errorMsg = nErrors == 1 ? getString(R.string.error_qa_message)
                            : getString(R.string.errors_qa_message, "" + nErrors);

                    qaCheckPointDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    qaCheckPointDialog.setCancelable(false);
                    qaCheckPointDialog.setContentView(R.layout.error_dialog);
                    qaCheckPointDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    TextView text = (TextView) qaCheckPointDialog.findViewById(R.id.text_dialog);
                    text.setText(errorMsg);

                    Button dialogButton = (Button) qaCheckPointDialog.findViewById(R.id.btn_dialog);
                    dialogButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            qaCheckPointDialog.dismiss();
                            QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                            checkNoteManager.showCheckNote();
                            enableCurrentView(true);
                        }
                    });
                    qaCheckPointDialog.show();

                } else {
                    final boolean send_working = PreferencesManager.getPreferencesManager(this).getBoolean(_PreferencesActivity.KEY_ALLOW_SEND_WORKING_DATA, false);
                    if (send_working) {
                        sendWorkingToServer(send_working);
                    }
                    nameofInstance = getInstanceDisplayName(mFormController);

                    saveDataToDisk(EXIT, true/*is check completed checkbox*/, nameofInstance, RTASurvey.STATUS_ACCEPT_SEND_FINALIZE);
                    updateInstanceplushInfor();
                }
            }
        }
    }

    private String getInstanceDisplayName(FormController fc) {
        String name = fc.getSubmissionMetadata().instanceName;
        if (name == null) {
            // no meta/instanceName field in the form -- see if we have a
            // name for this instance from a previous save attempt...
            Uri uri = getIntent().getData();
            if (InstanceColumns.CONTENT_ITEM_TYPE.equals(getContentResolver().getType(uri))) {
                Cursor instance = null;
                try {
                    instance = getContentResolver().query(uri, null, null, null, null);
                    if (instance != null && instance.getCount() > 0) {
                        instance.moveToFirst();
                        name = instance.getString(instance.getColumnIndex(InstanceColumns.DISPLAY_NAME));
                    }
                } finally {
                    if (instance != null) {
                        instance.close();
                    }
                }
            }
            // last resort, default to the form title
            if (name == null) {
                name = mFormController.getFormTitle();
            }
        }
        return name;
    }

    @Override
    public void haveEndSurvey(boolean isEndSurvey) {
        existEndSurvey = isEndSurvey;
    }

    private void enableCurrentView(boolean isEnable) {
        if (mCurrentView instanceof RTAView) {
            ((RTAView) mCurrentView).setEnabled(isEnable);
        } else {
            Log.e(FormEntryActivity.class.getName(), "mCurrentView is not instance of RTAView");
        }
    }

    @Override
    public void fireChooseForm(String lang) {
        final String[] languages = mFormController.getLanguages();
        int index = -1;
        if (languages != null) {
            for (int i = 0; i < languages.length; i++) {
                if (lang.equalsIgnoreCase(languages[i])) {
                    index = i;
                    break;
                }
            }
        }
        if (index == -1)
            return;
        ContentValues values = new ContentValues();
        values.put(FormsColumns.LANGUAGE, languages[index]);
        String selection = FormsColumns.FORM_FILE_PATH
                + "=?";
        String selectArgs[] = {mFormPath};
        int updated = getContentResolver().update(
                FormsColumns.CONTENT_URI, values,
                selection, selectArgs);

        mFormController
                .setLanguage(languages[index]);
        if (mCurrentView instanceof RTAView) {
            updateLabelAndResetXpath();
        }
    }

    @Override
    public void onSaverCurrentAnswerForCall() {
        saveAnswersForCurrentScreen(false);
        updateLabelAndResetXpath();
    }

    public void updateLabelAndResetXpath() {
        RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
        ((RTAView) mCurrentView).refreshAllView(prompts);
    }

    @Override
    public void createRepeatRecord(boolean refreshScreen) {
        RFormEntryPrompt[] prompts = mFormController.getQuestionPrompts();
        for (RFormEntryPrompt p : prompts) {
            if (p.getAppearance() != null && p.getAppearance().contains("addable")) {
                mFormController.increaseOrDecreaseRepeatCount(p.getFormIndex(), true);
                if (mFormController.getRepeatCount(mFormController.getModel().getForm().getChild(p.getFormIndex()), p.getFormIndex()) == 1)
                    refreshCurrentView();
                currentIsRepeat = true;
                //check repeat in here.
                updateCurrentRepeatName(p.getFormIndex());
                break;
            } else if (p.getmType() == FormEntryController.EVENT_PROMPT_NEW_REPEAT) {
                try {
                    mFormController.newRepeat(p.getFormIndex());
                    currentIsRepeat = true;
                    //check repeat in here.
                    updateCurrentRepeatName(p.getFormIndex());
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    createErrorDialog(e.getMessage(), DO_NOT_EXIT);
                }
                break;
            }
        }
        if (refreshScreen)
            try {
                refreshRepeatCurrentView();
            } catch (RuntimeException e) {
                e.printStackTrace();
                createErrorDialog(e.getMessage(), DO_NOT_EXIT);
            }

    }

    private void updateCurrentRepeatName(FormIndex index) {
        String repeatName = "";
        repeatName = index.getReference().toShortString();
        if (repeatName.contains("["))
            currentRepeatName = repeatName.substring(0, repeatName.indexOf("["));
        else currentRepeatName = repeatName;
    }

    @Override
    public void refreshComplete
            (ArrayList<QuestionWidget> listNew, ArrayList<QuestionWidget> listRem) {
        if (!isViewOnly) {
            for (QuestionWidget qw : listNew) {
                if (!qw.getPrompt().isReadOnly()) {
                    registerForContextMenu(qw);
                }
            }

            for (QuestionWidget qw : listRem) {
                if (!qw.getPrompt().isReadOnly()) {
                    unregisterForContextMenu(qw);
                }
            }
        }
    }

    @Override
    public void createCompleted() {
        //saveAnswersForCurrentScreen(false);
    }

    @Override
    public void refreshScreen() {
        FormController formController = RTASurvey.getInstance().getFormController();
        saveAnswersForCurrentScreen(false);
        try {
            RFormEntryPrompt[] prompts = formController.getQuestionPrompts();
            ((RTAView) mCurrentView).refreshView(prompts);
        } catch (RuntimeException e) {
            e.printStackTrace();
            createErrorDialog(e.getMessage(), DO_NOT_EXIT);
        }
    }

    public void setPermissionRequestListener(PermissionRequestListener listener) {
        this.listener = listener;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            boolean fullyGranted = true;
            for (int g : grantResults) {
                if (g != PackageManager.PERMISSION_GRANTED) {
                    fullyGranted = false;
                    break;
                }
            }
            if (listener != null) {
                if (fullyGranted) {
                    listener.onPermissionGranted();
                } else {
                    listener.onPermissionDenied();
                }
            }
        }
    }

    private boolean isScrollable(View parentView, RTAView scrollView) {
        int childHeight = parentView.getMeasuredHeight();
        int computeView = childHeight + scrollView.getPaddingTop() + scrollView.getPaddingBottom();
        Display display =
                ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(display.getWidth(), View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        scrollView.measure(widthMeasureSpec, heightMeasureSpec);
        int height = scrollView.getMeasuredHeight();
        return height > computeView;
    }

    private void setColorForScrollView(RTAView scrollView) {
        try {
            Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
            mScrollCacheField.setAccessible(true);
            Object mScrollCache = mScrollCacheField.get(scrollView); // scr is your Scroll View

            Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
            scrollBarField.setAccessible(true);
            Object scrollBar = scrollBarField.get(mScrollCache);

            Method method = scrollBar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
            method.setAccessible(true);

            // Set your drawable here.
            method.invoke(scrollBar, getResources().getDrawable(R.drawable.scrollbar_primarry));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismissFormDialog(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
            Log.e(t, "Dialog not found (" + id + ")", e);
        }
    }

    private String getSavingStatus() {
        String uuid = mFormController.getUuid();
        if (uuid == null)
            return null;
        String saveStt = null;
        Cursor c = null;
        try {
            c = getContentResolver()
                    .query(InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                            new String[]{InstanceProviderAPI.InstanceColumns.INSTANCE_SAVE_STATUS,
                                    InstanceProviderAPI.InstanceColumns.STATUS},
                            InstanceProviderAPI.InstanceColumns.INSTANCE_UUID + " = \"" + uuid + "\"", null, null);
            if (c == null || c.getCount() != 1) {
                Log.e(t, "ERROR:: Instance data not found (in instances.db)");
            } else {
                c.moveToFirst();
                saveStt = c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_SAVE_STATUS));
                if (saveStt == null) {
                    String stt = c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns.STATUS));
                    if (InstanceProviderAPI.STATUS_INCOMPLETE.equals(stt) || InstanceProviderAPI.STATUS_INCOMPLETE_BASELINE.equals(stt)
                            || InstanceProviderAPI.STATUS_INCOMPLETE_RETURNED.equals(stt))
                        saveStt = "1";
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return saveStt;
    }

    public RelativeLayout getContentLayout() {
        return content;
    }

    //Refresh the screen and widgets after jump to a new screen
    public void refreshScreenAfterJump() {
        // We may have jumped to a new index in hierarchy activity, so
        // refresh
        isShowFromtList = false;
        if (vn.rta.survey.android.ui.formhierarchy.FormHierarchyActivity.isShowFirst) {
            String uuid = mFormController.getUuid();
            if (!TextUtils.isEmpty(uuid)) {
                numberError = QACheckPointData.getNoErrorOfInstance(uuid);
            }

            if (numberError > 0 && !isViewOnly) {
                QACheckPointViewManager checkNoteManager = QACheckPointViewManager.getInstance();
                checkNoteManager.showCheckNote();
            }
            invalidateOptionsMenu();
        }
        refreshCurrentView();
    }

    //Toggle actionbar
    public void showSupportActionBar(boolean enable) {
        try {
            if (enable) {
                if (!getSupportActionBar().isShowing())
                    getSupportActionBar().show();
            } else {
                if (getSupportActionBar().isShowing())
                    getSupportActionBar().hide();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean hasNavigationBar() {
        boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if (!hasMenuKey && !hasBackKey) {
            // Do whatever you need to do, this device has a navigation bar
            return true;
        }
        return false;
    }

    public void notifyIDBadgeScanAnswer(Map<String, String> answers, Map<String, List<String>> questions) {
        if (mCurrentView instanceof RTAView) {
            RTAView rtaView = (RTAView) mCurrentView;
            ArrayList<QuestionWidget> widgets = rtaView.getWidgets();

            for (Map.Entry<String, List<String>> entry : questions.entrySet()) {
                String key = entry.getKey();
                List<String> array = entry.getValue();

                for (String qName : array) {
                    String answer = answers.get(key);
                    saveAnswerIDBadgeScan(qName, answer, widgets);
                }
            }
        }
    }

    public void notifyQRAPIScanAnswer(Map<String, String> answers, Map<String, HashMap<String, List<String>>> questions) {
        if (mCurrentView instanceof RTAView) {
            question = questions;
            RTAView rtaView = (RTAView) mCurrentView;
            ArrayList<QuestionWidget> widgets = rtaView.getWidgets();

            for (Map.Entry<String, HashMap<String, List<String>>> entryAll : questions.entrySet()) {
                String keyAll = entryAll.getKey();
                HashMap<String, List<String>> arrayAll = entryAll.getValue();
                if (keyAll.equals("output")) {
                    for (Map.Entry<String, List<String>> entry : arrayAll.entrySet()) {
                        String key = entry.getKey();
                        List<String> array = entry.getValue();
                        for (String qName : array) {
                            String answer = answers.get(key);
                            saveAnswerIDBadgeScan(qName, answer, widgets);
                        }
                    }
                }
            }
        }
    }

    public void notifyActionAnswer(Map<String, HashMap<String, List<String>>> questions) {
        if (mCurrentView instanceof RTAView) {
            if (questions != null) {
                RTAView rtaView = (RTAView) mCurrentView;
                ArrayList<QuestionWidget> widgets = rtaView.getWidgets();
                for (Map.Entry<String, HashMap<String, List<String>>> entryAll : questions.entrySet()) {
                    String keyAll = entryAll.getKey();
                    HashMap<String, List<String>> arrayAll = entryAll.getValue();
                    if (keyAll.equals("action")) {
                        for (Map.Entry<String, List<String>> entry : arrayAll.entrySet()) {
                            String key = entry.getKey();
                            List<String> array = entry.getValue();
                            String answer = action_qrapi;
                            for (String action : array) {
                                if (action.equals("image")) {
                                    saveAnswerActionQRAPI(key, answer, widgets);
                                } else {
                                    saveAnswerIDBadgeScan(key, answer, widgets);
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public void notifyStringApiAnswer(String[] listQuestion,String answer) {
        if (mCurrentView instanceof RTAView) {
            if(listQuestion!=null){
                RTAView rtaView = (RTAView) mCurrentView;
                ArrayList<QuestionWidget> widgets = rtaView.getWidgets();
                for (String question : listQuestion ) {
                    saveAnswerIDBadgeScan(question.substring(1,question.length()-1),answer,widgets);
                }
            }

        }
    }

    public void notifyStringApiActionAnswer(String question,String answer){
        if (mCurrentView instanceof RTAView) {
            if(question!=null){
                RTAView rtaView = (RTAView) mCurrentView;
                ArrayList<QuestionWidget> widgets = rtaView.getWidgets();
                saveAnswerIDBadgeScan(question.substring(1,question.length()-1),answer,widgets);

            }

        }
    }

    public void saveAnswerIDBadgeScan(String qName, String answer, ArrayList<QuestionWidget> widgets) {
        for (QuestionWidget widget : widgets) {
            if (mFormController.getQuestionName(widget.getIndex()).equals(qName)) {
                widget.saveIDBadgeAnswer(answer);
            }
        }
        saveAndReFreshCurrentView("", false, mFormController.getFormIndex());
    }

    public void saveAnswerActionQRAPI(String qName, String answer, ArrayList<QuestionWidget> widgets) {
        for (QuestionWidget widget : widgets) {
            if (mFormController.getQuestionName(widget.getIndex()).equals(qName)) {
                if (countAction == 0) {
                    File ifiLegacy = new File(answer);
                    String miInstanceFolderLegacy = mFormController.getInstancePath().getParent();
                    String isLegacy = miInstanceFolderLegacy + File.separator
                            + System.currentTimeMillis();
                    infLegacy = new File(isLegacy);
                    if (!infLegacy.exists()) {
                        Log.e(t, ifiLegacy.getAbsolutePath() + " this is not exists");
                    }

                    try {
                        org.apache.commons.io.FileUtils.moveFile(ifiLegacy, infLegacy);
                    } catch (IOException e) {
                        Log.e(t, "Failed to move file to " + infLegacy.getAbsolutePath());
                        e.printStackTrace();
                        break;
                    }
                    widget.saveActionValue(infLegacy);
                    countAction = 1;
                } else {
                    String miInstanceFolderLegacy = mFormController.getInstancePath().getParent();
                    String isLegacy = miInstanceFolderLegacy + File.separator
                            + System.currentTimeMillis();
                    File infLegacy1 = new File(isLegacy);
                    if (!infLegacy1.exists()) {
                        Log.e(t, infLegacy.getAbsolutePath() + " this is not exists");
                    }

                    try {
                        org.apache.commons.io.FileUtils.moveFile(infLegacy, infLegacy1);
                    } catch (IOException e) {
                        Log.e(t, "Failed to move file to " + infLegacy.getAbsolutePath());
                        e.printStackTrace();
                        break;
                    }
                    widget.saveActionValue(infLegacy1);
                }


            }

        }
        saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
    }

    @Override
    public void connectBluetoothDevice(Handler handler) {
        if (bluetoothService != null) {
            bluetoothService.stop();
            bluetoothService = null;
        }
        bluetoothService = new BluetoothService(this, handler);
        if (!bluetoothService.isBTopen()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }
    }

    @Override
    public void writeDataToBlueToothDevice(byte[] data) {
        if (bluetoothService != null && bluetoothService.isAvailable()) {
            bluetoothService.write(data);
        }
    }

    @Override
    public void writeRawMessageToBlueToothDevice(String message, String charset) {
        if (message != null && message.length() > 0 && bluetoothService != null && bluetoothService.isAvailable()) {
            try {
                bluetoothService.write(message.getBytes(charset));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                bluetoothService.write(message.getBytes());
            }
        }
    }

    enum AnimationType {
        LEFT, RIGHT, FADE
    }

    /*
     * Wrapper class to help with handing off the MediaPlayer to the next instance
     * of the activity in case of orientation change, without losing any state.
     */
    private static class PreviewPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener {
        FormEntryActivity mActivity;
        boolean mIsPrepared = false;

        public void setActivity(FormEntryActivity activity) {
            mActivity = activity;
            setOnPreparedListener(this);
            setOnErrorListener(mActivity);
            setOnCompletionListener(mActivity);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalArgumentException,
                SecurityException, IllegalStateException, IOException {
            setDataSource(mActivity, uri);
            prepareAsync();
        }

        /* (non-Javadoc)
         * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
         */
        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            mActivity.onPrepared(mp);
        }

        boolean isPrepared() {
            return mIsPrepared;
        }
    }

    class ProgressRefresher implements Runnable {

        public void run() {
            if (mPlayer != null && !mSeeking && mDuration != 0) {
                int progress = mPlayer.getCurrentPosition() / mDuration;
                mSeekBar.setProgress(mPlayer.getCurrentPosition());
            }
            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        }
    }
}
