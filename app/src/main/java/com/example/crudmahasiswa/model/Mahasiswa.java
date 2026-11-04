package com.example.crudmahasiswa.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Mahasiswa implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("nim")
    private String nim;

    @SerializedName("nama")
    private String nama;

    @SerializedName("jurusan")
    private String jurusan;

    @SerializedName("email")
    private String email;

    public Mahasiswa() {}

    public Mahasiswa(String nim, String nama, String jurusan, String email) {
        this.nim = nim;
        this.nama = nama;
        this.jurusan = jurusan;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getNim() {
        return nim;
    }

    public String getNama() {
        return nama;
    }

    public String getJurusan() {
        return jurusan;
    }

    public String getEmail() {
        return email;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public void setJurusan(String jurusan) {
        this.jurusan = jurusan;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

