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

package m3r7.android.vibrator.pattern;

import java.util.ArrayList;
import java.util.List;

import m3r7.android.vibrator.IVibrationPattern;
import m3r7.android.vibrator.util.VibratorConstants;
import android.util.Pair;

class VibrationPattern implements IVibrationPattern {

	private final String name;
	private final List<Pair<Integer, Integer>> pattern;

	private static final int MIN_LENGTH = VibratorConstants.MILLIS_IN_SECOND / 20;
	private static final int MAX_LENGTH = 10 * VibratorConstants.MILLIS_IN_SECOND;
	private static final double SCALER = 0.25;

	VibrationPattern(String name, List<Pair<Integer, Integer>> pattern) {
		this.name = name;
		this.pattern = pattern;
	}

	@Override
	public IVibrationPattern scale(int modifier) {
		if (modifier == 0) {
			return this;
		}
		List<Pair<Integer, Integer>> scaled = new ArrayList<Pair<Integer, Integer>>();
		final double scalingVal = SCALER * modifier;
		for (Pair<Integer, Integer> p : pattern) {
			final int newVibDuration = getScaledDuration(p.first, scalingVal);
			final int newSilenceDuration = getScaledDuration(p.second,
					scalingVal);
			scaled.add(new Pair<Integer, Integer>(newVibDuration,
					newSilenceDuration));
		}
		return new VibrationPattern(name, scaled);
	}

	@Override
	public Pair<Boolean, Boolean> isScalable(int modifier) {
		final double decScalingVal = SCALER * (modifier - 1);
		final double incScalingVal = SCALER * (modifier + 1);
		boolean allowNextDecrease = true;
		boolean allowNextIncrease = true;
		int vibrationTotal = 0;
		int silenceTotal = 0;
		final int lastIndex = pattern.size() - 1;
		for (int i = 0; i < lastIndex + 1; i++) {
			final Pair<Integer, Integer> p = pattern.get(i);
			vibrationTotal += p.first;
			silenceTotal += p.second;
			if (allowNextDecrease) {
				final int nextVibDuration = getScaledDuration(p.first,
						decScalingVal);
				final int nextSilenceDuration = getScaledDuration(p.second,
						decScalingVal);
				/*
				 * 0 at last pair's 'second' must be ignored when deciding
				 * scalability
				 */
				final boolean ignore = (i == lastIndex && p.second == 0);
				if (nextVibDuration < MIN_LENGTH
						|| (!ignore && nextSilenceDuration < MIN_LENGTH)) {
					allowNextDecrease = false;
				}
			}
			if (allowNextIncrease) {
				final int nextVibDuration = getScaledDuration(p.first,
						incScalingVal);
				final int nextSilenceDuration = getScaledDuration(p.second,
						incScalingVal);
				if (nextVibDuration > MAX_LENGTH
						|| nextSilenceDuration > MAX_LENGTH) {
					allowNextIncrease = false;
				}
			}
		}
		if (vibrationTotal == 0 || silenceTotal == 0) {
			return new Pair<Boolean, Boolean>(false, false);
		}
		return new Pair<Boolean, Boolean>(allowNextDecrease, allowNextIncrease);
	}

	@Override
	public long[] getVibratable(long waitTime) {
		long[] arr = new long[pattern.size() * 2 + 1];
		int arrIndex = 0;
		arr[arrIndex++] = waitTime;
		for (Pair<Integer, Integer> pair : pattern) {
			arr[arrIndex++] = pair.first.longValue();
			arr[arrIndex++] = pair.second.longValue();
		}
		return arr;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<Pair<Integer, Integer>> getPattern() {
		final List<Pair<Integer, Integer>> pairList = new ArrayList<Pair<Integer, Integer>>();
		for (Pair<Integer, Integer> p : pattern) {
			pairList.add(new Pair<Integer, Integer>(new Integer(p.first),
					new Integer(p.second)));
		}
		return pairList;
	}

	private static int getScaledDuration(int oldDuration, double scalingVal) {
		return Math.max(0, (int) (oldDuration * (1 + scalingVal)));
	}

}
