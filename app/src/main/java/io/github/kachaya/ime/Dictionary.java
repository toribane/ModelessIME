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
import android.util.Log;

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
    private BTree mBTreeSystemDic;
    private RecordManager mRecmanLearningDic;
    private BTree mBTreeLearningDic;
    private final ArrayList<BTree> dicList = new ArrayList<>();

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

    public Dictionary(Context context) {
        extractZip(context, "system_dic.zip");
        // 優先する辞書の順にdicListに追加する
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/learning_dic";
            mRecmanLearningDic = RecordManagerFactory.createRecordManager(name, props);
            long recid = mRecmanLearningDic.getNamedObject(BTREE_NAME);
            if (recid == 0) {
                mBTreeLearningDic = BTree.createInstance(mRecmanLearningDic, new StringComparator());
                mRecmanLearningDic.setNamedObject(BTREE_NAME, mBTreeLearningDic.getRecid());
                mRecmanLearningDic.commit();
                Log.i(getClass().getName(), "create learning dictoinary");
            } else {
                mBTreeLearningDic = BTree.load(mRecmanLearningDic, recid);
                Log.i(getClass().getName(), "load learning dictoinary");
            }
            dicList.add(mBTreeLearningDic);
        } catch (IOException e) {
            mRecmanLearningDic = null;
            mBTreeLearningDic = null;
        }
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/system_dic";
            RecordManager recman = RecordManagerFactory.createRecordManager(name, props);
            long recid = recman.getNamedObject(BTREE_NAME);
            mBTreeSystemDic = BTree.load(recman, recid);
            dicList.add(mBTreeSystemDic);
        } catch (IOException e) {
            mBTreeSystemDic = null;
        }
    }

    public ArrayList<String> search(@NonNull CharSequence cs, int limit) {
        String keyword = cs.toString();
        String hiragana = Converter.romajiToHiragana(keyword);
        Tuple tuple = new Tuple();
        TupleBrowser browser;
        ArrayList<String> list = new ArrayList<>();
        String pair;
        int num = 0;
        for (BTree btree : dicList) {
            try {
                browser = btree.browse(hiragana);
                while (browser.getNext(tuple)) {
                    String key = (String) tuple.getKey();
                    if (!key.startsWith(hiragana)) {
                        break;
                    }
                    String value = (String) tuple.getValue();
                    String[] ss = value.split("\t");
                    for (String s : ss) {
                        pair = key + "\t" + s;
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
            } catch (IOException ignored) {
            }
        }

        pair = hiragana + "\t" + hiragana;
        if (!list.contains(pair)) {
            list.add(pair);
        }
        pair = hiragana + "\t" + Converter.toWideKatakana(hiragana);
        if (!list.contains(pair)) {
            list.add(pair);
        }
        pair = hiragana + "\t" + Converter.toHalfKatakana(hiragana);
        if (!list.contains(pair)) {
            list.add(pair);
        }
        pair = keyword + "\t" + Converter.toWideLatin(keyword);
        if (!list.contains(pair)) {
            list.add(pair);
        }
        return list;
    }

    public void addLearning(String keyword, String word) {
        if (mBTreeLearningDic == null) {
            return;
        }
        String hiragana = Converter.romajiToHiragana(keyword);
        try {
            String value = (String) mBTreeLearningDic.find(hiragana);
            if (value == null) {
                mBTreeLearningDic.insert(hiragana, word, true);
            } else {
                String[] ss = value.split("\t");
                StringBuilder sb = new StringBuilder(word);
                for (String s : ss) {
                    if (s.equals(word)) {
                        continue;
                    }
                    sb.append("\t").append(s);
                }
                mBTreeLearningDic.insert(hiragana, sb.toString(), true);
            }
            mRecmanLearningDic.commit();
        } catch (IOException ignored) {
        }
    }
}
