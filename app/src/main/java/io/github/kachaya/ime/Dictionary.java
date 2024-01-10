/*
 * Copyright 2023-2024 kachaya
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
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class Dictionary implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Pattern punctuationPattern = Pattern.compile("[\\p{Punct}\\p{InCJK_SYMBOLS_AND_PUNCTUATION}]");
    private static final String BTREE_NAME = "btree_dic";
    private static final String SYSTEM_DIC_NAME = "system_dic";
    private static final String LEARNING_DIC_NAME = "learning_dic";
    private static final String CONNECTION_DIC_NAME = "connection_dic";
    private BTree mBTreeSystemDic;
    private RecordManager mRecmanLearningDic;
    private BTree mBTreeLearningDic;
    private RecordManager mRecmanConnectionDic;
    private BTree mBTreeConnectionDic;
    private int mSearchCounter;
    // 設定項目
    private boolean mConvertHalfkana;
    private int mSearchLimit = 50;

    public Dictionary(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mConvertHalfkana = sharedPreferences.getBoolean("convert_halfkana", false);

        copyFromResRaw(context);

        // 接続辞書
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/" + CONNECTION_DIC_NAME;
            mRecmanConnectionDic = RecordManagerFactory.createRecordManager(name, props);
            long recid = mRecmanConnectionDic.getNamedObject(BTREE_NAME);
            if (recid == 0) {
                mBTreeConnectionDic = BTree.createInstance(mRecmanConnectionDic, new StringComparator());
                mRecmanConnectionDic.setNamedObject(BTREE_NAME, mBTreeConnectionDic.getRecid());
                mRecmanConnectionDic.commit();
            } else {
                mBTreeConnectionDic = BTree.load(mRecmanConnectionDic, recid);
            }
        } catch (IOException e) {
            mRecmanConnectionDic = null;
            mBTreeConnectionDic = null;
        }
        // 学習辞書
        try {
            Properties props = new Properties();
            String name = context.getFilesDir().getAbsolutePath() + "/" + LEARNING_DIC_NAME;
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
            String name = context.getFilesDir().getAbsolutePath() + "/" + SYSTEM_DIC_NAME;
            RecordManager recman = RecordManagerFactory.createRecordManager(name, props);
            long recid = recman.getNamedObject(BTREE_NAME);
            mBTreeSystemDic = BTree.load(recman, recid);
        } catch (IOException e) {
            mBTreeSystemDic = null;
        }
    }

    public String getLearningDictionaryName() {
        return LEARNING_DIC_NAME;
    }

    public String getConnectionDictionaryName() {
        return CONNECTION_DIC_NAME;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) {
            return;
        }
        if (key.equals("convert_halfkana")) {
            mConvertHalfkana = sharedPreferences.getBoolean(key, false);
        }
    }

    private void copyFromResRaw(Context context) {
        final int BUFSIZE = 1024 * 1024;

        String filesDir = context.getFilesDir().getAbsolutePath();
        String dbFileName = filesDir + "/" + SYSTEM_DIC_NAME + ".db";
        File dbFile = new File(dbFileName);
        long dbFileLength = dbFile.length();

        InputStream is = context.getResources().openRawResource(R.raw.system_dic);

        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFSIZE];

        try {
            int len;
            while ((len = bis.read(buf, 0, BUFSIZE)) > 0) {
                baos.write(buf, 0, len);
            }
            int size = baos.size();
            if (size != dbFileLength) {
                byte[] data = baos.toByteArray();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dbFileName));
                bos.write(data, 0, size);
                bos.flush();
                bos.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 学習辞書内の完全一致する候補を返す
     *
     * @param key キー文字列
     * @return 候補
     */
    private String[] findLearningDic(String key) {
        ArrayList<String> list = new ArrayList<>();
        try {
            String value = (String) mBTreeLearningDic.find(key);
            if (value != null) {
                String[] words = value.split("\t");
                for (String word : words) {
                    list.add(key + "\t" + word);
                }
            }
        } catch (IOException ignored) {
        }
        return list.toArray(new String[0]);
    }

    /**
     * システム辞書内の完全一致する候補を返す
     *
     * @param key キー
     * @return 候補
     */
    private String[] findSystemDic(String key) {
        ArrayList<String> list = new ArrayList<>();
        try {
            String value = (String) mBTreeSystemDic.find(key);
            if (value != null) {
                String[] words = value.split("\t");
                for (String word : words) {
                    list.add(key + "\t" + word);
                }
            }
        } catch (IOException ignored) {
        }
        return list.toArray(new String[0]);
    }

    private String[] browseSystemDic(String key) {
        Tuple tuple = new Tuple();
        TupleBrowser browser;
        ArrayList<String> list = new ArrayList<>();
        try {
            browser = mBTreeSystemDic.browse(key);
            while (browser.getNext(tuple)) {
                String tupleKey = (String) tuple.getKey();
                if (!tupleKey.startsWith(key)) {
                    break;
                }
                if (tupleKey.length() > key.length() + 3) {
                    break;  // 補完する文字数制限 TODO:設定項目にする
                }
                String value = (String) tuple.getValue();
                if (value != null) {
                    String[] words = value.split("\t");
                    for (String word : words) {
                        list.add(key + "\t" + word);
                        mSearchCounter++;
                    }
                }
                if (mSearchCounter > mSearchLimit) {
                    break;
                }
            }
        } catch (IOException ignored) {
        }
        return list.toArray(new String[0]);
    }

    public Candidate[] search(String key) {
        mSearchCounter = 0;
        Set<String> set = new LinkedHashSet<>();
        String hiragana = Converter.romajiToHiragana(key);
        boolean hiraganaOnly = true;
        for (char ch : hiragana.toCharArray()) {
            if (ch < 0x80) {
                hiraganaOnly = false;    // ローマ字ひらがな変換後にASCII文字が残っている
                break;
            }
        }
        // 一致検索
        if (hiraganaOnly) {
            set.addAll(Arrays.asList(findLearningDic(hiragana)));
        }
        set.addAll(Arrays.asList(findLearningDic(key)));
        if (hiraganaOnly) {
            set.addAll(Arrays.asList(findSystemDic(hiragana)));
        }

        // 曖昧検索
        if (hiraganaOnly) {
            set.addAll(Arrays.asList(browseSystemDic(hiragana)));
        }

        // 辞書に無かったもの
        if (hiraganaOnly) {
            set.add(hiragana + "\t" + hiragana);
            set.add(hiragana + "\t" + Converter.toWideKatakana(hiragana));
            if (mConvertHalfkana) {
                set.add(hiragana + "\t" + Converter.toHalfKatakana(hiragana));
            }
        }
        set.add(key + "\t" + key);
        set.add(key + "\t" + Converter.toWideLatin(key));

        Candidate[] candidates = new Candidate[set.size()];
        int i = 0;
        for (String s : set) {
            String[] ss = s.split("\t");
            candidates[i++] = new Candidate(ss[0], ss[1]);
        }
        return candidates;
    }

    /**
     * 最後に確定した候補から予測した候補を返す
     *
     * @param lastCandidate 最後に確定した候補
     * @return 予測した候補
     */
    public Candidate[] predict(Candidate lastCandidate) {
        if (lastCandidate == null) {
            return null;
        }
        String key = lastCandidate.key + " " + lastCandidate.value;
        Set<String> set = new LinkedHashSet<>();
        try {
            String value = (String) mBTreeConnectionDic.find(key);
            if (value != null) {
                set.addAll(Arrays.asList(value.split("\t")));
            }
        } catch (IOException ignored) {
        }
        if (set.size() == 0) {
            return null;
        }
        Candidate[] candidates = new Candidate[set.size()];
        int i = 0;
        for (String s : set) {
            String[] ss = s.split(" ");
            candidates[i++] = new Candidate(ss[0], ss[1]);
        }
        return candidates;
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

    /**
     * 候補を連結して学習辞書に登録する
     *
     * @param left  左側候補
     * @param right 右側候補
     */
    public void addConcatenation(Candidate left, Candidate right) {
        if (left == null || right == null) {
            return;
        }
        if (left.key.length() < right.key.length()) {
            // 左側が短かければ連結登録しない
            return;
        }
        Matcher matcher;
        matcher = punctuationPattern.matcher(left.value + right.value);
        if (matcher.find()) {
            // 句読点を含むなら連結登録しない
            return;
        }
        if (!right.value.matches("^[ぁ-ゖー]+$")) {
            // 右側がひらがな以外を含むなら連結登録しない（助詞や語尾の連結を想定）
            return;
        }
        addLearning(left.key + right.key, left.value + right.value);
    }

    public void addLearning(String keyword, String word) {
        add(keyword, word, mRecmanLearningDic, mBTreeLearningDic);
    }

    public void addConnection(Candidate lastCandidate, Candidate followingCandidate) {
        if (lastCandidate == null || followingCandidate == null) {
            return;
        }
        Matcher matcher = punctuationPattern.matcher(lastCandidate.value);
        if (matcher.find()) {
            // 直前の語句が句読点を含むなら登録しない
            return;
        }
        String last = lastCandidate.key + " " + lastCandidate.value;
        String following = followingCandidate.key + " " + followingCandidate.value;
        add(last, following, mRecmanConnectionDic, mBTreeConnectionDic);
    }

    public void importDictionary(ArrayList<String> entries, RecordManager recman, BTree btree) {
        for (String entry : entries) {
            String[] ss = entry.split("\t");
            if (ss.length < 2) {
                continue;
            }
            String key = ss[0];
            for (int i = 1; i < ss.length; i++) {
                add(key, ss[i], recman, btree);
            }
        }
    }

    public void importLearningDictionary(ArrayList<String> entries) {
        importDictionary(entries, mRecmanLearningDic, mBTreeLearningDic);
    }

    public void importConnectionDictionary(ArrayList<String> entries) {
        importDictionary(entries, mRecmanConnectionDic, mBTreeConnectionDic);
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
        } catch (IOException ignored) {
        }
        return list;
    }

    public ArrayList<String> exportLearningDictionary() {
        return exportDictionary(mRecmanLearningDic, mBTreeLearningDic);
    }

    public ArrayList<String> exportConnectionDictionary() {
        return exportDictionary(mRecmanConnectionDic, mBTreeConnectionDic);
    }
}
