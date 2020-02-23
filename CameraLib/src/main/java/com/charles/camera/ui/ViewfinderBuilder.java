package com.charles.camera.ui;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Builder for {@link Viewfinder}.
 */
public final class ViewfinderBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new ViewfinderBuilder instance.
	 */
	public ViewfinderBuilder()
	{
		super(ComponentCreationPriority.HIGH, ViewfinderImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new ViewfinderImpl(cameraActivity);
	}
}
