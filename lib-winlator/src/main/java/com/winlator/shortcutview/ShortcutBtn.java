package com.winlator.shortcutview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.lib_winlator.R;
import com.winlator.core.AppUtils;

/**
 * Description: TODO.
 *
 * @author yh.liu
 * @date 2025年07月09日 11:06
 * <p>
 * Copyright (c) 2025年, 4399 Network CO.ltd. All Rights Reserved.
 */
public class ShortcutBtn extends RelativeLayout {

    private WindowManager.LayoutParams layoutParams;
    private boolean isMoving = false;
    private WindowManager windowManager;
    private Activity currentActivity;

    public ShortcutBtn(Context context) {
        this(context, null);
    }

    public ShortcutBtn(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.shortcut_buttom, this);

        findViewById(R.id.btInput).setOnClickListener(v -> AppUtils.showKeyboard(currentActivity));

        findViewById(R.id.btContainer).setVisibility(View.GONE);

        layoutParams = createWindowParams();

        findViewById(R.id.show_button).setOnTouchListener(new OnTouchListener() {
            float lastY = 0f;
            int paramY = 0;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastY = event.getRawY();
                        paramY = layoutParams.y;
                        isMoving = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dy = event.getRawY() - lastY;
                        layoutParams.y = (int) (paramY + dy);
                        if (getWindowToken() != null) {
                            updateWindowLayout();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        float stopY = event.getRawY();
                        if (Math.abs(lastY - stopY) >= 10) {
                            isMoving = true;
                        }
                        if (!isMoving) {
                            showExitBtn();
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void showExitBtn() {
        View btContainer = findViewById(R.id.btContainer);
        TextView showButton = findViewById(R.id.show_button);
        if (btContainer.getVisibility() == View.GONE) {
            btContainer.setVisibility(View.VISIBLE);
            showButton.setText("<");
        } else {
            btContainer.setVisibility(View.GONE);
            showButton.setText(">");
        }
    }

    public void addShortcutView(Activity activity) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> addShortcutViewInner(activity), 200);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Rect rect = new Rect();
            if (getLocalVisibleRect(rect) && !activity.isDestroyed()) {
                addShortcutViewInner(activity);
            }
        }, 500);
    }

    @SuppressLint("RtlHardcoded")
    private WindowManager.LayoutParams createWindowParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL;
        lp.format = PixelFormat.RGBA_8888;
        lp.x = 0;
        lp.y = 120;
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return lp;
    }

    private void addShortcutViewInner(Activity activity) {
        currentActivity = activity;
        windowManager = activity.getWindowManager();
        try {
            windowManager.removeViewImmediate(this);
        } catch (Throwable e) {
            Log.e("GameShortBtn", e.toString());
        }
        layoutParams.token = activity.getWindow().getDecorView().getWindowToken();
        try {
            windowManager.addView(this, layoutParams);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Log.i("GameShortcutMgr", "addShortcutView for " + activity);
    }

    private void updateWindowLayout() {
        if (!isDestroy(getContext())) {
            try {
                windowManager.updateViewLayout(this, layoutParams);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isDestroy(Context cxt) {
        if (cxt == null) return true;
        if (cxt instanceof Activity) return isDestroy((Activity) cxt);
        if (cxt instanceof ContextWrapper) {
            Context base = ((ContextWrapper) cxt).getBaseContext();
            if (base instanceof Activity) return isDestroy((Activity) base);
        }
        return false;
    }

    private boolean isDestroy(Activity activity) {
        return activity == null || activity.isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed());
    }
}
