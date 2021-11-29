package com.setoncios.mobileappdevelopment.tgn.models;

public enum CategoryType {
    TOPIC, COUNTRY, LANGUAGE;

    public static CategoryType getFromId(int id) {
        switch (id) {
            case 0:
                return TOPIC;
            case 1:
                return COUNTRY;
            case 2:
                return LANGUAGE;
            default:
                return null;
        }
    }

    public int getId() {
        switch (this) {
            case TOPIC:
                return 0;
            case COUNTRY:
                return 1;
            case LANGUAGE:
                return 2;
        }
        return -1;
    }
}
