package de.hetzge.eclipse.aicoder;

public final class JdkUtils {
	private static final String[] JRE_PACKAGE_PREFIXES = {
			"java.",
			"javax.",
			"sun.",
			"com.sun.",
			"org.w3c.",
			"org.xml.",
			"org.omg.",
			"jdk."
	};

	private JdkUtils() {
	}

	public static boolean isJREPackage(String name) {
		for (final String prefix : JRE_PACKAGE_PREFIXES) {
			if (name.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
