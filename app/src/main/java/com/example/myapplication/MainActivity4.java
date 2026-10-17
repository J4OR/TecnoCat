package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity4 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu);

        ImageView ivMenu = findViewById(R.id.iv_menu);
        if (ivMenu != null) {
            ivMenu.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity9.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

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

        // Clic en Buscar
        ImageView ivSearch = findViewById(R.id.iv_search);
        if (ivSearch != null) {
            ivSearch.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity4.this, MainActivity5.class);
                intent.putExtra("FILTER_CATEGORY", "Todos");
                startActivity(intent);
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}