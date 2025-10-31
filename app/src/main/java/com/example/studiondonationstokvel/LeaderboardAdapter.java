package com.example.studiondonationstokvel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    public interface OnStudentSelectedListener {
        void onStudentSelected(Student student);
    }

    private final List<Student> leaderboard;
    private final OnStudentSelectedListener listener;

    public LeaderboardAdapter(List<Student> leaderboard, OnStudentSelectedListener listener) {
        this.leaderboard = leaderboard;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_member, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        Student student = leaderboard.get(position);

        holder.tvRank.setText(String.format(Locale.getDefault(), "#%d", position + 1));
        holder.tvName.setText(student.getName() + " " + student.getSurname());
        holder.tvInstitution.setText(student.getInstitution());

        int progressValue = (int) Math.round(student.getProgress());
        holder.tvProgressValue.setText(String.format(Locale.getDefault(), "%d%% funded", progressValue));

        int tintColor;
        if (progressValue >= 90) {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.mint_400);
        } else if (progressValue >= 70) {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.purple_500);
        } else {
            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.coral_400);
        }
        holder.progressIndicator.setIndicatorColor(tintColor);
        holder.progressIndicator.setTrackColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_translucent));
        holder.progressIndicator.setProgressCompat(progressValue, false);

        holder.ivAvatar.setImageResource(R.drawable.ic_student);
        if (student.getImageUrl() != null && !student.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(student.getImageUrl())
                    .placeholder(R.drawable.ic_student)
                    .error(R.drawable.ic_student)
                    .into(holder.ivAvatar);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStudentSelected(student);
            }
        });
    }

    @Override
    public int getItemCount() {
        return leaderboard.size();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvName;
        TextView tvInstitution;
        TextView tvProgressValue;
        ImageView ivAvatar;
        LinearProgressIndicator progressIndicator;

        LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_student_name);
            tvInstitution = itemView.findViewById(R.id.tv_institution);
            tvProgressValue = itemView.findViewById(R.id.tv_progress_value);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            progressIndicator = itemView.findViewById(R.id.progress_indicator);
        }
    }
}
