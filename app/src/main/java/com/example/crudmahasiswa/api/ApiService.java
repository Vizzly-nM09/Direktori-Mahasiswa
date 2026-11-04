package com.example.crudmahasiswa.api;

import com.example.crudmahasiswa.model.Mahasiswa;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @GET("mahasiswa")
    Call<List<Mahasiswa>> getAllMahasiswa();

    @GET("mahasiswa/{id}")
    Call<Mahasiswa> getMahasiswaById(@Path("id") int id);

    @POST("mahasiswa")
    Call<Map<String, String>> createMahasiswa(@Body Mahasiswa mahasiswa);

    @PUT("mahasiswa/{id}")
    Call<Map<String, String>> updateMahasiswa(@Path("id") int id, @Body Mahasiswa mahasiswa);

    @DELETE("mahasiswa/{id}")
    Call<Map<String, String>> deleteMahasiswa(@Path("id") int id);
}

