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

package m3r7.android.vibrator.command;

import java.util.Collection;

import m3r7.android.vibrator.ICommand;
import m3r7.android.vibrator.VibratorActivity;
import m3r7.android.vibrator.ICommand.Type;

public class CommandMaker {
	public static ICommand make(String name, String description) {
		return new BaseCommand(name, description);
	}

	public static ICommand make(Collection<String> matches, String[] commands,
			Collection<String> patternNames, VibratorActivity activity) {
		for (String match : matches) {
			if (commands[Type.START.resourceArrayIndex].equalsIgnoreCase(match)) {
				return new StartVibratorCommand(activity);
			}
			if (commands[Type.STOP.resourceArrayIndex].equalsIgnoreCase(match)) {
				return new StopVibratorCommand(activity);
			}
			if (commands[Type.DEC_FREQ.resourceArrayIndex]
					.equalsIgnoreCase(match)) {
				return new DecreaseFreqCommand(activity);
			}
			if (commands[Type.INC_FREQ.resourceArrayIndex]
					.equalsIgnoreCase(match)) {
				return new IncreaseFreqCommand(activity);
			}
			if (commands[Type.DISABLE.resourceArrayIndex]
					.equalsIgnoreCase(match)) {
				return new DisableCommand(activity);
			}
			for (String patternName : patternNames) {
				if (patternName.equalsIgnoreCase(match)) {
					return new SelectCommand(activity, patternName);
				}
			}
		}
		return new BaseCommand(activity, Type.BASE);
	}
}
