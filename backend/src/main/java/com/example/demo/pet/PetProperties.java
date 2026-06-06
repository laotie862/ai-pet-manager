package com.example.demo.pet;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "petcare.pet")
public class PetProperties {
    private int freeLimit = 3;

    public int getFreeLimit() {
        return freeLimit;
    }

    public void setFreeLimit(int freeLimit) {
        this.freeLimit = freeLimit;
    }
}
