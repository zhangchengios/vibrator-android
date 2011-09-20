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
import java.util.List;
import java.util.Map;
import java.util.Set;

import m3r7.android.vibrator.pattern.PatternMaker;
import m3r7.android.vibrator.persistance.PersisterMaker;
import m3r7.android.vibrator.util.PatternMap;
import m3r7.android.vibrator.util.VibratorConstants;
import m3r7.android.vibrator.util.VibratorUtility;
import m3r7.android.vibrator.util.VibratorConstants.DIALOG_ID;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class PatternManagerActivity extends Activity {

	private IPersister mPatternPersister;
	private CaptureTimer mTimer;
	private Vibrator mVibrator;

	private EditText mTextPatternName;
	private Spinner mDurationSpinner;
	private Button mButtonCapture;
	private EditText mTextTimer;
	private Spinner mPatternSpinner;
	private Button mButtonDelete;

	private String selectedPattern;
	private int selectedDuration;
	private PatternMap patterns;
	private boolean isModified;
	private boolean isCapturing;
	private List<Integer> patternList;
	private long previousEventTime;
	private boolean isCaptureAborted;
	private String captureAbortedPatternName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Following line of code prevents keyboard from opening when window
		 * receives focus
		 */
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.patternmanager);
		mTextPatternName = (EditText) findViewById(R.id.txtPatternName);
		mDurationSpinner = (Spinner) findViewById(R.id.spinnerCaptureDuration);
		mButtonCapture = (Button) findViewById(R.id.buttonCapturePattern);
		mTextTimer = (EditText) findViewById(R.id.txtTimer);
		mPatternSpinner = (Spinner) findViewById(R.id.spinnerPatternsForDeletion);
		mButtonDelete = (Button) findViewById(R.id.buttonDeletePattern);
		mPatternPersister = PersisterMaker.make(this);
		patterns = new PatternMap(mPatternPersister.getPatternsMap()) {
			@Override
			protected void onCreated(Map<String, IVibrationPattern> patterns) {
				isModified = false;
				setDeletionEnabled(!patterns.isEmpty());
				setSpinnerAdapter(patterns.keySet());
			}

			@Override
			protected void onModified() {
				isModified = true;
				setDeletionEnabled(!patterns.isEmpty());
				setSpinnerAdapter(patterns.keySet());
			}
		};
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		setDurationSpinnerAdapter();
		setDurationSpinnerListener();
		setCaptureButtonListener();
		setSpinnerListener();
		setDeleteButtonListener();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (isCaptureAborted) {
			showDialog(DIALOG_ID.CAPTURE_ABORTED);
			isCaptureAborted = false;
			captureAbortedPatternName = null;
		}
	}

	@Override
	protected void onPause() {
		if (isCapturing) {// activity loses focus during pattern capture...
			toggleCapturingState();
			mVibrator.cancel();
			isCaptureAborted = true;
			captureAbortedPatternName = getPatternName();
		}
		if (isModified) {
			mPatternPersister.deletePatterns();
			mPatternPersister.savePatterns(patterns.getWrappedObject());
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		cancelTimer();
		releaseVibrator();
		super.onDestroy();
	}

	private void setCaptureButtonListener() {
		mButtonCapture.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				final int eventType = event.getAction();
				final long eventTime = event.getEventTime();
				switch (eventType) {
				case MotionEvent.ACTION_DOWN:
					if (isCapturing) {
						patternList.add((int) (eventTime - previousEventTime));
					} else {// start capturing...
						final String patternName = getPatternName();
						if (!VibratorUtility.hasText(patternName)) {
							showDialog(DIALOG_ID.ERROR_MISSING_NAME);
							return true;
						}
						if (patternName.length() > getResources().getInteger(
								R.integer.max_pattern_name_length)) {
							showDialog(DIALOG_ID.ERROR_NAME_TOO_LONG);
							return true;
						}
						if (!VibratorUtility.isSpellable(patternName)) {
							showDialog(DIALOG_ID.ERROR_NOT_SPELLABLE);
							return true;
						}
						if (VibratorUtility.isVoiceCommand(
								PatternManagerActivity.this, patternName)) {
							showDialog(DIALOG_ID.ERROR_RESERVED_NAME);
							return true;
						}
						if (patterns.containsKey(patternName)) {
							showDialog(DIALOG_ID.ERROR_DUPLICATE_NAME);
							return true;
						}
						mTextPatternName.setText(patternName);
						toggleCapturingState();
					}
					mVibrator.vibrate(VibratorConstants.INDEFINITELY, 0);
					previousEventTime = eventTime;
					return true;
				case MotionEvent.ACTION_UP:
					if (isCapturing) {
						patternList.add((int) (eventTime - previousEventTime));
						previousEventTime = eventTime;
						mVibrator.cancel();
					}
					return true;
					/*
					 * else {// time was up before this event was fired // we
					 * never enter here because we stop capturing within //
					 * onFinish() of CountdownTimer }
					 */
				default:
					return false;
				}
			}
		});
	}

	private void setDurationSpinnerListener() {
		mDurationSpinner
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						selectedDuration = Integer.valueOf((String) parent
								.getItemAtPosition(position))
								* VibratorConstants.MILLIS_IN_SECOND;
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
	}

	private void setDurationSpinnerAdapter() {
		final ArrayAdapter<CharSequence> adapter = ArrayAdapter
				.createFromResource(this, R.array.capture_times,
						android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDurationSpinner.setAdapter(adapter);
	}

	private void setSpinnerAdapter(Set<String> names) {
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
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
						selectedPattern = (String) parent
								.getItemAtPosition(position);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
	}

	private void setDeleteButtonListener() {
		mButtonDelete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (selectedPattern != null) {
					patterns.remove(selectedPattern);
					showDialog(DIALOG_ID.DELETE_SUCCESS);
				} else {
					showDialog(DIALOG_ID.ERROR_NO_PATTERN_SELECTED);
				}
			}
		});
	}

	private void toggleCapturingState() {
		isCapturing = !isCapturing;
		if (isCapturing) {
			mButtonCapture.setText(R.string.button_capture_capturing);
			patternList = new ArrayList<Integer>();
			cancelTimer();
			mTimer = new CaptureTimer();
			mTimer.start();
		} else {
			mButtonCapture.setText(R.string.button_capture_not_capturing);
			patternList = null;
			cancelTimer();
		}
		setEnabledForCapture(isCapturing);
	}

	private void setDeletionEnabled(boolean enabled) {
		mPatternSpinner.setEnabled(enabled);
		mButtonDelete.setEnabled(enabled);
	}

	private void setEnabledForCapture(boolean isCapturing) {
		setDeletionEnabled(!isCapturing);
		mTextPatternName.setEnabled(!isCapturing);
		mDurationSpinner.setEnabled(!isCapturing);
	}

	private void cancelTimer() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		mTextTimer.setText(R.string.timer_default_text);
	}

	private void releaseVibrator() {
		if (mVibrator != null) {
			mVibrator.cancel();
			mVibrator = null;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ID.CAPTURE_ABORTED:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_info).setMessage(
					String.format(getResources().getString(
							R.string.dialog_msg_capture_abort),
							captureAbortedPatternName)).setCancelable(false)
					.setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		case DIALOG_ID.CAPTURE_SUCCESS:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_info).setTitle(
					R.string.dialog_title_info).setMessage(
					R.string.dialog_msg_capture_success).setCancelable(false)
					.setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		case DIALOG_ID.DELETE_SUCCESS:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_info).setTitle(
					R.string.dialog_title_info).setMessage(
					R.string.dialog_msg_delete_success).setCancelable(false)
					.setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		case DIALOG_ID.ERROR_DUPLICATE_NAME:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_error).setMessage(
					String.format(getResources().getString(
							R.string.dialog_msg_duplicate_name),
							getPatternName())).setCancelable(false)
					.setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		case DIALOG_ID.ERROR_MISSING_NAME:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_error).setMessage(
					R.string.dialog_msg_pattern_name_missing).setCancelable(
					false).setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
						}
					}).create();
		case DIALOG_ID.ERROR_NAME_TOO_LONG:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_error).setMessage(
					String.format(getResources().getString(
							R.string.dialog_msg_pattern_name_too_long),
							getResources().getInteger(
									R.integer.max_pattern_name_length)))
					.setCancelable(false).setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		case DIALOG_ID.ERROR_NO_PATTERN_SELECTED:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_error).setMessage(
					R.string.dialog_msg_no_pattern_selected).setCancelable(
					false).setPositiveButton(R.string.button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
						}
					}).create();
		case DIALOG_ID.ERROR_RESERVED_NAME:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_error).setMessage(
					R.string.dialog_msg_reserved_name).setCancelable(false)
					.setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		case DIALOG_ID.ERROR_NOT_SPELLABLE:
			return new AlertDialog.Builder(this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.dialog_title_error).setMessage(
					R.string.dialog_msg_not_spellable).setCancelable(false)
					.setPositiveButton(R.string.button_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).create();
		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_ID.CAPTURE_ABORTED:
			((AlertDialog) dialog).setMessage(String.format(getResources()
					.getString(R.string.dialog_msg_capture_abort),
					captureAbortedPatternName));
			break;
		case DIALOG_ID.ERROR_DUPLICATE_NAME:
			((AlertDialog) dialog).setMessage(String.format(getResources()
					.getString(R.string.dialog_msg_duplicate_name),
					getPatternName()));
			break;
		}
	}

	private String getPatternName() {
		return VibratorConstants.WHITESPACE.matcher(
				mTextPatternName.getText().toString()).replaceAll(" ").trim()
				.toUpperCase();
	}

	private class CaptureTimer extends CountDownTimer {

		public CaptureTimer() {
			super(selectedDuration, VibratorConstants.MILLIS_IN_SECOND);
		}

		@Override
		public void onFinish() {
			mVibrator.cancel();
			final long timeSinceLastEvent = selectedDuration
					- VibratorUtility.getSumOfElements(patternList);
			/*
			 * patternList.size() being even means button was pressed down when
			 * countdown finished
			 */
			if (patternList.size() % 2 == 0) {
				if (timeSinceLastEvent > 0) {
					/*
					 * add vibration duration since last ACTION_DOWN event
					 */
					patternList.add((int) timeSinceLastEvent);
					patternList.add(0);
				}
			} else {
				if (timeSinceLastEvent > 0) {
					/*
					 * add silent duration since last ACTION_UP event
					 */
					patternList.add((int) timeSinceLastEvent);
				} else {
					patternList.add(0);
				}
			}
			final IVibrationPattern pattern = PatternMaker.make(
					getPatternName(), VibratorUtility.toPairList(patternList));
			patterns.put(pattern.getName(), pattern);
			PatternManagerActivity.this.showDialog(DIALOG_ID.CAPTURE_SUCCESS);
			toggleCapturingState();
		}

		@Override
		public void onTick(long millisUntilFinished) {
			mTextTimer.setText(String.valueOf(millisUntilFinished
					/ VibratorConstants.MILLIS_IN_SECOND));
		}
	}

}
