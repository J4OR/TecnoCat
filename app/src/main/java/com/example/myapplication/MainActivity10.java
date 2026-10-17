package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class MainActivity10 extends AppCompatActivity {

    private TextView tvTotalCount, tvCelCount, tvCompCount;
    private DatabaseReference productosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_panel);

        tvTotalCount = findViewById(R.id.tv_total_count);
        tvCelCount = findViewById(R.id.tv_cel_count);
        tvCompCount = findViewById(R.id.tv_comp_count);

        productosRef = FirebaseDatabase.getInstance().getReference("productos");

        ImageView ivMenuDrawer = findViewById(R.id.iv_menu_drawer);
        if (ivMenuDrawer != null) {
            ivMenuDrawer.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity10.this, MainActivity9.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        View clManageProducts = findViewById(R.id.cl_manage_products);
        if (clManageProducts != null) {
            clManageProducts.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity10.this, MainActivity11.class);
                startActivity(intent);
            });
        }

        cargarContadoresReales();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarContadoresReales() {
        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    actualizarTextos(0, 0, 0);
                    return;
                }

                long totalCount = snapshot.getChildrenCount();
                long celCount = 0;
                long compCount = 0;

                for (DataSnapshot productSnap : snapshot.getChildren()) {
                    String categoria = productSnap.child("categoria").getValue(String.class);
                    if (categoria != null) {
                        String catLower = categoria.toLowerCase().trim();
                        if (catLower.contains("cel")) {
                            celCount++;
                        } else if (catLower.contains("comp")) {
                            compCount++;
                        }
                    }
                }

                actualizarTextos(totalCount, celCount, compCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void actualizarTextos(long total, long cel, long comp) {
        if (tvTotalCount != null) {
            tvTotalCount.setText(String.valueOf(total));
        }
        if (tvCelCount != null) {
            tvCelCount.setText(String.valueOf(cel));
        }
        if (tvCompCount != null) {
            tvCompCount.setText(String.valueOf(comp));
        }
    }
}