/*
 * Copyright (C) 2022 Beka Gozalishvili
 * Copyright (C) 2012-2013 Reece H. Dunn
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reecedunn.espeak;

import android.content.Context;
import android.util.Log;

import java.io.File;

public final class CheckVoiceData {
    private static final String TAG = "eSpeakTTS";

    /** Resources required for eSpeak to run correctly. */
    private static final String[] BASE_RESOURCES = {
        "version",
        "intonations",
        "phondata",
        "phonindex",
        "phontab",
        "en_dict",
    };

    public static File getDataPath(Context context) {
        return new File(context.getDir("voices", Context.MODE_PRIVATE), "espeak-ng-data");
    }

    public static boolean hasBaseResources(Context context) {
        final File dataPath = getDataPath(context);

        for (String resource : BASE_RESOURCES) {
            final File resourceFile = new File(dataPath, resource);

            if (!resourceFile.exists()) {
                Log.e(TAG, "Missing base resource: " + resourceFile.getPath());
                return false;
            }
        }

        return true;
    }

    public static boolean canUpgradeResources(Context context) {
        try {
            final String version = FileUtils.read(context.getResources().openRawResource(R.raw.espeakdata_version));
            final String installedVersion = FileUtils.read(new File(getDataPath(context), "version"));
            return !version.equals(installedVersion);
        } catch (Exception e) {
            return false;
        }
    }
}
