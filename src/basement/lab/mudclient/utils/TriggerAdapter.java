package basement.lab.mudclient.utils;

import basement.lab.mudclient.R;
import basement.lab.mudclient.bean.ScriptEngine;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TriggerAdapter extends BaseAdapter {

	private Context ctx;

	public TriggerAdapter(Context ctx) {
		this.ctx = ctx;
	}

	public int getCount() {
		return ScriptEngine.triggers.size();
	}

	public Object getItem(int position) {
		return ScriptEngine.triggers.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View server = inflater.inflate(R.layout.triggerrow, null);
		TextView name;
		name = (TextView) server.findViewById(R.id.triggerpattern);
		name.setText(ScriptEngine.triggers.get(position).pattern);
		return server;
	}
}
