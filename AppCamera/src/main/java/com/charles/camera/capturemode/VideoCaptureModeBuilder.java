package com.charles.camera.capturemode;

import com.charles.camera.CameraActivity;

/**
 * Builder for video capture mode.
 */
public class VideoCaptureModeBuilder implements CaptureModeBuilder
{
	// Create capture mode.
	@Override
	public CaptureMode createCaptureMode(CameraActivity cameraActivity)
	{
		return new VideoCaptureMode(cameraActivity);
	}
}
