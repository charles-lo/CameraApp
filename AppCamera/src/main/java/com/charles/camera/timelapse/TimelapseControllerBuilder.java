package com.charles.camera.timelapse;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraThread;
import com.charles.camera.CameraThreadComponent;
import com.charles.camera.CameraThreadComponentBuilder;

/**
 * Builder for time-lapse controller component.
 */
public final class TimelapseControllerBuilder extends CameraThreadComponentBuilder
{
	/**
	 * Initialize new TimelapseControllerBuilder instance.
	 */
	public TimelapseControllerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, TimelapseController.class);
	}
	
	
	// Create components.
	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new TimelapseController(cameraThread);
	}
}
