package com.charles.camera.timelapse;

import com.charles.camera.CameraActivity;
import com.charles.camera.capturemode.CaptureMode;
import com.charles.camera.capturemode.CaptureModeBuilder;

/**
 * Builder for time-lapse video capture mode.
 */
public class TimelapseCaptureModeBuilder implements CaptureModeBuilder
{
	// Create capture mode.
	@Override
	public CaptureMode createCaptureMode(CameraActivity cameraActivity)
	{
		return new TimelapseCaptureMode(cameraActivity);
	}
}
