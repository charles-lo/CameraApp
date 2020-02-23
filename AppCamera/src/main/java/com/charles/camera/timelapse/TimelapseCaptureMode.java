package com.charles.camera.timelapse;

import android.graphics.drawable.Drawable;

import com.charles.camera.CameraActivity;
import com.charles.camera.R;
import com.charles.camera.capturemode.ComponentBasedCaptureMode;

final class TimelapseCaptureMode extends ComponentBasedCaptureMode<TimelapseUI>
{
	// Constructor.
	TimelapseCaptureMode(CameraActivity cameraActivity)
	{
		super(cameraActivity, "Time-lapse", "timelapse", TimelapseUI.class);
	}
	
	
	// Get display name
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.capture_mode_timelapse);
	}

	
	// Get related image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case CAPTURE_MODES_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.capture_mode_panel_icon_timelapse);
			default:
				return null;
		}
	}
}
