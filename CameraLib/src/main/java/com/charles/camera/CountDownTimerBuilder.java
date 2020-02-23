package com.charles.camera;

import com.charles.base.component.ComponentCreationPriority;

public class CountDownTimerBuilder extends UIComponentBuilder
{
	public CountDownTimerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, CountDownTimerImpl.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CountDownTimerImpl(cameraActivity);
	}
}
