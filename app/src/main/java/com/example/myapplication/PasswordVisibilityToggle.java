package com.example.myapplication;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageButton;

public final class PasswordVisibilityToggle {
    private PasswordVisibilityToggle() {
    }

    public static void bind(EditText passwordField, ImageButton visibilityButton) {
        if (passwordField == null || visibilityButton == null) return;

        visibilityButton.setOnClickListener(view -> {
            boolean showPassword = visibilityButton.getTag() == null
                    || !Boolean.TRUE.equals(visibilityButton.getTag());
            int selectionStart = passwordField.getSelectionStart();
            int selectionEnd = passwordField.getSelectionEnd();

            passwordField.setTransformationMethod(showPassword
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            if (selectionStart >= 0 && selectionEnd >= 0) {
                int textLength = passwordField.length();
                passwordField.setSelection(Math.min(selectionStart, textLength),
                        Math.min(selectionEnd, textLength));
            }

            visibilityButton.setTag(showPassword);
            visibilityButton.setImageResource(showPassword
                    ? R.drawable.ic_eye_open : R.drawable.ic_eye_closed);
            visibilityButton.setContentDescription(visibilityButton.getContext().getString(
                    showPassword ? R.string.password_hide : R.string.password_show));
        });
    }
}
