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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private boolean mVibration;

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
        sendKeyChar(charCode);
        resetShiftState();
    }

    public void handleSpace(View v) {
        sendKeyChar(' ');
        resetShiftState();
    }

    public void handleCursorLeft(View v) {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT);
    }

    public void handleCursorRight(View v) {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    public void handleBackspace(View v) {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        resetShiftState();
    }

    public void handleEnter(View v) {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        resetShiftState();
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
}
