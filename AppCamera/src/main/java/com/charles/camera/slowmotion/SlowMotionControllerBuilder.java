package com.charles.camera.slowmotion;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraThread;
import com.charles.camera.CameraThreadComponent;
import com.charles.camera.CameraThreadComponentBuilder;

/**
 * Builder for slow-motion controller component.
 */
public final class SlowMotionControllerBuilder extends CameraThreadComponentBuilder
{
	/**
	 * Initialize new SlowMotionControllerBuilder instance.
	 */
	public SlowMotionControllerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, SlowMotionController.class);
	}
	
	
	// Create components.
	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new SlowMotionController(cameraThread);
	}
}
