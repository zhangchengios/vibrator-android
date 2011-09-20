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

import java.util.regex.Pattern;

import android.view.Menu;

public interface VibratorConstants {
	int MILLIS_IN_SECOND = 1000;
	long[] INDEFINITELY = { 0, 6000, 0 };
	Pattern WHITESPACE = Pattern.compile("\\s+");

	public interface MENU_ID {
		int DEFINE_PATTERN = Menu.FIRST;
		int SEE_COMMANDS = Menu.FIRST + 1;
	}

	public interface REQUEST_CODE {
		int MANAGE = 0;
		int VOICE_RECOGNITION = 1;
	}

	public interface PREFERENCES {
		String FILE_NAME = "VIBRATOR_PREFS";
		String KEY_LAST_USED = "LAST_USED";
	}

	public interface DIALOG_ID {
		int CAPTURE_ABORTED = 0;
		int CAPTURE_SUCCESS = 1;
		int DELETE_SUCCESS = 2;
		int ERROR_DUPLICATE_NAME = 3;
		int ERROR_MISSING_NAME = 4;
		int ERROR_NAME_TOO_LONG = 5;
		int ERROR_NO_PATTERN_SELECTED = 6;
		int ERROR_RESERVED_NAME = 7;
		int ERROR_NOT_SPELLABLE = 8;
	}

}
