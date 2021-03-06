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
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private DrawerAdapter drawerAdapter;
    private ArticleAdapter articleAdapter;
    private ListView listView;
    private ViewPager2 viewPager;
    private Menu menu;

    private Map<CategoryType, List<String>> sortedKeys = new HashMap<>();
    private Map<String, String> countries = new HashMap<>();
    private Map<String, String> languages = new HashMap<>();
    private HashMap<String, Integer> colors = new HashMap<>();

    private HashMap<CategoryType, HashMap<String, HashSet<NewsSource>>> info = new HashMap<>();
    private ArrayList<NewsSource> defaultSources = new ArrayList<>();
    private HashMap<CategoryType, String> filters = new HashMap<>();
    private ArrayList<NewsSource> filteredSources = new ArrayList<>();
    private ArrayList<String> filteredItems = new ArrayList<>();

    private NewsSource selectedSource;
    private ArrayList<Article> articles = new ArrayList<>();
    private boolean shouldReloadMenu = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.initialize();
        this.drawerLayout = this.findViewById(R.id.drawer_layout);
        this.listView = this.findViewById(R.id.drawer);
        this.viewPager = this.findViewById(R.id.view_pager);
        this.drawerAdapter = new DrawerAdapter(
                this,
                R.layout.drawer_item,
                this.filteredItems,
                this.filteredSources,
                this.colors
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.loadData();
        this.fillSources();
        this.fillArticles();
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
        List<String> sortedKeys = this.sortedKeys.get(type);
        String filter;
        if (sortedKeys != null && item.getItemId() < sortedKeys.size()) {
            filter = sortedKeys.get(item.getItemId());
        } else {
            filter = item.getTitle().toString();
        }
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
    public void onNewsSourcesError() {
        this.runOnUiThread(() -> this.showError(getString(R.string.error_load_sources)));
    }

    @Override
    public void onArticlesLoaded(String response) {
        this.runOnUiThread(() -> this.parseArticles(response));
    }

    @Override
    public void onArticlesError() {
        this.runOnUiThread(() -> this.showError(getString(R.string.error_load_articles)));
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
        Objects.requireNonNull(this.info.get(CategoryType.TOPIC)).put(getString(R.string.category_all), new HashSet<>());
        Objects.requireNonNull(this.info.get(CategoryType.COUNTRY)).put(getString(R.string.category_all), new HashSet<>());
        Objects.requireNonNull(this.info.get(CategoryType.LANGUAGE)).put(getString(R.string.category_all), new HashSet<>());
        this.filters.put(CategoryType.TOPIC, getString(R.string.category_all));
        this.filters.put(CategoryType.COUNTRY, getString(R.string.category_all));
        this.filters.put(CategoryType.LANGUAGE, getString(R.string.category_all));
    }

    private void loadData() {
        try {
            this.loadCountryJson();
        } catch (IOException | JSONException e) {
            Log.d(TAG, "loadData: Unable to parse country JSON.");
        }
        try {
            this.loadLanguagesJson();
        } catch (IOException | JSONException e) {
            Log.d(TAG, "loadData: Unable to parse languages JSON.");
        }
        if (this.defaultSources.isEmpty()) {
            new Thread(new NewsSourcesRunnable(this)).start();
        }
    }

    private void loadCountryJson() throws IOException, JSONException {
        InputStream inputStream = getResources().openRawResource(R.raw.country_codes);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        JSONObject mainJson = new JSONObject(sb.toString());
        JSONArray jsonArray = mainJson.getJSONArray(getString(R.string.json_countries_key));
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                this.countries.put(jsonObject.getString(getString(R.string.json_code_key)).toLowerCase(Locale.ROOT), jsonObject.getString(getString(R.string.json_name_key)));
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void loadLanguagesJson() throws IOException, JSONException {
        InputStream inputStream = getResources().openRawResource(R.raw.language_codes);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        JSONObject mainJson = new JSONObject(sb.toString());
        JSONArray jsonArray = mainJson.getJSONArray(getString(R.string.json_languages_key));
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                this.languages.put(jsonObject.getString(getString(R.string.json_code_key)).toLowerCase(Locale.ROOT), jsonObject.getString(getString(R.string.json_name_key)));
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void didSelectSource(int position) {
        this.drawerLayout.closeDrawer(this.listView);
        NewsSource source = filteredSources.get(position);
        this.selectedSource = source;
        new Thread(new ArticlesRunnable(this, source.getId())).start();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateSelectedItems() {
        HashMap<String, HashSet<NewsSource>> topics = Objects.requireNonNull(this.info.get(CategoryType.TOPIC));
        HashMap<String, HashSet<NewsSource>> countries = Objects.requireNonNull(this.info.get(CategoryType.COUNTRY));
        HashMap<String, HashSet<NewsSource>> languages = Objects.requireNonNull(this.info.get(CategoryType.LANGUAGE));
        List<NewsSource> result = new ArrayList<>(this.defaultSources);
        if (topics.containsKey(this.filters.get(CategoryType.TOPIC))) {
            result.retainAll(Objects.requireNonNull(topics.get(this.filters.get(CategoryType.TOPIC))));
        }
        if (countries.containsKey(this.filters.get(CategoryType.COUNTRY))) {
            result.retainAll(Objects.requireNonNull(countries.get(this.filters.get(CategoryType.COUNTRY))));
        }
        if (languages.containsKey(this.filters.get(CategoryType.LANGUAGE))) {
            result.retainAll(Objects.requireNonNull(languages.get(this.filters.get(CategoryType.LANGUAGE))));
        }
        this.filteredSources.clear();
        this.filteredSources.addAll(result);
        this.filteredItems.clear();
        this.filteredItems.addAll(result.stream().map(NewsSource::getName).collect(Collectors.toList()));
        this.drawerAdapter.notifyDataSetChanged();
        this.updateTitle();
    }

    private void updateTitle() {
        if (this.selectedSource != null) {
            this.setTitle(String.format(
                    Locale.getDefault(),
                    "%s (%d)",
                    this.selectedSource.getName(),
                    this.articles.size()
            ));
        } else if (this.articles.isEmpty()) {
            this.setTitle(String.format(
                    Locale.getDefault(),
                    getString(R.string.app_title_number),
                    this.filteredItems.size()
            ));
        }
    }

    private void fillMenuInformation() {
        HashMap<String, HashSet<NewsSource>> topics = Objects.requireNonNull(this.info.get(CategoryType.TOPIC));
        HashMap<String, HashSet<NewsSource>> countries = Objects.requireNonNull(this.info.get(CategoryType.COUNTRY));
        HashMap<String, HashSet<NewsSource>> languages = Objects.requireNonNull(this.info.get(CategoryType.LANGUAGE));
        List<String> topicsKeys = new ArrayList<>(topics.keySet());
        Collections.sort(topicsKeys);
        Set<Integer> usedColors = new HashSet<>();
        for (int i = 0; i < topicsKeys.size(); i++) {
            int color;
            do {
                color = generateColor();
            } while (usedColors.contains(color));
            usedColors.add(color);
            this.colors.put(topicsKeys.get(i), color);
        }
        List<String> countryKeys = new ArrayList<>(countries.keySet());
        Collections.sort(countryKeys, Comparator.comparing(element -> this.countries.getOrDefault(element, element).toLowerCase(Locale.ROOT)));
        this.sortedKeys.put(CategoryType.COUNTRY, countryKeys);
        List<String> languageKeys = new ArrayList<>(languages.keySet());
        Collections.sort(languageKeys, Comparator.comparing(element -> this.languages.getOrDefault(element, element).toLowerCase(Locale.ROOT)));
        this.sortedKeys.put(CategoryType.LANGUAGE, languageKeys);
    }

    private void buildMenu() {
        if (this.menu == null) {
            this.shouldReloadMenu = true;
            return;
        }
        this.fillMenuInformation();
        this.menu.clear();
        List<String> topicsKeys = new ArrayList<>(Objects.requireNonNull(this.info.get(CategoryType.TOPIC)).keySet());
        Collections.sort(topicsKeys);
        List<String> countryKeys = Objects.requireNonNull(this.sortedKeys.get(CategoryType.COUNTRY));
        List<String> languageKeys = Objects.requireNonNull(this.sortedKeys.get(CategoryType.LANGUAGE));
        if (topicsKeys.size() > 0) {
            SubMenu subMenu = this.menu.addSubMenu(getString(R.string.submenu_title_topics));
            for (int j = 0; j < topicsKeys.size(); j++) {
                MenuItem menuItem = subMenu.add(CategoryType.TOPIC.getId(), j, j, topicsKeys.get(j));
                if (this.colors.containsKey(topicsKeys.get(j))) {
                    SpannableString spanString = new SpannableString(topicsKeys.get(j));
                    spanString.setSpan(new ForegroundColorSpan(this.colors.get(topicsKeys.get(j))), 0,     spanString.length(), 0); //fix the color to white
                    menuItem.setTitle(spanString);
                }
            }
        }
        if (countryKeys.size() > 0) {
            SubMenu subMenu = this.menu.addSubMenu(getString(R.string.submenu_title_countries));
            for (int j = 0; j < countryKeys.size(); j++) {
                String countryName = this.countries.containsKey(countryKeys.get(j)) ? this.countries.get(countryKeys.get(j)) : countryKeys.get(j);
                subMenu.add(CategoryType.COUNTRY.getId(), j, j, countryName);
            }
        }
        if (languageKeys.size() > 0) {
            SubMenu subMenu = this.menu.addSubMenu(getString(R.string.submenu_title_languages));
            for (int j = 0; j < languageKeys.size(); j++) {
                String languageName = this.languages.containsKey(languageKeys.get(j)) ? this.languages.get(languageKeys.get(j)) : languageKeys.get(j);
                subMenu.add(CategoryType.LANGUAGE.getId(), j, j, languageName);
            }
        }
    }

    private void fillSourcesData(String response) {
        if (response == null) { return; }
        HashMap<String, HashSet<NewsSource>> topics = new HashMap<>();
        HashMap<String, HashSet<NewsSource>> countries = new HashMap<>();
        HashMap<String, HashSet<NewsSource>> languages = new HashMap<>();
        ArrayList<NewsSource> defaultSources = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray sources = jsonObject.getJSONArray(getString(R.string.json_sources_key));
            for (int i = 0; i < sources.length(); i++) {
                JSONObject sourcesJSONObject = sources.getJSONObject(i);
                NewsSource newsSource = new NewsSource(sourcesJSONObject);
                if (!topics.containsKey(newsSource.getCategory())) {
                    topics.put(newsSource.getCategory(), new HashSet<>());
                }
                Objects.requireNonNull(topics.get(newsSource.getCategory())).add(newsSource);
                if (!countries.containsKey(newsSource.getCountry())) {
                    countries.put(newsSource.getCountry(), new HashSet<>());
                }
                Objects.requireNonNull(countries.get(newsSource.getCountry())).add(newsSource);
                if (!languages.containsKey(newsSource.getLanguage())) {
                    languages.put(newsSource.getLanguage(), new HashSet<>());
                }
                Objects.requireNonNull(languages.get(newsSource.getLanguage())).add(newsSource);
                defaultSources.add(newsSource);
            }
            topics.put(getString(R.string.category_all), new HashSet<>(defaultSources));
            countries.put(getString(R.string.category_all), new HashSet<>(defaultSources));
            languages.put(getString(R.string.category_all), new HashSet<>(defaultSources));
            this.info.put(CategoryType.TOPIC, topics);
            this.info.put(CategoryType.COUNTRY, countries);
            this.info.put(CategoryType.LANGUAGE, languages);
            this.defaultSources = defaultSources;
            this.fillSources();
            Log.d(TAG, "onNewsSourcesLoaded: " + info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fillSources() {
        this.buildMenu();
        this.updateSelectedItems();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void parseArticles(String response) {
        if (response == null) {
            this.onArticlesError();
            return;
        }
        try {
            this.articles.clear();
            JSONObject jsonObject = new JSONObject(response);
            JSONArray sources = jsonObject.getJSONArray(getString(R.string.json_articles_key));
            for (int i = 0; i < sources.length(); i++) {
                JSONObject sourcesJSONObject;
                try {
                    sourcesJSONObject = sources.getJSONObject(i);
                } catch (JSONException e) {
                    Log.d(TAG, "parseArticles: Unable to parse article" + i);
                    e.printStackTrace();
                    continue;
                }
                this.articles.add(new Article(sourcesJSONObject));
            }
            this.fillArticles();
            Log.d(TAG, "parseArticles: " + articles);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fillArticles() {
        if (this.articles.isEmpty()) { return; }
        this.viewPager.scrollTo(0, 0);
        this.articleAdapter.notifyDataSetChanged();
        this.updateTitle();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        this.drawerLayout.closeDrawer(this.listView);
        outState.putSerializable(getString(R.string.orientation_colors), this.colors);
        outState.putSerializable(getString(R.string.orientation_info_state), this.info);
        outState.putSerializable(getString(R.string.orientation_default_sources_state), this.defaultSources);
        outState.putSerializable(getString(R.string.orientation_filters_state), this.filters);
        outState.putSerializable(getString(R.string.orientation_selected_source_state), this.selectedSource);
        outState.putSerializable(getString(R.string.orientation_articles_state), this.articles);
        outState.putBoolean(getString(R.string.orientation_should_reload_menu_state), this.shouldReloadMenu);
        outState.putInt(getString(R.string.orientation_current_article), this.viewPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.colors = (HashMap<String, Integer>) savedInstanceState.getSerializable(getString(R.string.orientation_colors));
        this.info = (HashMap<CategoryType, HashMap<String, HashSet<NewsSource>>>) savedInstanceState.getSerializable(getString(R.string.orientation_info_state));
        this.defaultSources = (ArrayList<NewsSource>) savedInstanceState.getSerializable(getString(R.string.orientation_default_sources_state));
        this.filters = (HashMap<CategoryType, String>) savedInstanceState.getSerializable(getString(R.string.orientation_filters_state));
        this.selectedSource = (NewsSource) savedInstanceState.getSerializable(getString(R.string.orientation_selected_source_state));
        this.articles.clear();
        this.articles.addAll((ArrayList<Article>) savedInstanceState.getSerializable(getString(R.string.orientation_articles_state)));
        this.shouldReloadMenu = (boolean) savedInstanceState.getSerializable(getString(R.string.orientation_should_reload_menu_state));
        if (this.articles.size() > 0) {
            this.viewPager.setCurrentItem(savedInstanceState.getInt(getString(R.string.orientation_current_article)));
        }
    }

    private int generateColor() {
        int r = (int) (Math.random() * 255);
        int g = (int) (Math.random() * 255);
        int b = (int) (Math.random() * 255);
        return (0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
    }
}