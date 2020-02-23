package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

public final class CaptureBarBuilder extends UIComponentBuilder
{
	public CaptureBarBuilder()
	{
		super(CaptureBar.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new CaptureBar(cameraActivity);
	}
}
