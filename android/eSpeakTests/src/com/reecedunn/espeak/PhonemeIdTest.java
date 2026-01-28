/*
 * Instrumentation test to validate phoneme-id mapping for a known sentence.
 */

package com.reecedunn.espeak;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PhonemeIdTest {
    private static final String TEST_TEXT =
            "The crime, long carried on without detection, is now exposed.";

    private static final int[] EXPECTED_IDS = new int[] {
            1, 0, 41, 0, 59, 0, 3, 0, 23, 0, 88, 0, 120, 0, 14, 0, 74, 0,
            25, 0, 8, 0, 3, 0, 24, 0, 120, 0, 54, 0, 44, 0, 3, 0, 23, 0, 120,
            0, 39, 0, 88, 0, 21, 0, 17, 0, 3, 0, 120, 0, 54, 0, 26, 0, 3,
            0, 35, 0, 74, 0, 41, 0, 120, 0, 14, 0, 100, 0, 32, 0, 3, 0, 17,
            0, 74, 0, 32, 0, 120, 0, 61, 0, 23, 0, 96, 0, 59, 0, 26, 0, 8,
            0, 3, 0, 74, 0, 38, 0, 3, 0, 26, 0, 120, 0, 14, 0, 100, 0, 3,
            0, 61, 0, 23, 0, 31, 0, 28, 0, 120, 0, 27, 0, 100, 0, 38, 0,
            17, 0, 10, 0, 2
    };

    @Test
    public void testPhonemeIdsForSentence() throws Exception {
        int[] actualIds = EspeakPhonemizer.phonemizeEspeak(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                TEST_TEXT,
                "en-us");
        assertTrue("Phoneme conversion returned no ids.", actualIds.length > 0);

        assertEquals("Phoneme id length mismatch.", EXPECTED_IDS.length, actualIds.length);
        for (int i = 0; i < EXPECTED_IDS.length; i++) {
            assertEquals("Id mismatch at index " + i, EXPECTED_IDS[i], actualIds[i]);
        }
    }
}
