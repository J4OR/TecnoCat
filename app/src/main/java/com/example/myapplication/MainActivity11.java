package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
        mostrarCargando();

        TextView btnAddProduct = findViewById(R.id.btn_add_product);
        if (btnAddProduct != null) {
            btnAddProduct.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity11.this, MainActivity12.class);
                startActivity(intent);
            });
        }

        cargarProductos();

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

    private void cargarProductos() {
        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (llProductsContainer == null) return;
                llProductsContainer.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    mostrarMensaje(getString(R.string.empty_products_admin));
                    return;
                }

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String id = productSnapshot.child("id").getValue(String.class);
                    String nombre = productSnapshot.child("nombre").getValue(String.class);
                    Object precioObj = productSnapshot.child("precio").getValue();
                    String imagenUri = productSnapshot.child("imagenUri").getValue(String.class);
                    String imagenBase64 = productSnapshot.child("imagenBase64").getValue(String.class);
                    Boolean activo = productSnapshot.child("activo").getValue(Boolean.class);
                    boolean productoActivo = activo == null || activo;

                    View itemView = getLayoutInflater().inflate(R.layout.item_product, llProductsContainer, false);

                    TextView tvName = itemView.findViewById(R.id.tv_product_name);
                    TextView tvPrice = itemView.findViewById(R.id.tv_product_price);
                    TextView tvStatus = itemView.findViewById(R.id.tv_product_status);
                    ImageView ivImage = itemView.findViewById(R.id.iv_product_image);
                    ImageView ivDelete = itemView.findViewById(R.id.iv_delete);

                    if (tvName != null) {
                        tvName.setText(nombre != null ? nombre : "Sin nombre");
                    }

                    if (tvPrice != null) {
                        if (precioObj != null) {
                            tvPrice.setText(getString(R.string.price_amount_format, PriceUtils.format(precioObj)));
                        } else {
                            tvPrice.setText(R.string.price_zero);
                        }
                    }

                    if (tvStatus != null) {
                        tvStatus.setText(productoActivo ? R.string.status_active : R.string.status_inactive);
                        tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(
                                MainActivity11.this,
                                productoActivo ? R.color.brand_green : R.color.logout_red));
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
                mostrarMensaje(getString(R.string.error_loading_products));
                Toast.makeText(MainActivity11.this, "Error al cargar productos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarCargando() {
        if (llProductsContainer == null) return;
        llProductsContainer.removeAllViews();
        ProgressBar progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setContentDescription(getString(R.string.loading_products));
        llProductsContainer.addView(progress, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void mostrarMensaje(String mensaje) {
        if (llProductsContainer == null) return;
        llProductsContainer.removeAllViews();
        TextView estado = new TextView(this);
        estado.setText(mensaje);
        estado.setTextSize(16);
        estado.setPadding(16, 24, 16, 24);
        estado.setGravity(android.view.Gravity.CENTER);
        estado.setFocusable(true);
        llProductsContainer.addView(estado, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }
}
