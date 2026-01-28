/*
 * Synchronous phoneme-id conversion API for eSpeak NG.
 */

package com.reecedunn.espeak;

import android.content.Context;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class EspeakPhonemizer {
    private static final String DEFAULT_LANGUAGE = "en-us";

    private EspeakPhonemizer() {
    }

    /**
     * Convert text to phoneme ids using eSpeak NG (IPA + VITS mapper).
     *
     * @param context Caller context.
     * @param text Input text.
     * @param language Language code, defaulting to "en-us".
     */
    public static int[] phonemizeEspeak(Context context, String text, String language) throws IOException {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        if (text == null || text.length() == 0) {
            return new int[0];
        }

        Context appContext = context.getApplicationContext();
        Context storageContext = getStorageContext(appContext);

        ensureVoiceData(storageContext);

        SpeechSynthesis synthesis = new SpeechSynthesis(storageContext, null);
        if (!synthesis.isInitialized()) {
            throw new IOException("Failed to initialize SpeechSynthesis.");
        }

        String voice = normalizeLanguage(language);
        if (!DEFAULT_LANGUAGE.equals(voice) && !"en".equals(voice)) {
            voice = DEFAULT_LANGUAGE;
        }
        if (!synthesis.setVoiceByName(voice)) {
            synthesis.setVoiceByName(DEFAULT_LANGUAGE);
            synthesis.setVoiceByName("en");
        }

        String phonemes = convertPreservingPunctuation(synthesis, text, true);
        if (phonemes == null) {
            throw new IOException("Phoneme conversion failed.");
        }

        String formatted = PhonemeMapper.formatWithMarkers(phonemes);
        PhonemeMapper mapper = PhonemeMapper.fromAssets(appContext, "vitsMapper.json");
        return mapper.mapToIds(formatted);
    }

    public static int[] phonemizeEspeak(Context context, String text) throws IOException {
        return phonemizeEspeak(context, text, DEFAULT_LANGUAGE);
    }

    private static Context getStorageContext(Context appContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return appContext.createDeviceProtectedStorageContext();
        }
        return appContext;
    }

    private static String normalizeLanguage(String language) {
        if (language == null) {
            return DEFAULT_LANGUAGE;
        }
        String trimmed = language.trim();
        if (trimmed.length() == 0) {
            return DEFAULT_LANGUAGE;
        }
        return trimmed.toLowerCase(Locale.US);
    }

    private static void ensureVoiceData(Context storageContext) throws IOException {
        if (CheckVoiceData.hasBaseResources(storageContext)
                && !CheckVoiceData.canUpgradeResources(storageContext)) {
            return;
        }
        installVoiceData(storageContext);
    }

    private static void installVoiceData(Context storageContext) throws IOException {
        File dataPath = CheckVoiceData.getDataPath(storageContext);
        File outputDir = dataPath.getParentFile();

        FileUtils.rmdir(dataPath);

        InputStream rawStream = storageContext.getResources().openRawResource(R.raw.espeakdata);
        ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(rawStream));
        try {
            byte[] buffer = new byte[10240];
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                File outputFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs();
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    try {
                        int bytesRead;
                        while ((bytesRead = zipStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        outputStream.close();
                    }
                }
                zipStream.closeEntry();
            }

            String version = FileUtils.read(storageContext.getResources().openRawResource(R.raw.espeakdata_version));
            File versionFile = new File(outputDir, "espeak-ng-data/version");
            FileUtils.write(versionFile, version);
        } finally {
            try {
                zipStream.close();
            } catch (IOException e) {
                // Ignore close failures.
            }
        }
    }

    private static String convertPreservingPunctuation(SpeechSynthesis synthesis, String text, boolean useIpa) {
        StringBuilder output = new StringBuilder();
        StringBuilder word = new StringBuilder();
        boolean lastWasSpace = false;

        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            if (Character.isLetterOrDigit(codePoint) || codePoint == '\'') {
                word.appendCodePoint(codePoint);
                lastWasSpace = false;
            } else if (Character.isWhitespace(codePoint)) {
                if (word.length() > 0) {
                    String phonemes = synthesis.textToPhonemes(word.toString(), useIpa);
                    if (phonemes == null) {
                        return null;
                    }
                    output.append(phonemes);
                    word.setLength(0);
                }
                if (!lastWasSpace && output.length() > 0) {
                    output.append(' ');
                    lastWasSpace = true;
                }
            } else {
                if (word.length() > 0) {
                    String phonemes = synthesis.textToPhonemes(word.toString(), useIpa);
                    if (phonemes == null) {
                        return null;
                    }
                    output.append(phonemes);
                    word.setLength(0);
                }
                output.appendCodePoint(codePoint);
                lastWasSpace = false;
            }
            index += Character.charCount(codePoint);
        }

        if (word.length() > 0) {
            String phonemes = synthesis.textToPhonemes(word.toString(), useIpa);
            if (phonemes == null) {
                return null;
            }
            output.append(phonemes);
        }

        return output.toString();
    }
}
