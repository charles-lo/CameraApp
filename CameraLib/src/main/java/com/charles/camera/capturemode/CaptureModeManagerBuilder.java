package com.charles.camera.capturemode;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for {@link CaptureModeManager}.
 */
public final class CaptureModeManagerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new CaptureModeManagerBuilder instance.
	 */
	public CaptureModeManagerBuilder()
	{
		super(ComponentCreationPriority.LAUNCH, CaptureModeManagerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CaptureModeManagerImpl(cameraActivity);
	}
}
