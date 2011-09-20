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

package m3r7.android.vibrator.persistance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import m3r7.android.vibrator.IPersister;
import m3r7.android.vibrator.IVibrationPattern;
import m3r7.android.vibrator.pattern.PatternMaker;
import m3r7.android.vibrator.util.VibratorUtility;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

class SQLitePersister implements IPersister {

	private static final String DATABASE_NAME = "VIBRATOR_DB";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "PATTERNS";
	private static final String NAME_COLUMN = "NAME";
	private static final String PAIR_NO_COLUMN = "PAIR_NO";
	private static final String UPTIME_COLUMN = "UPTIME";
	private static final String DOWNTIME_COLUMN = "DOWNTIME";

	private final Context mContext;
	private SQLiteDatabase mVibratorDB;

	private static IPersister instance;

	private SQLitePersister(Context context) {
		mContext = context;
		final VibratorOpenHelper vibratorOpenHelper = new VibratorOpenHelper();
		mVibratorDB = vibratorOpenHelper.getWritableDatabase();
	}

	static IPersister Instance(Context context) {
		if (instance == null) {
			instance = new SQLitePersister(context);
		}
		return instance;
	}

	@Override
	public void savePatterns(Map<String, IVibrationPattern> map) {
		savePatterns(map.values());
	}

	@Override
	public Map<String, IVibrationPattern> getPatternsMap() {
		Map<String, IVibrationPattern> patternsMap = new HashMap<String, IVibrationPattern>();
		Collection<IVibrationPattern> collection = getPatterns();
		for (IVibrationPattern pattern : collection) {
			patternsMap.put(pattern.getName(), pattern);
		}
		return patternsMap;
	}

	@Override
	public void deletePatterns() {
		mVibratorDB.delete(TABLE_NAME, null, null);
	}

	@Override
	public void closeDB() {
		if (mVibratorDB != null) {
			mVibratorDB.close();
			mVibratorDB = null;
		}
		instance = null;
	}

	public void savePatterns(Collection<IVibrationPattern> collection) {
		for (IVibrationPattern pattern : collection) {
			final String patternName = pattern.getName();
			final List<Pair<Integer, Integer>> pairs = pattern.getPattern();
			int pairNo = 1;
			for (Pair<Integer, Integer> pair : pairs) {
				insertRow(patternName, pairNo++, pair.first, pair.second,
						mVibratorDB);
			}
		}
	}

	public Collection<IVibrationPattern> getPatterns() {
		final String orderBy = NAME_COLUMN + " asc, " + PAIR_NO_COLUMN + " asc";
		Cursor c = mVibratorDB.query(TABLE_NAME, null, null, null, null, null,
				orderBy);
		Collection<IVibrationPattern> patternList = new ArrayList<IVibrationPattern>();
		if (c.moveToFirst()) {
			final int indexName = c.getColumnIndexOrThrow(NAME_COLUMN);
			final int indexUptime = c.getColumnIndexOrThrow(UPTIME_COLUMN);
			final int indexDowntime = c.getColumnIndexOrThrow(DOWNTIME_COLUMN);
			List<Pair<Integer, Integer>> pattern = new ArrayList<Pair<Integer, Integer>>();
			String prevName = null;
			do {
				final String name = c.getString(indexName);
				final int uptime = c.getInt(indexUptime);
				final int downtime = c.getInt(indexDowntime);
				if (prevName != null && !prevName.equals(name)) {// found new
					// pattern
					patternList.add(PatternMaker.make(prevName, pattern));
					pattern = new ArrayList<Pair<Integer, Integer>>();
				}
				final Pair<Integer, Integer> pair = new Pair<Integer, Integer>(
						uptime, downtime);
				pattern.add(pair);
				prevName = name;
			} while (c.moveToNext());
			if (!pattern.isEmpty()) {
				patternList.add(PatternMaker.make(prevName, pattern));
			}
		}
		if (c != null && !c.isClosed()) {
			c.close();
			c = null;
		}
		return patternList;
	}

	private void insertRow(String patternName, int pairNo, int uptime,
			int downtime, SQLiteDatabase db) {
		ContentValues cv = new ContentValues(4);
		cv.put(NAME_COLUMN, patternName);
		cv.put(PAIR_NO_COLUMN, pairNo);
		cv.put(UPTIME_COLUMN, uptime);
		cv.put(DOWNTIME_COLUMN, downtime);
		db.insert(TABLE_NAME, null, cv);
	}

	private class VibratorOpenHelper extends SQLiteOpenHelper {

		public VibratorOpenHelper() {
			super(mContext, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
					+ NAME_COLUMN + " text, " + PAIR_NO_COLUMN + " integer, "
					+ UPTIME_COLUMN + " integer, " + DOWNTIME_COLUMN
					+ " integer, unique(" + NAME_COLUMN + ", " + PAIR_NO_COLUMN
					+ ") on conflict replace);";
			db.execSQL(TABLE_CREATE);
			// persist predefined patterns
			final List<IVibrationPattern> predefinedPatterns = VibratorUtility
					.getPredefinedPatterns(mContext);
			for (IVibrationPattern pattern : predefinedPatterns) {
				final String patternName = pattern.getName();
				final List<Pair<Integer, Integer>> pairs = pattern.getPattern();
				int pairNo = 1;
				for (Pair<Integer, Integer> pair : pairs) {
					insertRow(patternName, pairNo++, pair.first, pair.second,
							db);
				}
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

}
