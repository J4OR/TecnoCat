package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class MainActivity5 extends AppCompatActivity {

    private GridLayout glProductsContainer;
    private EditText etSearch;
    private Button btnFilterAll, btnFilterPhones, btnFilterPcs;
    private DatabaseReference productosRef;

    private final List<DataSnapshot> allProductsList = new ArrayList<>();
    private String selectedCategory = "Todos";
    private String searchQuery = "";
    private boolean productsLoaded = false;
    private final Set<String> favoriteProductIds = new HashSet<>();
    private boolean favoritesLoaded = false;
    private boolean favoritesOnly = false;
    private boolean restoreCatalogAfterLeavingFavorites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applySavedTheme(this);

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
        favoritesOnly = "Favoritos".equalsIgnoreCase(selectedCategory);
        View filterBar = findViewById(R.id.ll_filters);
        if (!favoritesOnly && filterBar != null) {
            filterBar.setAlpha(0f);
            filterBar.setTranslationY(-8f);
            filterBar.post(() -> filterBar.animate().alpha(1f).translationY(0f).setDuration(180).start());
        }
        if (favoritesOnly) {
            if (filterBar != null) filterBar.setVisibility(View.GONE);
            TextView title = findViewById(R.id.tv_header_title);
            if (title != null) title.setText(R.string.menu_favs);
        }
        cargarFavoritosDelUsuario();

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
                    searchQuery = s.toString().trim().toLowerCase(Locale.ROOT);
                    filtrarYMostrarProductos();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        cargarProductosDesdeFirebase();

        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.CATALOG);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            KeyboardUtils.applyImeInsets(v, insets);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (restoreCatalogAfterLeavingFavorites) {
            restoreCatalogAfterLeavingFavorites = false;
            mostrarCatalogoCompleto();
        }
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.CATALOG);
    }

    @Override
    protected void onPause() {
        if (favoritesOnly) {
            restoreCatalogAfterLeavingFavorites = true;
        }
        super.onPause();
    }

    private void mostrarCatalogoCompleto() {
        favoritesOnly = false;
        selectedCategory = "Todos";

        View filterBar = findViewById(R.id.ll_filters);
        if (filterBar != null) filterBar.setVisibility(View.VISIBLE);

        TextView title = findViewById(R.id.tv_header_title);
        if (title != null) title.setText(R.string.catalog_title);

        actualizarEstiloBotonesFiltro();
        filtrarYMostrarProductos();
    }

    private void actualizarEstiloBotonesFiltro() {
        int activeBg = R.drawable.bg_add_button;
        int inactiveBg = R.drawable.bg_button_secondary;
        int activeColor = ContextCompat.getColor(this, R.color.on_primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_black);

        if (btnFilterAll != null) {
            boolean active = "Todos".equalsIgnoreCase(selectedCategory);
            btnFilterAll.setBackgroundResource(active ? activeBg : inactiveBg);
            btnFilterAll.setTextColor(active ? activeColor : inactiveColor);
            aplicarColorSombra(btnFilterAll, active);
        }

        if (btnFilterPhones != null) {
            boolean active = selectedCategory.toLowerCase(Locale.ROOT).contains("cel");
            btnFilterPhones.setBackgroundResource(active ? activeBg : inactiveBg);
            btnFilterPhones.setTextColor(active ? activeColor : inactiveColor);
            aplicarColorSombra(btnFilterPhones, active);
        }

        if (btnFilterPcs != null) {
            boolean active = selectedCategory.toLowerCase(Locale.ROOT).contains("comp");
            btnFilterPcs.setBackgroundResource(active ? activeBg : inactiveBg);
            btnFilterPcs.setTextColor(active ? activeColor : inactiveColor);
            aplicarColorSombra(btnFilterPcs, active);
        }
    }

    private void aplicarColorSombra(Button button, boolean activo) {
        int shadowColor = ContextCompat.getColor(this,
                activo ? R.color.brand_blue : R.color.text_grey);
        button.setOutlineSpotShadowColor(shadowColor);
        button.setOutlineAmbientShadowColor(shadowColor);
    }

    private void cargarProductosDesdeFirebase() {
        mostrarEstado(getString(R.string.loading_products), true);
        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot productSnap : snapshot.getChildren()) {
                        allProductsList.add(productSnap);
                    }
                }
                productsLoaded = true;
                filtrarYMostrarProductos();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                productsLoaded = true;
                mostrarEstado(getString(R.string.error_loading_products), false);
            }
        });
    }

    private void cargarFavoritosDelUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            favoritesLoaded = true;
            if (favoritesOnly) mostrarEstado(getString(R.string.favorite_login_required), false);
            return;
        }
        mostrarEstado(getString(R.string.loading_products), true);
        FirebaseDatabase.getInstance().getReference("favoritos").child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favoriteProductIds.clear();
                        for (DataSnapshot favorite : snapshot.getChildren()) {
                            if (Boolean.TRUE.equals(favorite.getValue(Boolean.class))) {
                                favoriteProductIds.add(favorite.getKey());
                            }
                        }
                        favoritesLoaded = true;
                        filtrarYMostrarProductos();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        favoritesLoaded = true;
                        mostrarEstado(getString(R.string.favorite_load_error), false);
                    }
                });
    }

    private void filtrarYMostrarProductos() {
        if (glProductsContainer == null) return;
        if (favoritesOnly && FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (productsLoaded) mostrarEstado(getString(R.string.favorite_login_required), false);
            return;
        }
        glProductsContainer.removeAllViews();

        for (DataSnapshot productSnap : allProductsList) {
            String prodId = productSnap.child("id").getValue(String.class);
            if (prodId == null || prodId.isEmpty()) {
                prodId = productSnap.getKey();
            }
            Boolean activo = productSnap.child("activo").getValue(Boolean.class);
            if (Boolean.FALSE.equals(activo)) continue;
            if (favoritesOnly && !favoriteProductIds.contains(prodId)) continue;
            String nombre = productSnap.child("nombre").getValue(String.class);
            String categoria = productSnap.child("categoria").getValue(String.class);
            String descripcion = productSnap.child("descripcion").getValue(String.class);
            Object precioObj = productSnap.child("precio").getValue();
            String imagenUri = productSnap.child("imagenUri").getValue(String.class);
            String imagenBase64 = productSnap.child("imagenBase64").getValue(String.class);

            if (nombre == null) nombre = "";
            if (categoria == null) categoria = "";

            boolean pasaCategoria = true;
            if ("Celulares".equalsIgnoreCase(selectedCategory) || selectedCategory.toLowerCase(Locale.ROOT).contains("cel")) {
                pasaCategoria = categoria.toLowerCase(Locale.ROOT).contains("cel");
            } else if ("Computadores".equalsIgnoreCase(selectedCategory) || selectedCategory.toLowerCase(Locale.ROOT).contains("comp")) {
                pasaCategoria = categoria.toLowerCase(Locale.ROOT).contains("comp");
            }

            boolean pasaBusqueda = true;
            if (!searchQuery.isEmpty()) {
                pasaBusqueda = nombre.toLowerCase(Locale.ROOT).contains(searchQuery);
            }

            if (pasaCategoria && pasaBusqueda) {
                View itemView = getLayoutInflater().inflate(R.layout.item_catalog_product, glProductsContainer, false);

                TextView tvName = itemView.findViewById(R.id.tv_name);
                TextView tvPrice = itemView.findViewById(R.id.tv_price);
                ImageView ivProduct = itemView.findViewById(R.id.iv_product);
                ImageView ivFavorite = itemView.findViewById(R.id.iv_fav);

                if (tvName != null) {
                    tvName.setText(nombre.isEmpty() ? "Sin nombre" : nombre);
                }

                String precioTexto = getString(R.string.price_amount_format, PriceUtils.format(precioObj));
                if (tvPrice != null) {
                    tvPrice.setText(precioTexto);
                }

                if (ivProduct != null) {
                    ImageUtils.cargarImagen(MainActivity5.this, ivProduct, imagenUri, imagenBase64);
                }

                if (ivFavorite != null) {
                    boolean isFavorite = favoriteProductIds.contains(prodId);
                    ivFavorite.setImageResource(isFavorite
                            ? R.drawable.ic_star_filled
                            : R.drawable.ic_star_outline);
                    ivFavorite.setContentDescription(getString(isFavorite
                            ? R.string.favorite_accessibility_saved
                            : R.string.favorite_accessibility_add));
                }

                final String finalProdId = prodId;
                final String finalNombre = nombre;
                final String finalPrecio = precioTexto;
                final String finalDesc = descripcion != null ? descripcion : "";
                final String finalImgUri = imagenUri;
                final String finalImgBase64 = imagenBase64;

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity5.this, MainActivity6.class);
                    intent.putExtra("PRODUCT_ID", finalProdId);
                    intent.putExtra("PRODUCT_NAME", finalNombre);
                    intent.putExtra("PRODUCT_PRICE", finalPrecio);
                    intent.putExtra("PRODUCT_DESC", finalDesc);
                    intent.putExtra("PRODUCT_IMAGE_URI", finalImgUri);
                    intent.putExtra("PRODUCT_IMAGE_BASE64", finalImgBase64);
                    startActivity(intent);
                });

                glProductsContainer.addView(itemView);
            }
        }
        if (productsLoaded && (!favoritesOnly || favoritesLoaded) && glProductsContainer.getChildCount() == 0) {
            mostrarEstado(favoritesOnly ? getString(R.string.favorite_empty) : getString(R.string.empty_products_catalog), false);
        }
    }

    private void mostrarEstado(String mensaje, boolean cargando) {
        if (glProductsContainer == null) return;
        glProductsContainer.removeAllViews();
        View estado;
        if (cargando) {
            ProgressBar progress = new ProgressBar(this);
            progress.setIndeterminate(true);
            progress.setContentDescription(getString(R.string.loading_products));
            estado = progress;
        } else {
            TextView text = new TextView(this);
            text.setText(mensaje);
            text.setTextSize(16);
            text.setGravity(android.view.Gravity.CENTER);
            text.setPadding(16, 24, 16, 24);
            text.setFocusable(true);
            estado = text;
        }
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = GridLayout.LayoutParams.MATCH_PARENT;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(0, 2, 1f);
        estado.setLayoutParams(params);
        glProductsContainer.addView(estado);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.hideKeyboardWhenTappedOutside(this, event);
        return super.dispatchTouchEvent(event);
    }
}
