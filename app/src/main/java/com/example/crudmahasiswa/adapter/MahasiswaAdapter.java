package com.example.crudmahasiswa.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.crudmahasiswa.R;
import com.example.crudmahasiswa.model.Mahasiswa;
import java.util.ArrayList;
import java.util.List;

public class MahasiswaAdapter extends RecyclerView.Adapter<MahasiswaAdapter.ViewHolder> {
    private List<Mahasiswa> mahasiswaList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDetail(Mahasiswa mahasiswa);
        void onEdit(Mahasiswa mahasiswa);
        void onDelete(Mahasiswa mahasiswa);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Mahasiswa> data) {
        this.mahasiswaList = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mahasiswa, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mahasiswa mahasiswa = mahasiswaList.get(position);
        holder.tvNim.setText("NIM " + mahasiswa.getNim());
        holder.tvNama.setText(mahasiswa.getNama());
        holder.tvJurusan.setText(mahasiswa.getJurusan());
        holder.tvEmail.setText(mahasiswa.getEmail());

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) listener.onDetail(mahasiswa);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(mahasiswa);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(mahasiswa);
        });
    }

    @Override
    public int getItemCount() {
        return mahasiswaList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNim, tvNama, tvJurusan, tvEmail, btnEdit, btnDelete;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNim = itemView.findViewById(R.id.tv_nim);
            tvNama = itemView.findViewById(R.id.tv_nama);
            tvJurusan = itemView.findViewById(R.id.tv_jurusan);
            tvEmail = itemView.findViewById(R.id.tv_email);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }
}

