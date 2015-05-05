package com.oneplus.camera.io;

import com.oneplus.base.EventKey;
import com.oneplus.base.Handle;
import com.oneplus.base.component.Component;
import com.oneplus.camera.media.MediaEventArgs;

/**
 * File manager interface.
 */
public interface FileManager extends Component
{
	/**
	 * Event raised when media file saving completed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_FILE_SAVED = new EventKey<>("MediaFileSaved", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised when media saving cancelled.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_SAVE_CANCELLED = new EventKey<>("MediaSaveCancelled", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised when media saving process failed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_SAVE_FAILED = new EventKey<>("MediaSaveFailed", MediaEventArgs.class, FileManager.class);
	/**
	 * Event raised after media saving process completed.
	 */
	EventKey<MediaEventArgs> EVENT_MEDIA_SAVED = new EventKey<>("MediaSaved", MediaEventArgs.class, FileManager.class);
	
	
	/**
	 * Start media saving asynchronously.
	 * @param task Media save task.
	 * @param flags Flags, reserved.
	 * @return Handle to media saving.
	 */
	Handle saveMedia(MediaSaveTask task, int flags);
}
