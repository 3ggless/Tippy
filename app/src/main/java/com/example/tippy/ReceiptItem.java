package com.example.tippy;

import java.io.Serializable;

public class ReceiptItem implements Serializable {
    private final String name;
    private final double price;
    private int assignedPartyIndex;

    public ReceiptItem(String name, double price) {
        this.name = name;
        this.price = price;
        this.assignedPartyIndex = -1;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getAssignedPartyIndex() {
        return assignedPartyIndex;
    }

    public void setAssignedPartyIndex(int assignedPartyIndex) {
        this.assignedPartyIndex = assignedPartyIndex;
    }

    public boolean isAssigned() {
        return assignedPartyIndex >= 0;
    }
}
