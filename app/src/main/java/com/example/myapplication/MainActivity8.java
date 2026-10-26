package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class MainActivity8 extends AppCompatActivity {

    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private String profileName;
    private String profileEmail;
    private String profileRole;
    private Uri profilePhotoUri;
    private Uri storedProfilePhotoUri;
    private Bitmap profilePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applySavedTheme(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_user);

        ThemeUtils.setupThemeToggle(this);

        cargarPerfil();

        View adminPanel = findViewById(R.id.cl_admin_panel);
        if (adminPanel != null) {
            adminPanel.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity8.this, MainActivity10.class)));
        }

        TextView tvViewProfile = findViewById(R.id.tv_view_profile);
        if (tvViewProfile != null) {
            tvViewProfile.setOnClickListener(v -> cargarDetallesPerfil());
        }

        View favoritesRow = findViewById(R.id.cl_favorites);
        if (favoritesRow != null) {
            favoritesRow.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity8.this, MainActivity5.class);
                intent.putExtra("FILTER_CATEGORY", "Favoritos");
                startActivity(intent);
            });
        }

        View clLogout = findViewById(R.id.cl_logout);
        if (clLogout != null) {
            clLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity8.this, MainActivity2.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.PROFILE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.PROFILE);
    }

    private void cargarPerfil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            profileName = getString(R.string.user_name);
            profileEmail = getString(R.string.label_no_email);
            profileRole = getString(R.string.role_customer);
            mostrarPanelAdmin(false);
            actualizarNombreEnPantalla();
            return;
        }

        profileName = user.getDisplayName();
        profileEmail = user.getEmail();
        profileRole = getString(R.string.role_customer);
        profilePhotoUri = obtenerFotoPerfil(user);
        actualizarNombreEnPantalla();
        cargarFoto(findViewById(R.id.iv_avatar));

        FirebaseDatabase.getInstance().getReference("usuarios").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String storedName = snapshot.child("nombre").getValue(String.class);
                        String storedEmail = snapshot.child("correo").getValue(String.class);
                        String storedRole = snapshot.child("rol").getValue(String.class);
                        String storedPhoto = snapshot.child("fotoPerfil").getValue(String.class);
                        if (storedName != null && !storedName.trim().isEmpty()) profileName = storedName;
                        if (storedEmail != null && !storedEmail.trim().isEmpty()) profileEmail = storedEmail;
                        if (storedPhoto != null && !storedPhoto.trim().isEmpty()) {
                            storedProfilePhotoUri = Uri.parse(storedPhoto);
                            if (profilePhotoUri == null || profilePhotoUri.toString().trim().isEmpty()) {
                                profilePhotoUri = storedProfilePhotoUri;
                            }
                        }
                        if (profilePhoto == null) {
                            cargarFoto(findViewById(R.id.iv_avatar));
                        }
                        if (storedRole != null && !storedRole.trim().isEmpty()) {
                            boolean isAdmin = esRolAdministrador(storedRole)
                                    || "admin@technocat.com".equalsIgnoreCase(profileEmail);
                            profileRole = isAdmin
                                    ? getString(R.string.role_admin)
                                    : getString(R.string.role_customer);
                            mostrarPanelAdmin(isAdmin);
                        } else {
                            boolean isAdmin = "admin@technocat.com".equalsIgnoreCase(profileEmail);
                            profileRole = isAdmin ? getString(R.string.role_admin) : getString(R.string.role_customer);
                            mostrarPanelAdmin(isAdmin);
                        }
                        actualizarNombreEnPantalla();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        boolean isAdmin = "admin@technocat.com".equalsIgnoreCase(profileEmail);
                        profileRole = isAdmin ? getString(R.string.role_admin) : getString(R.string.role_customer);
                        mostrarPanelAdmin(isAdmin);
                        actualizarNombreEnPantalla();
                    }
                });
    }

    private boolean esRolAdministrador(String role) {
        return "admin".equalsIgnoreCase(role) || "administrador".equalsIgnoreCase(role);
    }

    private void mostrarPanelAdmin(boolean visible) {
        View adminPanel = findViewById(R.id.cl_admin_panel);
        View divider = findViewById(R.id.divider_admin_panel);
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (adminPanel != null) adminPanel.setVisibility(visibility);
        if (divider != null) divider.setVisibility(visibility);
    }

    private void actualizarNombreEnPantalla() {
        TextView tvUserName = findViewById(R.id.tv_user_name);
        if (tvUserName != null) {
            String name = profileName;
            if (name == null || name.trim().isEmpty()) name = getString(R.string.role_customer);
            tvUserName.setText(name);
        }
    }

    private Uri obtenerFotoPerfil(FirebaseUser user) {
        Uri photoUri = user.getPhotoUrl();
        if (photoUri != null) return photoUri;
        for (UserInfo providerUser : user.getProviderData()) {
            if (providerUser.getPhotoUrl() != null) return providerUser.getPhotoUrl();
        }
        return null;
    }

    private void cargarDetallesPerfil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            mostrarDialogoPerfil();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String storedName = snapshot.child("nombre").getValue(String.class);
                String storedEmail = snapshot.child("correo").getValue(String.class);
                String storedRole = snapshot.child("rol").getValue(String.class);
                String storedPhoto = snapshot.child("fotoPerfil").getValue(String.class);
                if (storedName != null && !storedName.trim().isEmpty()) profileName = storedName;
                if (storedEmail != null && !storedEmail.trim().isEmpty()) profileEmail = storedEmail;
                if (storedPhoto != null && !storedPhoto.trim().isEmpty()) {
                    storedProfilePhotoUri = Uri.parse(storedPhoto);
                    if (profilePhotoUri == null || profilePhotoUri.toString().trim().isEmpty()) {
                        profilePhotoUri = storedProfilePhotoUri;
                    }
                }
                if (storedRole != null && !storedRole.trim().isEmpty()) {
                    profileRole = esRolAdministrador(storedRole)
                            ? getString(R.string.role_admin)
                            : getString(R.string.role_customer);
                }
                mostrarDialogoPerfil();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mostrarDialogoPerfil();
            }
        });
    }

    private void mostrarDialogoPerfil() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_details, null);
        ImageView avatar = dialogView.findViewById(R.id.iv_profile_detail_avatar);
        TextView name = dialogView.findViewById(R.id.tv_profile_detail_name);
        TextView email = dialogView.findViewById(R.id.tv_profile_detail_email);
        TextView role = dialogView.findViewById(R.id.tv_profile_detail_role);

        String nameValue = profileName != null && !profileName.trim().isEmpty()
                ? profileName : getString(R.string.role_customer);
        String emailValue = profileEmail != null && !profileEmail.trim().isEmpty()
                ? profileEmail : getString(R.string.label_no_email);
        String roleValue = profileRole != null && !profileRole.trim().isEmpty()
                ? profileRole : getString(R.string.role_customer);
        name.setText(nameValue);
        email.setText(emailValue);
        role.setText(roleValue);

        if (profilePhoto != null) {
            avatar.setImageTintList(null);
            avatar.setImageBitmap(profilePhoto);
        } else {
            cargarFoto(avatar);
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void cargarFoto(ImageView imageView) {
        if (imageView == null || profilePhotoUri == null) return;
        if (profilePhoto != null) {
            imageView.setImageTintList(null);
            imageView.setImageBitmap(profilePhoto);
            return;
        }

        imageExecutor.execute(() -> {
            Bitmap bitmap = descargarFoto(profilePhotoUri);
            Uri fallbackUri = storedProfilePhotoUri;
            if (bitmap == null && fallbackUri != null && !fallbackUri.equals(profilePhotoUri)) {
                bitmap = descargarFoto(fallbackUri);
            }
            Bitmap loadedBitmap = bitmap;
            runOnUiThread(() -> {
                if (loadedBitmap != null && !isFinishing()) {
                    profilePhoto = loadedBitmap;
                    imageView.setImageTintList(null);
                    imageView.setImageBitmap(loadedBitmap);
                } else if (loadedBitmap == null) {
                    Log.w("MainActivity8", "No se pudo descargar la foto de perfil desde Google/Firebase.");
                }
            });
        });
    }

    private Bitmap descargarFoto(Uri photoUri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(photoUri.toString()).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "TechnoCat-Android");
            connection.setRequestProperty("Accept", "image/*");
            connection.connect();
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception e) {
            Log.w("MainActivity8", "Falló la descarga de la foto de perfil.", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        imageExecutor.shutdownNow();
        super.onDestroy();
    }
}
