package basement.lab.mudclient;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import basement.lab.mudclient.bean.ScriptEngine;
import basement.lab.mudclient.bean.TriggerEngine;
import basement.lab.mudclient.utils.TriggerAdapter;

public class TriggerListActivity extends ListActivity implements
		OnClickListener, OnItemClickListener {

	private Button add, finish;
	private TriggerAdapter adapter;
	public final static String SCREEN_TEXT = "B.L.M.SCREEN_TEXT";
	private ArrayList<String> screenText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.triggerlist);
		setTitle(R.string.trigereditor);
		add = (Button) findViewById(R.id.add);
		add.setOnClickListener(this);
		finish = (Button) findViewById(R.id.finish);
		finish.setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		adapter = new TriggerAdapter(this);
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		screenText = TriggerEngine.getBufferedText();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.finish:
			finish();
			break;
		case R.id.add:
			Intent i = new Intent(this, TriggerEditorActivity.class);
			i.putExtra(TriggerEditorActivity.REQUEST_TYPE,
					TriggerEditorActivity.REQUEST_ADD);
			i.putExtra(TriggerEditorActivity.TRIGGER_POSITION, -1);
			i.putExtra(SCREEN_TEXT, screenText);
			startActivityForResult(i, TriggerEditorActivity.REQUEST_ADD);
			break;
		}
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int id,
			long position) {
		Intent i = new Intent(this, TriggerEditorActivity.class);
		i.putExtra(TriggerEditorActivity.REQUEST_TYPE,
				TriggerEditorActivity.REQUEST_MODIFY);
		i.putExtra(TriggerEditorActivity.TRIGGER_POSITION, (int)position);
		i.putExtra(SCREEN_TEXT, screenText);
		startActivityForResult(i, TriggerEditorActivity.REQUEST_MODIFY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED)
			return;
		adapter.notifyDataSetChanged();
		try {
			ScriptEngine.commit();
		} catch (FileNotFoundException e) {
			Toast.makeText(this, R.string.commitfileerror, 0).show();
		}
	}
}
