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

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

public class SoftKeyboard extends InputMethodService {
    private View mInputView;
    private StrokeView mStrokeView;
    private QwertyView mQwertyView;
    private View mCandidateView;
    private ViewGroup mCandidateLayout;

    private final StringBuilder mInputText = new StringBuilder();
    private String mLastCommit = "";
    private int mCandidateIndex = -1;

    private Dictionary mDictionary;

    @Override
    public void onCreate() {
        super.onCreate();
        mDictionary = new Dictionary(this);
    }

    /**
     * Create and return the view hierarchy used for the input area (such as
     * a soft keyboard).  This will be called once, when the input area is
     * first displayed.  You can return null to have no input area; the default
     * implementation returns null.
     *
     * <p>To control when the input view is displayed, implement
     * {@link #onEvaluateInputViewShown()}.
     * To change the input view after the first one is created by this
     * function, use {@link #setInputView(View)}.
     */
    @Override
    public View onCreateInputView() {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.input_layout, null);
        mInputView = layout;

        mCandidateView = layout.findViewById(R.id.candidate_view);
        mCandidateLayout = layout.findViewById(R.id.candidate_layout);

        mStrokeView = layout.findViewById(R.id.stroke_view);
        mQwertyView = layout.findViewById(R.id.qwerty_view);

        return mInputView;
    }

    /**
     * Called when the input view is being shown and input has started on
     * a new editor.  This will always be called after {@link #onStartInput},
     * allowing you to do your general setup there and just view-specific
     * setup here.  You are guaranteed that {@link #onCreateInputView()} will
     * have been called some time before this function is called.
     *
     * @param editorInfo Description of the type of text being edited.
     * @param restarting Set to true if we are restarting input on the
     *                   same text field as before.
     */
    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String mKeyboardLayout = sharedPreferences.getString("keyboard_layout", "qwerty");
        switch (mKeyboardLayout) {
            default:
            case "qwerty":
                mQwertyView.setVisibility(View.VISIBLE);
                mStrokeView.setVisibility(View.INVISIBLE);
                break;
            case "stroke":
                mQwertyView.setVisibility(View.INVISIBLE);
                mStrokeView.setVisibility(View.VISIBLE);
                break;
        }
        resetInput();
    }

    private void resetInput() {
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

    // 候補未選択でEnterによる確定
    private void icCommitInputText() {
        String word = mInputText.toString();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(word, 1);
        }
        resetInput();
    }

    // 候補選択状態で確定
    private void icCommitCandidateText(View v) {
        TextView view = (TextView) v;
        mCandidateIndex = mCandidateLayout.indexOfChild(view);
        String keyword = (String) view.getTag();
        String word = (String) view.getText();
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(word, 1);
        }
        mDictionary.addLearning(keyword, word);
        mDictionary.addPrediction(mLastCommit, word);
        mLastCommit = word;
        resetInput();
        ArrayList<String> list = mDictionary.predict(word);
        buildCandidate(list);
    }

    /*
     * 各キー入力ハンドラ
     */
    public void handleCharacter(char charCode) {
        if (mInputText.length() == 0) {
            // 未入力
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中→選択中の候補をコミット
                icCommitCandidateText(mCandidateLayout.getChildAt(mCandidateIndex));
            }
        }
        mInputText.append(charCode);
        icSetComposingText(mInputText);
        ArrayList<String> list = mDictionary.search(mInputText.toString(), 40);
        buildCandidate(list);
    }

    public void handleSpace() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択→最初の候補を選択状態にする
                mCandidateIndex = 0;
            } else {
                // 候補選択中→次の候補を選択状態にする、最後に達したら先頭に戻る
                mCandidateIndex = (mCandidateIndex + 1) % mCandidateLayout.getChildCount();
            }
            selectCandidate();
        }
    }

    public void handleBackspace() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択→入力テキストの最後の文字を削除して候補を作り直す
                mInputText.deleteCharAt(mInputText.length() - 1);
                ArrayList<String> list;
                if (mInputText.length() == 0) {
                    list = mDictionary.predict(mLastCommit);
                } else {
                    list = mDictionary.search(mInputText.toString(), 50);
                }
                buildCandidate(list);
            } else {
                // 候補選択中→候補未選択に戻す
                mCandidateIndex = -1;
                selectCandidate();
            }
            icSetComposingText(mInputText);
        }
    }

    public void handleEnter() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択→入力テキストをそのままコミット
                icCommitInputText();
            } else {
                // 候補選択中→選択中の候補をコミット
                icCommitCandidateText(mCandidateLayout.getChildAt(mCandidateIndex));
            }
        }
    }

    public void handleCursorLeft() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中
            }
        }
    }

    public void handleCursorRight() {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中
            }
        }
    }

    public void handleCursorUp() {
        if (mInputText.length() == 0) {
            // 未入力
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中
            }
        }
    }

    public void handleCursorDown() {
        if (mInputText.length() == 0) {
            // 未入力
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中
            }
        }
    }

    public void handleSymbol() {
        if (mInputText.length() == 0) {
            // 未入力
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中
            }
        }
    }

    /*
     * 候補
     */
    private void buildCandidate(@NonNull ArrayList<String> list) {
        mCandidateIndex = -1;
        mCandidateLayout.removeAllViewsInLayout();
        int style = R.style.CandidateText;
        for (int i = 0; i < list.size(); i++) {
            TextView view = new TextView(new ContextThemeWrapper(this, style), null, style);
            String[] ss = list.get(i).split("\t");
            view.setTag(ss[0]);     // 確定時登録用テキスト
            view.setText(ss[1]);    // 表示用テキスト
            view.setOnClickListener(this::icCommitCandidateText);
            view.setSelected(false);
            view.setPressed(false);
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
        if (mCandidateIndex < 0) {
            // 未選択ならば先頭にスクロールする
            mCandidateView.scrollTo(0, 0);
        }
    }

}
