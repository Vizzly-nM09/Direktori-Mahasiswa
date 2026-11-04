package com.example.crudmahasiswa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.crudmahasiswa.adapter.MahasiswaAdapter;
import com.example.crudmahasiswa.api.ApiService;
import com.example.crudmahasiswa.api.RetrofitClient;
import com.example.crudmahasiswa.model.Mahasiswa;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView rvMahasiswa;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutLoading;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private FloatingActionButton fabAdd;
    private MahasiswaAdapter adapter;
    private ApiService apiService;

    private final ActivityResultLauncher<Intent> refreshLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadData();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        rvMahasiswa = findViewById(R.id.rv_mahasiswa);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutError = findViewById(R.id.layout_error);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        fabAdd = findViewById(R.id.fab_add);
        Button btnEmptyAdd = findViewById(R.id.btn_empty_add);
        Button btnRetry = findViewById(R.id.btn_retry);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        View.OnClickListener openAddForm = v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            refreshLauncher.launch(intent);
        };

        fabAdd.setOnClickListener(openAddForm);
        btnEmptyAdd.setOnClickListener(openAddForm);
        btnRetry.setOnClickListener(v -> loadData());
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void setupRecyclerView() {
        adapter = new MahasiswaAdapter();
        adapter.setOnItemClickListener(new MahasiswaAdapter.OnItemClickListener() {
            @Override
            public void onDetail(Mahasiswa mahasiswa) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("mahasiswa", mahasiswa);
                refreshLauncher.launch(intent);
            }

            @Override
            public void onEdit(Mahasiswa mahasiswa) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra("mahasiswa", mahasiswa);
                refreshLauncher.launch(intent);
            }

            @Override
            public void onDelete(Mahasiswa mahasiswa) {
                showDeleteConfirmation(mahasiswa);
            }
        });

        // Reflow across 3 viewport breakpoints: 1 column (<600dp), 2 columns (600-899dp), 3 columns (>=900dp).
        int widthDp = getResources().getConfiguration().screenWidthDp;
        int spanCount = widthDp >= 900 ? 3 : (widthDp >= 600 ? 2 : 1);
        rvMahasiswa.setLayoutManager(new GridLayoutManager(this, spanCount));
        rvMahasiswa.setAdapter(adapter);
    }

    private void loadData() {
        if (!swipeRefresh.isRefreshing()) {
            showState(State.LOADING, null);
        }

        apiService.getAllMahasiswa().enqueue(new Callback<List<Mahasiswa>>() {
            @Override
            public void onResponse(
                    @NonNull Call<List<Mahasiswa>> call,
                    @NonNull Response<List<Mahasiswa>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Mahasiswa> data = response.body();
                    adapter.setData(data);
                    updateSubtitleCount(data.size());
                    if (data.isEmpty()) {
                        showState(State.EMPTY, null);
                    } else {
                        showState(State.CONTENT, null);
                    }
                } else {
                    String err = RetrofitClient.extractErrorMessage(response, "Gagal memuat data mahasiswa dari server.");
                    showState(State.ERROR, err);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Mahasiswa>> call, @NonNull Throwable t) {
                swipeRefresh.setRefreshing(false);
                showState(
                        State.ERROR,
                        "Pastikan server Node.js berjalan (node server.js) dan adb reverse tcp:3000 tcp:3000 aktif.\nDetail: "
                                + t.getMessage());
            }
        });
    }

    private void updateSubtitleCount(int count) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(count + " mahasiswa terdaftar");
        }
    }

    private void showDeleteConfirmation(Mahasiswa mahasiswa) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Data Mahasiswa")
                .setMessage("Hapus data " + mahasiswa.getNama() + " (NIM " + mahasiswa.getNim() + ") dari database?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteMahasiswa(mahasiswa))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteMahasiswa(Mahasiswa mahasiswa) {
        showState(State.LOADING, null);
        apiService.deleteMahasiswa(mahasiswa.getId()).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(
                    @NonNull Call<Map<String, String>> call,
                    @NonNull Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().get("message");
                    Toast.makeText(
                                    MainActivity.this,
                                    msg != null ? msg : "Data berhasil dihapus",
                                    Toast.LENGTH_SHORT)
                            .show();
                    loadData();
                } else {
                    String err = RetrofitClient.extractErrorMessage(response, "Gagal menghapus data");
                    Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                    loadData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Gagal menghubungi server: " + t.getMessage(), Toast.LENGTH_LONG).show();
                loadData();
            }
        });
    }

    private enum State {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private void showState(State state, String errorMessage) {
        layoutLoading.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        rvMahasiswa.setVisibility(state == State.CONTENT ? View.VISIBLE : View.GONE);
        layoutEmpty.setVisibility(state == State.EMPTY ? View.VISIBLE : View.GONE);
        layoutError.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);

        if (state == State.EMPTY || state == State.ERROR) {
            adapter.setData(Collections.emptyList());
        }
        if (state == State.ERROR && errorMessage != null) {
            tvErrorMessage.setText(errorMessage);
        }
    }
}
