package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Builder for pinch zooming UI component. 
 */
public final class PinchZoomingUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new PinchZoomingUIBuilder instance.
	 */
	public PinchZoomingUIBuilder()
	{
		super(PinchZoomingUI.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new PinchZoomingUI(cameraActivity);
	}
}
