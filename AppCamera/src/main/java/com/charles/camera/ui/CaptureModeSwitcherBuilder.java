package com.charles.camera.ui;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

public final class CaptureModeSwitcherBuilder extends UIComponentBuilder
{
	public CaptureModeSwitcherBuilder()
	{
		super(ComponentCreationPriority.NORMAL, CaptureModeSwitcher.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CaptureModeSwitcher(cameraActivity);
	}
}
