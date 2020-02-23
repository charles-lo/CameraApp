package com.charles.camera;

import java.util.Collections;
import java.util.List;

import android.util.Range;

import com.charles.base.Handle;
import com.charles.base.PropertyKey;
import com.charles.base.component.Component;

/**
 * Exposure controller interface.
 */
public interface ExposureController extends Component
{
	/**
	 * Property to get or set AE regions.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	PropertyKey<List<Camera.MeteringRect>> PROP_AE_REGIONS = new PropertyKey<List<Camera.MeteringRect>>("AERegions", (Class)List.class, ExposureController.class, PropertyKey.FLAG_NOT_NULL, Collections.EMPTY_LIST);
	/**
	 * Property to get or set exposure compensation in EV.
	 */
	PropertyKey<Float> PROP_EXPOSURE_COMPENSATION = new PropertyKey<>("ExposureCompensation", Float.class, ExposureController.class, PropertyKey.FLAG_NOT_NULL, 0f);
	/**
	 * Read-only property to get exposure compensation range in EV.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	PropertyKey<Range<Float>> PROP_EXPOSURE_COMPENSATION_RANGE = new PropertyKey<Range<Float>>("ExposureCompensationRange", (Class)Range.class, ExposureController.class, new Range<Float>(0f, 0f));
	/**
	 * Read-only property to get minimum exposure compensation step in EV.
	 */
	PropertyKey<Float> PROP_EXPOSURE_COMPENSATION_STEP = new PropertyKey<>("ExposureCompensationStep", Float.class, ExposureController.class, 0f);
	/**
	 * Read-only property to check whether AE is locked or not.
	 */
	PropertyKey<Boolean> PROP_IS_AE_LOCKED = new PropertyKey<>("IsAELocked", Boolean.class, ExposureController.class, false);
	
	
	/**
	 * Lock auto exposure.
	 * @param flags Flags, reserved.
	 * @return Handle to AE lock.
	 */
	Handle lockAutoExposure(int flags);
}
