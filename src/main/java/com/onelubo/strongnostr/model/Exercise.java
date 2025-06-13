package com.onelubo.strongnostr.model;

import java.util.Objects;

public class Exercise {
    private final String name;
    private final String description;
    private final int sets;
    private int reps;
    private double weight;

    public Exercise(String name, String description, int sets, int reps, double weight) {
        this.name = name;
        this.description = description;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Exercise exercise = (Exercise) object;
        return Objects.equals(name, exercise.name) && Objects.equals(description, exercise.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, sets, reps, weight);
    }
}
