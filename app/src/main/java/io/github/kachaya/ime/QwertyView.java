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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

public class QwertyView extends View {

    private final static int COLS = 12;
    private final static int ROWS = 4;

    private final static char[/* ROWS * COLS */] charHalfNormal = {
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '^',
            'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '@', '[',
            'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', ':', ']',
            'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', '\\', '¥'
    };
    private final static char[/* ROWS * COLS */] charHalfShift = {
            '!', '"', '#', '$', '%', '&', '\'', '(', ')', 0, '=', '~',
            'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '`', '{',
            'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', '+', '*', '}',
            'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?', '_', '|',
    };

    private final static int SOFTKEY_ID_SHIFT = -1;
    private final static int SOFTKEY_ID_SPACE = -2;
    private final static int SOFTKEY_ID_CURSOR_LEFT = -3;
    private final static int SOFTKEY_ID_CURSOR_RIGHT = -4;
    private final static int SOFTKEY_ID_BACKSPACE = -5;
    private final static int SOFTKEY_ID_ENTER = -6;

    private final static int repeatTimeout = ViewConfiguration.getKeyRepeatTimeout();
    private final static int repeatDelay = ViewConfiguration.getKeyRepeatDelay();

    private final Context mContext;
    private final SoftKeyboard mSoftKeyboard;
    private final Handler mRepeatHandler;
    private final Drawable mShiftSingleDrawable;
    private final Drawable mShiftLockDrawable;
    private final Drawable mShiftNoneDrawable;

    private final SoftKey mShiftKey;
    private final SoftKey mSpaceKey;
    private final SoftKey mCursorLeftKey;
    private final SoftKey mCursorRightKey;
    private final SoftKey mBackspaceKey;
    private final SoftKey mEnterKey;
    private final ArrayList<SoftKey> mSoftKeys;
    private SoftKey mLastKey;
    private SoftKey mRepeatKey;

    private int mWidth;
    private int mHeight;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private int mBackgroundColor;

    private boolean mShiftSingleFlag;
    private boolean mShiftLockFlag;

    private final Runnable mRepeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRepeatKey == null) {
                return;
            }
            processSoftKey(mRepeatKey);
            mRepeatHandler.postDelayed(this, repeatDelay);
        }
    };

    public QwertyView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mSoftKeyboard = (SoftKeyboard) context;

        mBackgroundColor = getResources().getColor(R.color.gray1, null);
        mShiftNoneDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shift_none, null);
        mShiftSingleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shift_single, null);
        mShiftLockDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_shift_lock, null);

        mShiftSingleFlag = false;
        mShiftLockFlag = false;

        mRepeatHandler = new Handler(Looper.getMainLooper());
        mSoftKeys = new ArrayList<>();

        int id = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                mSoftKeys.add(new SoftKey(id++));
            }
        }

        mShiftKey = new SoftKey(SOFTKEY_ID_SHIFT);
        mSoftKeys.add(mShiftKey);

        mSpaceKey = new SoftKey(SOFTKEY_ID_SPACE);
        mSoftKeys.add(mSpaceKey);

        mCursorLeftKey = new SoftKey(SOFTKEY_ID_CURSOR_LEFT);
        mCursorLeftKey.setRepeatable(true);
        mSoftKeys.add(mCursorLeftKey);

        mCursorRightKey = new SoftKey(SOFTKEY_ID_CURSOR_RIGHT);
        mCursorRightKey.setRepeatable(true);
        mSoftKeys.add(mCursorRightKey);

        mBackspaceKey = new SoftKey(SOFTKEY_ID_BACKSPACE);
        mBackspaceKey.setRepeatable(true);
        mSoftKeys.add(mBackspaceKey);

        mEnterKey = new SoftKey(SOFTKEY_ID_ENTER);
        mSoftKeys.add(mEnterKey);
    }

    private void drawKeyboard() {
        if (mCanvas == null) {
            return;
        }

        mBackgroundColor = getResources().getColor(R.color.gray1, null);

        mCanvas.drawColor(mBackgroundColor);
        int foregoundColor = getResources().getColor(R.color.white, null);
        int backgroundColor = getResources().getColor(R.color.gray4, null);

        float x;
        float y;
        float w = (float) mWidth / COLS;
        float h = (float) mHeight / (ROWS + 1);
        int id = 0;
        for (int row = 0; row < ROWS; row++) {
            y = h * row;
            for (int col = 0; col < COLS; col++) {
                x = w * col;
                SoftKey softKey = mSoftKeys.get(id);
                softKey.setPos(x, y, w, h);
                softKey.setColor(foregoundColor, backgroundColor);
                if (mShiftSingleFlag || mShiftLockFlag) {
                    softKey.setCharacter(charHalfShift[id]);
                } else {
                    softKey.setCharacter(charHalfNormal[id]);
                }
                id++;
            }
        }

        y = h * ROWS;
        w = (float) mWidth / 8;
        backgroundColor = getResources().getColor(R.color.gray3, null);

        mShiftKey.setPos(w * 0, y, w, h);
        mShiftKey.setColor(foregoundColor, backgroundColor);
        mShiftKey.setDrawable(mShiftNoneDrawable);
        if (mShiftLockFlag) {
            mShiftKey.setDrawable(mShiftLockDrawable);
        }
        if (mShiftSingleFlag) {
            mShiftKey.setDrawable(mShiftSingleDrawable);
        }

        mSpaceKey.setPos(w * 1, y, w * 3, h);
        mSpaceKey.setColor(foregoundColor, backgroundColor);
        mSpaceKey.setCharacter('⎵');   // u23B5

        mCursorLeftKey.setPos(w * 4, y, w, h);
        mCursorLeftKey.setColor(foregoundColor, backgroundColor);
        mCursorLeftKey.setCharacter('◂');  // u25C2

        mCursorRightKey.setPos(w * 5, y, w, h);
        mCursorRightKey.setColor(foregoundColor, backgroundColor);
        mCursorRightKey.setCharacter('▸'); // u25B8

        mBackspaceKey.setPos(w * 6, y, w, h);
        mBackspaceKey.setColor(foregoundColor, backgroundColor);
        mBackspaceKey.setCharacter('⌫');   // u232B

        mEnterKey.setPos(w * 7, y, w, h);
        mEnterKey.setColor(foregoundColor, backgroundColor);
        mEnterKey.setCharacter('⏎');   // u23CE

        for (SoftKey softKey : mSoftKeys) {
            softKey.draw(mCanvas);
        }
    }

    private void processSoftKey(@NonNull SoftKey softKey) {
        int id = softKey.getId();
        if (id == SOFTKEY_ID_SHIFT) {
            if (mShiftLockFlag) {
                mShiftLockFlag = false;
                mShiftSingleFlag = false;
            } else {
                if (mShiftSingleFlag) {
                    mShiftLockFlag = true;
                    mShiftSingleFlag = false;
                } else {
                    mShiftSingleFlag = true;
                }
            }
            return;
        }
        switch (id) {
            case SOFTKEY_ID_SPACE:
                mSoftKeyboard.handleSpace();
                break;
            case SOFTKEY_ID_CURSOR_LEFT:
                mSoftKeyboard.handleCursorLeft();
                break;
            case SOFTKEY_ID_CURSOR_RIGHT:
                mSoftKeyboard.handleCursorRight();
                break;
            case SOFTKEY_ID_BACKSPACE:
                mSoftKeyboard.handleBackspace();
                break;
            case SOFTKEY_ID_ENTER:
                mSoftKeyboard.handleEnter();
                break;
            default:
                if (mShiftSingleFlag || mShiftLockFlag) {
                    mSoftKeyboard.handleCharacter(charHalfShift[id]);
                } else {
                    mSoftKeyboard.handleCharacter(charHalfNormal[id]);
                }
                break;
        }
        mShiftSingleFlag = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        SoftKey currentKey = null;  // キーがない場所の場合(外へ出て行った等)
        for (SoftKey softkey : mSoftKeys) {
            if (softkey.contains(x, y)) {
                currentKey = softkey;
                break;
            }
        }
        if (currentKey == null) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (currentKey.isRepeatable()) {
                    // 押されたキーの初回リピート
                    mRepeatKey = currentKey;
                    mRepeatHandler.postDelayed(mRepeatRunnable, repeatTimeout);
                }
                currentKey.setPressed(true);
                mLastKey = currentKey;
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentKey.equals(mLastKey)) {
                    return true;    // 同じキー内
                }
                if (currentKey.isRepeatable()) {
                    // 移動した先のキーの初回リピート
                    mRepeatKey = currentKey;
                    mRepeatHandler.postDelayed(mRepeatRunnable, repeatTimeout);
                }
                if (mLastKey != null) {
                    mLastKey.setPressed(false);
                }
                currentKey.setPressed(true);
                mLastKey = currentKey;
                break;

            case MotionEvent.ACTION_UP:
                currentKey.setPressed(false);
                processSoftKey(currentKey);
                mRepeatKey = null;
                mLastKey = null;
                break;

            default:
                break;
        }
        drawKeyboard();
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        drawKeyboard();
    }
}