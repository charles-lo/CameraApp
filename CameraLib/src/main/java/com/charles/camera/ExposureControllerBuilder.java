package com.charles.camera;

import com.charles.base.component.Component;
import com.charles.base.component.ComponentBuilder;
import com.charles.base.component.ComponentCreationPriority;

final class ExposureControllerBuilder implements ComponentBuilder
{
	// Create component.
	@Override
	public Component create(Object... args)
	{
		if(args != null && args.length > 0)
		{
			if(args[0] instanceof CameraActivity)
				return new UIExposureControllerImpl((CameraActivity)args[0]);
			if(args[0] instanceof CameraThread)
				return new ExposureControllerImpl((CameraThread)args[0]);
		}
		return null;
	}

	
	// Get priority.
	@Override
	public ComponentCreationPriority getPriority()
	{
		return ComponentCreationPriority.NORMAL;
	}

	
	// Check whether given type is supported or not.
	@Override
	public boolean isComponentTypeSupported(Class<?> componentType)
	{
		return ExposureController.class.isAssignableFrom(componentType);
	}
}
