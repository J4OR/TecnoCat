package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddEditUserActivity extends AppCompatActivity {

    private String userId;
    private DatabaseReference usuariosRef;

    private TextView tvTitle;
    private EditText etNombre, etIdentificacion, etTelefono, etCorreo, etContrasena, etConfirmarContrasena;
    private RadioGroup rgRol;
    private RadioButton rbUsuario, rbAdmin;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_user);

        if (getIntent() != null) {
            userId = getIntent().getStringExtra("USER_ID");
        }

        usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios");

        tvTitle = findViewById(R.id.tv_title);
        etNombre = findViewById(R.id.etNombreCompleto);
        etIdentificacion = findViewById(R.id.etIdentificacion);
        etTelefono = findViewById(R.id.etTelefono);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena);
        rgRol = findViewById(R.id.rgRol);
        rbUsuario = findViewById(R.id.rbUsuario);
        rbAdmin = findViewById(R.id.rbAdmin);
        btnGuardar = findViewById(R.id.btnGuardarUsuario);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        if (userId != null && !userId.isEmpty()) {
            if (tvTitle != null) {
                tvTitle.setText(R.string.edit_user_title);
            }
            if (btnGuardar != null) {
                btnGuardar.setText(R.string.btn_update_user);
            }
            cargarDatosUsuario();
        } else {
            if (tvTitle != null) {
                tvTitle.setText(R.string.add_user_title);
            }
            if (btnGuardar != null) {
                btnGuardar.setText(R.string.btn_save_user);
            }
        }

        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> guardarUsuario());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarDatosUsuario() {
        if (userId == null) return;

        usuariosRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String nombre = snapshot.child("nombre").getValue(String.class);
                String identificacion = snapshot.child("identificacion").getValue(String.class);
                String telefono = snapshot.child("telefono").getValue(String.class);
                String correo = snapshot.child("correo").getValue(String.class);
                if (correo == null) {
                    correo = snapshot.child("email").getValue(String.class);
                }
                String contrasena = snapshot.child("contrasena").getValue(String.class);
                if (contrasena == null) {
                    contrasena = snapshot.child("password").getValue(String.class);
                }
                String rol = snapshot.child("rol").getValue(String.class);

                if (etNombre != null && nombre != null) etNombre.setText(nombre);
                if (etIdentificacion != null && identificacion != null) etIdentificacion.setText(identificacion);
                if (etTelefono != null && telefono != null) etTelefono.setText(telefono);
                if (etCorreo != null && correo != null) etCorreo.setText(correo);
                if (etContrasena != null && contrasena != null) etContrasena.setText(contrasena);
                if (etConfirmarContrasena != null && contrasena != null) etConfirmarContrasena.setText(contrasena);

                if (rgRol != null) {
                    if ("admin".equalsIgnoreCase(rol) || "administrador".equalsIgnoreCase(rol)) {
                        rbAdmin.setChecked(true);
                    } else {
                        rbUsuario.setChecked(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddEditUserActivity.this, "Error al cargar datos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarUsuario() {
        String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
        String identificacion = etIdentificacion != null ? etIdentificacion.getText().toString().trim() : "";
        String telefono = etTelefono != null ? etTelefono.getText().toString().trim() : "";
        String correo = etCorreo != null ? etCorreo.getText().toString().trim() : "";
        String contrasena = etContrasena != null ? etContrasena.getText().toString().trim() : "";
        String confirmarContrasena = etConfirmarContrasena != null ? etConfirmarContrasena.getText().toString().trim() : "";

        String rol = (rbAdmin != null && rbAdmin.isChecked()) ? "admin" : "usuario";

        // Validaciones
        if (nombre.isEmpty()) {
            if (etNombre != null) etNombre.setError("Ingresa el nombre completo");
            return;
        }

        if (identificacion.isEmpty()) {
            if (etIdentificacion != null) etIdentificacion.setError("Ingresa el número de identificación");
            return;
        }

        if (telefono.isEmpty()) {
            if (etTelefono != null) etTelefono.setError("Ingresa el número de teléfono");
            return;
        }

        if (correo.isEmpty()) {
            if (etCorreo != null) etCorreo.setError("Ingresa el correo electrónico");
            return;
        }

        // Si es usuario nuevo o ingresó contraseña, validar coincidencia
        boolean esNuevo = (userId == null || userId.isEmpty());
        if (esNuevo || !contrasena.isEmpty()) {
            if (contrasena.isEmpty()) {
                if (etContrasena != null) etContrasena.setError("Ingresa la contraseña");
                return;
            }

            if (contrasena.length() < 6) {
                if (etContrasena != null) etContrasena.setError("La contraseña debe tener al menos 6 caracteres");
                return;
            }

            if (!contrasena.equals(confirmarContrasena)) {
                if (etConfirmarContrasena != null) etConfirmarContrasena.setError("Las contraseñas no coinciden");
                return;
            }
        }

        String targetUserId = userId;
        if (targetUserId == null || targetUserId.isEmpty()) {
            targetUserId = usuariosRef.push().getKey();
        }

        if (targetUserId == null) {
            Toast.makeText(this, "Error al generar ID para el usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", targetUserId);
        userMap.put("nombre", nombre);
        userMap.put("identificacion", identificacion);
        userMap.put("telefono", telefono);
        userMap.put("correo", correo);
        userMap.put("contrasena", contrasena);
        userMap.put("rol", rol);

        usuariosRef.child(targetUserId).updateChildren(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEditUserActivity.this, "Usuario guardado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AddEditUserActivity.this, "Error al guardar usuario: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
