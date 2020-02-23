package com.charles.camera;

import com.charles.base.component.ComponentCreationPriority;

/**
 * Component builder for {@link FlashController}.
 */
public final class FlashControllerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new FlashControllerBuilder instance.
	 */
	public FlashControllerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, FlashControllerImpl.class);
	}
	
	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new FlashControllerImpl(cameraActivity);
	}

}
