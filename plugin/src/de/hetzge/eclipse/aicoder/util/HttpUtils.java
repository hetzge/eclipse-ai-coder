package de.hetzge.eclipse.aicoder.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import mjson.Json;

public final class HttpUtils {
	private HttpUtils() {
	}

	public static void writeRequestBody(final HttpURLConnection connection, final Json json) throws IOException {
		try (OutputStream outputStream = connection.getOutputStream()) {
			outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	public static String readResponseBody(final HttpURLConnection connection) throws IOException {
		try (InputStream inputStream = connection.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public static String readErrorResponseBody(final HttpURLConnection connection) throws IOException {
		try (InputStream inputStream = connection.getErrorStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
