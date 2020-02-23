package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Builder for recording timer UI.
 */
public final class RecordingTimerUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new RecordingTimerUIBuilder instance.
	 */
	public RecordingTimerUIBuilder()
	{
		super(RecordingTimerUI.class);
	}
	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new RecordingTimerUI(cameraActivity);
	}
}
