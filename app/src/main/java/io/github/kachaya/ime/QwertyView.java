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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

public class QwertyView extends KeyboardLayout {
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

    public QwertyView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(VERTICAL);

        mImageView = new ImageView(context);
//        mImageView.setImageBitmap(mBitmap);
        addView(mImageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        int id = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                SoftKey softKey = new SoftKey(id);
                softKey.setColor(mKeyForegroundColor, mCharacterKeyBackgroundColor);
                mSoftKeys.add(softKey);
                id++;
            }
        }
        mSoftKeys.add(mShiftKey);
        mSoftKeys.add(mSymbolViewKey);
        mSoftKeys.add(mSpaceKey);
        mSoftKeys.add(mCursorLeftKey);
        mSoftKeys.add(mCursorRightKey);
        mSoftKeys.add(mBackspaceKey);
        mSoftKeys.add(mEnterKey);
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
                    mRepeatHandler.postDelayed(mRepeatRunnable, mRepeatTimeout);
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
                    mRepeatHandler.postDelayed(mRepeatRunnable, mRepeatTimeout);
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
    public void processSoftKey(@NonNull SoftKey softKey) {
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
        if (id >= 0) {
            if (mShiftSingleFlag || mShiftLockFlag) {
                mSoftKeyboard.handleCharacter(charHalfShift[id]);
            } else {
                mSoftKeyboard.handleCharacter(charHalfNormal[id]);
            }
            mShiftSingleFlag = false;
            return;
        }
        super.processSoftKey(softKey);
    }

    private void drawKeyboard() {
        if (mCanvas == null) {
            return;
        }
        mCanvas.drawColor(mBackgroundColor);
        int id = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                SoftKey softKey = mSoftKeys.get(id);
                if (mShiftSingleFlag || mShiftLockFlag) {
                    softKey.setCharacter(charHalfShift[id]);
                } else {
                    softKey.setCharacter(charHalfNormal[id]);
                }
                id++;
            }
        }
        mShiftKey.setDrawable(mShiftNoneDrawable);
        if (mShiftLockFlag) {
            mShiftKey.setDrawable(mShiftLockDrawable);
        }
        if (mShiftSingleFlag) {
            mShiftKey.setDrawable(mShiftSingleDrawable);
        }
        for (SoftKey softKey : mSoftKeys) {
            softKey.draw(mCanvas);
        }
        mImageView.setImageBitmap(mBitmap);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mImageView.setImageBitmap(mBitmap);

        float kx;
        float ky;
        float kw = (float) mWidth / COLS;
        float kh = (float) mHeight / (ROWS + 1);
        int id = 0;
        for (int row = 0; row < ROWS; row++) {
            ky = kh * row;
            for (int col = 0; col < COLS; col++) {
                kx = kw * col;
                SoftKey softKey = mSoftKeys.get(id);
                softKey.setPos(kx, ky, kw, kh);
                id++;
            }
        }

        ky = kh * ROWS;
        kw = (float) mWidth / 8;

        mShiftKey.setPos(kw * 0, ky, kw, kh);
        mSymbolViewKey.setPos(kw * 1, ky, kw, kh);
        mSpaceKey.setPos(kw * 2, ky, kw * 2, kh);
        mCursorLeftKey.setPos(kw * 4, ky, kw, kh);
        mCursorRightKey.setPos(kw * 5, ky, kw, kh);
        mBackspaceKey.setPos(kw * 6, ky, kw, kh);
        mEnterKey.setPos(kw * 7, ky, kw, kh);

        drawKeyboard();
    }
}
