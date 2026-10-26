package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ManageUsersActivity extends AppCompatActivity {

    private LinearLayout llUsersContainer;
    private DatabaseReference usuariosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);

        verificarAccesoAdmin();

        llUsersContainer = findViewById(R.id.ll_users_container);
        usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios");

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        View btnAddUser = findViewById(R.id.btn_add_user);
        if (btnAddUser != null) {
            btnAddUser.setOnClickListener(v -> {
                Intent intent = new Intent(ManageUsersActivity.this, AddEditUserActivity.class);
                startActivity(intent);
            });
        }

        cargarUsuarios();

        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.NONE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.NONE);
    }

    private void verificarAccesoAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.msg_access_denied_admin, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String email = currentUser.getEmail();
        if ("admin@technocat.com".equalsIgnoreCase(email)) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(currentUser.getUid());
        userRef.child("rol").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String rol = snapshot.getValue(String.class);
                if (!"admin".equalsIgnoreCase(rol) && !"administrador".equalsIgnoreCase(rol)) {
                    Toast.makeText(ManageUsersActivity.this, R.string.msg_access_restricted_admin, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this, R.string.msg_permission_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void cargarUsuarios() {
        usuariosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llUsersContainer == null) return;
                llUsersContainer.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    return;
                }

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String userId = userSnap.getKey();
                    String nombre = userSnap.child("nombre").getValue(String.class);
                    String correo = userSnap.child("correo").getValue(String.class);
                    if (correo == null) {
                        correo = userSnap.child("email").getValue(String.class);
                    }
                    View itemView = getLayoutInflater().inflate(R.layout.item_user, llUsersContainer, false);

                    TextView tvName = itemView.findViewById(R.id.tv_user_name);
                    TextView tvEmail = itemView.findViewById(R.id.tv_user_email);
                    ImageView ivEdit = itemView.findViewById(R.id.iv_edit);
                    ImageView ivDelete = itemView.findViewById(R.id.iv_delete);

                    if (tvName != null) {
                        tvName.setText(nombre != null && !nombre.isEmpty() ? nombre : getString(R.string.label_no_name));
                    }

                    if (tvEmail != null) {
                        tvEmail.setText(correo != null && !correo.isEmpty() ? correo : getString(R.string.label_no_email));
                    }

                    final String finalUserId = userId;
                    if (ivEdit != null && finalUserId != null) {
                        ivEdit.setOnClickListener(v -> {
                            Intent intent = new Intent(ManageUsersActivity.this, AddEditUserActivity.class);
                            intent.putExtra("USER_ID", finalUserId);
                            startActivity(intent);
                        });
                    }

                    if (ivDelete != null && finalUserId != null) {
                        ivDelete.setOnClickListener(v -> confirmarEliminarUsuario(finalUserId, nombre));
                    }

                    llUsersContainer.addView(itemView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this, "Error al cargar usuarios: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarEliminarUsuario(String userId, String nombre) {
        String nombreMostrado = nombre != null ? nombre : getString(R.string.label_this_user);
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_user_title)
                .setMessage(getString(R.string.delete_user_confirm_msg, nombreMostrado))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    usuariosRef.child(userId).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(ManageUsersActivity.this, R.string.msg_user_deleted, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(ManageUsersActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}
