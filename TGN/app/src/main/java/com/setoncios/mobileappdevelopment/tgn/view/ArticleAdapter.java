package com.setoncios.mobileappdevelopment.tgn.view;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.setoncios.mobileappdevelopment.tgn.R;
import com.setoncios.mobileappdevelopment.tgn.models.Article;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleDetailViewHolder> {
    private Context context;
    private final Picasso picasso;
    private final List<Article> articles;
    private final ArticleDetailViewDelegate delegate;

    public ArticleAdapter(List<Article> articles, ArticleDetailViewDelegate delegate, Context context) {
        this.articles = articles;
        this.picasso = Picasso.get();
        this.delegate = delegate;
        this.context = context;
    }

    @NonNull
    @Override
    public ArticleDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ArticleDetailViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(
                                R.layout.article_detail_view,
                                parent,
                                false
                        )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleDetailViewHolder holder, int position) {
        Article article = articles.get(position);
        this.setText(holder.titleTextView, article.getTitle());
        this.setText(holder.authorTextView, article.getAuthor());
        this.setText(holder.bodyTextView, article.getDescription());
        if (article.getPublishedAt() == null) {
            holder.dateTextView.setVisibility(View.GONE);
        } else {
            holder.dateTextView.setText(
                    DateFormat.format(
                            "MMM dd, yyyy kk:mm",
                            article.getPublishedAt()
                    )
            );
            holder.dateTextView.setVisibility(View.VISIBLE);
        }
        if (article.getUrlToImage() == null || article.getUrlToImage().equals("null")) {
            holder.imageView.setImageDrawable(AppCompatResources.getDrawable(this.context, R.drawable.noimage));
        } else {
            picasso.load(article.getUrlToImage())
                    .error(R.drawable.brokenimage)
                    .into(holder.imageView);
        }
        this.setText(
                holder.stepTextView,
                String.format(
                        Locale.getDefault(),
                        "%d of %d",
                        position + 1, articles.size()
                )
        );
        holder.titleTextView.setOnClickListener((view) -> this.delegate.didTapInArticle(position));
        holder.imageView.setOnClickListener((view) -> this.delegate.didTapInArticle(position));
        holder.bodyTextView.setOnClickListener((view) -> this.delegate.didTapInArticle(position));
    }

    private void setText(TextView toTextView, @Nullable String text) {
        if (text == null || text.toLowerCase(Locale.getDefault()).equals("null")) {
            toTextView.setVisibility(View.GONE);
        } else {
            toTextView.setText(text);
            toTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return this.articles.size();
    }
}
