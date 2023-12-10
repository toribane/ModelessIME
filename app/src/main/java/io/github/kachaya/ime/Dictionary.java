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

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class Dictionary {

    static String BTREE_NAME = "btree_dic";
    private final Context mContext;
    private BTree mBTreeSystemDic;
    private RecordManager mRecmanLearningDic;
    private BTree mBTreeLearningDic;
    private RecordManager mRecmanPredictionDic;
    private BTree mBTreePredictionDic;

    public Dictionary(Context context) {
        mContext = context;
        extractZip(context, "system_dic.zip");

        // 予測辞書
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/prediction_dic";
            mRecmanPredictionDic = RecordManagerFactory.createRecordManager(name, props);
            long recid = mRecmanPredictionDic.getNamedObject(BTREE_NAME);
            if (recid == 0) {
                mBTreePredictionDic = BTree.createInstance(mRecmanPredictionDic, new StringComparator());
                mRecmanPredictionDic.setNamedObject(BTREE_NAME, mBTreePredictionDic.getRecid());
                mRecmanPredictionDic.commit();
            } else {
                mBTreePredictionDic = BTree.load(mRecmanPredictionDic, recid);
            }
        } catch (IOException e) {
            mRecmanPredictionDic = null;
            mBTreePredictionDic = null;
        }

        // 学習辞書
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/learning_dic";
            mRecmanLearningDic = RecordManagerFactory.createRecordManager(name, props);
            long recid = mRecmanLearningDic.getNamedObject(BTREE_NAME);
            if (recid == 0) {
                mBTreeLearningDic = BTree.createInstance(mRecmanLearningDic, new StringComparator());
                mRecmanLearningDic.setNamedObject(BTREE_NAME, mBTreeLearningDic.getRecid());
                mRecmanLearningDic.commit();
            } else {
                mBTreeLearningDic = BTree.load(mRecmanLearningDic, recid);
            }
        } catch (IOException e) {
            mRecmanLearningDic = null;
            mBTreeLearningDic = null;
        }
        // システム辞書
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/system_dic";
            RecordManager recman = RecordManagerFactory.createRecordManager(name, props);
            long recid = recman.getNamedObject(BTREE_NAME);
            mBTreeSystemDic = BTree.load(recman, recid);
        } catch (IOException e) {
            mBTreeSystemDic = null;
        }
    }

    private void extractZip(@NonNull Context context, String zipFileName) {
        try {
            InputStream is = context.getAssets().open(zipFileName);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String fn = context.getFilesDir().getAbsolutePath() + "/" + ze.getName();
                File f = new File(fn);
                boolean skip = true;
                if (f.exists()) {
                    if (f.length() != ze.getSize()) {
                        skip = false;
                    }
                } else {
                    skip = false;
                }
                if (skip) {
                    continue;
                }
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fn));
                byte[] buf = new byte[16 * 1024];
                int size;
                while ((size = zis.read(buf, 0, buf.length)) > 0) {
                    bos.write(buf, 0, size);
                }
                bos.flush();
                bos.close();
            }
        } catch (IOException ignored) {
        }
    }

    public ArrayList<String> search(String keyword, int limit) {
        ArrayList<String> list = new ArrayList<>();
        String hiragana = Converter.romajiToHiragana(keyword);
        Tuple tuple = new Tuple();
        TupleBrowser browser;
        String pair;
        int num = 0;

        ArrayList<String> keys = new ArrayList<>();
        keys.add(hiragana);
        // 学習辞書の完全一致
        for (String key : keys) {
            try {
                String value = (String) mBTreeLearningDic.find(key);
                if (value != null) {
                    String[] ss = value.split("\t");
                    for (String s : ss) {
                        pair = key + "\t" + s;
                        if (list.contains(pair)) {
                            continue;
                        }
                        list.add(pair);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        // システム辞書の完全一致
        for (String key : keys) {
            try {
                String value = (String) mBTreeSystemDic.find(key);
                if (value != null) {
                    String[] ss = value.split("\t");
                    for (String s : ss) {
                        pair = key + "\t" + s;
                        if (list.contains(pair)) {
                            continue;
                        }
                        list.add(pair);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        // 直接変換
        for (String key : keys) {
            pair = key + "\t" + key;
            if (!list.contains(pair)) {
                list.add(pair);
            }
            pair = key + "\t" + Converter.toWideKatakana(key);
            if (!list.contains(pair)) {
                list.add(pair);
            }
            pair = key + "\t" + Converter.toHalfKatakana(key);
            if (!list.contains(pair)) {
                list.add(pair);
            }
        }
        pair = keyword + "\t" + Converter.toWideLatin(keyword); // 元のアルファベットから
        if (!list.contains(pair)) {
            list.add(pair);
        }
        // システム辞書の曖昧検索、制限あり
        char lastChar = Character.toLowerCase(hiragana.charAt(hiragana.length() - 1));
        if ("bcdfghjklmnpqrstvwxyz".indexOf(lastChar) >= 0) {
            keys.add(Converter.romajiToHiragana(keyword + "a"));
            keys.add(Converter.romajiToHiragana(keyword + "i"));
            keys.add(Converter.romajiToHiragana(keyword + "u"));
            keys.add(Converter.romajiToHiragana(keyword + "e"));
            keys.add(Converter.romajiToHiragana(keyword + "o"));
            if (lastChar == 'n') {
                keys.add(Converter.romajiToHiragana(keyword + "'"));
            }
        }
        for (String key : keys) {
            try {
                browser = mBTreeSystemDic.browse(key);
                while (browser.getNext(tuple)) {
                    String tupleKey = (String) tuple.getKey();
                    if (!tupleKey.startsWith(key)) {
                        break;
                    }
                    String value = (String) tuple.getValue();
                    String[] ss = value.split("\t");
                    for (String s : ss) {
                        pair = tupleKey + "\t" + s;
                        if (list.contains(pair)) {
                            continue;
                        }
                        list.add(pair);
                        num++;
                        if (num > limit) {
                            break;
                        }
                    }
                    if (num > limit) {
                        break;
                    }
                }
                if (num > limit) {
                    break;
                }
            } catch (IOException ignored) {
            }
        }

        return list;
    }

    public ArrayList<String> predict(String keyword) {
        ArrayList<String> list = new ArrayList<>();
        if (mRecmanPredictionDic == null || mBTreePredictionDic == null) {
            return list;
        }
        if (keyword.length() == 0) {
            return list;
        }
        try {
            String value = (String) mBTreePredictionDic.find(keyword);
            if (value != null) {
                String[] ss = value.split("\t");
                for (String s : ss) {
                    String pair = keyword + "\t" + s;
                    if (list.contains(pair)) {
                        continue;
                    }
                    list.add(pair);
                }
            }
        } catch (IOException ignored) {
        }
        return list;
    }

    public void addLearning(String keyword, String word) {
        String hiragana = Converter.romajiToHiragana(keyword);
        add(hiragana, word, mRecmanLearningDic, mBTreeLearningDic);
    }

    private void add(String keyword, String word, RecordManager recman, BTree btree) {
        if (recman == null || btree == null) {
            return;
        }
        if (keyword.length() == 0 || word.length() == 0) {
            return;
        }
        try {
            String value = (String) btree.find(keyword);
            if (value == null) {
                btree.insert(keyword, word, true);
            } else {
                StringBuilder sb = new StringBuilder(word);
                String[] ss = value.split("\t");
                for (String s : ss) {
                    if (s.equals(word)) {
                        continue;
                    }
                    sb.append("\t").append(s);
                }
                btree.insert(keyword, sb.toString(), true);
            }
            recman.commit();
        } catch (IOException ignored) {
        }
    }

    public void addPrediction(String keyword, String word) {
        add(keyword, word, mRecmanPredictionDic, mBTreePredictionDic);
    }

    public void importLearning(String entry) {
        importDictionary(entry, mRecmanLearningDic, mBTreeLearningDic);
    }

    private void importDictionary(String entry, RecordManager recman, BTree btree) {
        String[] ss = entry.split("\t");
        if (ss.length < 2) {
            return;
        }
        String keyword = ss[0];
        for (int i = 1; i < ss.length; i++) {
            add(keyword, ss[i], recman, btree);
        }
    }

    public void importPrediction(String entry) {
        importDictionary(entry, mRecmanPredictionDic, mBTreePredictionDic);
    }

    public ArrayList<String> exportLearning() {
        return exportDictionary(mRecmanLearningDic, mBTreeLearningDic);
    }

    private ArrayList<String> exportDictionary(RecordManager recman, BTree btree) {
        ArrayList<String> list = new ArrayList<>();
        Tuple tuple = new Tuple();
        try {
            recman.commit();
            TupleBrowser browser = btree.browse();
            while (browser.getNext(tuple)) {
                list.add(tuple.getKey() + "\t" + tuple.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public ArrayList<String> exportPrediction() {
        return exportDictionary(mRecmanPredictionDic, mBTreePredictionDic);
    }

}
