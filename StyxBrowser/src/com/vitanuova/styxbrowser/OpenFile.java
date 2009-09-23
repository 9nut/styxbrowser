package com.vitanuova.styxbrowser;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

public class OpenFile extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();

		String file = extras.getString("file");

		if (!"".equals(file)) {
			ScrollView sv = new ScrollView(this);
			TextView tv = new TextView(sv.getContext());

			sv.addView(tv);
			tv.setText(StyxBrowser.styx.file(file));
			tv.setTextSize(10);
			setContentView(sv);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.file_options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.edit:
			// To do.
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
