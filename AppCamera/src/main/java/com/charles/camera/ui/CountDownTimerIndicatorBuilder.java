package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for count-down timer indicator.
 */
public final class CountDownTimerIndicatorBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new CountDownTimerIndicatorBuilder instance.
	 */
	public CountDownTimerIndicatorBuilder()
	{
		super(CountDownTimerIndicator.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CountDownTimerIndicator(cameraActivity);
	}
}
