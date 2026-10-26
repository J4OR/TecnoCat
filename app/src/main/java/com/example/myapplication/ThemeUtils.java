package com.example.myapplication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        if (prefs.contains("is_dark_mode")) {
            boolean isDark = prefs.getBoolean("is_dark_mode", false);
            int desiredMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
                AppCompatDelegate.setDefaultNightMode(desiredMode);
            }
        }
    }

    public static void setupThemeToggle(AppCompatActivity activity) {
        ImageView btnThemeToggle = activity.findViewById(R.id.btn_theme_toggle);
        View vThemeOverlay = activity.findViewById(R.id.v_theme_overlay);

        int currentNightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        if (btnThemeToggle != null) {
            btnThemeToggle.setImageResource(isDarkMode ? R.drawable.ic_sun : R.drawable.ic_moon);

            btnThemeToggle.setOnClickListener(v -> {
                btnThemeToggle.setEnabled(false);
                boolean targetDarkMode = !isDarkMode;

                // Animación de rotación del icono
                btnThemeToggle.animate()
                        .rotationBy(180f)
                        .setDuration(500)
                        .start();

                SharedPreferences prefs = activity.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);

                if (vThemeOverlay != null) {
                    vThemeOverlay.setBackgroundColor(targetDarkMode ? Color.parseColor("#101511") : Color.WHITE);
                    vThemeOverlay.setAlpha(0f);
                    vThemeOverlay.setVisibility(View.VISIBLE);

                    vThemeOverlay.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    prefs.edit().putBoolean("is_dark_mode", targetDarkMode).apply();
                                    AppCompatDelegate.setDefaultNightMode(
                                            targetDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                                    );

                                    Intent intent = new Intent(activity, activity.getClass());
                                    if (activity.getIntent() != null && activity.getIntent().getExtras() != null) {
                                        intent.putExtras(activity.getIntent().getExtras());
                                    }
                                    intent.putExtra("THEME_ANIM_TYPE", targetDarkMode ? "TO_DARK" : "TO_LIGHT");
                                    activity.startActivity(intent);
                                    activity.overridePendingTransition(0, 0);
                                    activity.finish();
                                }
                            });
                } else {
                    prefs.edit().putBoolean("is_dark_mode", targetDarkMode).apply();
                    AppCompatDelegate.setDefaultNightMode(
                            targetDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                    );
                    activity.recreate();
                }
            });
        }

        // Animación suave de entrada tras reiniciar con el nuevo tema
        if (vThemeOverlay != null && activity.getIntent() != null && activity.getIntent().hasExtra("THEME_ANIM_TYPE")) {
            String animType = activity.getIntent().getStringExtra("THEME_ANIM_TYPE");
            vThemeOverlay.setBackgroundColor("TO_DARK".equals(animType) ? Color.parseColor("#101511") : Color.WHITE);
            vThemeOverlay.setAlpha(1f);
            vThemeOverlay.setVisibility(View.VISIBLE);

            vThemeOverlay.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            vThemeOverlay.setVisibility(View.GONE);
                        }
                    });
        }
    }
}
