/**
 * APICloud Modules
 * Copyright (c) 2014-2015 by APICloud, Inc. All Rights Reserved.
 * Licensed under the terms of the The MIT License (MIT).
 * Please see the license.html included with this distribution for details.
 */
package com.jiashizhan.myscanner.utlis;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Vibrator;

import java.io.FileDescriptor;

public class BeepUtil {
	private static final float BEEP_VOLUME = 0.10f;
	private static final long VIBRATE_DURATION = 200L;
	private Context mContext;
	private MediaPlayer mMediaPlayer;
	private String mBeepPath;
	private boolean mIsBeep;

	public BeepUtil(Context context, String beepPath) {
		this.mContext = context;
		this.mBeepPath = beepPath;
	}

	public void initBeep() {
		mIsBeep = true;
		AudioManager audioService = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			mIsBeep = false;
		}
		initBeepSound();
	}

	public void playBeepSoundAndVibrate() {
		if (mIsBeep && mMediaPlayer != null) {
			mMediaPlayer.start();
		}
		Vibrator vibrator = (Vibrator) mContext
				.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATE_DURATION);
	}

	private void initBeepSound() {
		if (!isBlank(mBeepPath)) {
			if (mIsBeep && mMediaPlayer == null) {
				((Activity) mContext)
						.setVolumeControlStream(AudioManager.STREAM_MUSIC);
				mMediaPlayer = new MediaPlayer();
				try {
					setBeepSource();
					mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mMediaPlayer.setOnCompletionListener(mBeepListener);
					mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
					mMediaPlayer.prepare();
				} catch (Exception e) {
					e.printStackTrace();
					mMediaPlayer = null;
				}
			}
		}
	}

	private void setBeepSource() throws Exception {
		int question = mBeepPath.indexOf("?");
		if (question > -1) {
			mBeepPath = mBeepPath.substring(0, question);
		}
		FileDescriptor fd = null;
		long offset = 0;
		long length = 0;
		if (mBeepPath.contains("android_asset")) {
			if (mBeepPath.startsWith("file://")) {
				mBeepPath = mBeepPath.substring(7);
			}
			String relativePath = mBeepPath.substring(15);
			AssetFileDescriptor afd = mContext.getAssets().openFd(relativePath);
			fd = afd.getFileDescriptor();
			offset = afd.getStartOffset();
			length = afd.getLength();
			mMediaPlayer.setDataSource(fd, offset, length);
		} else {
			mMediaPlayer.setDataSource(mBeepPath);
		}
	}

	public boolean isBlank(CharSequence cs) {
		int strLen;
		if ((cs == null) || ((strLen = cs.length()) == 0))
			return true;
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(cs.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private final OnCompletionListener mBeepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};
}
