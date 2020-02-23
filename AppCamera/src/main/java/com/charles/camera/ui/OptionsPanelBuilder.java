package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

public final class OptionsPanelBuilder extends UIComponentBuilder
{
	public OptionsPanelBuilder()
	{
		super(OptionsPanelImpl.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new OptionsPanelImpl(cameraActivity);
	}
}
