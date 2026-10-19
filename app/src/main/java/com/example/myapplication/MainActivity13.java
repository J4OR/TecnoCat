package com.example.myapplication;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity13 extends AppCompatActivity {

    private String productId;
    private DatabaseReference productoRef;

    private ImageView ivProductImage;
    private EditText etNombre, etPrecio, etDescripcion;
    private TextView tvSelectedCategory;

    private Bitmap imageBitmapSelected = null;
    private Uri imageUriSelected = null;
    private boolean imageChanged = false;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    this.imageBitmapSelected = bitmap;
                    this.imageUriSelected = null;
                    this.imageChanged = true;
                    if (ivProductImage != null) {
                        ivProductImage.setImageBitmap(bitmap);
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    this.imageUriSelected = uri;
                    this.imageBitmapSelected = null;
                    this.imageChanged = true;
                    if (ivProductImage != null) {
                        ivProductImage.setImageURI(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.update_product);

        if (getIntent() != null) {
            productId = getIntent().getStringExtra("PRODUCT_ID");
        }

        ivProductImage = findViewById(R.id.iv_product_image);
        etNombre = findViewById(R.id.et_product_name);
        etPrecio = findViewById(R.id.et_product_price);
        etDescripcion = findViewById(R.id.et_product_desc);
        tvSelectedCategory = findViewById(R.id.tv_selected_category);

        TextView btnChangeImage = findViewById(R.id.btn_change_image);
        View comboCategory = findViewById(R.id.combo_category);
        Button btnUpdateProduct = findViewById(R.id.btn_update_product);
        ImageView ivBack = findViewById(R.id.iv_back);

        if (productId != null && !productId.isEmpty()) {
            productoRef = FirebaseDatabase.getInstance().getReference("productos").child(productId);
            cargarDatosProducto();
        } else {
            Toast.makeText(this, "Error: No se encontró el producto", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (btnChangeImage != null) {
            btnChangeImage.setOnClickListener(v -> mostrarOpcionesImagen());
        }

        if (ivProductImage != null) {
            ivProductImage.setOnClickListener(v -> mostrarOpcionesImagen());
        }

        if (comboCategory != null) {
            comboCategory.setOnClickListener(v -> mostrarOpcionesCategoria());
        }

        if (btnUpdateProduct != null) {
            btnUpdateProduct.setOnClickListener(v -> actualizarProducto());
        }

        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void cargarDatosProducto() {
        if (productoRef == null) return;

        productoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(MainActivity13.this, "El producto no existe", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String categoria = snapshot.child("categoria").getValue(String.class);
                Object precioObj = snapshot.child("precio").getValue();
                String descripcion = snapshot.child("descripcion").getValue(String.class);
                String imagenUri = snapshot.child("imagenUri").getValue(String.class);
                String imagenBase64 = snapshot.child("imagenBase64").getValue(String.class);

                if (etNombre != null && nombre != null) {
                    etNombre.setText(nombre);
                }

                if (tvSelectedCategory != null && categoria != null) {
                    tvSelectedCategory.setText(categoria);
                    tvSelectedCategory.setTextColor(ContextCompat.getColor(MainActivity13.this, R.color.text_black));
                }

                if (etPrecio != null && precioObj != null) {
                    etPrecio.setText(precioObj.toString());
                }

                if (etDescripcion != null && descripcion != null) {
                    etDescripcion.setText(descripcion);
                }

                if (ivProductImage != null) {
                    ImageUtils.cargarImagen(MainActivity13.this, ivProductImage, imagenUri, imagenBase64);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity13.this, "Error al cargar datos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarOpcionesImagen() {
        String[] opciones = {"Tomar foto", "Elegir de la galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cambiar imagen");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                cameraLauncher.launch(null);
            } else if (which == 1) {
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }

    private void mostrarOpcionesCategoria() {
        String[] categorias = {"Celular", "Computador"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar categoría");
        builder.setItems(categorias, (dialog, which) -> {
            if (tvSelectedCategory != null) {
                tvSelectedCategory.setText(categorias[which]);
                tvSelectedCategory.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            }
        });
        builder.show();
    }

    private void actualizarProducto() {
        String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
        String categoria = tvSelectedCategory != null ? tvSelectedCategory.getText().toString().trim() : "";
        String precioStr = etPrecio != null ? etPrecio.getText().toString().trim() : "";
        String descripcion = etDescripcion != null ? etDescripcion.getText().toString().trim() : "";

        if (nombre.isEmpty()) {
            if (etNombre != null) {
                etNombre.setError("Ingresa el nombre del producto");
                etNombre.requestFocus();
            }
            return;
        }

        String hintCategoria = getString(R.string.hint_category);
        if (categoria.isEmpty() || categoria.equalsIgnoreCase(hintCategoria)) {
            Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }

        if (precioStr.isEmpty()) {
            if (etPrecio != null) {
                etPrecio.setError("Ingresa el precio del producto");
                etPrecio.requestFocus();
            }
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            if (etPrecio != null) {
                etPrecio.setError("Ingresa un precio válido");
                etPrecio.requestFocus();
            }
            return;
        }

        if (descripcion.isEmpty()) {
            if (etDescripcion != null) {
                etDescripcion.setError("Ingresa la descripción del producto");
                etDescripcion.requestFocus();
            }
            return;
        }

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("nombre", nombre);
        updateMap.put("categoria", categoria);
        updateMap.put("precio", precio);
        updateMap.put("descripcion", descripcion);

        if (imageChanged) {
            String imageBase64 = null;
            if (imageUriSelected != null) {
                imageBase64 = ImageUtils.uriToBase64(getContentResolver(), imageUriSelected);
            } else if (imageBitmapSelected != null) {
                imageBase64 = ImageUtils.bitmapToBase64(imageBitmapSelected);
            }

            if (imageBase64 != null) {
                updateMap.put("imagenBase64", imageBase64);
                updateMap.put("imagenUri", null);
            } else if (imageUriSelected != null) {
                updateMap.put("imagenUri", imageUriSelected.toString());
            }
        }

        if (productoRef != null) {
            productoRef.updateChildren(updateMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity13.this, "Producto actualizado correctamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity13.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }
}