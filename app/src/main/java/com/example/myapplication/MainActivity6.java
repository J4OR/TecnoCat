package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity6 extends AppCompatActivity {

    private ImageView ivProduct;
    private TextView tvName, tvPrice, tvDesc;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.detail_phone);

        ivProduct = findViewById(R.id.iv_product);
        tvName = findViewById(R.id.tv_detail_name);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvDesc = findViewById(R.id.tv_detail_desc);

        ImageView ivFavorite = findViewById(R.id.iv_fav);
        if (ivFavorite != null) {
            ivFavorite.setOnClickListener(v -> confirmarAgregarFavorito());
        }

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        android.view.View btnWhatsapp = findViewById(R.id.btn_whatsapp);
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> {
                String nombreProducto = tvName != null ? tvName.getText().toString() : "este producto";
                String precioProducto = tvPrice != null ? tvPrice.getText().toString() : "";
                String mensaje = "Hola, me interesa obtener más información sobre " + nombreProducto;
                if (!precioProducto.isEmpty()) {
                    mensaje += " (" + precioProducto + ")";
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://wa.me/573137458590?text=" + Uri.encode(mensaje)));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity6.this, R.string.msg_cannot_open_whatsapp, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Obtener datos pasados por Intent
        if (getIntent() != null) {
            productId = getIntent().getStringExtra("PRODUCT_ID");
            String nombre = getIntent().getStringExtra("PRODUCT_NAME");
            String precio = getIntent().getStringExtra("PRODUCT_PRICE");
            String desc = getIntent().getStringExtra("PRODUCT_DESC");
            String imgUri = getIntent().getStringExtra("PRODUCT_IMAGE_URI");
            String imgBase64 = getIntent().getStringExtra("PRODUCT_IMAGE_BASE64");

            if (nombre != null && tvName != null) tvName.setText(nombre);
            if (precio != null && tvPrice != null) tvPrice.setText(precio);
            if (desc != null && !desc.isEmpty() && tvDesc != null) tvDesc.setText(desc);

            cargarImagen(imgUri, imgBase64);
        }

        if (productId != null && !productId.isEmpty()) {
            if (ivFavorite != null) actualizarEstadoFavorito(ivFavorite);
            cargarDetallesDesdeFirebase(productId);
        }

        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.CATALOG);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.CATALOG);
    }

    private void cargarDetallesDesdeFirebase(String id) {
        DatabaseReference prodRef = FirebaseDatabase.getInstance().getReference("productos").child(id);
        prodRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String nombre = snapshot.child("nombre").getValue(String.class);
                Object precioObj = snapshot.child("precio").getValue();
                String desc = snapshot.child("descripcion").getValue(String.class);
                String imgUri = snapshot.child("imagenUri").getValue(String.class);
                String imgBase64 = snapshot.child("imagenBase64").getValue(String.class);

                if (tvName != null && nombre != null) tvName.setText(nombre);
                if (tvPrice != null && precioObj != null) {
                    tvPrice.setText(getString(R.string.price_amount_format, PriceUtils.format(precioObj)));
                }
                if (tvDesc != null && desc != null && !desc.isEmpty()) tvDesc.setText(desc);

                cargarImagen(imgUri, imgBase64);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void cargarImagen(String imgUri, String imgBase64) {
        ImageUtils.cargarImagen(this, ivProduct, imgUri, imgBase64);
    }

    private void actualizarEstadoFavorito(ImageView icono) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || productId == null || productId.isEmpty()) return;
        FirebaseDatabase.getInstance().getReference("favoritos").child(user.getUid()).child(productId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isFinishing()) {
                            mostrarIconoFavorito(icono, snapshot.exists(), snapshot.exists());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void confirmarAgregarFavorito() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.favorite_confirm_add)
                .setPositiveButton(R.string.btn_yes, (dialog, which) -> guardarFavorito())
                .setNegativeButton(R.string.btn_no, null)
                .show();
    }

    private void guardarFavorito() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.favorite_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(this, R.string.favorite_product_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseDatabase.getInstance().getReference("favoritos").child(user.getUid()).child(productId)
                .setValue(true)
                .addOnSuccessListener(unused -> {
                    ImageView icon = findViewById(R.id.iv_fav);
                    if (icon != null) mostrarIconoFavorito(icon, true, true);
                    Toast.makeText(this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> Toast.makeText(this, R.string.favorite_save_error, Toast.LENGTH_SHORT).show());
    }

    private void mostrarIconoFavorito(ImageView icon, boolean favorito, boolean animar) {
        icon.setImageResource(favorito ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        icon.setContentDescription(getString(favorito
                ? R.string.favorite_accessibility_saved
                : R.string.favorite_accessibility_add));
        if (!animar) return;
        icon.animate().cancel();
        icon.setScaleX(0.78f);
        icon.setScaleY(0.78f);
        icon.animate()
                .scaleX(1.18f)
                .scaleY(1.18f)
                .setDuration(110)
                .withEndAction(() -> icon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }
}
