package basement.lab.mudclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MenuActivity extends Activity implements OnClickListener {

	private Button start, setting, about, exit, colors, help;
	private AlertDialog aboutDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (SettingsManager.isFullScreen(this)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		setContentView(basement.lab.mudclient.R.layout.menu);
		start = (Button) findViewById(R.id.start);
		start.setOnClickListener(this);
		setting = (Button) findViewById(R.id.setting);
		setting.setOnClickListener(this);
		about = (Button) findViewById(R.id.about);
		about.setOnClickListener(this);
		help = (Button) findViewById(R.id.help);
		help.setOnClickListener(this);
		exit = (Button) findViewById(R.id.exit);
		exit.setOnClickListener(this);
		colors = (Button) findViewById(R.id.color);
		colors.setOnClickListener(this);
		aboutDialog = new AlertDialog.Builder(this).setIcon(
				R.drawable.smallicon).setTitle(R.string.about_title)
				.setMessage(R.string.about_body).setNegativeButton(
						R.string.cancel, null).create();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start:
			Intent s = new Intent(this, ServerListActivity.class);
			startActivity(s);
			break;
		case R.id.setting:
			Intent i = new Intent(this, SettingsManager.class);
			this.startActivity(i);
			break;
		case R.id.color:
			Intent c = new Intent(this, ColorsActivity.class);
			this.startActivity(c);
			break;
		case R.id.about:
			aboutDialog.show();
			break;
		case R.id.exit:
			finish();
			break;
		case R.id.help:
			Intent h = new Intent(this, HelpActivity.class);
			startActivity(h);
			break;
		}
	}
}
