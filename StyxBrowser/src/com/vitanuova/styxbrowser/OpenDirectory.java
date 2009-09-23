package com.vitanuova.styxbrowser;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class OpenDirectory extends ListActivity {
	private static final int ACTIVITY_OPEN_FILE = 0;
	private static final int ACTIVITY_OPEN_DIRECTORY = 1;
	private static final int DIALOG_RENAME = 1;
	private static final int DIALOG_CREATE_FILE = 2;
	private static final int DIALOG_CREATE_DIRECTORY = 3;

	private String dir;
	private String ls[];

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerForContextMenu(getListView());

		Bundle extras = getIntent().getExtras();

		dir = extras.getString("dir");

		if (!"".equals(dir)) {
			ls = StyxBrowser.styx.dir(dir);

			setListAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, ls));
			getListView().setTextFilterEnabled(true);
		}
	}

	/*
	 * OptionsMenu
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.directory_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.create_file:
			showDialog(DIALOG_CREATE_FILE);
			return true;
		case R.id.create_directory:
			showDialog(DIALOG_CREATE_DIRECTORY);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * ContextMenu
	 */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.directory_context_menu, menu);
		return;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.rename:
			showDialog(DIALOG_RENAME);
			return true;
		case R.id.delete:
			// To do.
			return true;
		}
		return false;
	}

	/*
	 * ListItemClick
	 */

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (!dir.endsWith("/"))
			dir = dir + "/";

		String path = dir + ls[(int) id];
		if (path.endsWith("/")) {
			Intent i1 = new Intent(this, OpenDirectory.class);
			i1.putExtra("dir", path);
			startActivityForResult(i1, ACTIVITY_OPEN_DIRECTORY);
		} else {
			Intent i2 = new Intent(this, OpenFile.class);
			i2.putExtra("file", path);
			startActivityForResult(i2, ACTIVITY_OPEN_FILE);
		}
	}

	/*
	 * Dialog
	 */

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.dialog);
		final Button ok = (Button) findViewById(R.id.ButtonOk);

		switch (id) {
		case DIALOG_RENAME:
			// To do.
			break;
		case DIALOG_CREATE_FILE:
			// To do.
			break;
		case DIALOG_CREATE_DIRECTORY:
			// To do.
			break;
		default:
			dialog = null;
		}

		return dialog;
	}
}
