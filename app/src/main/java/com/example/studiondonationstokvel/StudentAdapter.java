package com.example.studiondonationstokvel;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import java.util.Locale;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<Student> students;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
    }

    public StudentAdapter(List<Student> students, OnStudentClickListener listener) {
        this.students = students;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);
        holder.tvName.setText(student.getName() + " " + student.getSurname());
        holder.tvInstitution.setText(student.getInstitution());

        int progressValue = (int) Math.round(student.getProgress());
        holder.progressIndicator.setProgressCompat(progressValue, false);
        holder.tvProgressLabel.setText(String.format(Locale.getDefault(), "%d%% funded", progressValue));

        int tintColor;
        if (progressValue >= 80) {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.mint_400);
        } else if (progressValue >= 50) {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.coral_400);
        } else {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_500);
        }
        ImageViewCompat.setImageTintList(holder.ivStatus, ColorStateList.valueOf(tintColor));

        // Load student avatar with Glide (if imageUrl exists)
        if (student.getImageUrl() != null && !student.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(student.getImageUrl())
                    .placeholder(R.drawable.ic_student)
                    .error(R.drawable.ic_student)
                    .into(holder.ivStudentAvatar);
        } else {
            holder.ivStudentAvatar.setImageResource(R.drawable.ic_student);
        }

        // Handle clicks on the "Like" button and entire item
        holder.btnLike.setOnClickListener(v -> listener.onStudentClick(student));
        holder.itemView.setOnClickListener(v -> listener.onStudentClick(student));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInstitution, tvProgressLabel;
        LinearProgressIndicator progressIndicator;
        Button btnLike;
        ImageView ivStudentAvatar, ivStatus;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvInstitution = itemView.findViewById(R.id.tv_institution);
            progressIndicator = itemView.findViewById(R.id.progress_bar);
            tvProgressLabel = itemView.findViewById(R.id.tv_progress_label);
            btnLike = itemView.findViewById(R.id.btn_like);
            ivStudentAvatar = itemView.findViewById(R.id.iv_student_avatar); // Now resolves
            ivStatus = itemView.findViewById(R.id.iv_status);
        }
    }
}