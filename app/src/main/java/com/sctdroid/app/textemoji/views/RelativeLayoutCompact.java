package com.sctdroid.app.textemoji.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by lixindong on 5/2/17.
 */

public class RelativeLayoutCompact extends RelativeLayout {
    private OnImmStatusChangedListener listener;

    public RelativeLayoutCompact(Context context) {
        super(context);
    }

    public RelativeLayoutCompact(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutCompact(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface OnImmStatusChangedListener {
        void show(int height);

        void hidden();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (oldh > h) {
            if (listener != null) {
                listener.show(oldh - h);
            }
        } else{
            if (listener != null) {
                listener.hidden();
            }
        }
    }

    public void setOnImmStatusChangedListener(OnImmStatusChangedListener listener) {
        this.listener = listener;
    }
}
