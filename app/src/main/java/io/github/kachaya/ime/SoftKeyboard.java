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
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

public class SoftKeyboard extends InputMethodService {

    private View mInputView;
    private View mCandidateView;
    private ViewGroup mCandidateLayout;
    private StrokeView mStrokeView;
    private QwertyView mQwertyView;
    private SymbolView mSymbolView;
    private String mKeyboardLayout;

    private StringBuilder mInputText;
    private boolean isPrediction;
    private int mCandidateIndex;
    private Dictionary mDictionary;
    private Candidate[] mCandidates;
    private Candidate mLastCandidate;

    @Override
    public void onCreate() {
        super.onCreate();
        mDictionary = new Dictionary(this);
        mInputText = new StringBuilder();
    }

    @Override
    public View onCreateInputView() {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.input_layout, null);
        mInputView = layout;

        mCandidateView = layout.findViewById(R.id.candidate_view);
        mCandidateLayout = layout.findViewById(R.id.candidate_layout);

        mStrokeView = layout.findViewById(R.id.stroke_view);
        mQwertyView = layout.findViewById(R.id.qwerty_view);
        mSymbolView = layout.findViewById(R.id.symbol_view);

        return mInputView;
    }

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mKeyboardLayout = sharedPreferences.getString("keyboard_layout", "qwerty");
        switch (mKeyboardLayout) {
            default:
            case "qwerty":
                mQwertyView.setVisibility(View.VISIBLE);
                mStrokeView.setVisibility(View.INVISIBLE);
                mSymbolView.setVisibility(View.INVISIBLE);
                break;
            case "stroke":
                mQwertyView.setVisibility(View.INVISIBLE);
                mStrokeView.setVisibility(View.VISIBLE);
                mSymbolView.setVisibility(View.INVISIBLE);
                break;
        }
        mInputText.setLength(0);
        mCandidateLayout.removeAllViewsInLayout();
        mCandidateIndex = -1;
    }

    private void icSetComposingText(CharSequence cs) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText(cs, 1);
        }
    }

    private void icCommitText(CharSequence cs) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(cs, 1);
        }
    }

    // 入力中テキストをコミット
    private void commitInputText() {
        icCommitText(mInputText.toString());
        mInputText.setLength(0);
        mCandidateLayout.removeAllViewsInLayout();
        mCandidateIndex = -1;
    }

    /**
     * 選択中の候補をコミット
     */
    private void commitCandidateText() {
        Candidate candidate = mCandidates[mCandidateIndex];
        icCommitText(candidate.value);
        mInputText.setLength(0);
        mCandidateLayout.removeAllViewsInLayout();
        mCandidateIndex = -1;

        if (isPrediction) {
            mDictionary.addPrediction(candidate.key, candidate.value);
        } else {
            mDictionary.addLearning(candidate.key, candidate.value);
            if (mLastCandidate != null) {
                mDictionary.addPrediction(mLastCandidate.value, candidate.value);
                // 連接したものを学習
                mDictionary.addConnection(mLastCandidate, candidate);
            }
        }
        mLastCandidate = candidate;
        buildPredictionCandidate();
    }

    /**
     * 文字列キー処理
     *
     * @param s 文字列
     */
    public void handleString(String s) {
        if (mCandidateIndex >= 0) {
            commitCandidateText();
        }
        mInputText.append(s);
        icSetComposingText(mInputText);
        buildConversionCandidate();
    }

    /**
     * 文字キー処理
     *
     * @param c 文字コード
     */
    public void handleCharacter(char c) {
        if (mCandidateIndex >= 0) {
            commitCandidateText();
        }
        mInputText.append(c);
        icSetComposingText(mInputText);
        buildConversionCandidate();
    }

    /**
     * Enterキー処理
     */
    public void handleEnter() {
        mLastCandidate = null;  // 続く入力を連接させない
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
            return;
        }
        if (mCandidateIndex >= 0) {
            // 候補選択中→選択中の候補をコミット
            commitCandidateText();
        } else {
            // 候補未選択→入力テキストをそのままコミット
            commitInputText();
            icCommitText(mInputText);
            mInputText.setLength(0);
            mCandidateLayout.removeAllViewsInLayout();
            mCandidateIndex = -1;
        }
    }

    /**
     * スペースキー処理
     */
    public void handleSpace() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE);
            return;
        }
        mCandidateIndex = (mCandidateIndex + 1) % mCandidateLayout.getChildCount();
        selectCandidate();
    }

    public void handleBackspace() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            return;
        }
        if (mCandidateIndex >= 0) {
            // 候補選択中→候補未選択に戻す
            mCandidateIndex = -1;
            selectCandidate();
            return;
        }
        // 候補未選択→入力テキストの最後の文字を削除して候補を作り直す
        mInputText.deleteCharAt(mInputText.length() - 1);
        icSetComposingText(mInputText);
        if (mInputText.length() == 0) {
            buildPredictionCandidate();
        } else {
            buildConversionCandidate();
        }
        icSetComposingText(mInputText);
    }

    public void handleCursorLeft() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
        }
    }

    public void handleCursorRight() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
    }

    public void handleCursorUp() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_UP);
        }
    }

    public void handleCursorDown() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_DOWN);
        }
    }

    /**
     * シンボルキーボードに切り替え
     */
    public void handleSymbol() {
        if (mInputText.length() > 0) {
            commitInputText();
        }
        mQwertyView.setVisibility(View.INVISIBLE);
        mStrokeView.setVisibility(View.INVISIBLE);
        mSymbolView.setVisibility(View.VISIBLE);
    }

    /**
     * テキストキーボードに切り替え
     */
    public void handleKeyboard() {
        if (mInputText.length() > 0) {
            commitInputText();
        }
        switch (mKeyboardLayout) {
            default:
            case "qwerty":
                mQwertyView.setVisibility(View.VISIBLE);
                mStrokeView.setVisibility(View.INVISIBLE);
                mSymbolView.setVisibility(View.INVISIBLE);
                break;
            case "stroke":
                mQwertyView.setVisibility(View.INVISIBLE);
                mStrokeView.setVisibility(View.VISIBLE);
                mSymbolView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * 現在入力中の文字列から変換候補を作り出す
     */
    private void buildConversionCandidate() {
        isPrediction = false;
        mCandidates = mDictionary.search(mInputText.toString());
        setCandidateText();
    }

    /**
     * 最後に確定した候補から予測候補を作り出す
     */
    private void buildPredictionCandidate() {
        isPrediction = true;
        mCandidates = mDictionary.predict(mLastCandidate);
        setCandidateText();
    }

    private void onClickCandidateTextListener(View view) {
        mCandidateIndex = mCandidateLayout.indexOfChild(view);
        commitCandidateText();
    }

    /**
     * 候補ビューに候補一覧を表示する
     */
    private void setCandidateText() {
        mCandidateIndex = -1;
        mCandidateLayout.removeAllViewsInLayout();
        mCandidateView.scrollTo(0, 0);
        if (mCandidates == null) {
            return;
        }
        int style = R.style.CandidateText;
        Context context = new ContextThemeWrapper(this, style);
        for (Candidate candidate : mCandidates) {
            TextView view = new TextView(context, null, style);
            view.setText(candidate.value);    // 表示用テキスト
            view.setOnClickListener(this::onClickCandidateTextListener);
            mCandidateLayout.addView(view);
        }
    }

    private void selectCandidate() {
        TextView view;
        int cX = mCandidateView.getScrollX();
        int cW = mCandidateView.getWidth();
        for (int i = 0; i < mCandidateLayout.getChildCount(); i++) {
            view = (TextView) mCandidateLayout.getChildAt(i);
            if (i == mCandidateIndex) {
                // 見える場所にスクロールする
                int bT = view.getTop();
                int bL = view.getLeft();
                int bR = view.getRight();
                if (bL < cX) {
                    mCandidateView.scrollTo(bL, bT);
                }
                if (bR > (cX + cW)) {
                    mCandidateView.scrollTo(bR - cW, bT);
                }
                view.setSelected(true);
                icSetComposingText(view.getText());
            } else {
                view.setSelected(false);
            }
        }
    }
}
