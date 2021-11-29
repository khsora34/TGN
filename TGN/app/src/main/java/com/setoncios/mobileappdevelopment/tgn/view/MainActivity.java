package com.setoncios.mobileappdevelopment.tgn.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.setoncios.mobileappdevelopment.tgn.R;
import com.setoncios.mobileappdevelopment.tgn.models.Article;
import com.setoncios.mobileappdevelopment.tgn.models.CategoryType;
import com.setoncios.mobileappdevelopment.tgn.models.NewsSource;
import com.setoncios.mobileappdevelopment.tgn.runnable.ArticlesObservable;
import com.setoncios.mobileappdevelopment.tgn.runnable.ArticlesRunnable;
import com.setoncios.mobileappdevelopment.tgn.runnable.NewsSourcesObservable;
import com.setoncios.mobileappdevelopment.tgn.runnable.NewsSourcesRunnable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements NewsSourcesObservable, ArticlesObservable, ArticleDetailViewDelegate {
    private static final String TAG = "MainActivity";
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private ArrayAdapter<String> drawerAdapter;
    private ArticleAdapter articleAdapter;
    private ListView listView;
    private ViewPager2 viewPager;
    private Menu menu;

    private final Map<CategoryType, Map<String, Set<NewsSource>>> info = new HashMap<>();
    private final List<NewsSource> defaultSources = new ArrayList<>();
    private final Map<CategoryType, String> filters = new HashMap<>();
    private final List<NewsSource> filteredSources = new ArrayList<>();
    private final List<String> filteredItems = new ArrayList<>();
    private NewsSource selectedSource;
    private final List<Article> articles = new ArrayList<>();
    private boolean shouldReloadMenu = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.initialize();
        this.drawerLayout = this.findViewById(R.id.drawer_layout);
        this.listView = this.findViewById(R.id.drawer);
        this.viewPager = this.findViewById(R.id.view_pager);
        this.drawerAdapter = new ArrayAdapter<String>(
                this,
                R.layout.drawer_item,
                this.filteredItems
        );
        this.articleAdapter = new ArticleAdapter(this.articles, this, this);
        this.viewPager.setAdapter(this.articleAdapter);
        this.listView.setAdapter(this.drawerAdapter);
        this.listView.setOnItemClickListener((parent, view, position, id) -> didSelectSource(position));
        this.drawerToggle = new ActionBarDrawerToggle(
                this,
                this.drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        );
        this.loadData();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.drawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.hasSubMenu()) {
            return true;
        }
        if (this.drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int groupId = item.getGroupId();
        CategoryType type = CategoryType.getFromId(groupId);
        String filter = item.getTitle().toString();
        this.filters.put(type, filter);
        this.updateSelectedItems();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        if (this.shouldReloadMenu) {
            this.shouldReloadMenu = false;
            this.buildMenu();
        }
        return true;
    }

    @Override
    public void onNewsSourcesLoaded(String response) {
        this.runOnUiThread(() -> this.fillSourcesData(response));
    }

    @Override
    public void onArticlesLoaded(String response) {
        this.runOnUiThread(() -> this.fillArticles(response));
    }

    @Override
    public void didTapInArticle(int position) {
        Uri uri = Uri.parse(this.articles.get(position).getUrl());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        this.startActivity(intent);
    }

    private void initialize() {
        this.info.put(CategoryType.TOPIC, new HashMap<>());
        this.info.put(CategoryType.COUNTRY, new HashMap<>());
        this.info.put(CategoryType.LANGUAGE, new HashMap<>());
        this.info.get(CategoryType.TOPIC).put("all", new HashSet<>());
        this.info.get(CategoryType.COUNTRY).put("all", new HashSet<>());
        this.info.get(CategoryType.LANGUAGE).put("all", new HashSet<>());
        this.filters.put(CategoryType.TOPIC, "all");
        this.filters.put(CategoryType.COUNTRY, "all");
        this.filters.put(CategoryType.LANGUAGE, "all");
    }

    private void loadData() {
        if (this.defaultSources.isEmpty()) {
            new Thread(new NewsSourcesRunnable(this)).start();
        }
    }

    private void didSelectSource(int position) {
        this.drawerLayout.closeDrawer(this.listView);
        NewsSource source = filteredSources.get(position);
        this.selectedSource = source;
        new Thread(new ArticlesRunnable(this, source.getId())).start();
    }

    private void updateSelectedItems() {
        List<NewsSource> result = new ArrayList<>(this.defaultSources);
        if (this.info.get(CategoryType.TOPIC).containsKey(this.filters.get(CategoryType.TOPIC))) {
            result.retainAll(Objects.requireNonNull(this.info.get(CategoryType.TOPIC).get(this.filters.get(CategoryType.TOPIC))));
        }
        if (this.info.get(CategoryType.COUNTRY).containsKey(this.filters.get(CategoryType.COUNTRY))) {
            result.retainAll(Objects.requireNonNull(this.info.get(CategoryType.COUNTRY).get(this.filters.get(CategoryType.COUNTRY))));
        }
        if (this.info.get(CategoryType.LANGUAGE).containsKey(this.filters.get(CategoryType.LANGUAGE))) {
            result.retainAll(Objects.requireNonNull(this.info.get(CategoryType.LANGUAGE).get(this.filters.get(CategoryType.LANGUAGE))));
        }
        this.filteredSources.clear();
        this.filteredSources.addAll(result);
        this.filteredItems.clear();
        this.filteredItems.addAll(result.stream().map(NewsSource::getName).collect(Collectors.toList()));
        this.drawerAdapter.notifyDataSetChanged();
        if (this.articles.isEmpty()) {
            this.setTitle(String.format(
                    Locale.getDefault(),
                    "TGN (%d)",
                    this.filteredItems.size()
            ));
        }
    }

    private void buildMenu() {
        if (this.menu == null) {
            this.shouldReloadMenu = true;
            return;
        }
        this.menu.clear();
        List<String> topicsKeys = new ArrayList<>(this.info.get(CategoryType.TOPIC).keySet());
        Collections.sort(topicsKeys);
        List<String> countryKeys = new ArrayList<>(this.info.get(CategoryType.COUNTRY).keySet());
        Collections.sort(countryKeys);
        List<String> languageKeys = new ArrayList<>(this.info.get(CategoryType.LANGUAGE).keySet());
        Collections.sort(languageKeys);
        if (topicsKeys.size() > 0) {
            SubMenu subMenu = this.menu.addSubMenu("Topics");
            for (int j = 0; j < topicsKeys.size(); j++) {
                subMenu.add(CategoryType.TOPIC.getId(), j, j, topicsKeys.get(j));
            }
        }
        if (countryKeys.size() > 0) {
            SubMenu subMenu = this.menu.addSubMenu("Countries");
            for (int j = 0; j < countryKeys.size(); j++) {
                subMenu.add(CategoryType.COUNTRY.getId(), j, j, countryKeys.get(j));
            }
        }
        if (languageKeys.size() > 0) {
            SubMenu subMenu = this.menu.addSubMenu("Languages");
            for (int j = 0; j < languageKeys.size(); j++) {
                subMenu.add(CategoryType.LANGUAGE.getId(), j, j, languageKeys.get(j));
            }
        }
    }

    private void fillSourcesData(String response) {
        if (response == null) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray sources = jsonObject.getJSONArray("sources");
            for (int i = 0; i < sources.length(); i++) {
                JSONObject sourcesJSONObject = sources.getJSONObject(i);
                NewsSource newsSource = new NewsSource(sourcesJSONObject);
                if (!this.info.get(CategoryType.TOPIC).containsKey(newsSource.getCategory())) {
                    this.info.get(CategoryType.TOPIC).put(newsSource.getCategory(), new HashSet<>());
                }
                this.info.get(CategoryType.TOPIC).get(newsSource.getCategory()).add(newsSource);
                if (!this.info.get(CategoryType.COUNTRY).containsKey(newsSource.getCountry())) {
                    this.info.get(CategoryType.COUNTRY).put(newsSource.getCountry(), new HashSet<>());
                }
                this.info.get(CategoryType.COUNTRY).get(newsSource.getCountry()).add(newsSource);
                if (!this.info.get(CategoryType.LANGUAGE).containsKey(newsSource.getLanguage())) {
                    this.info.get(CategoryType.LANGUAGE).put(newsSource.getLanguage(), new HashSet<>());
                }
                this.info.get(CategoryType.LANGUAGE).get(newsSource.getLanguage()).add(newsSource);
                this.defaultSources.add(newsSource);
            }
            this.info.get(CategoryType.TOPIC).get("all").addAll(this.defaultSources);
            this.info.get(CategoryType.COUNTRY).get("all").addAll(this.defaultSources);
            this.info.get(CategoryType.LANGUAGE).get("all").addAll(this.defaultSources);
            this.buildMenu();
            this.updateSelectedItems();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
            }
            Log.d(TAG, "onNewsSourcesLoaded: " + info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fillArticles(String response) {
        if (response == null) {
            return;
        }
        this.viewPager.setBackground(null);
        this.articles.clear();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray sources = jsonObject.getJSONArray("articles");
            for (int i = 0; i < sources.length(); i++) {
                JSONObject sourcesJSONObject;
                try {
                    sourcesJSONObject = sources.getJSONObject(i);
                } catch (JSONException e) {
                    Log.d(TAG, "fillArticles: Unable to parse article" + i);
                    e.printStackTrace();
                    continue;
                }
                Article article = new Article(sourcesJSONObject);
                articles.add(article);
            }
            this.articleAdapter.notifyDataSetChanged();
            if (this.selectedSource != null) {
                this.setTitle(String.format(
                        Locale.getDefault(),
                        "%s (%d)",
                        this.selectedSource.getName(),
                        this.articles.size()
                ));
            }
            Log.d(TAG, "onArticlesLoaded: " + articles);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}