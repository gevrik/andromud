package basement.lab.mudclient.bean;

public class Trigger {
	public String pattern;
	public String body;

	public Trigger(String p, String b) {
		pattern = removeBrackets(p);
		body = removeBrackets(b);
	}

	@Override
	public String toString() {
		return "#TRIGGER {" + pattern + "} {" + body + "}";
	}

	private String removeBrackets(String text) {
		if (text == null || text.length() < 2)
			return text;
		text = text.trim();
		if (text.startsWith("{"))
			text = text.substring(1);
		if (text.endsWith("}"))
			text = text.substring(0, text.length() - 1);
		return text;
	}
}
