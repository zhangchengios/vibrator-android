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

import java.util.Map;
import java.util.Set;

import m3r7.android.vibrator.IVibrationPattern;

public abstract class PatternMap {

	private final Map<String, IVibrationPattern> patterns;

	protected abstract void onModified();

	protected abstract void onCreated(Map<String, IVibrationPattern> patterns);

	protected PatternMap(Map<String, IVibrationPattern> patterns) {
		super();
		this.patterns = patterns;
		onCreated(patterns);
	}

	public IVibrationPattern put(String name, IVibrationPattern pattern) {
		IVibrationPattern returnVal = patterns.put(name, pattern);
		onModified();
		return returnVal;
	}

	public IVibrationPattern remove(String name) {
		IVibrationPattern returnVal = patterns.remove(name);
		onModified();
		return returnVal;
	}

	public IVibrationPattern get(String name) {
		return patterns.get(name);
	}

	public boolean isEmpty() {
		return patterns.isEmpty();
	}

	public boolean containsKey(String key) {
		return patterns.containsKey(key);
	}

	public Set<String> keySet() {
		return patterns.keySet();
	}

	public Map<String, IVibrationPattern> getWrappedObject() {
		return patterns;
	}

}
