package com.example.studiondonationstokvel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class InstitutionAdapter extends RecyclerView.Adapter<InstitutionAdapter.InstitutionViewHolder> {
    private List<Institution> institutions;
    private OnInstitutionClickListener listener;

    public interface OnInstitutionClickListener {
        void onInstitutionClick(Institution institution);
    }

    public InstitutionAdapter(List<Institution> institutions, OnInstitutionClickListener listener) {
        this.institutions = institutions;
        this.listener = listener;
    }

    public void updateList(List<Institution> newList) {
        institutions = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InstitutionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_institution, parent, false);
        return new InstitutionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstitutionViewHolder holder, int position) {
        Institution institution = institutions.get(position);
        holder.tvInstitutionName.setText(institution.getName());
        holder.itemView.setOnClickListener(v -> listener.onInstitutionClick(institution));
    }

    @Override
    public int getItemCount() {
        return institutions.size();
    }

    static class InstitutionViewHolder extends RecyclerView.ViewHolder {
        TextView tvInstitutionName;

        public InstitutionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInstitutionName = itemView.findViewById(R.id.tv_institution_name);
        }
    }
}