/*
 * Load a symbol-to-id mapping from assets and format phoneme/id pairs.
 */

package com.reecedunn.espeak;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PhonemeMapper {
    private final Map<String, Integer> symbolToId;

    private PhonemeMapper(Map<String, Integer> symbolToId) {
        this.symbolToId = symbolToId;
    }

    public static PhonemeMapper fromAssets(Context context, String assetName) throws IOException {
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(context.getAssets().open(assetName));
            String json = readAll(stream);
            JSONObject root = new JSONObject(json);
            JSONObject symbols = root.getJSONObject("symbol_to_id");

            Map<String, Integer> map = new HashMap<String, Integer>();
            Iterator<String> keys = symbols.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, symbols.getInt(key));
            }
            return new PhonemeMapper(map);
        } catch (Exception e) {
            throw new IOException("Failed to load mapping", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore close failures.
                }
            }
        }
    }

    public String formatPairs(String phonemeString) {
        if (phonemeString == null || phonemeString.length() == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (index < phonemeString.length()) {
            int codePoint = phonemeString.codePointAt(index);
            String symbol = new String(Character.toChars(codePoint));
            Integer id = symbolToId.get(symbol);
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(displaySymbol(symbol)).append(" -> ").append(id == null ? -1 : id.intValue());
            index += Character.charCount(codePoint);
        }
        return builder.toString();
    }

    public static String formatWithMarkers(String phonemeString) {
        if (phonemeString == null || phonemeString.length() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('^');
        builder.append('_');
        int index = 0;
        boolean first = true;
        while (index < phonemeString.length()) {
            int codePoint = phonemeString.codePointAt(index);
            if (!first) {
                builder.append('_');
            }
            builder.appendCodePoint(codePoint);
            first = false;
            index += Character.charCount(codePoint);
        }
        builder.append('_');
        builder.append('$');
        return builder.toString();
    }

    private static String displaySymbol(String symbol) {
        if (" ".equals(symbol)) {
            return "[space]";
        }
        if ("\n".equals(symbol)) {
            return "[newline]";
        }
        if ("\t".equals(symbol)) {
            return "[tab]";
        }
        return symbol;
    }

    private static String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString("UTF-8");
    }
}
