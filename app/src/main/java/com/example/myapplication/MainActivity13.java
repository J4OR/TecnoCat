package com.example.myapplication;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;

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
    private SwitchCompat switchProductActive;

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
        switchProductActive = findViewById(R.id.switch_product_active);

        TextView btnChangeImage = findViewById(R.id.btn_change_image);
        View comboCategory = findViewById(R.id.combo_category);
        Button btnUpdateProduct = findViewById(R.id.btn_update_product);
        ImageView ivBack = findViewById(R.id.iv_back);

        if (productId != null && !productId.isEmpty()) {
            productoRef = FirebaseDatabase.getInstance().getReference("productos").child(productId);
            cargarDatosProducto();
        } else {
            Toast.makeText(this, R.string.msg_product_not_found, Toast.LENGTH_SHORT).show();
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

        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.NONE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            KeyboardUtils.applyImeInsets(v, insets);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavBarHelper.setup(this, BottomNavBarHelper.Tab.NONE);
    }

    private void cargarDatosProducto() {
        if (productoRef == null) return;

        productoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(MainActivity13.this, R.string.msg_product_does_not_exist, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String categoria = snapshot.child("categoria").getValue(String.class);
                Object precioObj = snapshot.child("precio").getValue();
                String descripcion = snapshot.child("descripcion").getValue(String.class);
                String imagenUri = snapshot.child("imagenUri").getValue(String.class);
                String imagenBase64 = snapshot.child("imagenBase64").getValue(String.class);
                Boolean activo = snapshot.child("activo").getValue(Boolean.class);

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
                if (switchProductActive != null) {
                    switchProductActive.setChecked(activo == null || activo);
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
        String[] opciones = {getString(R.string.option_take_photo), getString(R.string.option_choose_gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_image_title);
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
        String[] categorias = {getString(R.string.cat_celulares), getString(R.string.cat_computadoras)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_category_title);
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
                etNombre.setError(getString(R.string.err_enter_product_name));
                etNombre.requestFocus();
            }
            return;
        }

        String hintCategoria = getString(R.string.hint_category);
        if (categoria.isEmpty() || categoria.equalsIgnoreCase(hintCategoria)) {
            Toast.makeText(this, R.string.msg_select_category, Toast.LENGTH_SHORT).show();
            return;
        }

        if (precioStr.isEmpty()) {
            if (etPrecio != null) {
                etPrecio.setError(getString(R.string.err_enter_product_price));
                etPrecio.requestFocus();
            }
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            if (etPrecio != null) {
                etPrecio.setError(getString(R.string.err_enter_valid_price));
                etPrecio.requestFocus();
            }
            return;
        }

        if (descripcion.isEmpty()) {
            if (etDescripcion != null) {
                etDescripcion.setError(getString(R.string.err_enter_product_desc));
                etDescripcion.requestFocus();
            }
            return;
        }

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("nombre", nombre);
        updateMap.put("categoria", categoria);
        updateMap.put("precio", precio);
        updateMap.put("descripcion", descripcion);
        updateMap.put("activo", switchProductActive == null || switchProductActive.isChecked());

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
                        Toast.makeText(MainActivity13.this, R.string.msg_product_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity13.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.hideKeyboardWhenTappedOutside(this, event);
        return super.dispatchTouchEvent(event);
    }
}
