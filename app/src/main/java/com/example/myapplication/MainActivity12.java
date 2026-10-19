package com.example.myapplication;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity12 extends AppCompatActivity {

    private ImageView imageCamara;
    private TextView tvAddImage;
    private EditText etNombre, etPrecio, etDescripcion;
    private TextView tvSelectedCategory;

    private Bitmap imageBitmapSelected = null;
    private Uri imageUriSelected = null;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    this.imageBitmapSelected = bitmap;
                    this.imageUriSelected = null;
                    mostrarImagenBitmap(bitmap);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    this.imageUriSelected = uri;
                    this.imageBitmapSelected = null;
                    mostrarImagenUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.add_product);

        LinearLayout llUploadImage = findViewById(R.id.ll_upload_image);
        imageCamara = findViewById(R.id.image_camara);
        tvAddImage = findViewById(R.id.tv_add_image);

        etNombre = findViewById(R.id.textBox_name);
        etPrecio = findViewById(R.id.textBox_price);
        etDescripcion = findViewById(R.id.textBox_description);
        tvSelectedCategory = findViewById(R.id.tv_selected_category);

        Button btnSaveProduct = findViewById(R.id.btn_save_product);

        if (llUploadImage != null) {
            llUploadImage.setOnClickListener(v -> mostrarOpcionesImagen());
        }

        // Selección de categoría (Celular / Computador)
        View comboCategory = findViewById(R.id.combo_category);

        if (comboCategory != null) {
            comboCategory.setOnClickListener(v -> mostrarOpcionesCategoria(tvSelectedCategory));
        }

        // Botón Guardar Producto
        if (btnSaveProduct != null) {
            btnSaveProduct.setOnClickListener(v -> guardarProducto());
        }

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void mostrarOpcionesImagen() {
        String[] opciones = {"Tomar foto", "Elegir de la galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                cameraLauncher.launch(null);
            } else if (which == 1) {
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }

    private void mostrarOpcionesCategoria(TextView tvSelectedCategory) {
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

    private void guardarProducto() {
        String nombre = etNombre != null ? etNombre.getText().toString().trim() : "";
        String categoria = tvSelectedCategory != null ? tvSelectedCategory.getText().toString().trim() : "";
        String precioStr = etPrecio != null ? etPrecio.getText().toString().trim() : "";
        String descripcion = etDescripcion != null ? etDescripcion.getText().toString().trim() : "";

        // Validaciones
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

        // Referencia a la colección/nodo "productos" en Firebase Realtime Database
        DatabaseReference productosRef = FirebaseDatabase.getInstance().getReference("productos");
        String productoId = productosRef.push().getKey();

        if (productoId == null) {
            Toast.makeText(this, "Error al generar ID para el producto", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> productoMap = new HashMap<>();
        productoMap.put("id", productoId);
        productoMap.put("nombre", nombre);
        productoMap.put("categoria", categoria);
        productoMap.put("precio", precio);
        productoMap.put("descripcion", descripcion);

        String imageBase64 = null;
        if (imageUriSelected != null) {
            imageBase64 = ImageUtils.uriToBase64(getContentResolver(), imageUriSelected);
        } else if (imageBitmapSelected != null) {
            imageBase64 = ImageUtils.bitmapToBase64(imageBitmapSelected);
        }

        if (imageBase64 != null) {
            productoMap.put("imagenBase64", imageBase64);
            productoMap.put("imagenUri", null);
        } else if (imageUriSelected != null) {
            productoMap.put("imagenUri", imageUriSelected.toString());
        }

        // Guardar en la base de datos
        productosRef.child(productoId).setValue(productoMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity12.this, "Producto guardado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity12.this, "Error al guardar el producto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarImagenBitmap(Bitmap bitmap) {
        prepararImageView();
        imageCamara.setImageBitmap(bitmap);
    }

    private void mostrarImagenUri(Uri uri) {
        prepararImageView();
        imageCamara.setImageURI(uri);
    }

    private void prepararImageView() {
        if (imageCamara != null) {
            imageCamara.clearColorFilter();
            imageCamara.setImageTintList(null);
            ViewGroup.LayoutParams params = imageCamara.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            imageCamara.setLayoutParams(params);
            imageCamara.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        if (tvAddImage != null) {
            tvAddImage.setVisibility(View.GONE);
        }
    }
}