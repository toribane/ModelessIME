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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class PredictionDictionaryTool extends AppCompatActivity {
    private final static String DEFAULT_FILENAME = "prediction_dic.txt";
    private Dictionary mDictionary;
    ActivityResultLauncher<Intent> exportResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent resultData = result.getData();
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        try {
                            OutputStream outputStream = getContentResolver().openOutputStream(uri);
                            BufferedWriter writer = new BufferedWriter((new OutputStreamWriter(outputStream)));
                            ArrayList<String> entryList = mDictionary.exportPrediction();
                            for (String entry : entryList) {
                                writer.write(entry + "\n");
                            }
                            writer.flush();
                            writer.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
    private ArrayAdapter<String> mAdapter;
    ActivityResultLauncher<Intent> importResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent resultData = result.getData();
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            BufferedReader reader = new BufferedReader((new InputStreamReader(inputStream)));
                            String entry;
                            while ((entry = reader.readLine()) != null) {
                                mDictionary.importPrediction(entry);
                            }
                            reader.close();
                            buildList();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dictionary_tool);
        mDictionary = new Dictionary(this);

        Button exportButton = findViewById(R.id.export_button);
        exportButton.setOnClickListener(this::exportDictionary);
        Button importButton = findViewById(R.id.import_button);
        importButton.setOnClickListener(this::importDictionary);

        ListView listView = findViewById(R.id.list_view);
        listView.setEmptyView(findViewById(R.id.empty_text));

        ArrayList<String> dataList = new ArrayList<>();

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(mAdapter);
        buildList();
    }

    private void exportDictionary(View v) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, DEFAULT_FILENAME);
        exportResultLauncher.launch(intent);
    }

    private void importDictionary(View v) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DEFAULT_FILENAME);
        importResultLauncher.launch(intent);
    }

    private void buildList() {
        mAdapter.clear();
        ArrayList<String> entryList = mDictionary.exportPrediction();
        for (String entry : entryList) {
            mAdapter.add(entry);
        }
        mAdapter.notifyDataSetChanged();
    }
}
