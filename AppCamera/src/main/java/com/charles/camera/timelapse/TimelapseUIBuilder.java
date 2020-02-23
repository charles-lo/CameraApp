package com.charles.camera.timelapse;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Builder for time-lapse UI component.
 */
public final class TimelapseUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new TimelapseUIBuilder instance.
	 */
	public TimelapseUIBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, TimelapseUI.class);
	}

	
	// Create components.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new TimelapseUI(cameraActivity);
	}
}
