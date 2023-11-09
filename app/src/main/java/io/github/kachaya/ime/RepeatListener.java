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
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

public class RepeatListener implements OnTouchListener {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final OnClickListener mOnClickListener;
    private View mView;

    private final Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mView.isEnabled()) {
                mHandler.postDelayed(this, ViewConfiguration.getKeyRepeatDelay());
                mOnClickListener.onClick(mView);
            } else {
                mHandler.removeCallbacks(handlerRunnable);
                mView.setPressed(false);
                mView = null;
            }
        }
    };

    public RepeatListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mHandler.removeCallbacks(handlerRunnable);
                mHandler.postDelayed(handlerRunnable, ViewConfiguration.getKeyRepeatTimeout());
                mView = view;
                mView.setPressed(true);
                mOnClickListener.onClick(view);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(handlerRunnable);
                mView.setPressed(false);
                mView = null;
                return true;
        }
        return false;
    }
}
