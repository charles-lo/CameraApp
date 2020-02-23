package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for {@link CameraPreviewGrid}.
 */
public final class CameraPreviewGridBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new CameraPreviewGridBuilder instance.
	 */
	public CameraPreviewGridBuilder()
	{
		super(CameraPreviewGridImpl.class);
	}
	
	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CameraPreviewGridImpl(cameraActivity);
	}
}
