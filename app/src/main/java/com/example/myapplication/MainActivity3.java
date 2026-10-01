package com.example.myapplication;

import android.os.Bundle;
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
    private Button btnCrearCuenta;
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
        btnCrearCuenta = findViewById(R.id.btnGuardar);

        // Inicializar Firebase Authentication
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("usuarios");

        // Evento del botón
        btnCrearCuenta.setOnClickListener(v -> registrarUsuario());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void registrarUsuario() {

        // Obtener los datos escritos
        String nombre = etNombreCompleto.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();
        String confirmarContrasena =
                etConfirmarContrasena.getText().toString().trim();

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
}