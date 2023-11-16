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
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class Dictionary {

    static String BTREE_NAME = "btree_dic";
    private BTree mBTreeSystemDic;

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

    public ArrayList<String> search(@NonNull CharSequence cs, int limit) {
        String keyword = cs.toString();
        String hiragana = Converter.romajiToHiragana(keyword);
        Tuple tuple = new Tuple();
        TupleBrowser browser;
        ArrayList<String> list = new ArrayList<>();

        // TODO: 学習したものを先頭に入れる
        if (!list.contains(hiragana)) {
            list.add(hiragana);
        }
        String katakana = Converter.toWideKatakana(hiragana);
        if (!list.contains(katakana)) {
            list.add(katakana);
        }
//        String halfkana = Converter.toHalfKatakana(hiragana);
//        if (!list.contains(halfkana)) {
//            list.add(halfkana);
//        }
        // 住所入力等で全角英数字を強制されることがある
        String wideLatin = Converter.toWideLatin(keyword);
        if (!list.contains(wideLatin)) {
            list.add(wideLatin);
        }

        int num = 0;
        try {
            browser = mBTreeSystemDic.browse(hiragana);
            while (browser.getNext(tuple)) {
                if (!((String) tuple.getKey()).startsWith(hiragana)) {
                    break;
                }
                String value = (String) tuple.getValue();
                String[] ss = value.split("\t");
                for (String s : ss) {
                    if (list.contains(s)) {
                        continue;
                    }
                    list.add(s);
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
        return list;
    }
}
