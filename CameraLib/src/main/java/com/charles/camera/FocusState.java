package com.charles.camera;

/**
 * Camera focus state.
 */
public enum FocusState
{
	/**
	 * Initial state.
	 */
	INACTIVE,
	/**
	 * Scanning.
	 */
	SCANNING,
	/**
	 * Locked with focus.
	 */
	FOCUSED,
	/**
	 * Locked without focus.
	 */
	UNFOCUSED,
}
