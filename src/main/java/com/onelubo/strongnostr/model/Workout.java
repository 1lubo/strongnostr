package com.onelubo.strongnostr.model;

import java.util.ArrayList;
import java.util.List;

public class Workout {
    private List<Exercise> exercises = new ArrayList<>();

    public Workout() {}

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void updateWorkout(Exercise updatedExercise) {
        for (int i = 0; i < exercises.size(); i++) {
            Exercise exercise = exercises.get(i);
            if (exercise.getName().equals(updatedExercise.getName())) {
                exercises.set(i, updatedExercise);
                return;
            }
        }
        // If the exercise is not found, add it
        exercises.add(updatedExercise);
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void addExercise(Exercise exercise) {
        this.exercises.add(exercise);
    }
}
