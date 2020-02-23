package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for focus/exposure indicator.
 */
public final class FocusExposureIndicatorBuilder extends UIComponentBuilder
{
	// Constructor.
	public FocusExposureIndicatorBuilder()
	{
		super(FocusExposureIndicator.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new FocusExposureIndicator(cameraActivity);
	}
}
