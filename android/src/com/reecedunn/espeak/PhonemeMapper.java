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
        if (phonemeString == null) {
            return "";
        }
        String trimmed = phonemeString.trim();
        if (trimmed.length() == 0) {
            return "";
        }

        String[] tokens = trimmed.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.length() == 0) {
                continue;
            }
            Integer id = symbolToId.get(token);
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(token).append(" -> ").append(id == null ? -1 : id.intValue());
        }
        return builder.toString();
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
