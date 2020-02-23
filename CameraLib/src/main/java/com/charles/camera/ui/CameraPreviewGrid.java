package com.charles.camera.ui;

import com.charles.base.PropertyKey;
import com.charles.base.component.Component;

/**
 * Interface for grid on camera preview.
 */
public interface CameraPreviewGrid extends Component
{
	/**
	 * Settings key for grid visibility.
	 */
	String SETTINGS_KEY_IS_GRID_VISIBLE = "Grid.IsVisible";
	
	
	/**
	 * Read-only property to check grid visibility.
	 */
	PropertyKey<Boolean> PROP_IS_VISIBLE = new PropertyKey<>("IsVisible", Boolean.class, CameraPreviewGrid.class, false);
}
