package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for {@link TouchAutoExposureUI} and {@link TouchAutoFocusUI}.
 */
public final class TouchFocusExposureUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new TouchFocusExposureUIBuilder instance.
	 */
	public TouchFocusExposureUIBuilder()
	{
		super(TouchFocusExposureUI.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new TouchFocusExposureUI(cameraActivity);
	}
}
