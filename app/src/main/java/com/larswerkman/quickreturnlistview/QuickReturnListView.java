/*
 * Copyright 2013 Lars Werkman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larswerkman.quickreturnlistview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.ListView;

import de.kalass.agime.R;

public class QuickReturnListView extends ListView {

    private int mItemCount;
    private int mItemOffsetY[];
    private boolean scrollIsComputed = false;
    private int mHeight;

    // Start QuickReturn
    private View mHeader;
    // private TextView mQuickReturnView;
    private View mPlaceHolder;


    private int mCachedVerticalScrollRange;
    private int mQuickReturnHeight;

    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;
    private int mState = STATE_ONSCREEN;
    private int mScrollY;
    private int mMinRawY = 0;

    private TranslateAnimation anim;
    private View mQuickReturnHeader;
// END QuickReturn

    public QuickReturnListView(Context context) {
        super(context);
    }

    public QuickReturnListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getListHeight() {
        return mHeight;
    }

    public void computeScrollY() {
        mHeight = 0;
        mItemCount = getAdapter().getCount();
        if (mItemOffsetY == null || mItemOffsetY.length != mItemCount) {
            mItemOffsetY = new int[mItemCount];
        }
        for (int i = 0; i < mItemCount; ++i) {
            // FIXME - wow, this is expensive, isn't it? Creating and initializing new view
            // instances that will be thrown away afterwards?
            View view = getAdapter().getView(i, null, this);
            view.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            mItemOffsetY[i] = mHeight;
            mHeight += view.getMeasuredHeight();
            System.out.println(mHeight);
        }
        scrollIsComputed = true;
    }

    public boolean scrollYIsComputed() {
        return scrollIsComputed;
    }

    public int getComputedScrollY() {
        int pos, nScrollY, nItemY;
        View view = null;
        pos = getFirstVisiblePosition();
        view = getChildAt(0);
        nItemY = view == null ? 0 : view.getTop();
        nScrollY = mItemOffsetY[pos] - nItemY;
        return nScrollY;
    }

    public void setQuickReturnHeader(final View quickReturnHeader) {
        mQuickReturnHeader = quickReturnHeader;

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        mHeader = inflater.inflate(R.layout.quick_return_header, null);

        mPlaceHolder = mHeader.findViewById(R.id.placeholder);

        final QuickReturnListView mListView = this;
        mListView.addHeaderView(mHeader, null, false);

        mListView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mQuickReturnHeight = quickReturnHeader.getHeight();
                        //mPlaceHolder.setMinimumHeight(mQuickReturnHeight);

                        mListView.computeScrollY();
                        mCachedVerticalScrollRange = mListView.getListHeight();
                    }
                });

        mListView.setOnScrollListener(new OnScrollListener() {
            @SuppressLint("NewApi")
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                mScrollY = 0;
                int translationY = 0;

                if (mListView.scrollYIsComputed()) {
                    mScrollY = mListView.getComputedScrollY();
                }

                int rawY = mPlaceHolder.getTop()
                        - Math.min(
                        mCachedVerticalScrollRange
                                - mListView.getHeight(), mScrollY);

                switch (mState) {
                    case STATE_OFFSCREEN:
                        if (rawY <= mMinRawY) {
                            mMinRawY = rawY;
                        } else {
                            mState = STATE_RETURNING;
                        }
                        translationY = rawY;
                        break;

                    case STATE_ONSCREEN:
                        if (rawY < -mQuickReturnHeight) {
                            mState = STATE_OFFSCREEN;
                            mMinRawY = rawY;
                        }
                        translationY = rawY;
                        break;

                    case STATE_RETURNING:
                        translationY = (rawY - mMinRawY) - mQuickReturnHeight;
                        if (translationY > 0) {
                            translationY = 0;
                            mMinRawY = rawY - mQuickReturnHeight;
                        }

                        if (rawY > 0) {
                            mState = STATE_ONSCREEN;
                            translationY = rawY;
                        }

                        if (translationY < -mQuickReturnHeight) {
                            mState = STATE_OFFSCREEN;
                            mMinRawY = rawY;
                        }
                        break;
                }

                /** this can be used if the build is below honeycomb **/
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
                    anim = new TranslateAnimation(0, 0, translationY,
                            translationY);
                    anim.setFillAfter(true);
                    anim.setDuration(0);
                    quickReturnHeader.startAnimation(anim);
                } else {
                    quickReturnHeader.setTranslationY(translationY);
                }

            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });
    }

    public void invalidateQuickReturnHeaderSize() {
        mQuickReturnHeader.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams p = mPlaceHolder.getLayoutParams();
                p.height = mQuickReturnHeader.getHeight();
                mPlaceHolder.setLayoutParams(p);
            }
        });

    }
}