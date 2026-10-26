package com.example.myapplication;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity4 extends AppCompatActivity {

    private static final long BANNER_INTERVAL_MS = 8_000L;
    private static final long BANNER_TRANSITION_MS = 350L;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final int[] bannerImages = {
            R.drawable.banner_technocat,
            R.drawable.banner_promo_mes,
            R.drawable.banner_celulares,
            R.drawable.banner_gaming,
            R.drawable.banner_alto_rendimiento
    };
    private ImageView bannerImageView;
    private int currentBannerIndex;
    private final Runnable bannerSlideTask = new Runnable() {
        @Override
        public void run() {
            if (bannerImageView == null || bannerImages.length < 2) return;

            int nextIndex = (currentBannerIndex + 1) % bannerImages.length;
            float slideDistance = Math.max(1f, bannerImageView.getWidth() * 0.12f);
            bannerImageView.animate().cancel();
            bannerImageView.animate()
                    .alpha(0f)
                    .translationX(-slideDistance)
                    .setDuration(BANNER_TRANSITION_MS)
                    .withEndAction(() -> {
                        if (bannerImageView == null) return;
                        currentBannerIndex = nextIndex;
                        bannerImageView.setImageResource(bannerImages[currentBannerIndex]);
                        bannerImageView.setTranslationX(slideDistance);
                        bannerImageView.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(BANNER_TRANSITION_MS)
                                .start();
                    })
                    .start();
            bannerHandler.postDelayed(this, BANNER_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.applySavedTheme(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.menu);
        bannerImageView = findViewById(R.id.iv_banner_img);

        // Clic en categoría Celulares
        View clCatCelulares = findViewById(R.id.cl_cat_celulares);
        if (clCatCelulares != null) {
            clCatCelulares.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity5.class);
                intent.putExtra("FILTER_CATEGORY", "Celulares");
                startActivity(intent);
            });
        }

        // Clic en categoría Computadoras
        View clCatComputadoras = findViewById(R.id.cl_cat_computadoras);
        if (clCatComputadoras != null) {
            clCatComputadoras.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity5.class);
                intent.putExtra("FILTER_CATEGORY", "Computadores");
                startActivity(intent);
            });
        }

        cargarProductosDestacados();

        // Configurar barra de navegación inferior
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.HOME);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarProductosDestacados() {
        TextView featuredState = findViewById(R.id.tv_featured_state);
        HorizontalScrollView featuredScroll = findViewById(R.id.hs_featured_products);
        LinearLayout featuredContainer = findViewById(R.id.ll_featured_products);
        if (featuredState == null || featuredScroll == null || featuredContainer == null) return;

        FirebaseDatabase.getInstance().getReference("productos")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<DataSnapshot> activeProducts = new ArrayList<>();
                        for (DataSnapshot product : snapshot.getChildren()) {
                            if (!Boolean.FALSE.equals(product.child("activo").getValue(Boolean.class))) {
                                activeProducts.add(product);
                            }
                        }

                        if (activeProducts.isEmpty()) {
                            featuredScroll.setVisibility(View.GONE);
                            featuredState.setVisibility(View.VISIBLE);
                            featuredState.setText(R.string.featured_products_empty);
                            return;
                        }

                        Collections.shuffle(activeProducts);
                        int productCount = Math.min(5, activeProducts.size());
                        featuredContainer.removeAllViews();
                        for (int index = 0; index < productCount; index++) {
                            agregarProductoDestacado(activeProducts.get(index), featuredContainer);
                        }

                        featuredState.setVisibility(View.GONE);
                        featuredScroll.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        featuredScroll.setVisibility(View.GONE);
                        featuredState.setVisibility(View.VISIBLE);
                        featuredState.setText(R.string.error_loading_products);
                    }
                });
    }

    private void agregarProductoDestacado(DataSnapshot product, LinearLayout container) {
        String productId = product.child("id").getValue(String.class);
        if (productId == null || productId.isEmpty()) productId = product.getKey();
        if (productId == null || productId.isEmpty()) return;

        String name = product.child("nombre").getValue(String.class);
        if (name == null || name.trim().isEmpty()) name = getString(R.string.placeholder_product);
        Object rawPrice = product.child("precio").getValue();
        String price = rawPrice == null
                ? getString(R.string.price_zero)
                : getString(R.string.price_amount_format, PriceUtils.format(rawPrice));

        View productCard = getLayoutInflater().inflate(R.layout.item_featured_product, container, false);
        TextView nameView = productCard.findViewById(R.id.tv_featured_product_name);
        TextView priceView = productCard.findViewById(R.id.tv_featured_product_price);
        ImageView imageView = productCard.findViewById(R.id.iv_featured_product);
        nameView.setText(name);
        priceView.setText(price);
        ImageUtils.cargarImagen(this, imageView,
                product.child("imagenUri").getValue(String.class),
                product.child("imagenBase64").getValue(String.class));

        String selectedProductId = productId;
        productCard.setOnClickListener(view -> {
            Intent detailIntent = new Intent(MainActivity4.this, MainActivity6.class);
            detailIntent.putExtra("PRODUCT_ID", selectedProductId);
            startActivity(detailIntent);
        });
        container.addView(productCard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.HOME);
        bannerHandler.removeCallbacks(bannerSlideTask);
        bannerHandler.postDelayed(bannerSlideTask, BANNER_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        bannerHandler.removeCallbacks(bannerSlideTask);
        if (bannerImageView != null) bannerImageView.animate().cancel();
        super.onPause();
    }

}
