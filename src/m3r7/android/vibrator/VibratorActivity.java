/*
 * Copyright (C) 2011 Mert Dönmez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package m3r7.android.vibrator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import m3r7.android.vibrator.command.CommandMaker;
import m3r7.android.vibrator.persistance.PersisterMaker;
import m3r7.android.vibrator.util.PatternMap;
import m3r7.android.vibrator.util.VibratorConstants.MENU_ID;
import m3r7.android.vibrator.util.VibratorConstants.PREFERENCES;
import m3r7.android.vibrator.util.VibratorConstants.REQUEST_CODE;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.speech.RecognizerIntent;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class VibratorActivity extends Activity {

	private Vibrator mVibrator;
	private WakeLock mWakeLock;
	private IPersister mPatternPersister;
	private Animation mAnimVibration;
	private RecognitionLifetimeCountdown mRecogTimer;

	private Spinner mPatternSpinner;
	private Button mButtonFaster;
	private Button mButtonSlower;
	private ToggleButton mButtonOnOff;
	private ImageView mImageViewIcon;
	private ToggleButton mButtonVoiceCommands;
	private Button mButtonManage;
	private Button mButtonListCommands;

	private String selectedPattern;
	private PatternMap patterns;
	private int modifier;
	private boolean isSpeechRecognitionAvailable;
	private boolean voiceDialogUp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vibrator);
		mPatternSpinner = (Spinner) findViewById(R.id.spinnerPatterns);
		mButtonFaster = (Button) findViewById(R.id.buttonFaster);
		mButtonSlower = (Button) findViewById(R.id.buttonSlower);
		mButtonFaster.setEnabled(false);
		mButtonSlower.setEnabled(false);
		mButtonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
		mImageViewIcon = (ImageView) findViewById(R.id.imageviewIcon);
		mButtonVoiceCommands = (ToggleButton) findViewById(R.id.buttonToggleVoiceCommands);
		mButtonManage = (Button) findViewById(R.id.buttonManagePatterns);
		mButtonListCommands = (Button) findViewById(R.id.buttonCommandsList);
		mPatternPersister = PersisterMaker.make(this);
		patterns = new VibratorMainPatternMap(mPatternPersister
				.getPatternsMap());
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mAnimVibration = AnimationUtils.loadAnimation(this, R.anim.vibration);
		setVoiceCommandButtonListener();
		setSpinnerListener();
		setToggleButtonListener();
		setManageButtonListener();
		setListCommandsButtonListener();
		setFastButtonListener();
		setSlowButtonListener();

		isSpeechRecognitionAvailable = isSpeechRecognitionAvailable();

		// read preferences
		SharedPreferences settings = getSharedPreferences(
				PREFERENCES.FILE_NAME, 0);
		final String lastUsed = settings.getString(PREFERENCES.KEY_LAST_USED,
				null);
		if (lastUsed != null && patterns.containsKey(lastUsed)) {
			mPatternSpinner.setSelection(getPosition(lastUsed));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		acquireWakeLock();
		if (!isSpeechRecognitionAvailable) {
			mButtonVoiceCommands.setEnabled(false);
			mButtonVoiceCommands
					.setText(R.string.speech_recognition_unavailable);
		}
	}

	@Override
	protected void onPause() {
		if (!voiceDialogUp) {
			mButtonOnOff.setChecked(false);
			mButtonVoiceCommands.setChecked(false);
			resetToggleRelated();
			cancelTimer();
		}
		releaseWakeLock();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		cancelTimer();
		releaseVibrator();
		if (mPatternPersister != null) {
			mPatternPersister.closeDB();
			mPatternPersister = null;
		}
		// save preferences
		final SharedPreferences.Editor editor = getSharedPreferences(
				PREFERENCES.FILE_NAME, 0).edit();
		editor.putString(PREFERENCES.KEY_LAST_USED, selectedPattern).commit();
		releaseWakeLock();
		super.onDestroy();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (mButtonVoiceCommands.isChecked()) {
			if (!voiceDialogUp) {
				cancelTimer();
				mRecogTimer = new RecognitionLifetimeCountdown();
				launchSpeechRecognitionActivity();
				mRecogTimer.start();
			}
			return true;
		} else {
			return super.dispatchTouchEvent(ev);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ID.DEFINE_PATTERN, 0, R.string.manage_pattern)
				.setShortcut('0', 'd');
		menu.add(0, MENU_ID.SEE_COMMANDS, 0, R.string.list_commands)
				.setShortcut('1', 's');
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID.DEFINE_PATTERN:
			launchPatternManagerActivity();
			return true;
		case MENU_ID.SEE_COMMANDS:
			launchCommandListAcivity();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void acquireWakeLock() {
		mWakeLock = ((PowerManager) getSystemService(POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass()
						.getName());
		mWakeLock.acquire();
	}

	private void releaseWakeLock() {
		if (mWakeLock != null) {
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
			}
			mWakeLock = null;
		}
	}

	private void releaseVibrator() {
		if (mVibrator != null) {
			mVibrator.cancel();
			mVibrator = null;
		}
	}

	private void cancelTimer() {
		if (mRecogTimer != null) {
			mRecogTimer.cancel();
			mRecogTimer = null;
		}
	}

	private void setSpinnerAdapter(Set<String> names) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for (String patternName : names) {
			adapter.add(patternName);
		}
		adapter.sort(new Comparator<String>() {

			@Override
			public int compare(String object1, String object2) {
				return object1.compareTo(object2);
			}
		});
		mPatternSpinner.setAdapter(adapter);
	}

	private void setSpinnerListener() {
		mPatternSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						final String previousSelection = selectedPattern;
						selectedPattern = (String) parent
								.getItemAtPosition(position);
						if (!selectedPattern.equals(previousSelection)) {
							if (mButtonOnOff.isChecked()) {
								resetToggleRelated();
								setScalerButtonsEnabled();
								vibrate(0L);
							}
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
	}

	private void setToggleButtonListener() {
		mButtonOnOff.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mButtonOnOff.isChecked()) {
					setScalerButtonsEnabled();
					vibrate(0L);
				} else {
					resetToggleRelated();
				}
			}
		});
	}

	private void setManageButtonListener() {
		mButtonManage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				launchPatternManagerActivity();
			}
		});
	}

	private void setListCommandsButtonListener() {
		mButtonListCommands.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				launchCommandListAcivity();
			}
		});
	}

	private void setSlowButtonListener() {
		mButtonSlower.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mVibrator.cancel();
				modifier++;
				setScalerButtonsEnabled();
				vibrate(getResources().getInteger(
						R.integer.wait_time_after_scaling));
			}
		});
	}

	private void setFastButtonListener() {
		mButtonFaster.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mVibrator.cancel();
				modifier--;
				setScalerButtonsEnabled();
				vibrate(getResources().getInteger(
						R.integer.wait_time_after_scaling));
			}
		});
	}

	private void setVoiceCommandButtonListener() {
		mButtonVoiceCommands.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mButtonVoiceCommands.isChecked()) {
					final Animation fadeOut = AnimationUtils.loadAnimation(
							VibratorActivity.this, R.anim.fade_out);
					animateManualControls(fadeOut);
				}
			}
		});
	}

	private void animateManualControls(Animation animation) {
		mPatternSpinner.startAnimation(animation);
		mButtonOnOff.startAnimation(animation);
		mButtonSlower.startAnimation(animation);
		mButtonFaster.startAnimation(animation);
	}

	private void resetToggleRelated() {
		mVibrator.cancel();
		mImageViewIcon.clearAnimation();
		mButtonSlower.setEnabled(false);
		mButtonFaster.setEnabled(false);
		modifier = 0;
	}

	private void setVibratorEnabled(boolean enabled) {
		mPatternSpinner.setEnabled(enabled);
		mButtonOnOff.setEnabled(enabled);
		if (isSpeechRecognitionAvailable) {
			mButtonVoiceCommands.setEnabled(enabled);
		}
	}

	private void launchPatternManagerActivity() {
		final Intent intent = new Intent(this, PatternManagerActivity.class);
		startActivityForResult(intent, REQUEST_CODE.MANAGE);
	}

	private void launchCommandListAcivity() {
		final Intent intent = new Intent(this, CommandsListActivity.class);
		startActivity(intent);
	}

	private void launchSpeechRecognitionActivity() {
		final Intent intent = new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources()
				.getString(R.string.speech_recog_prompt));
		startActivityForResult(intent, REQUEST_CODE.VOICE_RECOGNITION);
		voiceDialogUp = true;
	}

	private void closeSpeechRecognitionActivity() {
		finishActivity(REQUEST_CODE.VOICE_RECOGNITION);
		voiceDialogUp = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE.MANAGE) {
			patterns = new VibratorMainPatternMap(mPatternPersister
					.getPatternsMap());
		} else if (requestCode == REQUEST_CODE.VOICE_RECOGNITION) {
			mButtonVoiceCommands.setChecked(true);
			if (resultCode == RESULT_OK) {
				voiceDialogUp = false;
				final ArrayList<String> matches = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				final String[] commands = getResources().getStringArray(
						R.array.command_names);
				CommandMaker.make(matches, commands, patterns.keySet(), this)
						.execute();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void vibrate(long waitTime) {
		if (selectedPattern != null) {
			final long[] patternArr = patterns.get(selectedPattern).scale(
					modifier).getVibratable(waitTime);
			mVibrator.vibrate(patternArr, 0);
			mImageViewIcon.startAnimation(mAnimVibration);
		}
	}

	private void setScalerButtonsEnabled() {
		if (selectedPattern != null) {
			final Pair<Boolean, Boolean> isScalable = patterns.get(
					selectedPattern).isScalable(modifier);
			mButtonFaster.setEnabled(isScalable.first);
			mButtonSlower.setEnabled(isScalable.second);
		} else {
			mButtonFaster.setEnabled(false);
			mButtonSlower.setEnabled(false);
		}
	}

	private boolean isSpeechRecognitionAvailable() {
		return !getPackageManager().queryIntentActivities(
				new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
				.isEmpty();
	}

	public void actionStartVibrator() {
		if (mButtonOnOff.isEnabled() && !mButtonOnOff.isChecked()) {
			mButtonOnOff.performClick();
		}
	}

	public void actionStopVibrator() {
		if (mButtonOnOff.isEnabled() && mButtonOnOff.isChecked()) {
			mButtonOnOff.performClick();
		}
	}

	public void actionIncreaseFreq() {
		if (mButtonFaster.isEnabled()) {
			mButtonFaster.performClick();
		}
	}

	public void actionDecreaseFreq() {
		if (mButtonSlower.isEnabled()) {
			mButtonSlower.performClick();
		}
	}

	public void actionSelectPattern(String patternName) {
		if (mPatternSpinner.isEnabled()) {
			mPatternSpinner.setSelection(getPosition(patternName));
		}
	}

	public void actionDisableVoice() {
		cancelTimer();
		closeSpeechRecognitionActivity();
		mButtonVoiceCommands.setChecked(false);
		final Animation fadeIn = AnimationUtils.loadAnimation(
				VibratorActivity.this, R.anim.fade_in);
		animateManualControls(fadeIn);
	}

	private int getPosition(String patternName) {
		return ((ArrayAdapter<String>) mPatternSpinner.getAdapter())
				.getPosition(patternName);
	}

	private class VibratorMainPatternMap extends PatternMap {

		protected VibratorMainPatternMap(Map<String, IVibrationPattern> patterns) {
			super(patterns);
		}

		@Override
		protected void onCreated(Map<String, IVibrationPattern> patterns) {
			setVibratorEnabled(!patterns.isEmpty());
			setSpinnerAdapter(patterns.keySet());
		}

		@Override
		protected void onModified() {
			setVibratorEnabled(!patterns.isEmpty());
			setSpinnerAdapter(patterns.keySet());
		}
	}

	private class RecognitionLifetimeCountdown extends CountDownTimer {

		private static final long MILLIS_IN_FUTURE = 6000L;
		private static final long COUNTDOWN_INTERVAL = 6000L;

		public RecognitionLifetimeCountdown() {
			super(MILLIS_IN_FUTURE, COUNTDOWN_INTERVAL);
		}

		@Override
		public void onFinish() {
			closeSpeechRecognitionActivity();
		}

		@Override
		public void onTick(long millisUntilFinished) {
		}

	}

}