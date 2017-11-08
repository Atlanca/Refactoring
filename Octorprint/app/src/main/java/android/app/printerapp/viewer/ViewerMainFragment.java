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

    public static void openFile(String filePath) {
        DataStorage data = null;
        //Open the file
        if (LibraryController.hasExtension(0, filePath)) {

            data = new DataStorage();

            mFile = new File(filePath);
            StlFile.openStlFile(mContext, mFile, data, DONT_SNAPSHOT);
            mCurrentViewMode = NORMAL;

        } else if (LibraryController.hasExtension(1, filePath)) {

            data = new DataStorage();
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

        } else if (LibraryController.hasExtension(1, filePath)) {
            if (mDataList.size() > 1)
                while (mDataList.size() > 1) {
                    mDataList.remove(0);
                }
        }

        //Add the view
        mLayout.removeAllViews();
        mLayout.addView(mSurface, 0);
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
