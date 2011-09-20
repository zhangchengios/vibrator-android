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

import java.util.List;

import m3r7.android.vibrator.util.VibratorUtility;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CommandsListActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.commandslist);
		final ListView listView = (ListView) findViewById(R.id.commmadsListView);
		listView.setAdapter(new CommandsAdapter(VibratorUtility
				.getCommandsList(this)));
	}

	private class CommandsAdapter extends ArrayAdapter<ICommand> {

		public CommandsAdapter(List<ICommand> commandsList) {
			super(CommandsListActivity.this, R.layout.commandslistitem,
					commandsList);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View itemView = convertView;
			if (itemView == null) {
				itemView = getLayoutInflater().inflate(
						R.layout.commandslistitem, null);
			}
			final TextView txtCommandName = (TextView) itemView
					.findViewById(R.id.txtCommandName);
			final TextView txtCommandDesc = (TextView) itemView
					.findViewById(R.id.txtCommandDesc);
			final ICommand command = getItem(position);
			txtCommandName.setText(command.getName());
			txtCommandDesc.setText(command.getDescription());
			return itemView;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}

	}

}