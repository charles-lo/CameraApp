package com.charles.camera;

/**
 * Photo capture state.
 */
public enum PhotoCaptureState
{
	/**
	 * Preparing.
	 */
	PREPARING,
	/**
	 * Ready to capture.
	 */
	READY,
	/**
	 * Starting.
	 */
	STARTING,
	/**
	 * Capturing.
	 */
	CAPTURING,
	/**
	 * Stopping or processing.
	 */
	STOPPING,
	/**
	 * Reviewing.
	 */
	REVIEWING,
}
