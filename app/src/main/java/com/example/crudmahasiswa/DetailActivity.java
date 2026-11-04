package com.example.crudmahasiswa;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class DetailActivity extends AppCompatActivity {
    private TextView tvNim, tvNama, tvJurusan, tvEmail, tvId;
    private Button btnEmail, btnEdit, btnDelete;
    private ProgressBar progressBar;
    private ApiService apiService;
    private Mahasiswa mahasiswa;
    private boolean hasChanges = false;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            hasChanges = true;
                            setResult(RESULT_OK);
                            refreshDetailFromServer();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_detail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detail Mahasiswa");
        }
        toolbar.setNavigationOnClickListener(v -> finishWithState());

        tvNim = findViewById(R.id.tv_detail_nim);
        tvNama = findViewById(R.id.tv_detail_nama);
        tvJurusan = findViewById(R.id.tv_detail_jurusan);
        tvEmail = findViewById(R.id.tv_detail_email);
        tvId = findViewById(R.id.tv_detail_id);
        btnEmail = findViewById(R.id.btn_detail_email);
        btnEdit = findViewById(R.id.btn_detail_edit);
        btnDelete = findViewById(R.id.btn_detail_delete);
        progressBar = findViewById(R.id.progress_detail);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        mahasiswa = (Mahasiswa) getIntent().getSerializableExtra("mahasiswa");
        if (mahasiswa == null) {
            finish();
            return;
        }

        bindData(mahasiswa);

        btnEmail.setOnClickListener(v -> sendEmailToStudent());
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, AddEditActivity.class);
            intent.putExtra("mahasiswa", mahasiswa);
            editLauncher.launch(intent);
        });
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void bindData(Mahasiswa item) {
        tvNim.setText("NIM " + item.getNim());
        tvNama.setText(item.getNama());
        tvJurusan.setText(item.getJurusan());
        tvEmail.setText(item.getEmail());
        tvId.setText("#" + item.getId());
    }

    private void refreshDetailFromServer() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getMahasiswaById(mahasiswa.getId()).enqueue(new Callback<Mahasiswa>() {
            @Override
            public void onResponse(@NonNull Call<Mahasiswa> call, @NonNull Response<Mahasiswa> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    mahasiswa = response.body();
                    bindData(mahasiswa);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Mahasiswa> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void sendEmailToStudent() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + Uri.encode(mahasiswa.getEmail())));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Informasi Akademik - " + mahasiswa.getNama());
        try {
            startActivity(Intent.createChooser(emailIntent, "Kirim email menggunakan:"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Tidak ada aplikasi email di perangkat ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Data Mahasiswa")
                .setMessage("Hapus data " + mahasiswa.getNama() + " (NIM " + mahasiswa.getNim() + ") secara permanen?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteStudent())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteStudent() {
        progressBar.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);
        btnEdit.setEnabled(false);

        apiService.deleteMahasiswa(mahasiswa.getId()).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(
                    @NonNull Call<Map<String, String>> call,
                    @NonNull Response<Map<String, String>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().get("message");
                    Toast.makeText(
                                    DetailActivity.this,
                                    msg != null ? msg : "Data berhasil dihapus",
                                    Toast.LENGTH_SHORT)
                            .show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnDelete.setEnabled(true);
                    btnEdit.setEnabled(true);
                    String err = RetrofitClient.extractErrorMessage(response, "Gagal menghapus data");
                    Toast.makeText(DetailActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                btnEdit.setEnabled(true);
                Toast.makeText(DetailActivity.this, "Gagal menghubungi server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finishWithState() {
        if (hasChanges) {
            setResult(RESULT_OK);
        }
        finish();
    }
}

