package com.setoncios.mobileappdevelopment.tgn.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.setoncios.mobileappdevelopment.tgn.R;

public class ArticleDetailViewHolder extends RecyclerView.ViewHolder {
    TextView titleTextView;
    TextView dateTextView;
    TextView authorTextView;
    ImageView imageView;
    TextView bodyTextView;
    TextView stepTextView;

    public ArticleDetailViewHolder(View itemView) {
        super(itemView);
        this.titleTextView = itemView.findViewById(R.id.article_detail_title_text_view);
        this.dateTextView = itemView.findViewById(R.id.article_detail_date_text_view);
        this.authorTextView = itemView.findViewById(R.id.article_detail_author_text_view);
        this.imageView = itemView.findViewById(R.id.article_detail_image_view);
        this.bodyTextView = itemView.findViewById(R.id.article_detail_body_text_view);
        this.stepTextView = itemView.findViewById(R.id.article_detail_step_text_view);
    }
}
