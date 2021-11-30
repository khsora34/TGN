package com.setoncios.mobileappdevelopment.tgn.runnable;

public interface ArticlesObservable {
    void onArticlesLoaded(String response);
    void onArticlesError();
}
