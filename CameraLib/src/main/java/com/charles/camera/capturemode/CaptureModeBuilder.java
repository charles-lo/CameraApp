package com.charles.camera.capturemode;

import com.charles.camera.CameraActivity;

/**
 * Capture mode builder interface.
 */
public interface CaptureModeBuilder
{
	/**
	 * Create capture mode.
	 * @param cameraActivity Camera activity.
	 * @return Created capture mode.
	 */
	CaptureMode createCaptureMode(CameraActivity cameraActivity);
}
