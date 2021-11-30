package com.setoncios.mobileappdevelopment.tgn.models;

import org.json.JSONException;
import org.json.JSONObject;

public class LanguageDTO {
    private final String code;
    private final String name;

    public LanguageDTO(JSONObject jsonObject) throws JSONException {
        this.code = jsonObject.getString("code");
        this.name = jsonObject.getString("name");
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
