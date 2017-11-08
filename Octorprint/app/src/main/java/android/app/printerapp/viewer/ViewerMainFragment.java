package android.app.printerapp.viewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.printerapp.Log;
import android.app.printerapp.MainActivity;
import android.app.printerapp.R;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.library.LibraryController;
import android.app.printerapp.model.ModelProfile;
import android.app.printerapp.util.ui.CustomEditableSlider;
import android.app.printerapp.util.ui.CustomPopupWindow;
import android.app.printerapp.util.ui.ListIconPopupWindowAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.devsmart.android.ui.HorizontalListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class ViewerMainFragment extends Fragment {
    //Tabs
    private static final int NORMAL = 0;
    private static final int OVERHANG = 1;
    private static final int TRANSPARENT = 2;
    private static final int XRAY = 3;
    private static final int LAYER = 4;

    private static int mCurrentViewMode = 0;

    //Constants
    public static final int DO_SNAPSHOT = 0;
    public static final int DONT_SNAPSHOT = 1;
    public static final int PRINT_PREVIEW = 3;
    public static final boolean STL = true;
    public static final boolean GCODE = false;

    private static final float POSITIVE_ANGLE = 15;
    private static final float NEGATIVE_ANGLE = -15;

    private static final int MENU_HIDE_OFFSET_SMALL = 20;
    private static final int MENU_HIDE_OFFSET_BIG = 1000;

    //Variables
    private static File mFile;

    private static ViewerSurfaceView mSurface;
    private static FrameLayout mLayout;
    private boolean isKeyboardShown = false;

    private static List<DataStorage> mDataList = new ArrayList<DataStorage>();


    private static Context mContext;
    private static View mRootView;

    private static LinearLayout mStatusBottomBar;
    private static FrameLayout mBottomBar;
    private static LinearLayout mRotationLayout;
    private static LinearLayout mScaleLayout;
    private static CustomEditableSlider mRotationSlider;
//    private static ImageView mActionImage;

    private static EditText mScaleEditX;
    private static EditText mScaleEditY;
    private static EditText mScaleEditZ;
    private static ImageButton mUniformScale;

    private static ScaleChangeListener mTextWatcherX;
    private static ScaleChangeListener mTextWatcherY;
    private static ScaleChangeListener mTextWatcherZ;

    /**
     * ****************************************************************************
     */
    private static int mCurrentType = WitboxFaces.TYPE_WITBOX;
    ;
    private static int[] mCurrentPlate = new int[]{WitboxFaces.WITBOX_LONG, WitboxFaces.WITBOX_WITDH, WitboxFaces.WITBOX_HEIGHT};
    ;

    private static int mCurrentAxis;

    //Empty constructor
    public ViewerMainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retain instance to keep the Fragment from destroying itself
        setRetainInstance(true);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Reference to View
        mRootView = null;

        //If is not new
        if (savedInstanceState == null) {

            //Show custom option menu
            setHasOptionsMenu(true);

            //Inflate the fragment
            mRootView = inflater.inflate(R.layout.print_panel_main,
                    container, false);

            mContext = getActivity();



            initUIElements();

//            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

            //Init slicing elements
            mCurrentType = WitboxFaces.TYPE_WITBOX;
            mCurrentPlate = new int[]{WitboxFaces.WITBOX_LONG, WitboxFaces.WITBOX_WITDH, WitboxFaces.WITBOX_HEIGHT};

            mSurface = new ViewerSurfaceView(mContext, mDataList, NORMAL, DONT_SNAPSHOT);
            draw();

            //Hide the action bar when editing the scale of the model
            mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    Rect r = new Rect();
                    mRootView.getWindowVisibleDisplayFrame(r);

                    if (mSurface.getEditionMode() == ViewerSurfaceView.SCALED_EDITION_MODE){

                        int[] location = new int[2];
                        int heightDiff = mRootView.getRootView().getHeight() - (r.bottom - r.top);

                        if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...

                            if (!isKeyboardShown) {
                                isKeyboardShown = true;
                                mActionModePopupWindow.getContentView().getLocationInWindow(location);

                                if (Build.VERSION.SDK_INT >= 19)
                                    mActionModePopupWindow.update(location[0], location[1] - MENU_HIDE_OFFSET_SMALL);
                                else  mActionModePopupWindow.update(location[0], location[1] + MENU_HIDE_OFFSET_BIG);
                            }
                        } else {
                            if (isKeyboardShown) {
                                isKeyboardShown = false;
                                mActionModePopupWindow.getContentView().getLocationInWindow(location);

                                if (Build.VERSION.SDK_INT >= 19)
                                    mActionModePopupWindow.update(location[0], location[1] + MENU_HIDE_OFFSET_SMALL);
                                else  mActionModePopupWindow.update(location[0], location[1] - MENU_HIDE_OFFSET_BIG);

                            }

                        }
                    }

                }
            });
        }

        return mRootView;

    }

    public static void resetWhenCancel() {


        //Crashes on printview
        try {
            mDataList.remove(mDataList.size() - 1);
            mSurface.requestRender();

            mCurrentViewMode = NORMAL;
            mSurface.configViewMode(mCurrentViewMode);

        } catch (Exception e) {

            e.printStackTrace();

        }


    }

    /**
     * ********************** UI ELEMENTS *******************************
     */

    private void initUIElements() {

        mLayout = (FrameLayout) mRootView.findViewById(R.id.viewer_container_framelayout);

        mRotationSlider = (CustomEditableSlider) mRootView.findViewById(R.id.print_panel_slider);
        mRotationSlider.setValue(12);
        mRotationSlider.setShownValue(0);
        mRotationSlider.setMax(24);
        mRotationSlider.setShowNumberIndicator(true);
        mRotationSlider.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                return false;
            }
        });
        mRotationSlider.setOnValueChangedListener(new CustomEditableSlider.OnValueChangedListener() {

            boolean lock = false;


            @Override
            public void onValueChanged(int i) {

                //Calculation on a 12 point seekbar
                float newAngle = (i - 12) * POSITIVE_ANGLE;

                mRotationSlider.setShownValue((int)newAngle);

                try {


                    if (!lock) {

                        switch (mCurrentAxis) {

                            case 0:
                                mSurface.rotateAngleAxisX(newAngle);
                                break;
                            case 1:
                                mSurface.rotateAngleAxisY(newAngle);
                                break;
                            case 2:
                                mSurface.rotateAngleAxisZ(newAngle);
                                break;
                            default:
                                return;

                        }

                    }

                    mSurface.requestRender();


                } catch (ArrayIndexOutOfBoundsException e) {

                    e.printStackTrace();
                }


            }
        });

        mStatusBottomBar = (LinearLayout) mRootView.findViewById(R.id.model_status_bottom_bar);
        mRotationLayout = (LinearLayout) mRootView.findViewById(R.id.model_button_rotate_bar_linearlayout);
        mScaleLayout  = (LinearLayout) mRootView.findViewById(R.id.model_button_scale_bar_linearlayout);

        mTextWatcherX = new ScaleChangeListener(0);
        mTextWatcherY = new ScaleChangeListener(1);
        mTextWatcherZ = new ScaleChangeListener(2);

        mScaleEditX = (EditText) mScaleLayout.findViewById(R.id.scale_bar_x_edittext);
        mScaleEditY = (EditText) mScaleLayout.findViewById(R.id.scale_bar_y_edittext);
        mScaleEditZ = (EditText) mScaleLayout.findViewById(R.id.scale_bar_z_edittext);
        mUniformScale = (ImageButton) mScaleLayout.findViewById(R.id.scale_uniform_button);
        mUniformScale.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mUniformScale.isSelected()){
                    mUniformScale.setSelected(false);
                } else {
                    mUniformScale.setSelected(true);
                }


            }
        });
        mUniformScale.setSelected(true);

        mScaleEditX.addTextChangedListener(mTextWatcherX);
        mScaleEditY.addTextChangedListener(mTextWatcherY);
        mScaleEditZ.addTextChangedListener(mTextWatcherZ);

        mStatusBottomBar.setVisibility(View.VISIBLE);
        mBottomBar = (FrameLayout) mRootView.findViewById(R.id.bottom_bar);
        mBottomBar.setVisibility(View.INVISIBLE);
        mCurrentAxis = -1;

    }

    /**
     * Change the current rotation axis and update the text accordingly
     * <p/>
     * Alberto
     */
    public static void changeCurrentAxis(int currentAxis) {

        mCurrentAxis = currentAxis;

        float currentAngle = 12;

        switch (mCurrentAxis) {

            case 0:
                mRotationSlider.setBackgroundColor(Color.GREEN);
                break;

            case 1:
                mRotationSlider.setBackgroundColor(Color.RED);
                break;
            case 2:
                mRotationSlider.setBackgroundColor(Color.BLUE);
                break;
            default:
                mRotationSlider.setBackgroundColor(Color.TRANSPARENT);
                break;

        }

        mSurface.setRendererAxis(mCurrentAxis);

        mRotationSlider.setValue((int) currentAngle);

    }


    /**
     * Open a dialog if it's a GCODE to warn the user about unsaved data loss
     *
     * @param filePath
     */
    public static void openFileDialog(final String filePath) {

        if (LibraryController.hasExtension(0, filePath)) {

            if (!StlFile.checkFileSize(new File(filePath), mContext)) {
                new MaterialDialog.Builder(mContext)
                        .title(R.string.warning)
                        .content(R.string.viewer_file_size)
                        .negativeText(R.string.cancel)
                        .negativeColorRes(R.color.body_text_2)
                        .positiveText(R.string.ok)
                        .positiveColorRes(R.color.theme_accent_1)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                openFile(filePath);

                            }
                        })
                        .build()
                        .show();

            } else {
                openFile(filePath);
            }
        } else if (LibraryController.hasExtension(1, filePath)) {

            new MaterialDialog.Builder(mContext)
                    .title(R.string.warning)
                    .content(R.string.viewer_open_gcode_dialog)
                    .negativeText(R.string.cancel)
                    .negativeColorRes(R.color.body_text_2)
                    .positiveText(R.string.ok)
                    .positiveColorRes(R.color.theme_accent_1)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            openFile(filePath);
                        }
                    })
                    .build()
                    .show();
        }


    }


    //Select the last object added
    public static void doPress(){

        mSurface.doPress(mDataList.size() - 1);

    }


    public static void openFile(String filePath) {
        DataStorage data = null;
        //Open the file
        if (LibraryController.hasExtension(0, filePath)) {

            data = new DataStorage();

//            mVisibilityModeButton.setVisibility(View.VISIBLE);
            mFile = new File(filePath);
            StlFile.openStlFile(mContext, mFile, data, DONT_SNAPSHOT);
            mCurrentViewMode = NORMAL;

        } else if (LibraryController.hasExtension(1, filePath)) {

            data = new DataStorage();
            if (!filePath.contains("/temp")) {
//                mVisibilityModeButton.setVisibility(View.GONE);

            }
            mFile = new File(filePath);
            GcodeFile.openGcodeFile(mContext, mFile, data, DONT_SNAPSHOT);
            mCurrentViewMode = LAYER;

        }

        mDataList.add(data);
    }

    public static void draw() {
        //Once the file has been opened, we need to refresh the data list. If we are opening a .gcode file, we need to ic_action_delete the previous files (.stl and .gcode)
        //If we are opening a .stl file, we need to ic_action_delete the previous file only if it was a .gcode file.
        //We have to do this here because user can cancel the opening of the file and the Print Panel would appear empty if we clear the data list.

        String filePath = "";
        if (mFile != null) filePath = mFile.getAbsolutePath();

        if (LibraryController.hasExtension(0, filePath)) {
            if (mDataList.size() > 1) {
                if (LibraryController.hasExtension(1, mDataList.get(mDataList.size() - 2).getPathFile())) {
                    mDataList.remove(mDataList.size() - 2);
                }
            }
            Geometry.relocateIfOverlaps(mDataList);
//            mSeekBar.setVisibility(View.INVISIBLE);

        } else if (LibraryController.hasExtension(1, filePath)) {
            if (mDataList.size() > 1)
                while (mDataList.size() > 1) {
                    mDataList.remove(0);
                }
//            mSeekBar.setVisibility(View.VISIBLE);
        }

        //Add the view
        mLayout.removeAllViews();
        mLayout.addView(mSurface, 0);
//        mLayout.addView(mSeekBar, 1);
//        mLayout.addView(mSizeText, 1);

//      mLayout.addView(mUndoButtonBar, 3);
//      mLayout.addView(mEditionLayout, 2);
    }

    /**
     * ********************** SURFACE CONTROL *******************************
     */
    //This method will set the visibility of the surfaceview so it doesn't overlap
    //with the video grid view
    public void setSurfaceVisibility(int i) {

        if (mSurface != null) {
            switch (i) {
                case 0:
                    mSurface.setVisibility(View.GONE);
                    break;
                case 1:
                    mSurface.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private static PopupWindow mActionModePopupWindow;
    private static PopupWindow mCurrentActionPopupWindow;

    /**
     * ********************** ACTION MODE *******************************
     */

    /**
     * Show a pop up window with the available actions of the item
     */
    public static void showActionModePopUpWindow() {

        hideCurrentActionPopUpWindow();


        if (mActionModePopupWindow == null) {

            //Get the content view of the pop up window
            final LinearLayout popupLayout = (LinearLayout) ((Activity) mContext).getLayoutInflater()
                    .inflate(R.layout.item_edit_popup_menu, null);
            popupLayout.measure(0, 0);

            //Set the behavior of the action buttons
            int imageButtonHeight = 0;
            for (int i = 0; i < popupLayout.getChildCount(); i++) {
                View v = popupLayout.getChildAt(i);
                if (v instanceof ImageButton) {
                    ImageButton ib = (ImageButton) v;
                    imageButtonHeight = ib.getMeasuredHeight();
                    ib.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            onActionItemSelected((ImageButton) view);
                        }
                    });
                }
            }

            //Show the pop up window in the correct position
            int[] viewerContainerCoordinates = new int[2];
            mLayout.getLocationOnScreen(viewerContainerCoordinates);
            int popupLayoutPadding = (int) mContext.getResources().getDimensionPixelSize(R.dimen.content_padding_normal);
            int popupLayoutWidth = popupLayout.getMeasuredWidth();
            int popupLayoutHeight = popupLayout.getMeasuredHeight();
            final int popupLayoutX = viewerContainerCoordinates[0] + mLayout.getWidth() - popupLayoutWidth;
            final int popupLayoutY = viewerContainerCoordinates[1] + imageButtonHeight + popupLayoutPadding;

            mActionModePopupWindow = (new CustomPopupWindow(popupLayout, popupLayoutWidth,
                    popupLayoutHeight, R.style.SlideRightAnimation).getPopupWindow());

            mActionModePopupWindow.showAtLocation(mSurface, Gravity.NO_GRAVITY,
                    popupLayoutX, popupLayoutY);

        }
    }

    /**
     * Hide the action mode pop up window
     */
    public static void hideActionModePopUpWindow() {
        if (mActionModePopupWindow != null) {
            mActionModePopupWindow.dismiss();
            mSurface.exitEditionMode();
            mRotationLayout.setVisibility(View.GONE);
            mScaleLayout.setVisibility(View.GONE);
            mStatusBottomBar.setVisibility(View.VISIBLE);
            mBottomBar.setVisibility(View.INVISIBLE);
            mActionModePopupWindow = null;
            mSurface.setRendererAxis(-1);
        }

//        //Hide size text
//        if (mSizeText != null)
//            if (mSizeText.getVisibility() == View.VISIBLE) mSizeText.setVisibility(View.INVISIBLE);

        //hideCurrentActionPopUpWindow();
    }

    /**
     * Hide the current action pop up window if it is showing
     */
    public static void hideCurrentActionPopUpWindow() {
        if (mCurrentActionPopupWindow != null) {
            mCurrentActionPopupWindow.dismiss();
            mCurrentActionPopupWindow = null;
        }
        hideSoftKeyboard();
    }

    public static void hideSoftKeyboard() {
        try{
            InputMethodManager inputMethodManager = (InputMethodManager)  mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(((Activity)mContext).getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e){

        }

    }

    /**
     * Perform the required action depending on the pressed button
     *
     * @param item Action button that has been pressed
     */
    public static void onActionItemSelected(final ImageButton item) {

        mStatusBottomBar.setVisibility(View.VISIBLE);
        mSurface.setRendererAxis(-1);
        mRotationLayout.setVisibility(View.GONE);
        mScaleLayout.setVisibility(View.GONE);
        mBottomBar.setVisibility(View.INVISIBLE);
//        mSizeText.setVisibility(View.VISIBLE);

        selectActionButton(item.getId());

        switch (item.getId()) {
            case R.id.move_item_button:
                hideCurrentActionPopUpWindow();
                mSurface.setEditionMode(ViewerSurfaceView.MOVE_EDITION_MODE);
                break;
            case R.id.rotate_item_button:

                if (mCurrentActionPopupWindow == null) {
                    final String[] actionButtonsValues = mContext.getResources().getStringArray(R.array.rotate_model_values);
                    final TypedArray actionButtonsIcons = mContext.getResources().obtainTypedArray(R.array.rotate_model_icons);
                    showHorizontalMenuPopUpWindow(item, actionButtonsValues, actionButtonsIcons,
                            null, new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    changeCurrentAxis(Integer.parseInt(actionButtonsValues[position]));
                                    mBottomBar.setVisibility(View.VISIBLE);
                                    mRotationLayout.setVisibility(View.VISIBLE);
                                    mSurface.setEditionMode(ViewerSurfaceView.ROTATION_EDITION_MODE);
                                    hideCurrentActionPopUpWindow();
                                    item.setImageResource(actionButtonsIcons.getResourceId(position, -1));
//                                    mActionImage.setImageDrawable(mContext.getResources().getDrawable(actionButtonsIcons.getResourceId(position, -1)));
                                }
                            });
                } else {
                    hideCurrentActionPopUpWindow();
                }
                break;
            case R.id.scale_item_button:
                hideCurrentActionPopUpWindow();
                mBottomBar.setVisibility(View.VISIBLE);
                mScaleLayout.setVisibility(View.VISIBLE);
                mSurface.setEditionMode(ViewerSurfaceView.SCALED_EDITION_MODE);
//                mActionImage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_scale));
                displayModelSize(mSurface.getObjectPresed());
                break;
                /*case R.id.mirror:
                    mSurface.setEditionMode(ViewerSurfaceView.MIRROR_EDITION_MODE);
                    mSurface.doMirror();

                    slicingCallback();
                    break;*/
            case R.id.delete_item_button:
                hideCurrentActionPopUpWindow();
                mSurface.deleteObject();
                hideActionModePopUpWindow();
                break;
        }

    }


    /**
     * Set the state of the selected action button
     *
     * @param selectedId Id of the action button that has been pressed
     */
    public static void selectActionButton(int selectedId) {

        if (mActionModePopupWindow != null) {
            //Get the content view of the pop up window
            final LinearLayout popupLayout = (LinearLayout) mActionModePopupWindow.getContentView();

            //Set the behavior of the action buttons
            for (int i = 0; i < popupLayout.getChildCount(); i++) {
                View v = popupLayout.getChildAt(i);
                if (v instanceof ImageButton) {
                    ImageButton ib = (ImageButton) v;
                    if (ib.getId() == selectedId)
                        ib.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.oval_background_green));
                    else
                        ib.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.action_button_selector_dark));
                }
            }
        }
    }

    /**
     * Show a pop up window with a horizontal list view as a content view
     */
    public static void showHorizontalMenuPopUpWindow(View currentView, String[] actionButtonsValues,
                                                     TypedArray actionButtonsIcons,
                                                     String selectedOption,
                                                     AdapterView.OnItemClickListener onItemClickListener) {

        HorizontalListView landscapeList = new HorizontalListView(mContext, null);
        ListIconPopupWindowAdapter listAdapter = new ListIconPopupWindowAdapter(mContext, actionButtonsValues, actionButtonsIcons, selectedOption);
        landscapeList.setOnItemClickListener(onItemClickListener);
        landscapeList.setAdapter(listAdapter);

        landscapeList.measure(0, 0);

        int popupLayoutHeight = 0;
        int popupLayoutWidth = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View mView = listAdapter.getView(i, null, landscapeList);
            mView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            popupLayoutHeight = mView.getMeasuredHeight();
            popupLayoutWidth += mView.getMeasuredWidth();
        }

        //Show the pop up window in the correct position
        int[] actionButtonCoordinates = new int[2];
        currentView.getLocationOnScreen(actionButtonCoordinates);
        int popupLayoutPadding = (int) mContext.getResources().getDimensionPixelSize(R.dimen.content_padding_normal);
        final int popupLayoutX = actionButtonCoordinates[0] - popupLayoutWidth - popupLayoutPadding / 2;
        final int popupLayoutY = actionButtonCoordinates[1];

        mCurrentActionPopupWindow = (new CustomPopupWindow(landscapeList, popupLayoutWidth,
                popupLayoutHeight + popupLayoutPadding, R.style.SlideRightAnimation).getPopupWindow());

        mCurrentActionPopupWindow.showAtLocation(mSurface, Gravity.NO_GRAVITY, popupLayoutX, popupLayoutY);
    }

    /**
     * Display model width, depth and height when touched
     */
    public static void displayModelSize(int position) {
        try {
            //TODO RANDOM CRASH ArrayIndexOutOfBoundsException
            DataStorage data = mDataList.get(position);

            //Set point instead of comma
            DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
            otherSymbols.setDecimalSeparator('.');
            otherSymbols.setGroupingSeparator(',');

            //Define new decimal format to display only 2 decimals
            DecimalFormat df = new DecimalFormat("##.##", otherSymbols);

            String width = df.format((data.getMaxX() - data.getMinX()));
            String depth = df.format((data.getMaxY() - data.getMinY()));
            String height = df.format((data.getMaxZ() - data.getMinZ()));

            Log.i("Scale","Vamos a petar " + width);

            if (mScaleLayout.getVisibility() == View.VISIBLE){

                mScaleEditX.removeTextChangedListener(mTextWatcherX);
                mScaleEditY.removeTextChangedListener(mTextWatcherY);
                mScaleEditZ.removeTextChangedListener(mTextWatcherZ);

                mScaleEditX.setText(width);
                mScaleEditX.setSelection(mScaleEditX.getText().length());
                mScaleEditY.setText(depth);
                mScaleEditY.setSelection(mScaleEditY.getText().length());
                mScaleEditZ.setText(height);
                mScaleEditZ.setSelection(mScaleEditZ.getText().length());

                mScaleEditX.addTextChangedListener(mTextWatcherX);
                mScaleEditY.addTextChangedListener(mTextWatcherY);
                mScaleEditZ.addTextChangedListener(mTextWatcherZ);
            }

        } catch (ArrayIndexOutOfBoundsException e) {

            e.printStackTrace();
        }


    }

    /**
     * Notify the side panel adapters, check for null if they're not available yet (rare case)
     */

    //Refresh printers when the fragmetn is shown
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

    }

    /**
     * *********************************  SIDE PANEL *******************************************************
     */

    public static File getFile() {
        return mFile;
    }

    public static int[] getCurrentPlate() {
        return mCurrentPlate;
    }

    /********************************* RESTORE PANEL *************************/

    public static void displayErrorInAxis(int axis){

        if (mScaleLayout.getVisibility() == View.VISIBLE){
            switch (axis){

                case 0: mScaleEditX.setError(mContext.getResources().getString(R.string.viewer_error_bigger_plate,mCurrentPlate[0] * 2));
                    break;

                case 1: mScaleEditY.setError(mContext.getResources().getString(R.string.viewer_error_bigger_plate,mCurrentPlate[1] * 2));
                    break;

            }
        }


    }


    private class ScaleChangeListener implements TextWatcher{

        int mAxis;

        private ScaleChangeListener(int axis){

            mAxis = axis;

        }


        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            mScaleEditX.setError(null);
            mScaleEditY.setError(null);
            mScaleEditZ.setError(null);

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            boolean valid = true;


            //Check decimals
           if (editable.toString().endsWith(".")){
               valid = false;

            }


            if (valid)
            try{
                switch (mAxis){

                    case 0:
                        mSurface.doScale(Float.parseFloat(editable.toString()), 0, 0, mUniformScale.isSelected());
                        break;

                    case 1:
                        mSurface.doScale(0, Float.parseFloat(editable.toString()), 0, mUniformScale.isSelected());
                        break;

                    case 2:
                        mSurface.doScale(0, 0, Float.parseFloat(editable.toString()), mUniformScale.isSelected());
                        break;

                }
            } catch (NumberFormatException e){

                e.printStackTrace();

            }



        }
    }


}
