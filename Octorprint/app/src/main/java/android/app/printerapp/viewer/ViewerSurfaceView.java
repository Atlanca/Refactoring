package android.app.printerapp.viewer;


import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class ViewerSurfaceView extends GLSurfaceView{
	//View Modes
	public static final int NORMAL = 0;
	public static final int XRAY = 4;
	public static final int TRANSPARENT = 2;
	public static final int LAYERS = 3;
	public static final int OVERHANG = 1;

    //Zoom limits
    public static final int MIN_ZOOM = -500;
    public static final int MAX_ZOOM = -30;
    public static final float SCALE_FACTOR = 400f;

	ViewerRenderer mRenderer;
	private List<DataStorage> mDataList = new ArrayList<DataStorage>();
	//Touch
	private int mMode;
	private final float TOUCH_SCALE_FACTOR_ROTATION = 90.0f / 320;  //180.0f / 320;
	private float mPreviousX;
	private float mPreviousY;
    private float mPreviousDragX;
    private float mPreviousDragY;

   // zoom rate (larger > 1.0f > smaller)
	private float pinchScale = 1.0f;

	private PointF pinchStartPoint = new PointF();
	private float pinchStartY = 0.0f;
	private float pinchStartZ = 0.0f;
	private float pinchStartDistance = 0.0f;
	private float pinchStartFactorX = 0.0f;
	private float pinchStartFactorY = 0.0f;
	private float pinchStartFactorZ = 0.0f;

	// for touch event handling
	private static final int TOUCH_NONE = 0;
	private static final int TOUCH_DRAG = 1;
	private static final int TOUCH_ZOOM = 2;
	private int touchMode = TOUCH_NONE;

	//Viewer modes
	public static final int ROTATION_MODE =0;
	public static final int TRANSLATION_MODE = 1;
	public static final int LIGHT_MODE = 2;

	private int mMovementMode;

	//Edition mode
	private boolean mEdition = false;

	private int mObjectPressed = -1;


	public ViewerSurfaceView(Context context) {
	    super(context);
	}
	public ViewerSurfaceView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}

    //Double tap logic
    boolean mDoubleTapFirstTouch = false;
    long mDoubleTapCurrentTime = 0;

    public static final int DOUBLE_TAP_MAX_TIME = 300;

	/**
	 *
	 * @param context Context
	 * @param data Data to render
	 * @param state Type of rendering: normal, triangle, overhang, layers
	 * @param mode Mode of rendering: do snapshot (take picture for library), dont snapshot (normal) and print_preview (gcode preview in print progress)
	 */
	public ViewerSurfaceView(Context context, List<DataStorage> data, int state, int mode) {
		super(context);
		// Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        this.mMode = mode;
        this.mDataList = data;
		this.mRenderer = new ViewerRenderer (data, context, state, mode);
		setRenderer(mRenderer);

		// Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	/**
	 * Set the view options depending on the model
	 * @param state
	 */
	public void configViewMode (int state) {
		switch (state) {
		case (ViewerSurfaceView.NORMAL):
			setOverhang(false);
			setXray(false);
			setTransparent(false);
			break;
		case (ViewerSurfaceView.XRAY):
			setOverhang(false);
			setXray(true);
			setTransparent(false);
			break;
		case (ViewerSurfaceView.TRANSPARENT):
			setOverhang(false);
			setXray(false);
			setTransparent(true);
			break;
		case (ViewerSurfaceView.OVERHANG):
			setOverhang(true);
			setXray(false);
			setTransparent(false);
			break;
		}

		requestRender();
	}


	/**
	 * Tells the render if overhang is activated or not
	 * @param overhang
	 */
	public void setOverhang (boolean overhang) {
		mRenderer.setOverhang(overhang);
	}

	/**
	 * Tell the render if transparent view is activated or not
	 * @param trans
	 */
	public void setTransparent (boolean trans) {
		mRenderer.setTransparent(trans);
	}

	/**
	 * Tells render if xray view (triangles view) is activated or not
	 * @param xray
	 */
	public void setXray (boolean xray) {
		mRenderer.setXray(xray);
	}

	/**
	 * On touch events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float x = event.getX();
        float y = event.getY();

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// starts pinch
			case MotionEvent.ACTION_POINTER_DOWN:

                if (mMovementMode!=TRANSLATION_MODE)
				if (event.getPointerCount() >= 2) {

                    mMovementMode = TRANSLATION_MODE;

					pinchStartDistance = getPinchDistance(event);
					pinchStartY = mRenderer.getCameraPosY();
					pinchStartZ = mRenderer.getCameraPosZ();

					if (mObjectPressed!=-1) {
						pinchStartFactorX = mDataList.get(mObjectPressed).getLastScaleFactorX();
						pinchStartFactorY = mDataList.get(mObjectPressed).getLastScaleFactorY();
						pinchStartFactorZ = mDataList.get(mObjectPressed).getLastScaleFactorZ();
					}

					if (pinchStartDistance > 0f) {
						getPinchCenterPoint(event, pinchStartPoint);
						mPreviousX = pinchStartPoint.x;
						mPreviousY = pinchStartPoint.y;
						touchMode = TOUCH_ZOOM;

					}

				}
				break;
			case MotionEvent.ACTION_DOWN:

                mPreviousX = event.getX();
                mPreviousY = event.getY();
                mPreviousDragX = mPreviousX;
                mPreviousDragY = mPreviousY;

                if (mMode!= ViewerMainFragment.PRINT_PREVIEW){

                /*
                Detect double-tapping to restore the panel
                 */


                    if(mDoubleTapFirstTouch && (System.currentTimeMillis() - mDoubleTapCurrentTime) <= DOUBLE_TAP_MAX_TIME) { //Second touch

                        //do stuff here for double tap
                        mDoubleTapFirstTouch = false;

                        //Move the camera to the initial values once per frame
                        while (!mRenderer.restoreInitialCameraPosition(0,0, false, true)){
                            requestRender();
                        };

                    } else { //First touch

                        mDoubleTapFirstTouch = true;
                        mDoubleTapCurrentTime = System.currentTimeMillis();
                    }

                }

                touchMode = TOUCH_DRAG;


				break;
			case MotionEvent.ACTION_MOVE:

					if (touchMode == TOUCH_ZOOM && pinchStartDistance > 0f) {

                        pinchScale = getPinchDistance(event) / pinchStartDistance;

                        // on pinch
                        PointF pt = new PointF();
                        getPinchCenterPoint(event, pt);

                        mPreviousX = pt.x;
                        mPreviousY = pt.y;

						/**
						 * Zoom controls will be limited to MIN and MAX
						 */

						if ((mRenderer.getCameraPosY() < MIN_ZOOM) && (pinchScale < 1.0)) {


						} else if ((mRenderer.getCameraPosY() > MAX_ZOOM) && (pinchScale > 1.0)){


						} else{
							mRenderer.setCameraPosY(pinchStartY / pinchScale);
							mRenderer.setCameraPosZ(pinchStartZ / pinchScale);
						}

						requestRender();


					}

                //Drag plate
                if (touchMode != TOUCH_NONE)
                if (pinchScale<1.5f) { //Min value to end dragging

                    //Hold its own previous drag
                    float dx = x - mPreviousDragX;
                    float dy = y - mPreviousDragY;

                        mPreviousDragX = x;
                        mPreviousDragY = y;

						if (!mEdition) dragAccordingToMode (dx,dy); //drag if there is no model


                }


					requestRender();
	                break;

			// end pinch
			case MotionEvent.ACTION_UP:

                mMovementMode = ROTATION_MODE;

			case MotionEvent.ACTION_POINTER_UP:

				if (touchMode == TOUCH_ZOOM) {
					pinchScale = 1.0f;
					pinchStartPoint.x = 0.0f;
					pinchStartPoint.y = 0.0f;
				}

				touchMode = TOUCH_NONE;

			    requestRender();
				break;
		}
		return true;
	}
		
	/**
	 * It rotates the plate (ROTATION or TRANSLATION) 
	 * @param dx movement on x axis
	 * @param dy movement on y axis
	 */
	private void dragAccordingToMode (float dx, float dy) {
		switch (mMovementMode) {
		case ROTATION_MODE:
			doRotation (dx,dy);
            //doTranslation (dx,dy);
			break;
		case TRANSLATION_MODE:
            float scale = -mRenderer.getCameraPosY() / 500f;
			doTranslation ((dx * scale) , (dy * scale));
			break;
		}
	}
	
	/**
	 * Do rotation (plate rotation, not model rotation)
	 * @param dx movement on x axis
	 * @param dy movement on y axis
	 */
	private void doRotation (float dx, float dy) {              
        mRenderer.setSceneAngleX(dx*TOUCH_SCALE_FACTOR_ROTATION);
        mRenderer.setSceneAngleY(dy*TOUCH_SCALE_FACTOR_ROTATION);		
	} 
	
	/**
	 * Do rotation (plate rotation, not model rotation)
	 * @param dx movement on x axis
	 * @param dy movement on y axis
	 */
	private void doTranslation(float dx, float dy) {

        //mRenderer.setCenterX(-1);
		//mRenderer.setCenterY(-1);
        mRenderer.matrixTranslate(dx,-dy,0);
	}

	/**
	 * Get distanced pinched
	 * @param event
	 * @return
	 */
	private float getPinchDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	
	/**
	 * Get center point
	 * @param event
	 * @param pt pinched point
	 */
	private void getPinchCenterPoint(MotionEvent event, PointF pt) {
		pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
		pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
	}
}


