package com.onelubo.strongnostr.model;

import java.util.ArrayList;
import java.util.List;

public class Workout {
    private List<Exercise> exercises = new ArrayList<>();

    public Workout() {}

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void updateWorkout(Exercise newExercise) {
        exercises.stream()
                .filter(exercise -> exercise.equals(newExercise))
                .findFirst()
                .ifPresentOrElse(
                        exercise -> exercises.remove(exercise),
                        () -> exercises.add(newExercise)
                                );
    }

    public void addExercise(Exercise exercise) {
        this.exercises.add(exercise);
    }
}
