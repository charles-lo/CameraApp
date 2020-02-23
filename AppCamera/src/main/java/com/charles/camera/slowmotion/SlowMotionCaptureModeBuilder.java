package com.charles.camera.slowmotion;

import com.charles.camera.CameraActivity;
import com.charles.camera.capturemode.CaptureMode;
import com.charles.camera.capturemode.CaptureModeBuilder;

/**
 * Builder for slow-motion video capture mode.
 */
public class SlowMotionCaptureModeBuilder implements CaptureModeBuilder
{
	// Create capture mode.
	@Override
	public CaptureMode createCaptureMode(CameraActivity cameraActivity)
	{
		return new SlowMotionCaptureMode(cameraActivity);
	}
}
