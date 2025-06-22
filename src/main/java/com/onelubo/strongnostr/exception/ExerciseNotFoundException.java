package com.onelubo.strongnostr.exception;

public class ExerciseNotFoundException extends RuntimeException {
    public ExerciseNotFoundException(String exerciseId) {
        super("Exercise not found with ID: " + exerciseId);
    }

    public ExerciseNotFoundException(String exerciseName, String exerciseDescription) {
        super("Exercise not found with name: " + exerciseName + " and description: " + exerciseDescription);
    }
}
