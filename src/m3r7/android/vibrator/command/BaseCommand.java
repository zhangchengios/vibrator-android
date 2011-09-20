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

import m3r7.android.vibrator.ICommand;
import m3r7.android.vibrator.VibratorActivity;

class BaseCommand implements ICommand {

	private final String name;
	private final String description;
	private final Type type;
	private final VibratorActivity activity;

	BaseCommand(String nameStr, String descriptionStr) {
		this.name = nameStr;
		this.description = descriptionStr;
		this.type = Type.BASE;
		this.activity = null;
	}

	BaseCommand(VibratorActivity activity, Type type) {
		this.name = null;
		this.description = null;
		this.type = type;
		this.activity = activity;
	}

	@Override
	public void execute() {
		// DO NOTHING!
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Type getType() {
		return type;
	}

	VibratorActivity getActivity() {
		return activity;
	}

}
