package com.indigo.booking.model;

public enum Gender {
    M("Male"),
    F("Female"),
    O("Other");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
