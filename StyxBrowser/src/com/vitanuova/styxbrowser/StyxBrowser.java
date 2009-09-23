package com.vitanuova.styxbrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StyxBrowser extends Activity {
	private static final int ACTIVITY_CONNECTION = 0;

	/* Globals. */
	static public AndroidStyx styx;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = new Intent(this, Connection.class);
		startActivityForResult(i, ACTIVITY_CONNECTION);
		finish();
	}
}
