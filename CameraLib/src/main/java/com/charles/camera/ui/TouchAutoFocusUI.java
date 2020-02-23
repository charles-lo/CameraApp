package com.charles.camera.ui;

import com.charles.base.EventArgs;
import com.charles.base.EventKey;
import com.charles.base.component.Component;

/**
 * Touch AF UI interface.
 */
public interface TouchAutoFocusUI extends Component
{
	/**
	 * Event raised when triggering touch AF.
	 */
	EventKey<EventArgs> EVENT_TOUCH_AF = new EventKey<EventArgs>("TouchAF", EventArgs.class, TouchAutoFocusUI.class);
}
