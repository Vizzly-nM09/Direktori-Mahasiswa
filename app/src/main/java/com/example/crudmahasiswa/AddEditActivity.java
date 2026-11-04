package com.example.crudmahasiswa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.crudmahasiswa.api.ApiService;
import com.example.crudmahasiswa.api.RetrofitClient;
import com.example.crudmahasiswa.model.Mahasiswa;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditActivity extends AppCompatActivity {
    private EditText etNim, etNama, etJurusan, etEmail;
    private Button btnSave;
    private LinearLayout layoutSaving;
    private ApiService apiService;
    private Mahasiswa mahasiswa;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_add_edit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_add_edit), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        etNim = findViewById(R.id.et_nim);
        etNama = findViewById(R.id.et_nama);
        etJurusan = findViewById(R.id.et_jurusan);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);
        layoutSaving = findViewById(R.id.layout_saving);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        mahasiswa = (Mahasiswa) getIntent().getSerializableExtra("mahasiswa");
        if (mahasiswa != null) {
            isEdit = true;
            setTitle("Ubah Data Mahasiswa");
            btnSave.setText("Simpan Perubahan");
            fillData();
        } else {
            setTitle("Tambah Mahasiswa Baru");
            btnSave.setText("Simpan Data Mahasiswa");
            mahasiswa = new Mahasiswa();
        }

        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                if (isEdit) {
                    updateMahasiswa();
                } else {
                    createMahasiswa();
                }
            }
        });
    }

    private void fillData() {
        etNim.setText(mahasiswa.getNim());
        etNama.setText(mahasiswa.getNama());
        etJurusan.setText(mahasiswa.getJurusan());
        etEmail.setText(mahasiswa.getEmail());
    }

    private boolean validateInput() {
        String nim = etNim.getText().toString().trim();
        String nama = etNama.getText().toString().trim();
        String jurusan = etJurusan.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (nim.isEmpty()) {
            etNim.setError("NIM tidak boleh kosong");
            etNim.requestFocus();
            return false;
        }
        if (nama.isEmpty()) {
            etNama.setError("Nama lengkap tidak boleh kosong");
            etNama.requestFocus();
            return false;
        }
        if (jurusan.isEmpty()) {
            etJurusan.setError("Program studi / jurusan tidak boleh kosong");
            etJurusan.requestFocus();
            return false;
        }
        if (email.isEmpty()) {
            etEmail.setError("Alamat email tidak boleh kosong");
            etEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format alamat email tidak valid");
            etEmail.requestFocus();
            return false;
        }
        return true;
    }

    private void createMahasiswa() {
        showLoading(true);
        Mahasiswa payload = new Mahasiswa(
                etNim.getText().toString().trim(),
                etNama.getText().toString().trim(),
                etJurusan.getText().toString().trim(),
                etEmail.getText().toString().trim());

        apiService.createMahasiswa(payload).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(
                    @NonNull Call<Map<String, String>> call,
                    @NonNull Response<Map<String, String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().get("message");
                    Toast.makeText(
                                    AddEditActivity.this,
                                    msg != null ? msg : "Data berhasil ditambahkan",
                                    Toast.LENGTH_SHORT)
                            .show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String err = RetrofitClient.extractErrorMessage(response, "Gagal menambahkan data");
                    if (err.toLowerCase().contains("nim")) {
                        etNim.setError(err);
                        etNim.requestFocus();
                    }
                    Toast.makeText(AddEditActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(AddEditActivity.this, "Gagal menghubungi server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateMahasiswa() {
        showLoading(true);
        mahasiswa.setNim(etNim.getText().toString().trim());
        mahasiswa.setNama(etNama.getText().toString().trim());
        mahasiswa.setJurusan(etJurusan.getText().toString().trim());
        mahasiswa.setEmail(etEmail.getText().toString().trim());

        apiService.updateMahasiswa(mahasiswa.getId(), mahasiswa).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(
                    @NonNull Call<Map<String, String>> call,
                    @NonNull Response<Map<String, String>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().get("message");
                    Toast.makeText(
                                    AddEditActivity.this,
                                    msg != null ? msg : "Data berhasil diupdate",
                                    Toast.LENGTH_SHORT)
                            .show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("mahasiswa", mahasiswa);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    String err = RetrofitClient.extractErrorMessage(response, "Gagal mengupdate data");
                    if (err.toLowerCase().contains("nim")) {
                        etNim.setError(err);
                        etNim.requestFocus();
                    }
                    Toast.makeText(AddEditActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(AddEditActivity.this, "Gagal menghubungi server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        layoutSaving.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
        btnSave.setText(isLoading ? "Menyimpan..." : (isEdit ? "Simpan Perubahan" : "Simpan Data Mahasiswa"));
    }
}

