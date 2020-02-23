package com.charles.camera.location;

import java.util.List;

import com.charles.base.PropertyKey;
import com.charles.base.component.ComponentOwner;
import com.charles.camera.CameraThread;
import com.charles.camera.ProxyComponent;

final class LocationManagerImpl extends ProxyComponent<LocationManager> implements LocationManager
{
	// Constructor.
	LocationManagerImpl(CameraThread cameraThread)
	{
		super("Location Manager (Camera Thread)", cameraThread, (ComponentOwner)cameraThread.getContext(), LocationManager.class);
	}
	
	
	// Called when binding to target properties.
	@Override
	protected void onBindingToTargetProperties(List<PropertyKey<?>> keys)
	{
		super.onBindingToTargetProperties(keys);
		keys.add(PROP_IS_LOCATION_LISTENER_STARTED);
		keys.add(PROP_LOCATION);
	}
}
