package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for zoom bar.
 */
public final class ZoomBarBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new ZoomBarBuilder instance.
	 */
	public ZoomBarBuilder()
	{
		super(ZoomBarImpl.class);
	}
	
	
	// Create components.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new ZoomBarImpl(cameraActivity);
	}
}
