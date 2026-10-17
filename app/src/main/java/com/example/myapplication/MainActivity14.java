package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity14 extends AppCompatActivity {

    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.delete_product);

        if (getIntent() != null) {
            productId = getIntent().getStringExtra("PRODUCT_ID");
        }

        Button btnDelete = findViewById(R.id.btn_delete);
        Button btnCancel = findViewById(R.id.btn_cancel);
        ImageView ivClose = findViewById(R.id.iv_close);

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> eliminarProducto());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        if (ivClose != null) {
            ivClose.setOnClickListener(v -> finish());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void eliminarProducto() {
        if (productId != null && !productId.isEmpty()) {
            DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");
            productosRef.child(productId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity14.this, "Producto eliminado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity14.this, "Error al eliminar el producto", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No se especificó un producto a eliminar", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}