package com.oneplus.camera.timelapse;

import com.oneplus.base.Handle;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.ModeUI;
import com.oneplus.camera.R;
import com.oneplus.camera.VideoCaptureState;
import com.oneplus.camera.media.MediaType;
import com.oneplus.camera.ui.CaptureButtons;

final class TimelapseUI extends ModeUI<TimelapseController>
{
	// Private fields.
	private Handle m_CaptureButtonBgHandle;
	private Handle m_CaptureButtonIconHandle;
	private CaptureButtons m_CaptureButtons;
	private Handle m_RecordingTimeRatioHandle;
	
	
	// Constructor.
	TimelapseUI(CameraActivity cameraActivity)
	{
		super("Time-lapse UI", cameraActivity, TimelapseController.class);
	}
	
	
	// Called when CaptureButtons interface found.
	private void onCaptureButtonsFound(CaptureButtons component)
	{
		// save instance
		m_CaptureButtons = component;
		
		// set capture button images
		if(this.isEntered())
			this.setCaptureButtonImages();
	}
	
	
	// Enter mode.
	@Override
	protected boolean onEnter(int flags)
	{
		// change to video mode
		if(!this.getCameraActivity().setMediaType(MediaType.VIDEO))
			return false;
		
		// call super
		if(!super.onEnter(flags))
			return false;
		
		// change recording time ratio
		m_RecordingTimeRatioHandle = this.getCameraActivity().setRecordingTimeRatio(1 / TimelapseController.SPEED_RATIO);
		
		// change capture button images
		this.setCaptureButtonImages();
		
		// complete
		return true;
	}
	
	
	// Exit mode.
	@Override
	protected void onExit(int flags)
	{
		// restore recording time ratio
		m_RecordingTimeRatioHandle = Handle.close(m_RecordingTimeRatioHandle);
		
		// restore capture button images
		m_CaptureButtonBgHandle = Handle.close(m_CaptureButtonBgHandle);
		m_CaptureButtonIconHandle = Handle.close(m_CaptureButtonIconHandle);
		
		// call super
		super.onExit(flags);
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		this.findComponent(CaptureButtons.class, new ComponentSearchCallback<CaptureButtons>()
		{
			@Override
			public void onComponentFound(CaptureButtons component)
			{
				onCaptureButtonsFound(component);
			}
		});
		
		// add property changed call-backs
		CameraActivity cameraActivity = this.getCameraActivity();
		cameraActivity.addCallback(CameraActivity.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				onVideoCaptureStateChanged(e);
			}
		});
	}
	
	
	// Called when video capture state changed.
	@SuppressWarnings("incomplete-switch")
	private void onVideoCaptureStateChanged(PropertyChangeEventArgs<VideoCaptureState> e)
	{
		if(!this.isEntered())
			return;
		switch(e.getNewValue())
		{
			case CAPTURING:
				if(m_CaptureButtons != null)
				{
					Handle oldHandle = m_CaptureButtonIconHandle;
					m_CaptureButtonIconHandle = m_CaptureButtons.setPrimaryButtonIcon(this.getCameraActivity().getDrawable(R.drawable.capture_button_timelapse_icon_recording), 0);
					Handle.close(oldHandle);
				}
				break;
			case STOPPING:
				if(m_CaptureButtons != null)
				{
					Handle oldHandle = m_CaptureButtonIconHandle;
					m_CaptureButtonIconHandle = m_CaptureButtons.setPrimaryButtonIcon(this.getCameraActivity().getDrawable(R.drawable.capture_button_timelapse_icon), 0);
					Handle.close(oldHandle);
				}
				break;
		}
	}
	
	
	// Set capture button images.
	private void setCaptureButtonImages()
	{
		if(m_CaptureButtons == null)
			return;
		CameraActivity camaraActivity = this.getCameraActivity();
		if(!Handle.isValid(m_CaptureButtonBgHandle))
			m_CaptureButtonBgHandle = m_CaptureButtons.setPrimaryButtonBackground(camaraActivity.getDrawable(R.drawable.capture_button_timelapse_border), 0);
		if(!Handle.isValid(m_CaptureButtonIconHandle))
			m_CaptureButtonIconHandle = m_CaptureButtons.setPrimaryButtonIcon(camaraActivity.getDrawable(R.drawable.capture_button_timelapse_icon), 0);
	}
}
