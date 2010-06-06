package basement.lab.mudclient.bean;

import java.util.ArrayList;

import basement.lab.mudclient.MudTerminalActivity;

public class TriggerEngine {

	private static ArrayList<String> buffer = new ArrayList<String>();
	private static String leftOver = null;
	private static int maxSize = 30;
	public static boolean useTrigger = false;
	private static MudTerminalActivity mud;

	public static void registerMudTerminal(MudTerminalActivity mud) {
		TriggerEngine.mud = mud;
	}

	public static void addText(String text) {
		if (text == null)
			return;
		boolean noLeftOver = text.endsWith("\n");
		String[] tokens = text.split("\n");
		int tokenCount = tokens.length;
		if (tokenCount <= 0)
			return;
		int count;
		if (!noLeftOver)
			count = tokenCount - 1;
		else
			count = tokenCount;
		if (leftOver != null) {
			tokens[0] = leftOver + tokens[0];
			leftOver = null;
		}
		for (int i = 0; i < count; i++) {
			if (buffer.size() >= maxSize) {
				buffer.remove(0);
			}
			addOneLine(tokens[i]);
		}
		if (!noLeftOver) {
			leftOver = tokens[tokenCount - 1];
		}
	}

	public static ArrayList<String> getBufferedText() {
		return new ArrayList<String>(buffer);
	}

	private static void addOneLine(String line) {
		buffer.add(line);
		if (useTrigger) {
			ArrayList<Trigger> triggers = ScriptEngine.triggers;
			for (Trigger t : triggers) {
				if (line.contains(t.pattern)) {
					mud.sendCommand("#wa 100;" + t.body);
				}
			}
		}
	}

}
