package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

public class MainActivity2 extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private FirebaseAuth auth;
    private DatabaseReference database;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.inicio_sesion);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("usuarios");
        credentialManager = CredentialManager.create(getBaseContext());

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        PasswordVisibilityToggle.bind(etPassword, findViewById(R.id.btn_toggle_login_password));

        Button btnLogin = findViewById(R.id.btn_login);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> iniciarSesion());
        }

        View btnGoogle = findViewById(R.id.btn_google);
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> iniciarSesionConGoogle());
        }

        TextView txtRegistrar = findViewById(R.id.txtRegistrar);
        if (txtRegistrar != null) {
            txtRegistrar.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
                startActivity(intent);
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            KeyboardUtils.applyImeInsets(v, insets);
            return insets;
        });
    }

    private void iniciarSesion() {
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            if (etEmail != null) etEmail.setError(getString(R.string.err_enter_login_email));
            return;
        }

        if (password.isEmpty()) {
            if (etPassword != null) etPassword.setError(getString(R.string.err_enter_login_password));
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            verificarRolYRedirigir(user.getUid(), email);
                        }
                    } else {
                        String msg = getString(R.string.msg_login_generic_error);
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            msg = task.getException().getMessage();
                        }
                        Toast.makeText(MainActivity2.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.hideKeyboardWhenTappedOutside(this, event);
        return super.dispatchTouchEvent(event);
    }

    private void iniciarSesionConGoogle() {
        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId.trim().isEmpty()) {
            Toast.makeText(this, R.string.msg_google_setup_required, Toast.LENGTH_LONG).show();
            return;
        }

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse result) {
                        procesarCredencialGoogle(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof NoCredentialException) {
                            Toast.makeText(MainActivity2.this, R.string.msg_google_no_credential,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        String detail = e.getMessage() != null ? e.getMessage() : getString(R.string.msg_google_login_error);
                        Toast.makeText(MainActivity2.this, getString(
                                R.string.msg_google_login_error_detail,
                                e.getClass().getSimpleName(),
                                detail), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void procesarCredencialGoogle(Credential credential) {
        if (!(credential instanceof CustomCredential)
                || !TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            Toast.makeText(this, R.string.msg_google_login_error, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(
                    ((CustomCredential) credential).getData());
            firebaseAuthWithGoogle(
                    googleCredential.getIdToken(),
                    googleCredential.getDisplayName(),
                    googleCredential.getProfilePictureUri());
        } catch (RuntimeException e) {
            String detail = e.getMessage() != null ? e.getMessage() : getString(R.string.msg_google_login_error);
            Toast.makeText(this, getString(
                    R.string.msg_google_login_error_detail,
                    e.getClass().getSimpleName(),
                    detail), Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken, String googleName, Uri googlePhotoUri) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            boolean needsProfileUpdate =
                                    (isEmpty(user.getDisplayName()) && !isEmpty(googleName))
                                            || (user.getPhotoUrl() == null && googlePhotoUri != null);
                            if (needsProfileUpdate) {
                                UserProfileChangeRequest.Builder profileRequest = new UserProfileChangeRequest.Builder();
                                if (isEmpty(user.getDisplayName()) && !isEmpty(googleName)) {
                                    profileRequest.setDisplayName(googleName);
                                }
                                if (user.getPhotoUrl() == null && googlePhotoUri != null) {
                                    profileRequest.setPhotoUri(googlePhotoUri);
                                }
                                user.updateProfile(profileRequest.build())
                                        .addOnCompleteListener(updateTask ->
                                                guardarONoUsuarioDatabase(user, googleName, googlePhotoUri));
                            } else {
                                guardarONoUsuarioDatabase(user, googleName, googlePhotoUri);
                            }
                        }
                    } else {
                        String msg = getString(R.string.msg_google_login_error);
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            msg += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(MainActivity2.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarONoUsuarioDatabase(FirebaseUser user, String googleName, Uri googlePhotoUri) {
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String nombre = !isEmpty(user.getDisplayName())
                ? user.getDisplayName()
                : (!isEmpty(googleName) ? googleName : getString(R.string.google_default_user_name));
        String telefono = user.getPhoneNumber();

        database.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", uid);
                    userMap.put("nombre", nombre);
                    userMap.put("correo", email);
                    userMap.put("email", email);
                    userMap.put("rol", "cliente");
                    userMap.put("identificacion", "");
                    userMap.put("telefono", telefono != null ? telefono : "");
                    userMap.put("proveedorAutenticacion", "google");
                    String photoUrl = obtenerFotoPerfil(user, googlePhotoUri);
                    if (photoUrl != null) userMap.put("fotoPerfil", photoUrl);
                    database.child(uid).setValue(userMap)
                            .addOnSuccessListener(unused -> verificarRolYRedirigir(uid, email))
                            .addOnFailureListener(error -> {
                                auth.signOut();
                                Toast.makeText(MainActivity2.this, R.string.msg_google_database_error, Toast.LENGTH_LONG).show();
                            });
                    return;
                }
                String photoUrl = obtenerFotoPerfil(user, googlePhotoUri);
                if (photoUrl != null && !snapshot.child("fotoPerfil").exists()) {
                    database.child(uid).child("fotoPerfil").setValue(photoUrl);
                }
                String telefonoGuardado = snapshot.child("telefono").getValue(String.class);
                if (!isEmpty(telefono) && isEmpty(telefonoGuardado)) {
                    database.child(uid).child("telefono").setValue(telefono);
                }
                verificarRolYRedirigir(uid, email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                auth.signOut();
                Toast.makeText(MainActivity2.this, R.string.msg_google_database_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String obtenerFotoPerfil(FirebaseUser user, Uri googlePhotoUri) {
        if (user.getPhotoUrl() != null) return user.getPhotoUrl().toString();
        for (UserInfo providerUser : user.getProviderData()) {
            if (providerUser.getPhotoUrl() != null) return providerUser.getPhotoUrl().toString();
        }
        return googlePhotoUri != null ? googlePhotoUri.toString() : null;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void verificarRolYRedirigir(String uid, String email) {
        database.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String rol = snapshot.child("rol").getValue(String.class);

                if ("admin".equalsIgnoreCase(rol) || "admin@technocat.com".equalsIgnoreCase(email)) {
                    Toast.makeText(MainActivity2.this, R.string.msg_admin_access, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity2.this, MainActivity10.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity2.this, R.string.msg_welcome, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity2.this, MainActivity4.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity2.this, R.string.msg_verification_error, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity2.this, MainActivity4.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
