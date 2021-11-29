package com.setoncios.mobileappdevelopment.tgn.models;

import org.json.JSONException;
import org.json.JSONObject;

public class NewsSource {
    private final String id;
    private final String name;
    private final String category;
    private final String language;
    private final String country;

    public NewsSource(JSONObject jsonObject) throws JSONException {
        this.id = jsonObject.getString("id");
        this.name = jsonObject.getString("name");
        this.category = jsonObject.getString("category");
        this.language = jsonObject.getString("language");
        this.country = jsonObject.getString("country");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }
}
