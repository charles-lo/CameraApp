package com.charles.camera.media;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for {@link ResolutionManager}.
 */
public final class ResolutionManagerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new ResolutionManagerBuilder instance.
	 */
	public ResolutionManagerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, ResolutionManagerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new ResolutionManagerImpl(cameraActivity);
	}
}
