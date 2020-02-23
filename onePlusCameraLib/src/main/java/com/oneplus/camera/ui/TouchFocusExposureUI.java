package com.oneplus.camera.ui;

import java.util.Arrays;
import java.util.List;

import android.graphics.PointF;
import android.os.Message;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.oneplus.base.BaseActivity.State;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.ScreenSize;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.CameraComponent;
import com.oneplus.camera.ExposureController;
import com.oneplus.camera.FocusController;
import com.oneplus.camera.FocusMode;
import com.oneplus.camera.FocusState;
import com.oneplus.camera.PhotoCaptureState;
import com.oneplus.camera.Camera.MeteringRect;

final class TouchFocusExposureUI extends CameraComponent implements TouchAutoFocusUI, TouchAutoExposureUI
{
	// Constants.
	private static final long DURATION_AF_LOCK_THREAHOLD = 1000;
	private static final long DURATION_START_AF_THREAHOLD = 500;
	private static final long DURATION_MIN_TOUCH_AF_INTERVAL = 300;
	private static final float AF_REGION_WIDTH = 0.25f;
	private static final float AF_REGION_HEIGHT = 0.25f;
	private static final float TOUCH_AF_DISTANCE_THRESHOLD = 0.2f;
	private static final int MSG_START_AF = 10000;
	private static final int MSG_LOCK_AE_AF = 10001;
	
	
	// Private fields.
	private ExposureController m_ExposureController;
	private Handle m_ExposureLockHandle;
	private FocusController m_FocusController;
	private Handle m_FocusLockHandle;
	private boolean m_IsPointerUppedWhenFocusScanning;
	private long m_LastAFTriggeredTime;
	private float m_TouchAfDistanceThreshold;
	private Handle m_TouchAfHandle;
	private final PointF m_TouchDownPosition = new PointF(-1, -1);
	
	
	// Constructor.
	TouchFocusExposureUI(CameraActivity cameraActivity)
	{
		super("Touch AE/AF UI", cameraActivity, true);
	}
	
	
	// Bind to ExposureController.
	private boolean bindToExposureController()
	{
		if(m_ExposureController != null)
			return true;
		m_ExposureController = this.findComponent(ExposureController.class);
		if(m_ExposureController == null)
			return false;
		//
		return true;
	}
	
	
	// Bind to FocusController.
	private boolean bindToFocusController()
	{
		if(m_FocusController != null)
			return true;
		m_FocusController = this.findComponent(FocusController.class);
		if(m_FocusController == null)
			return false;
		m_FocusController.addCallback(FocusController.PROP_FOCUS_STATE, new PropertyChangedCallback<FocusState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<FocusState> key, PropertyChangeEventArgs<FocusState> e)
			{
				onFocusStateChanged(e.getNewValue());
			}
		});
		return true;
	}
	
	
	// Check current state for touch focus.
	private boolean canTouchFocus()
	{
		// check activity state
		CameraActivity cameraActivity = this.getCameraActivity();
		if(cameraActivity.get(CameraActivity.PROP_STATE) != State.RUNNING)
			return false;
		
		// check preview state
		if(!cameraActivity.get(CameraActivity.PROP_IS_CAMERA_PREVIEW_RECEIVED))
			return false;
		
		// check capture state
		switch(cameraActivity.get(CameraActivity.PROP_MEDIA_TYPE))
		{
			case PHOTO:
				if(cameraActivity.get(CameraActivity.PROP_PHOTO_CAPTURE_STATE) != PhotoCaptureState.READY)
					return false;
				break;
				
			case VIDEO:
				switch(cameraActivity.get(CameraActivity.PROP_VIDEO_CAPTURE_STATE))
				{
					case READY:
					case CAPTURING:
					case PAUSED:
						break;
					default:
						return false;
				}
				break;
		}
		
		// OK
		return true;
	}
	
	
	// Handle messages.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_LOCK_AE_AF:
				this.lockFocusAndExposure();
				break;
				
			case MSG_START_AF:
				this.startAutoFocus();
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Lock AE and AF.
	private void lockFocusAndExposure()
	{
		Log.w(TAG, "lockFocusAndExposure()");
		
		// lock focus
		if(!Handle.isValid(m_FocusLockHandle) && m_FocusController != null)
			m_FocusLockHandle = m_FocusController.lockFocus(0);
		
		// lock AE
		if(!Handle.isValid(m_ExposureLockHandle) && m_ExposureController != null)
			m_ExposureLockHandle = m_ExposureController.lockAutoExposure(0);
	}
	
	
	// Called when focus state changes.
	@SuppressWarnings("incomplete-switch")
	private void onFocusStateChanged(FocusState focusState)
	{
		switch(focusState)
		{
			case SCANNING:
				m_IsPointerUppedWhenFocusScanning = !this.getCameraActivity().get(CameraActivity.PROP_IS_TOUCHING_ON_SCREEN);
				break;
			case FOCUSED:
			case UNFOCUSED:
				if(m_FocusController.get(FocusController.PROP_FOCUS_MODE) == FocusMode.NORMAL_AF 
						&& this.getCameraActivity().get(CameraActivity.PROP_IS_TOUCHING_ON_SCREEN)
						&& m_LastAFTriggeredTime > 0
						&& !m_IsPointerUppedWhenFocusScanning)
				{
					long delay = (DURATION_AF_LOCK_THREAHOLD - (SystemClock.elapsedRealtime() - m_LastAFTriggeredTime));
					if(delay > 0)
					{
						Log.v(TAG, "onFocusStateChanged() - Start AE/AF lock timer : ", delay, "ms");
						this.getHandler().sendEmptyMessageDelayed(MSG_LOCK_AE_AF, delay);
					}
					else
					{
						Log.v(TAG, "onFocusStateChanged() - Lock AE/AF immediately");
						this.lockFocusAndExposure();
					}
				}
				break;
		}
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// add event handlers
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.addHandler(CameraActivity.EVENT_TOUCH, new EventHandler<MotionEventArgs>()
		{
			@Override
			public void onEventReceived(EventSource source, EventKey<MotionEventArgs> key, MotionEventArgs e)
			{
				onTouch(e);
			}
		});
		
		// add property changed call-backs
		cameraActivity.addCallback(CameraActivity.PROP_IS_TOUCHING_ON_SCREEN, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				if(!e.getNewValue())
					m_IsPointerUppedWhenFocusScanning = true;
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_SCREEN_SIZE, new PropertyChangedCallback<ScreenSize>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<ScreenSize> key, PropertyChangeEventArgs<ScreenSize> e)
			{
				updateDistanceThresholds(e.getNewValue());
			}
		});
		
		// setup touch AF threshold
		this.updateDistanceThresholds(this.getScreenSize());
	}
	
	
	// Handle touch event.
	private void onTouch(MotionEventArgs e)
	{
		// check event
		if(e.isHandled() || e.getPointerCount() > 1)
		{
			this.getHandler().removeMessages(MSG_START_AF);
			return;
		}
		
		// handle event
		switch(e.getAction())
		{
			case MotionEvent.ACTION_DOWN:
			{
				PointF focusCenter = new PointF();
				if(!this.getCameraActivity().getViewfinder().pointToPreview(e.getX(), e.getY(), focusCenter, 0))
					return;
				m_TouchDownPosition.x = e.getX();
				m_TouchDownPosition.y = e.getY();
				this.getHandler().sendEmptyMessageDelayed(MSG_START_AF, DURATION_START_AF_THREAHOLD);
				break;
			}
			
			case MotionEvent.ACTION_MOVE:
			{
				if(m_TouchDownPosition.x < 0 || m_TouchDownPosition.y < 0)
					break;
				float diffX = Math.abs(e.getX() - m_TouchDownPosition.x);
				float diffY = Math.abs(e.getY() - m_TouchDownPosition.y);
				if((diffX * diffX + diffY * diffY) > (m_TouchAfDistanceThreshold * m_TouchAfDistanceThreshold))
					m_TouchDownPosition.set(-1, -1);
				break;
			}
				
			case MotionEvent.ACTION_CANCEL:
				this.getHandler().removeMessages(MSG_START_AF);
				this.getHandler().removeMessages(MSG_LOCK_AE_AF);
				m_TouchDownPosition.set(-1, -1);
				break;
				
			case MotionEvent.ACTION_UP:
				this.getHandler().removeMessages(MSG_LOCK_AE_AF);
				if(this.getHandler().hasMessages(MSG_START_AF))
				{
					this.getHandler().removeMessages(MSG_START_AF);
					this.startAutoFocus();
				}
				m_TouchDownPosition.set(-1, -1);
				break;
		}
	}
	
	
	// Start auto focus.
	private void startAutoFocus()
	{
		// check state
		if(!this.bindToFocusController())
			return;
		if(!this.canTouchFocus())
			return;
		if(m_TouchDownPosition.x < 0 || m_TouchDownPosition.y < 0)
			return;
		if((SystemClock.elapsedRealtime() - m_LastAFTriggeredTime) < DURATION_MIN_TOUCH_AF_INTERVAL)
			return;
		
		// calculate focus position
		PointF focusCenter = new PointF();
		if(!this.getCameraActivity().getViewfinder().pointToPreview(m_TouchDownPosition.x, m_TouchDownPosition.y, focusCenter, 0))
			return;
		float left = (focusCenter.x - (AF_REGION_WIDTH / 2));
		float top = (focusCenter.y - (AF_REGION_HEIGHT / 2));
		float right = (left + AF_REGION_WIDTH);
		float bottom = (top + AF_REGION_HEIGHT);
		if(left < 0)
			left = 0;
		if(top < 0)
			top = 0;
		if(right > 1)
			right = 1;
		if(bottom > 1)
			bottom = 1;
		MeteringRect focusRect = new MeteringRect(left, top, right, bottom, 1);
		
		// cancel previous AF
		m_TouchAfHandle = Handle.close(m_TouchAfHandle);
		
		// unlock AE and AF
		this.unlockFocusAndExposure();
		
		// start AF
		List<MeteringRect> regions = Arrays.asList(focusRect);
		m_TouchAfHandle = m_FocusController.startAutoFocus(regions, FocusController.FLAG_SINGLE_AF);
		if(!Handle.isValid(m_TouchAfHandle))
		{
			Log.e(TAG, "startAutoFocus() - Fail to start touch AF");
			return;
		}
		
		// save current time
		m_LastAFTriggeredTime = SystemClock.elapsedRealtime();
		
		// change AE region
		if(this.bindToExposureController())
		{
			m_ExposureController.set(ExposureController.PROP_EXPOSURE_COMPENSATION, 0f);
			m_ExposureController.set(ExposureController.PROP_AE_REGIONS, regions);
		}
		
		// raise event
		this.raise(EVENT_TOUCH_AF, EventArgs.EMPTY);
		this.raise(EVENT_TOUCH_AE, EventArgs.EMPTY);
	}
	
	
	// Unlock AE and AF.
	private void unlockFocusAndExposure()
	{
		m_ExposureLockHandle = Handle.close(m_ExposureLockHandle);
		m_FocusLockHandle = Handle.close(m_FocusLockHandle);
	}
	
	
	// Update touch AF threshold.
	private void updateDistanceThresholds(ScreenSize screenSize)
	{
		int length = Math.min(screenSize.getWidth(), screenSize.getHeight());
		m_TouchAfDistanceThreshold = (length * TOUCH_AF_DISTANCE_THRESHOLD);
	}
}
