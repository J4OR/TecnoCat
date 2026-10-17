package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity5 extends AppCompatActivity {

    private GridLayout glProductsContainer;
    private EditText etSearch;
    private Button btnFilterAll, btnFilterPhones, btnFilterPcs;
    private DatabaseReference productosRef;

    private final List<DataSnapshot> allProductsList = new ArrayList<>();
    private String selectedCategory = "Todos";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.catalog_phone);

        glProductsContainer = findViewById(R.id.gl_products_container);
        etSearch = findViewById(R.id.et_search);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterPhones = findViewById(R.id.btn_filter_phones);
        btnFilterPcs = findViewById(R.id.btn_filter_pcs);

        productosRef = FirebaseDatabase.getInstance().getReference("productos");

        if (getIntent() != null && getIntent().hasExtra("FILTER_CATEGORY")) {
            String catParam = getIntent().getStringExtra("FILTER_CATEGORY");
            if (catParam != null && !catParam.isEmpty()) {
                selectedCategory = catParam;
            }
        }

        actualizarEstiloBotonesFiltro();

        ImageView ivMenu = findViewById(R.id.iv_menu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity5.this, MainActivity9.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (btnFilterAll != null) {
            btnFilterAll.setOnClickListener(v -> {
                selectedCategory = "Todos";
                actualizarEstiloBotonesFiltro();
                filtrarYMostrarProductos();
            });
        }

        if (btnFilterPhones != null) {
            btnFilterPhones.setOnClickListener(v -> {
                selectedCategory = "Celulares";
                actualizarEstiloBotonesFiltro();
                filtrarYMostrarProductos();
            });
        }

        if (btnFilterPcs != null) {
            btnFilterPcs.setOnClickListener(v -> {
                selectedCategory = "Computadores";
                actualizarEstiloBotonesFiltro();
                filtrarYMostrarProductos();
            });
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase();
                    filtrarYMostrarProductos();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        cargarProductosDesdeFirebase();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void actualizarEstiloBotonesFiltro() {
        int activeBg = R.drawable.bg_add_button;
        int inactiveBg = R.drawable.bg_button_secondary;
        int activeColor = ContextCompat.getColor(this, R.color.white);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_black);

        if (btnFilterAll != null) {
            boolean active = "Todos".equalsIgnoreCase(selectedCategory);
            btnFilterAll.setBackgroundResource(active ? activeBg : inactiveBg);
            btnFilterAll.setTextColor(active ? activeColor : inactiveColor);
        }

        if (btnFilterPhones != null) {
            boolean active = selectedCategory.toLowerCase().contains("cel");
            btnFilterPhones.setBackgroundResource(active ? activeBg : inactiveBg);
            btnFilterPhones.setTextColor(active ? activeColor : inactiveColor);
        }

        if (btnFilterPcs != null) {
            boolean active = selectedCategory.toLowerCase().contains("comp");
            btnFilterPcs.setBackgroundResource(active ? activeBg : inactiveBg);
            btnFilterPcs.setTextColor(active ? activeColor : inactiveColor);
        }
    }

    private void cargarProductosDesdeFirebase() {
        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot productSnap : snapshot.getChildren()) {
                        allProductsList.add(productSnap);
                    }
                }
                filtrarYMostrarProductos();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void filtrarYMostrarProductos() {
        if (glProductsContainer == null) return;
        glProductsContainer.removeAllViews();

        for (DataSnapshot productSnap : allProductsList) {
            String nombre = productSnap.child("nombre").getValue(String.class);
            String categoria = productSnap.child("categoria").getValue(String.class);
            Object precioObj = productSnap.child("precio").getValue();
            String imagenUri = productSnap.child("imagenUri").getValue(String.class);
            String imagenBase64 = productSnap.child("imagenBase64").getValue(String.class);

            if (nombre == null) nombre = "";
            if (categoria == null) categoria = "";

            boolean pasaCategoria = true;
            if ("Celulares".equalsIgnoreCase(selectedCategory) || selectedCategory.toLowerCase().contains("cel")) {
                pasaCategoria = categoria.toLowerCase().contains("cel");
            } else if ("Computadores".equalsIgnoreCase(selectedCategory) || selectedCategory.toLowerCase().contains("comp")) {
                pasaCategoria = categoria.toLowerCase().contains("comp");
            }

            boolean pasaBusqueda = true;
            if (!searchQuery.isEmpty()) {
                pasaBusqueda = nombre.toLowerCase().contains(searchQuery);
            }

            if (pasaCategoria && pasaBusqueda) {
                View itemView = getLayoutInflater().inflate(R.layout.item_catalog_product, glProductsContainer, false);

                TextView tvName = itemView.findViewById(R.id.tv_name);
                TextView tvPrice = itemView.findViewById(R.id.tv_price);
                ImageView ivProduct = itemView.findViewById(R.id.iv_product);

                if (tvName != null) {
                    tvName.setText(nombre.isEmpty() ? "Sin nombre" : nombre);
                }

                if (tvPrice != null) {
                    tvPrice.setText(precioObj != null ? "$" + precioObj.toString() : "$0");
                }

                if (ivProduct != null) {
                    if (imagenUri != null && !imagenUri.isEmpty()) {
                        try {
                            ivProduct.setImageURI(Uri.parse(imagenUri));
                        } catch (Exception e) {
                            ivProduct.setImageResource(R.drawable.gato__1_);
                        }
                    } else if (imagenBase64 != null && !imagenBase64.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(imagenBase64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivProduct.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            ivProduct.setImageResource(R.drawable.gato__1_);
                        }
                    } else {
                        ivProduct.setImageResource(R.drawable.gato__1_);
                    }
                }

                glProductsContainer.addView(itemView);
            }
        }
    }
}