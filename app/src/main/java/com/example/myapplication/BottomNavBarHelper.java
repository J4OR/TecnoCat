package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BottomNavBarHelper {

    public enum Tab {
        HOME,
        MAP,
        CATALOG,
        PROFILE,
        NONE
    }

    private static Bitmap cachedProfileBitmap = null;
    private static final ExecutorService navExecutor = Executors.newSingleThreadExecutor();

    public static void setup(Activity activity, Tab activeTab) {
        View navView = activity.findViewById(R.id.custom_bottom_nav);
        if (navView == null) return;

        // Tab HOME
        View tabHome = navView.findViewById(R.id.tab_home);
        View ringHome = navView.findViewById(R.id.v_home_ring);
        FrameLayout containerHome = navView.findViewById(R.id.fl_home_icon_container);
        ImageView inactiveHome = navView.findViewById(R.id.iv_home_inactive_icon);
        TextView tvHome = navView.findViewById(R.id.tv_home_label);

        // Tab MAP
        View tabMap = navView.findViewById(R.id.tab_map);
        View ringMap = navView.findViewById(R.id.v_map_ring);
        FrameLayout containerMap = navView.findViewById(R.id.fl_map_icon_container);
        ImageView inactiveMap = navView.findViewById(R.id.iv_map_inactive_icon);
        TextView tvMap = navView.findViewById(R.id.tv_map_label);

        // Tab CATALOG
        View tabCatalog = navView.findViewById(R.id.tab_catalog);
        View ringCatalog = navView.findViewById(R.id.v_catalog_ring);
        FrameLayout containerCatalog = navView.findViewById(R.id.fl_catalog_icon_container);
        ImageView inactiveCatalog = navView.findViewById(R.id.iv_catalog_inactive_icon);
        TextView tvCatalog = navView.findViewById(R.id.tv_catalog_label);

        // Tab PROFILE
        View tabProfile = navView.findViewById(R.id.tab_profile);
        View ringProfile = navView.findViewById(R.id.v_profile_ring);
        FrameLayout containerProfile = navView.findViewById(R.id.fl_profile_icon_container);
        ShapeableImageView activeAvatarProfile = navView.findViewById(R.id.iv_profile_active_avatar);
        ShapeableImageView inactiveAvatarProfile = navView.findViewById(R.id.iv_profile_inactive_avatar);
        TextView tvProfile = navView.findViewById(R.id.tv_profile_label);

        setTabState(activeTab == Tab.HOME, ringHome, containerHome, inactiveHome, tvHome);
        setTabState(activeTab == Tab.MAP, ringMap, containerMap, inactiveMap, tvMap);
        setTabState(activeTab == Tab.CATALOG, ringCatalog, containerCatalog, inactiveCatalog, tvCatalog);
        setTabState(activeTab == Tab.PROFILE, ringProfile, containerProfile, inactiveAvatarProfile, tvProfile);

        // Listeners para abrir cada vista
        if (tabHome != null) {
            tabHome.setOnClickListener(v -> navigateTo(activity, MainActivity4.class, activeTab == Tab.HOME));
        }
        if (tabMap != null) {
            tabMap.setOnClickListener(v -> navigateTo(activity, StoreMapActivity.class, activeTab == Tab.MAP));
        }
        if (tabCatalog != null) {
            tabCatalog.setOnClickListener(v -> navigateTo(activity, MainActivity5.class, activeTab == Tab.CATALOG));
        }
        if (tabProfile != null) {
            tabProfile.setOnClickListener(v -> navigateTo(activity, MainActivity8.class, activeTab == Tab.PROFILE));
        }

        // Cargar foto de perfil de la cuenta
        loadProfileImage(activity, activeAvatarProfile, inactiveAvatarProfile);
    }

    private static void setTabState(boolean isActive, View ring, View activeContainer, View inactiveView, TextView label) {
        if (ring != null || activeContainer != null) {
            int activeBlue = ContextCompat.getColor(ring != null ? ring.getContext() : activeContainer.getContext(), R.color.brand_blue);
            int inactiveGrey = ContextCompat.getColor(ring != null ? ring.getContext() : activeContainer.getContext(), R.color.text_grey);

            if (ring != null) ring.setVisibility(isActive ? View.VISIBLE : View.GONE);
            if (activeContainer != null) activeContainer.setVisibility(isActive ? View.VISIBLE : View.GONE);
            if (inactiveView != null) inactiveView.setVisibility(isActive ? View.GONE : View.VISIBLE);
            if (label != null) {
                label.setTextColor(isActive ? activeBlue : inactiveGrey);
                label.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    private static void navigateTo(Activity currentActivity, Class<?> targetClass, boolean isCurrentScreen) {
        if (isCurrentScreen && currentActivity.getClass().equals(targetClass)) {
            return;
        }
        Intent intent = new Intent(currentActivity, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        currentActivity.startActivity(intent);
        currentActivity.overridePendingTransition(0, 0);
    }

    private static void loadProfileImage(Activity activity, ShapeableImageView activeAvatar, ShapeableImageView inactiveAvatar) {
        if (activeAvatar == null && inactiveAvatar == null) return;

        if (cachedProfileBitmap != null) {
            applyBitmapToAvatars(activity, cachedProfileBitmap, activeAvatar, inactiveAvatar);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Uri photoUri = user.getPhotoUrl();
        if (photoUri == null) {
            for (UserInfo providerUser : user.getProviderData()) {
                if (providerUser.getPhotoUrl() != null) {
                    photoUri = providerUser.getPhotoUrl();
                    break;
                }
            }
        }

        final Uri initialUri = photoUri;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String storedPhoto = snapshot.child("fotoPerfil").getValue(String.class);
                Uri finalUri = initialUri;
                if (storedPhoto != null && !storedPhoto.trim().isEmpty()) {
                    finalUri = Uri.parse(storedPhoto);
                }

                if (finalUri != null) {
                    final Uri uriToFetch = finalUri;
                    navExecutor.execute(() -> {
                        Bitmap bitmap = fetchImageBitmap(uriToFetch);
                        if (bitmap != null) {
                            cachedProfileBitmap = bitmap;
                            activity.runOnUiThread(() -> applyBitmapToAvatars(activity, bitmap, activeAvatar, inactiveAvatar));
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (initialUri != null) {
                    navExecutor.execute(() -> {
                        Bitmap bitmap = fetchImageBitmap(initialUri);
                        if (bitmap != null) {
                            cachedProfileBitmap = bitmap;
                            activity.runOnUiThread(() -> applyBitmapToAvatars(activity, bitmap, activeAvatar, inactiveAvatar));
                        }
                    });
                }
            }
        });
    }

    private static void applyBitmapToAvatars(Activity activity, Bitmap bitmap, ShapeableImageView activeAvatar, ShapeableImageView inactiveAvatar) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (activeAvatar != null) {
            activeAvatar.setImageTintList(null);
            activeAvatar.setImageBitmap(bitmap);
        }
        if (inactiveAvatar != null) {
            inactiveAvatar.setImageTintList(null);
            inactiveAvatar.setImageBitmap(bitmap);
        }
    }

    private static Bitmap fetchImageBitmap(Uri uri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception e) {
            Log.w("BottomNavBarHelper", "Error descargando foto de perfil: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
