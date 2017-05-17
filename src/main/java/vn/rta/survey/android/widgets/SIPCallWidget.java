package vn.rta.survey.android.widgets;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.rta.ipcall.LinphoneManager;
import com.rta.ipcall.LinphoneService;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.external.ExternalDataUtil;
import org.odk.collect.android.utilities.MediaUtils;
import org.odk.collect.android.widgets.QuestionWidget;

import java.io.File;
import java.text.DecimalFormat;

import vn.rta.cpms.keyboard.RTAInputManager;
import vn.rta.cpms.listener.PermissionRequestListener;
import vn.rta.survey.android.R;
import vn.rta.survey.android.application.Constants;
import vn.rta.survey.android.application.RTASurvey;
import vn.rta.survey.android.entities.SIPProfile;
import vn.rta.survey.android.listeners.SaveCurrentAnswerListener;
import vn.rta.survey.android.listeners.SipCallUpdateUIListener;
import vn.rta.survey.android.manager.ActivityLogManager;
import vn.rta.survey.android.manager.IPCallManager;

/**
 * Created by ThiNguyen on 8/25/16.
 */
public class SIPCallWidget extends QuestionWidget implements SipCallUpdateUIListener {
    private final static String t = "MediaWidget";
    private AppCompatButton
            mCaptureButton;
    private String mBinaryName;
    private String mInstanceFolder;
    LinearLayout buttonLayout;
    private boolean isShowQuestionText;
    private FormEntryActivity mEntryActivity;
    private SIPProfile sipProfile;
    private SaveCurrentAnswerListener saveCurrentAnswerListener;
    private boolean locked = false;

    public SIPCallWidget(Context context, FormEntryPrompt prompt, boolean isRecord,
                         FormEntryActivity entryActivity) {
        super(context, prompt);
        ActivityLogManager.InsertActionWithWidget(Constants.ENTER_DATA_INSTANCE, Constants.WIDGET_SIP_CALL, Constants.CREATE_WIDGET, prompt.getAnswerText(), mPrompt);
        mEntryActivity = entryActivity;
        saveCurrentAnswerListener = (SaveCurrentAnswerListener) context;
        mInstanceFolder = Collect.getInstance().getFormController()
                .getInstancePath().getParent();
        if (prompt.getAppearanceHint() != null && prompt.getAppearanceHint().contains("text-nolabel")) {
            isShowQuestionText = false;
            if (prompt.getAppearanceHint().contains("call")) {
                addQuestionText(prompt);
            }
        } else isShowQuestionText = true;

        setOrientation(LinearLayout.VERTICAL);

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOT);
        XPathFuncExpr xPathFuncExpr = ExternalDataUtil.getSIPXpathFuncExpr(appearance);
        if (xPathFuncExpr != null) {
            sipProfile = ExternalDataUtil.populateSIPValue(mPrompt.getIndex(), xPathFuncExpr);
        }



        locked = false;
        mBinaryName = prompt.getAnswerText();
        if (mBinaryName == null && defaultValue != null)
            mBinaryName = defaultValue;
        if (mPrompt.isReadOnly()) {
            mCaptureButton.setVisibility(View.GONE);
        }

        IPCallManager.getInstance().setFormEntryActivity(mEntryActivity);
        IPCallManager.getInstance().setUpdateUIListener(this);
        IPCallManager.getInstance().setSipProfile(sipProfile);
        IPCallManager.getInstance().setSaveListener(saveCurrentAnswerListener);

