package com.charles.camera.ui;

import com.charles.base.EventArgs;
import com.charles.base.EventKey;
import com.charles.base.component.Component;

/**
 * Touch auto exposure UI interface.
 */
public interface TouchAutoExposureUI extends Component
{
	/**
	 * Event raised when triggering touch AE.
	 */
	EventKey<EventArgs> EVENT_TOUCH_AE = new EventKey<EventArgs>("TouchAE", EventArgs.class, TouchAutoFocusUI.class);
}
