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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

public class KeyboardLayout extends LinearLayout {

    public final static int SOFTKEY_ID_SPACE = -1;
    public final static int SOFTKEY_ID_BACKSPACE = -2;
    public final static int SOFTKEY_ID_ENTER = -3;
    public final static int SOFTKEY_ID_CURSOR_LEFT = -4;
    public final static int SOFTKEY_ID_CURSOR_RIGHT = -5;
    public final static int SOFTKEY_ID_CURSOR_UP = -6;
    public final static int SOFTKEY_ID_CURSOR_DOWN = -7;
    public final static int SOFTKEY_ID_SHIFT = -8;
    public final static int SOFTKEY_ID_SYMBOL = -9;
    public final static int SOFTKEY_ID_KEYBOARD_VIEW = -10;
    public final static int SOFTKEY_ID_SYMBOL_VIEW = -11;

    public Context mContext;
    public SoftKeyboard mSoftKeyboard;

    public SoftKey mBackspaceKey;
    public SoftKey mCursorLeftKey;
    public SoftKey mCursorRightKey;
    public SoftKey mEnterKey;
    public SoftKey mKeyboardViewKey;
    public SoftKey mLastKey;
    public SoftKey mRepeatKey;
    public SoftKey mShiftKey;
    public SoftKey mSpaceKey;
    public SoftKey mSymbolViewKey;
    public SoftKey mSymbolKey;
    public SoftKey mKigouKey;
    public ArrayList<SoftKey> mSoftKeys;

    public Bitmap mBitmap;
    public Canvas mCanvas;
    public Drawable mShiftLockDrawable;
    public Drawable mShiftNoneDrawable;
    public Drawable mShiftSingleDrawable;
    public Drawable mSymbolViewDrawable;
    public Drawable mSymbolEmojiDrawable;
    public Drawable mSymbolKigouDrawable;
    public Handler mRepeatHandler;
    public ImageView mImageView;

    public int mBackgroundColor;
    public int mCharacterKeyBackgroundColor;
    public int mFunctionKeyBackgroundColor;
    public int mKeyForegroundColor;

    public boolean mShiftLockFlag;
    public boolean mShiftSingleFlag;

    public int mWidth;
    public int mHeight;
    public float mDensity;

    public int mRepeatTimeout;
    public int mRepeatDelay;
    public final Runnable mRepeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRepeatKey == null) {
                return;
            }
            processSoftKey(mRepeatKey);
            mRepeatHandler.postDelayed(this, mRepeatDelay);
        }
    };

    public KeyboardLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mSoftKeyboard = (SoftKeyboard) context;

        mRepeatTimeout = ViewConfiguration.getKeyRepeatTimeout();
        mRepeatDelay = ViewConfiguration.getKeyRepeatDelay();
        mRepeatHandler = new Handler(Looper.getMainLooper());

        mBackgroundColor = getResources().getColor(R.color.background, null);
        mCharacterKeyBackgroundColor = getResources().getColor(R.color.character_key_background, null);
        mFunctionKeyBackgroundColor = getResources().getColor(R.color.function_key_background, null);
        mKeyForegroundColor = getResources().getColor(R.color.key_foreground, null);

        mShiftNoneDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shift_none, null);
        mShiftSingleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shift_single, null);
        mShiftLockDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shift_lock, null);
        mSymbolViewDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_symbol_view, null);
        mSymbolEmojiDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_symbol_emoji, null);
        mSymbolKigouDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_symbol_kigou, null);

        mShiftSingleFlag = false;
        mShiftLockFlag = false;

        mSoftKeys = new ArrayList<>();

        mShiftKey = new SoftKey(SOFTKEY_ID_SHIFT);
        mShiftKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mShiftKey.setDrawable(mShiftNoneDrawable);

        mSymbolKey = new SoftKey(SOFTKEY_ID_SYMBOL);
        mSymbolKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mSymbolKey.setDrawable(mSymbolKigouDrawable);

        mSymbolViewKey = new SoftKey(SOFTKEY_ID_SYMBOL_VIEW);
        mSymbolViewKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mSymbolViewKey.setDrawable(mSymbolViewDrawable);

        mKeyboardViewKey = new SoftKey(SOFTKEY_ID_KEYBOARD_VIEW);
        mKeyboardViewKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mKeyboardViewKey.setCharacter('⌨');   // u2328

        mSpaceKey = new SoftKey(SOFTKEY_ID_SPACE);
        mSpaceKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mSpaceKey.setCharacter('⎵');   // u23B5

        mCursorLeftKey = new SoftKey(SOFTKEY_ID_CURSOR_LEFT);
        mCursorLeftKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mCursorLeftKey.setCharacter('◂');  // u25C2
        mCursorLeftKey.setRepeatable(true);

        mCursorRightKey = new SoftKey(SOFTKEY_ID_CURSOR_RIGHT);
        mCursorRightKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mCursorRightKey.setCharacter('▸'); // u25B8
        mCursorRightKey.setRepeatable(true);

        mBackspaceKey = new SoftKey(SOFTKEY_ID_BACKSPACE);
        mBackspaceKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mBackspaceKey.setCharacter('⌫');   // u232B
        mBackspaceKey.setRepeatable(true);

        mEnterKey = new SoftKey(SOFTKEY_ID_ENTER);
        mEnterKey.setColor(mKeyForegroundColor, mFunctionKeyBackgroundColor);
        mEnterKey.setCharacter('⏎');   // u23CE
    }

    public void processSoftKey(@NonNull SoftKey softKey) {
        int id = softKey.getId();
        switch (id) {
            case SOFTKEY_ID_KEYBOARD_VIEW:
                mSoftKeyboard.handleKeyboard();
                break;
            case SOFTKEY_ID_SYMBOL_VIEW:
                mSoftKeyboard.handleSymbol();
                break;
            case SOFTKEY_ID_SPACE:
                mSoftKeyboard.handleSpace();
                break;
            case SOFTKEY_ID_CURSOR_LEFT:
                mSoftKeyboard.handleCursorLeft();
                break;
            case SOFTKEY_ID_CURSOR_RIGHT:
                mSoftKeyboard.handleCursorRight();
                break;
            case SOFTKEY_ID_CURSOR_UP:
                mSoftKeyboard.handleCursorUp();
                break;
            case SOFTKEY_ID_CURSOR_DOWN:
                mSoftKeyboard.handleCursorDown();
                break;
            case SOFTKEY_ID_BACKSPACE:
                mSoftKeyboard.handleBackspace();
                break;
            case SOFTKEY_ID_ENTER:
                mSoftKeyboard.handleEnter();
                break;
            default:
                break;
        }
    }
}
