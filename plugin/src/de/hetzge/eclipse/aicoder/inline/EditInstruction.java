package de.hetzge.eclipse.aicoder.inline;

public record EditInstruction(
		String key,
		String title,
		String content) {

	public boolean match(String query) {
		return this.key.toLowerCase().contains(query.toLowerCase())
				|| this.title.toLowerCase().contains(query.toLowerCase())
				|| this.content.toLowerCase().contains(query.toLowerCase());
	}

}
