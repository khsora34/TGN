package com.setoncios.mobileappdevelopment.tgn.models;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

public class Article implements Serializable {
    private String author;
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private Date publishedAt;

    public Article(JSONObject jsonObject) {
        try {
            this.author = jsonObject.getString("author");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.title = jsonObject.getString("title");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.description = jsonObject.getString("description");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.url = jsonObject.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            this.urlToImage = jsonObject.getString("urlToImage");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            DateTimeFormatter dtf = new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .optionalStart()
                    .appendLiteral('T')
                    .optionalEnd()
                    .appendOptional(DateTimeFormatter.ISO_TIME)
                    .toFormatter();
            Instant l = LocalDateTime.from(dtf.parse(jsonObject.getString("publishedAt"))).toInstant(ZoneOffset.UTC);
            this.publishedAt = Date.from(l);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public String getAuthor() {
        return author;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getUrlToImage() {
        return urlToImage;
    }

    @Nullable
    public Date getPublishedAt() {
        return publishedAt;
    }
}
