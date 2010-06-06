package basement.lab.mudclient.utils;

import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import basement.lab.mudclient.R;
import basement.lab.mudclient.bean.Alias;
import basement.lab.mudclient.bean.ScriptEngine;

public class AliasAdapter extends BaseAdapter {

	
	private ArrayList<Alias> aliases;
	private Context ctx;

	public AliasAdapter(Context ctx) {
		this.ctx = ctx;

		refreshArrayList();
	}

	private void refreshArrayList() {
		aliases = new ArrayList<Alias>();
		Set<Entry<String, String>> temp = ScriptEngine.aliases.entrySet();
		for (Entry<String, String> t : temp) {
			aliases.add(new Alias(t.getKey(), t.getValue()));
		}
	}

	public int getCount() {
		return aliases.size();
	}

	public Object getItem(int position) {
		return aliases.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View server = inflater.inflate(R.layout.aliasrow, null);
		TextView name;
		name = (TextView) server.findViewById(R.id.aliasname);
		name.setText(aliases.get(position).name);
		return server;
	}

	@Override
	public void notifyDataSetChanged() {
		refreshArrayList();
		super.notifyDataSetChanged();
	}

}
