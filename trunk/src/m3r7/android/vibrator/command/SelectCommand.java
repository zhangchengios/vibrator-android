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

import m3r7.android.vibrator.VibratorActivity;

class SelectCommand extends BaseCommand {

	private final String patternName;

	SelectCommand(VibratorActivity activity, String patternName) {
		super(activity, Type.SELECT);
		this.patternName = patternName;
	}

	@Override
	public void execute() {
		getActivity().actionSelectPattern(patternName);
	}

}
