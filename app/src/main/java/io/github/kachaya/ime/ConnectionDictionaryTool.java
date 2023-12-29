/*
 * Copyright 2023 kachaya
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

package io.github.kachaya.ime;

import java.util.ArrayList;

public class ConnectionDictionaryTool extends DictionaryTool {

    public String getDefaultFileName() {
        return mDictionary.getConnectionDictionaryName() + ".txt";
    }

    public void importDictionary(ArrayList<String> entries) {
        mDictionary.importConnectionDictionary(entries);
    }

    public ArrayList<String> exportDictionary() {
        return mDictionary.exportConnectionDictionary();
    }
}
