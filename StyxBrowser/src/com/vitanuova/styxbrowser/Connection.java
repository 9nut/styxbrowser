package com.vitanuova.styxbrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Connection extends Activity {
	private static final int ACTIVITY_OPEN_DIRECTORY = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connection);

		final Button connection = (Button) findViewById(R.id.ButtonConnection);

		connection.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final EditText address = (EditText) findViewById(R.id.EditTextAddress);
				final EditText directory = (EditText) findViewById(R.id.EditTextDirectory);
				final EditText login = (EditText) findViewById(R.id.EditTextLogin);
				final EditText password = (EditText) findViewById(R.id.EditTextPassword);
				startDirectory(address, directory, login, password);
			}
		});
	}

	private void startDirectory(EditText address, EditText directory,
			EditText login, EditText password) {
		StyxBrowser.styx = new AndroidStyx(address.getText().toString());
		Intent i = new Intent(this, OpenDirectory.class);
		i.putExtra("dir", directory.getText().toString());
		startActivityForResult(i, ACTIVITY_OPEN_DIRECTORY);
	}
}
