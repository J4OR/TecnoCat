package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

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

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        ImageView ivMenu = findViewById(R.id.iv_menu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity6.this, MainActivity9.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
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
            cargarDetallesDesdeFirebase(productId);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                if (tvPrice != null && precioObj != null) tvPrice.setText("$" + precioObj.toString());
                if (tvDesc != null && desc != null && !desc.isEmpty()) tvDesc.setText(desc);

                cargarImagen(imgUri, imgBase64);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void cargarImagen(String imgUri, String imgBase64) {
        if (ivProduct == null) return;

        if (imgUri != null && !imgUri.isEmpty()) {
            try {
                ivProduct.setImageURI(Uri.parse(imgUri));
            } catch (Exception e) {
                ivProduct.setImageResource(R.drawable.gato__1_);
            }
        } else if (imgBase64 != null && !imgBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imgBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivProduct.setImageBitmap(decodedByte);
            } catch (Exception e) {
                ivProduct.setImageResource(R.drawable.gato__1_);
            }
        } else {
            ivProduct.setImageResource(R.drawable.gato__1_);
        }
    }
}