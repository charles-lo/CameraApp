package com.charles.camera.media;

import com.charles.base.component.Component;
import com.charles.base.component.ComponentBuilder;
import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraThread;

/**
 * Component builder for {@link AudioManager}.
 */
public final class AudioManagerBuilder implements ComponentBuilder
{
	// Create component.
	@Override
	public Component create(Object... args)
	{
		if(args != null && args.length > 0)
		{
			if(args[0] instanceof CameraActivity)
				return new AudioManagerImpl((CameraActivity)args[0]);
			if(args[0] instanceof CameraThread)
				return new AudioManagerImpl((CameraThread)args[0]);
		}
		return null;
	}

	
	// Get priority.
	@Override
	public ComponentCreationPriority getPriority()
	{
		return ComponentCreationPriority.ON_DEMAND;
	}

	
	// Check whether given type is supported or not.
	@Override
	public boolean isComponentTypeSupported(Class<?> componentType)
	{
		return AudioManager.class.isAssignableFrom(componentType);
	}
}
