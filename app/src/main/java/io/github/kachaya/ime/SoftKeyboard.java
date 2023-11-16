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

import android.annotation.SuppressLint;
import android.inputmethodservice.InputMethodService;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SoftKeyboard extends InputMethodService {
    private final static int SHIFT_STATE_NONE = 0;
    private final static int SHIFT_STATE_SINGLE = 1;
    private final static int SHIFT_STATE_LOCK = 2;
    private final static int SHIFT_STATE_NUM = 3;
    private int mShiftState;

    private View mInputView;
    private GridLayout mQwertyNormalLayout;
    private GridLayout mQwertyShiftLayout;
    private ImageView mShiftKey;
    private ImageView mSpaceKey;
    private ImageView mCursorLeftKey;
    private ImageView mCursorRightKey;
    private ImageView mBackspaceKey;
    private ImageView mEnterKey;

    private final StringBuilder mInputText = new StringBuilder();
    private View mCandidateView;
    private ViewGroup mCandidateLayout;
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
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateInputView() {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.input_layout, null);
        mInputView = layout;

        mCandidateView = layout.findViewById(R.id.candidate_view);
        mCandidateLayout = layout.findViewById(R.id.candidate_layout);

        mQwertyNormalLayout = layout.findViewById(R.id.qwerty_normal);
        for (int i = 0; i < mQwertyNormalLayout.getChildCount(); i++) {
            View view = mQwertyNormalLayout.getChildAt(i);
            if (view instanceof TextView) {
                view.setOnClickListener(this::onClickTextView);
            }
        }

        mQwertyShiftLayout = layout.findViewById(R.id.qwerty_shift);
        for (int i = 0; i < mQwertyShiftLayout.getChildCount(); i++) {
            View view = mQwertyShiftLayout.getChildAt(i);
            if (view instanceof TextView) {
                view.setOnClickListener(this::onClickTextView);
            }
        }

        mShiftKey = layout.findViewById(R.id.key_shift);
        mShiftKey.setOnClickListener(this::handleShift);

        mSpaceKey = layout.findViewById(R.id.key_space);
        mSpaceKey.setOnClickListener(this::handleSpace);

        mCursorLeftKey = layout.findViewById(R.id.key_cursor_left);
        mCursorLeftKey.setOnTouchListener(new RepeatListener(this::handleCursorLeft));

        mCursorRightKey = layout.findViewById(R.id.key_cursor_right);
        mCursorRightKey.setOnTouchListener(new RepeatListener(this::handleCursorRight));

        mBackspaceKey = layout.findViewById(R.id.key_backspace);
        mBackspaceKey.setOnTouchListener(new RepeatListener(this::handleBackspace));

        mEnterKey = layout.findViewById(R.id.key_enter);
        mEnterKey.setOnClickListener(this::handleEnter);

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
        mShiftState = SHIFT_STATE_NONE;
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

    private void icCommitText(CharSequence cs) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(cs, 1);
        }
        resetInput();
    }

    private void onClickTextView(View v) {
        CharSequence text = ((TextView) v).getText();
        if (text == null) {
            return;
        }
        if (text.length() == 0) {
            return;
        }
        handleCharacter(text.charAt(0));
    }

    /*
     * 各キー入力ハンドラ
     */
    private void handleCharacter(char charCode) {
        if (mInputText.length() == 0) {
            // 未入力
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択
            } else {
                // 候補選択中→選択中の候補をコミット
                TextView view = (TextView) mCandidateLayout.getChildAt(mCandidateIndex);
                icCommitText(view.getText());
            }
        }
        mInputText.append(charCode);
        icSetComposingText(mInputText);
        buildCandidate();
        resetShiftState();
    }

    public void handleSpace(View v) {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_SPACE);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択→最初の候補を選択状態にする
                buildCandidate();
                mCandidateIndex = 0;
            } else {
                // 候補選択中→次の候補を選択状態にする、最後に達したら先頭に戻る
                mCandidateIndex = (mCandidateIndex + 1) % mCandidateLayout.getChildCount();
            }
            selectCandidate();
        }
        resetShiftState();
    }

    public void handleBackspace(View v) {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択→入力テキストの最後の文字を削除して候補を作り直す
                mInputText.deleteCharAt(mInputText.length() - 1);
                buildCandidate();
            } else {
                // 候補選択中→候補未選択に戻す
                mCandidateIndex = -1;
                selectCandidate();
            }
            icSetComposingText(mInputText);
        }
        resetShiftState();
    }

    public void handleEnter(View v) {
        if (mInputText.length() == 0) {
            // 未入力
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        } else {
            if (mCandidateIndex < 0) {
                // 候補未選択→入力テキストをそのままコミット
                icCommitText(mInputText);
            } else {
                // 候補選択中→選択中の候補をコミット
                TextView view = (TextView) mCandidateLayout.getChildAt(mCandidateIndex);
                icCommitText(view.getText());
            }
        }
        resetShiftState();
    }

    public void handleCursorLeft(View v) {
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

    public void handleCursorRight(View v) {
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

    public void handleShift(View v) {
        mShiftState = (mShiftState + 1) % SHIFT_STATE_NUM;
        updateKeyboard();
    }

    private void resetShiftState() {
        if (mShiftState != SHIFT_STATE_LOCK) {
            mShiftState = SHIFT_STATE_NONE;
            updateKeyboard();
        }
    }

    /**
     * キーボード表示更新
     */
    private void updateKeyboard() {
        switch (mShiftState) {
            default:
                mShiftState = SHIFT_STATE_NONE;
            case SHIFT_STATE_NONE:
                mShiftKey.setImageResource(R.drawable.ic_shift_none);
                break;
            case SHIFT_STATE_SINGLE:
                mShiftKey.setImageResource(R.drawable.ic_shift_single);
                break;
            case SHIFT_STATE_LOCK:
                mShiftKey.setImageResource(R.drawable.ic_shift_lock);
                break;
        }
        if (mShiftState == SHIFT_STATE_NONE) {
            mQwertyNormalLayout.setVisibility(View.VISIBLE);
            mQwertyShiftLayout.setVisibility(View.INVISIBLE);
        } else {
            mQwertyNormalLayout.setVisibility(View.INVISIBLE);
            mQwertyShiftLayout.setVisibility(View.VISIBLE);
        }
    }

    /*
     * 候補
     */
    private void buildCandidate() {
        mCandidateIndex = -1;
        mCandidateLayout.removeAllViewsInLayout();
        if (mInputText.length() == 0) {
            return;
        }
        ArrayList<String> list = mDictionary.search(mInputText, 50);
        int style = R.style.CandidateText;
        for (int i = 0; i < list.size(); i++) {
            TextView view = new TextView(new ContextThemeWrapper(this, style), null, style);
            view.setTag(i);
            view.setText(list.get(i));
            view.setOnClickListener(this::onClickCandidateText);
            view.setSelected(false);
            view.setPressed(false);
            mCandidateLayout.addView(view);
        }
    }

    private void onClickCandidateText(View v) {
        TextView view = (TextView) v;
        icCommitText(view.getText());
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
