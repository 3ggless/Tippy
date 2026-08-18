package com.example.tippy;

import java.io.Serializable;

public class Party implements Serializable {
    public static final int[] PARTY_COLORS = {
            R.color.party_red,
            R.color.party_blue,
            R.color.party_green,
            R.color.party_yellow
    };

    public static final String[] DEFAULT_NAMES = {
            "Red", "Blue", "Green", "Yellow"
    };

    public static final String[] SWIPE_DIRECTIONS = {
            "Swipe Left", "Swipe Right", "Swipe Up", "Swipe Down"
    };

    private final int index;
    private String name;
    private final int colorResId;

    public Party(int index, String name) {
        this.index = index;
        this.name = name;
        this.colorResId = PARTY_COLORS[index];
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColorResId() {
        return colorResId;
    }
}
