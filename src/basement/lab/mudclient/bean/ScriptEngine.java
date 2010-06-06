package basement.lab.mudclient.bean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map.Entry;

public class ScriptEngine implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 145443829051697734L;
	private final static int MAX_EXPAND_LEVEL = 50;

	public static HashMap<String, String> aliases;
	public static ArrayList<Trigger> triggers;
	public static String filename;

	private static class AliasParseLine {
		public String title;
		public String body;

		public AliasParseLine(String t, String b) {
			title = t;
			if (b.length() >= 2) {
				if (b.startsWith("{"))
					b = b.substring(1);
				if (b.endsWith("}"))
					b = b.substring(0, b.length() - 1);
			}
			body = b;
		}
	}

	public static void init(String file) throws Exception {
		aliases = new HashMap<String, String>();
		triggers = new ArrayList<Trigger>();
		ScriptEngine.filename = file;
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(file))));

		LinkedList<String> lines = new LinkedList<String>();
		do {
			String line = br.readLine();
			if (line == null) {
				break;
			} else {
				lines.add(line.trim());
			}
		} while (true);
		parseLines(lines);
		br.close();
	}

	private static void parseLines(LinkedList<String> lines) throws Exception {
		LinkedList<AliasParseLine> apl = new LinkedList<AliasParseLine>();
		for (String line : lines) {
			if (line.length() == 0 || line.charAt(0) != '#') {
				continue;
			} else {
				char[] lineArray = line.toCharArray();
				int length = lineArray.length;
				int lastSpaceIndex = 0;
				int bracketLevel = 0;
				ArrayList<String> tokens = new ArrayList<String>();
				for (int i = 1; i < length; i++) {
					switch (lineArray[i]) {
					case '{':
						bracketLevel++;
						break;
					case '}':
						bracketLevel--;
						if (bracketLevel == 0) {
							String token = new String(lineArray,
									lastSpaceIndex + 1, i - lastSpaceIndex);
							tokens.add(token.trim());
							lastSpaceIndex = i;
						}
						break;
					case ' ':
					case '\r':
					case '\n':
						if (bracketLevel == 0) {
							String token = new String(lineArray,
									lastSpaceIndex + 1, i - lastSpaceIndex);
							tokens.add(token.trim());
							lastSpaceIndex = i;
						}
						break;
					}
				}
				if (tokens.size() == 3
						&& tokens.get(0).equalsIgnoreCase("alias")) {
					AliasParseLine ap = new AliasParseLine(tokens.get(1),
							tokens.get(2));
					apl.add(ap);
				} else if (tokens.size() == 4
						&& tokens.get(0).equalsIgnoreCase("trigger")) {
					triggers.add(new Trigger(tokens.get(1), tokens.get(3)));
				}
			}
		}
		if (apl.size() != 0) {
			expandAllAliases(apl);
		}
	}

	private static void expandAllAliases(LinkedList<AliasParseLine> apl)
			throws Exception {
		HashMap<String, Boolean> doneMap = new HashMap<String, Boolean>();
		int aliasCount = apl.size();
		for (AliasParseLine ap : apl) {
			doneMap.put(ap.title, false);
			aliases.put(ap.title, ap.body);
		}
		int doneAlias = 0;
		int expandLevel = 0;
		while (doneAlias < aliasCount && expandLevel < MAX_EXPAND_LEVEL) {
			Set<Entry<String, String>> aliasSet = aliases.entrySet();
			for (Entry<String, String> entry : aliasSet) {
				String myKey = entry.getKey();
				if (doneMap.get(myKey))
					continue;
				else {
					String[] cmds = entry.getValue().split(";");
					String newCmd = "";
					int len = cmds.length;
					boolean thisDone = true;
					for (int i = 0; i < len; i++) {
						if (cmds[i].equals(""))
							continue;
						String aliasCmd = aliases.get(cmds[i]);
						if (cmds[i].equals(myKey)) {
							throw new Exception(
									"Error! Infinite Loop Detected in Alias!");
						}
						boolean isAlias = aliasCmd != null;
						if (isAlias) {
							if (doneMap.get(cmds[i])) {
								cmds[i] = new String(aliasCmd);
							} else {
								thisDone = false;
							}
						}
						newCmd += cmds[i] + ";";
					}
					if (thisDone) {
						doneAlias++;
						doneMap.put(entry.getKey(), true);
					} else {
						doneMap.put(entry.getKey(), false);
					}
					aliases.put(entry.getKey(), newCmd.substring(0, newCmd
							.length() - 1));
				}
			}
			expandLevel++;
		}
		if (expandLevel == MAX_EXPAND_LEVEL) {
			throw new Exception(
					"Taking too long to analyze aliases. Possible Infinite loops.");
		}
	}

	public static void removeAlias(String title) {
		aliases.remove(title);
	}

	public static void addNewAlias(String title, String body) {
		String cmd = "";
		String[] token = body.split(";");
		for (String tm : token) {
			String t = tm.trim();
			if (!t.startsWith("#")) { // regular expr
				String aliasCmd = aliases.get(t);
				if (aliasCmd != null) {
					cmd += aliasCmd + ";";
				} else {
					cmd += t + ";";
				}
			} else { // numbered expr
				int space = t.indexOf(' ');
				try {
					int repeat = Integer.parseInt(t.substring(1, space));
					String subToken = t.substring(space).trim();
					String subCmd = aliases.get(subToken);
					if (subCmd == null) {
						subCmd = subToken;
					}
					for (int i = 0; i < repeat; i++) {
						cmd += subCmd + ";";
					}
				} catch (NumberFormatException e) {
					cmd += t + ";";
					continue;
				}
			}
		}
		aliases.put(title, cmd.substring(0, cmd.length()));
	}

	public static void commit() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(filename));
		Set<Entry<String, String>> set = aliases.entrySet();
		for (Entry<String, String> entry : set) {
			pw.println("#ALIAS " + entry.getKey() + " " + "{"
					+ entry.getValue() + "}");
		}
		for (Trigger t : triggers) {
			pw.println(t.toString());
		}
		pw.close();
	}

	public static void removeTrigger(int position) {
		if (position < 0 || position >= triggers.size())
			return;
		triggers.remove(position);
	}

	public static void addTrigger(Trigger t) {
		triggers.add(t);
	}

	public static void modifyTrigger(int position, String p, String b) {
		if (position < 0 || position >= triggers.size())
			return;
		if (p == null || p.length() == 0 || b == null || b.length() == 0) {
			return;
		}
		triggers.get(position).pattern = p;
		triggers.get(position).body = b;
	}
}
