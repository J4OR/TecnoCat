package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity3 extends AppCompatActivity {
    private EditText etNombreCompleto, etCorreo, etContrasena, etConfirmarContrasena;
    private Button btnCrearCuenta, btnCancel;
    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.create_account);

        // Conectar los elementos del XML con Java
        etNombreCompleto = findViewById(R.id.etNombreCompleto);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena);
        PasswordVisibilityToggle.bind(etContrasena, findViewById(R.id.btn_toggle_register_password));
        PasswordVisibilityToggle.bind(etConfirmarContrasena,
                findViewById(R.id.btn_toggle_confirm_password));
        btnCrearCuenta = findViewById(R.id.btnGuardar);
        btnCancel = findViewById(R.id.btn_cancel);

        // Inicializar Firebase Authentication
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("usuarios");

        // Evento del botón para crear cuenta
        if (btnCrearCuenta != null) {
            btnCrearCuenta.setOnClickListener(v -> registrarUsuario());
        }

        // Evento del botón para cancelar y regresar a iniciar sesión
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity3.this, MainActivity2.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            KeyboardUtils.applyImeInsets(v, insets);
            return insets;
        });
    }

    private void registrarUsuario() {

        // Obtener los datos escritos
        String nombre = etNombreCompleto != null ? etNombreCompleto.getText().toString().trim() : "";
        String correo = etCorreo != null ? etCorreo.getText().toString().trim() : "";
        String contrasena = etContrasena != null ? etContrasena.getText().toString().trim() : "";
        String confirmarContrasena = etConfirmarContrasena != null ? etConfirmarContrasena.getText().toString().trim() : "";

        // Verificar campos vacíos
        if (nombre.isEmpty() ||
                correo.isEmpty() ||
                contrasena.isEmpty() ||
                confirmarContrasena.isEmpty()) {

            Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Verificar que las contraseñas coincidan
        if (!contrasena.equals(confirmarContrasena)) {

            Toast.makeText(
                    this,
                    "Las contraseñas no coinciden",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Registrar usuario en Firebase
        auth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        String uid = auth.getCurrentUser().getUid();

                        database.child(uid).child("nombre").setValue(nombre);
                        database.child(uid).child("correo").setValue(correo);
                        database.child(uid).child("rol").setValue("cliente");

                        Toast.makeText(
                                this,
                                "Cuenta creada correctamente",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    } else {

                        // Obtener el error de Firebase
                        String mensaje = "Error al crear la cuenta";

                        if (task.getException() != null && task.getException().getLocalizedMessage() != null) {
                            mensaje = task.getException().getLocalizedMessage();
                        }

                        Toast.makeText(
                                this,
                                mensaje,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.hideKeyboardWhenTappedOutside(this, event);
        return super.dispatchTouchEvent(event);
    }
}
