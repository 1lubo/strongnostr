package com.onelubo.strongnostr.exception;

public class WorkoutNotFoundException extends RuntimeException {
    public WorkoutNotFoundException(String workoutId) {
        super("Workout not found with ID: " + workoutId);
    }
}
