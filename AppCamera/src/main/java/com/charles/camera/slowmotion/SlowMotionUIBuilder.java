package com.charles.camera.slowmotion;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Builder for slow-motion UI component.
 */
public final class SlowMotionUIBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new SlowMotionUIBuilder instance.
	 */
	public SlowMotionUIBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, SlowMotionUI.class);
	}

	
	// Create components.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new SlowMotionUI(cameraActivity);
	}
}
