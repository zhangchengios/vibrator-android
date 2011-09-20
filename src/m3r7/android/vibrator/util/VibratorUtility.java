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

package m3r7.android.vibrator.util;

import java.util.ArrayList;
import java.util.List;

import m3r7.android.vibrator.ICommand;
import m3r7.android.vibrator.IVibrationPattern;
import m3r7.android.vibrator.R;
import m3r7.android.vibrator.command.CommandMaker;
import m3r7.android.vibrator.pattern.PatternMaker;
import android.content.Context;
import android.util.Pair;

public class VibratorUtility {
	public static List<Pair<Integer, Integer>> toPairList(int[] arr) {
		List<Pair<Integer, Integer>> pairList = new ArrayList<Pair<Integer, Integer>>();
		boolean isFirstOfPair = true;
		int firstOfPair = 0;
		for (int i : arr) {
			if (isFirstOfPair) {
				firstOfPair = i;
			} else {
				pairList.add(new Pair<Integer, Integer>(firstOfPair, i));
			}
			isFirstOfPair = !isFirstOfPair;
		}
		return pairList;
	}

	public static List<Pair<Integer, Integer>> toPairList(List<Integer> list) {
		int[] arr = new int[list.size()];
		int arrIndex = 0;
		for (int i : list) {
			arr[arrIndex++] = i;
		}
		return toPairList(arr);
	}

	public static long getSumOfElements(List<Integer> list) {
		long sum = 0L;
		for (Integer i : list) {
			sum += i.longValue();
		}
		return sum;
	}

	/**
	 * This method is called when the database gets created. Following the
	 * creation of the database table(s), predefined patterns returned by this
	 * method are inserted into the appropriate table.
	 * 
	 * @param context
	 * @return
	 */
	public static List<IVibrationPattern> getPredefinedPatterns(Context context) {
		// continious
		final String nameContinious = context
				.getString(R.string.predefined_pattern_continuous);
		final int[] arrContinious = context.getResources().getIntArray(
				R.array.predefined_pattern_continuous);
		final IVibrationPattern continious = PatternMaker.make(nameContinious,
				VibratorUtility.toPairList(arrContinious));
		// regular
		final String nameRegular = context
				.getString(R.string.predefined_pattern_regular);
		final int[] arrRegular = context.getResources().getIntArray(
				R.array.predefined_pattern_regular);
		final IVibrationPattern regular = PatternMaker.make(nameRegular,
				VibratorUtility.toPairList(arrRegular));

		List<IVibrationPattern> predefinedPatterns = new ArrayList<IVibrationPattern>();
		predefinedPatterns.add(continious);
		predefinedPatterns.add(regular);
		return predefinedPatterns;
	}

	public static boolean hasText(String str) {
		return (str != null && str.trim().length() > 0);
	}

	public static boolean isSpellable(String str) {
		if (str == null) {
			return false;
		}
		final char[] arr = str.toCharArray();
		for (char c : arr) {
			if (!(Character.isLetter(c) || Character.isWhitespace(c))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isVoiceCommand(Context context, String str) {
		final String[] names = context.getResources().getStringArray(
				R.array.command_names);
		return isVoiceCommand(names, str);
	}

	public static boolean isVoiceCommand(String[] names, String str) {
		for (String name : names) {
			if (name.equalsIgnoreCase(str)) {
				return true;
			}
		}
		return false;
	}

	public static List<ICommand> getCommandsList(Context context) {
		final List<ICommand> commandList = new ArrayList<ICommand>();
		final String[] names = context.getResources().getStringArray(
				R.array.command_names);
		final String[] descriptions = context.getResources().getStringArray(
				R.array.command_descriptions);
		for (int i = 0; i < names.length; i++) {
			commandList.add(CommandMaker.make(names[i], descriptions[i]));
		}
		return commandList;
	}

}
