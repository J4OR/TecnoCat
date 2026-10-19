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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity11 extends AppCompatActivity {

    private LinearLayout llProductsContainer;
    private DatabaseReference productosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_product);

        llProductsContainer = findViewById(R.id.ll_products_container);
        productosRef = FirebaseDatabase.getInstance().getReference("productos");

        TextView btnAddProduct = findViewById(R.id.btn_add_product);
        if (btnAddProduct != null) {
            btnAddProduct.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity11.this, MainActivity12.class);
                startActivity(intent);
            });
        }

        ImageView ivMenu = findViewById(R.id.iv_menu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity11.this, MainActivity9.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        cargarProductos();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarProductos() {
        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llProductsContainer == null) return;
                llProductsContainer.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    return;
                }

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String id = productSnapshot.child("id").getValue(String.class);
                    String nombre = productSnapshot.child("nombre").getValue(String.class);
                    Object precioObj = productSnapshot.child("precio").getValue();
                    String imagenUri = productSnapshot.child("imagenUri").getValue(String.class);
                    String imagenBase64 = productSnapshot.child("imagenBase64").getValue(String.class);

                    View itemView = getLayoutInflater().inflate(R.layout.item_product, llProductsContainer, false);

                    TextView tvName = itemView.findViewById(R.id.tv_product_name);
                    TextView tvPrice = itemView.findViewById(R.id.tv_product_price);
                    ImageView ivImage = itemView.findViewById(R.id.iv_product_image);
                    ImageView ivDelete = itemView.findViewById(R.id.iv_delete);

                    if (tvName != null) {
                        tvName.setText(nombre != null ? nombre : "Sin nombre");
                    }

                    if (tvPrice != null) {
                        if (precioObj != null) {
                            tvPrice.setText("$" + precioObj.toString());
                        } else {
                            tvPrice.setText("$0");
                        }
                    }

                    if (ivImage != null) {
                        ImageUtils.cargarImagen(MainActivity11.this, ivImage, imagenUri, imagenBase64);
                    }

                    ImageView ivEdit = itemView.findViewById(R.id.iv_edit);

                    if (ivDelete != null && id != null) {
                        ivDelete.setOnClickListener(v -> {
                            Intent intent = new Intent(MainActivity11.this, MainActivity14.class);
                            intent.putExtra("PRODUCT_ID", id);
                            startActivity(intent);
                        });
                    }

                    if (ivEdit != null && id != null) {
                        ivEdit.setOnClickListener(v -> {
                            Intent intent = new Intent(MainActivity11.this, MainActivity13.class);
                            intent.putExtra("PRODUCT_ID", id);
                            startActivity(intent);
                        });
                    }

                    llProductsContainer.addView(itemView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity11.this, "Error al cargar productos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}