package com.example.myapplication;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

final class KeyboardUtils {

    private KeyboardUtils() { }

    static void hideKeyboardWhenTappedOutside(Activity activity, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return;
        View focused = activity.getCurrentFocus();
        if (!(focused instanceof EditText)) return;

        Rect fieldBounds = new Rect();
        focused.getGlobalVisibleRect(fieldBounds);
        if (fieldBounds.contains((int) event.getRawX(), (int) event.getRawY())) return;

        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
        focused.clearFocus();
    }

    static void applyImeInsets(View view, WindowInsetsCompat insets) {
        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
        view.setPadding(systemBars.left, systemBars.top, systemBars.right,
                Math.max(systemBars.bottom, ime.bottom));
    }
}