        mCaptureButton.setEnabled(false);
        if (ActivityCompat.checkSelfPermission(mEntryActivity,
                Manifest.permission.USE_SIP) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(mEntryActivity,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(mEntryActivity,
                Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            mEntryActivity.setPermissionRequestListener(new PermissionRequestListener() {
                @Override
                public void onPermissionGranted() {
                    Collect.getInstance().getFormController()
                            .setIndexWaitingForData(mPrompt.getIndex());
                    IPCallManager.getInstance().connectToServer();
                }

                @Override
                public void onPermissionDenied() {
                    Toast.makeText(mEntryActivity, R.string.cpms_permission_not_allowed,
                            Toast.LENGTH_LONG).show();
                }
            });

            ActivityCompat.requestPermissions(mEntryActivity,
                    new String[]{Manifest.permission.USE_SIP, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS},
                    FormEntryActivity.PERMISSIONS_REQUEST);
        } else {
            Collect.getInstance().getFormController()
                    .setIndexWaitingForData(mPrompt.getIndex());
            IPCallManager.getInstance().connectToServer();
        }

        if (IPCallManager.getInstance().isRunning() && LinphoneService.isReady()) {
            updateCallButton(true);
            //TODO: Check here: call before service created so linphone manager is not instantiated
            LinphoneManager.getInstance().setAlreadyAcceptedOrDeniedCall(false);
        }
    }

    @Override
    public void refreshQuestion(FormEntryPrompt prompt) {
        super.refreshQuestion(prompt);
        this.mPrompt = prompt;
        if (mCaptureButton != null)
            mCaptureButton.setEnabled(true);
    }

    // Override QuestionWidget's add question text. Build it the same
    // but add it to the relative layout
    protected void addQuestionText(FormEntryPrompt p) {

        // Add the text view. Textview always exists, regardless of whether there's text.
        initTypoGraph(p.getLongText());

        if (p.getAppearanceHint() != null && p.getAppearanceHint().contains("text-nolabel")) {
            if (mQuestionText != null) {
                mQuestionText.setVisibility(GONE);
            }
            isShowQuestionText = false;
        } else isShowQuestionText = true;

        if (p.getLongText() == null) {
            mQuestionText.setVisibility(GONE);
        }

        // Put the question text on the left half of the screen
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.weight = 0.6f;

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        if (isShowQuestionText)
            imageParams.weight = 0.4f;
//        else imageParams.weight = 0.5f;

        imageParams.gravity = Gravity.CENTER;
        imageParams.setMargins(MARGIN_BOT, MARGIN_BOT, MARGIN_BOT, MARGIN_BOT);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(getContext(), R.style.ButtonCustom);
        mCaptureButton = new AppCompatButton(contextThemeWrapper);
        mCaptureButton.setId(QuestionWidget.newUniqueId());

        if (!LinphoneService.isReady())
            mCaptureButton.setText(this.getResources().getString(R.string.call_connecting));
        else
            mCaptureButton.setText(this.getResources().getString(R.string.call));

        mCaptureButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mCaptureButton.getBackground().setColorFilter(colorButton, PorterDuff.Mode.SRC);

        // launch capture intent on click
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locked)
                    return;
                locked = true;
                mCaptureButton.setEnabled(false);
                IPCallManager.getInstance().placeCall(getIndex());
            }
        });


        // and hide the capture and choose button if read-only
        if (mPrompt.isReadOnly()) {
            mCaptureButton.setVisibility(View.GONE);
        }

        LinearLayout.LayoutParams playimageParams =
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        if (isShowQuestionText)
            playimageParams.weight = 0.1f;
        else playimageParams.weight = 0.5f;
        playimageParams.gravity = Gravity.CENTER | Gravity.CENTER_VERTICAL;
        playimageParams.setMargins(0, 0, 0, 0);


        if (mBinaryName != null) {
            File f = new File(mInstanceFolder + File.separator + mBinaryName);
            // MediaPlayer mp = MediaPlayer.create(yourActivity,
            // Uri.parse(pathofyourrecording));
            // int duration = mp.getDuration();
            DecimalFormat decimalFormat = new DecimalFormat("##.##");
            double size = f.length() / 1024;
            String length = "" + decimalFormat.format(size) + "kb";

            final MediaPlayer mediaPlayer = new MediaPlayer();
            int duration = 0;

            try {
                mediaPlayer.setDataSource(f.getAbsolutePath());
                mediaPlayer.prepare();
                duration = mediaPlayer.getDuration();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
        }
        // and hide the capture and choose button if read-only
        if (mPrompt.isReadOnly()) {
            mCaptureButton.setVisibility(View.GONE);
        }

        questionLayout = new LinearLayout(getContext());
        questionLayout.setOrientation(LinearLayout.HORIZONTAL);
        if (isShowQuestionText) {
            questionLayout.addView(mQuestionText, labelParams);
            questionLayout.addView(mCaptureButton, imageParams);
        } else {
            questionLayout.addView(mCaptureButton, imageParams);
        }

        // and hide the capture and choose button if read-only
        if (mPrompt.isReadOnly()) {
            mCaptureButton.setVisibility(View.GONE);
        }
    }

    private void deleteMedia() {
        String name = mBinaryName;
        mBinaryName = null;
        FormEntryActivity activity = (FormEntryActivity) RTASurvey.getInstance().getActivity();
        if (activity != null) {
            activity.saveAndrefeshWhenRemoveData();
        }
        // delete from media provider
        int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder + File.separator + name);
    }

    private void deleteMedianotSave() {
        // get the file path and delete the file
        String name = mBinaryName;
        // clean up variables
        mBinaryName = null;
        // delete from media provider
        int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder + File.separator + name);
    }

    @Override
    public void clearAnswer() {
        deleteMedia();

        ActivityLogManager.InsertActionWithWidget(Constants.ENTER_DATA_INSTANCE, Constants.WIDGET_SIP_CALL, Constants.CLEAR_ANSWER, mPrompt);
        if (onFocusListener != null) {
            onFocusListener.onFocus(mPrompt.getIndex(), isQAcheckpoint, mPrompt.getAppearanceHint());
        }
        locked = false;
    }

    @Override
    public IAnswerData getAnswer() {
        if (mPrompt.getAnswerText() != null)
            return new StringData(mPrompt.getAnswerText());
        else return null;
    }


    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        RTAInputManager inputManager = RTAInputManager.getInstance();
        inputManager.hideRTAKeyboard();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        if (mCaptureButton != null)
            mCaptureButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mCaptureButton.cancelLongPress();
        if (mQuestionText != null) {
            mQuestionText.cancelLongPress();
        }
        if (mHelpText != null) {
            mHelpText.cancelLongPress();
        }
    }

    @Override
    public String getAlertMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void exportCSV() {
    }

    @Override
    public void setViewOnLy() {
        mCaptureButton.setEnabled(false);
        super.setViewOnLy();
    }

    @Override
    public boolean isShowRemoveIcon() {
        return false;
    }

    private Drawable createDrawable(int color, float radius) {
        GradientDrawable defaultDrawable = new GradientDrawable();
        defaultDrawable.setCornerRadius(radius);
        defaultDrawable.setColor(color);
        return defaultDrawable;
    }

    @Override
    public void refreshQuestionXpath() {
        super.refreshQuestionXpath();
        if (appearance != null) {
            XPathFuncExpr xPathFuncExpr = ExternalDataUtil.getSIPXpathFuncExpr(appearance);
            if (xPathFuncExpr != null) {
                sipProfile = ExternalDataUtil.populateSIPValue(mPrompt.getIndex(), xPathFuncExpr);
                IPCallManager.getInstance().setSipProfile(sipProfile);
            }
            locked = false;
        }
    }


    @Override
    public void updateCallButton(boolean isConnected) {
        if (mCaptureButton != null) {
            mCaptureButton.setEnabled(isConnected);
            mCaptureButton.setText(getResources().getString(R.string.call));
        }
    }

    @Override
    public void updateToCallWidget(boolean isCalled) {

    }
}