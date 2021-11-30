package com.setoncios.mobileappdevelopment.tgn.view;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.setoncios.mobileappdevelopment.tgn.R;
import com.setoncios.mobileappdevelopment.tgn.models.Article;
import com.setoncios.mobileappdevelopment.tgn.models.NewsSource;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DrawerAdapter extends ArrayAdapter<String> {
    private List<NewsSource> identifiers;
    private Map<String, Integer> colors;

    public DrawerAdapter(@NonNull Context context, int resource, @NonNull List<String> objects, List<NewsSource> identifiers, Map<String, Integer> colors) {
        super(context, resource, objects);
        this.identifiers = identifiers;
        this.colors = colors;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        String category = this.identifiers.get(position).getCategory();
        if (this.colors.containsKey(category)) {
            textView.setTextColor(this.colors.get(category));
        } else {
            textView.setTextColor(Color.WHITE);
        }
        return view;
    }
}