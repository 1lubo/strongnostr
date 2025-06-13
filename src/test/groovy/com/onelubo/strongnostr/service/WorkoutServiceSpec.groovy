package com.onelubo.strongnostr.service

import com.onelubo.strongnostr.model.Exercise
import spock.lang.Specification

class WorkoutServiceSpec extends Specification {

    def "should create workout with valid exercise"() {
        given: "A valid exercise"
        def exerciseName = "Lowbar Squats"
        def exercise = new Exercise(exerciseName, null, 5,5, 405.0)

        when: "Creating a workout"
        def workoutService = new WorkoutService()
        def workout = workoutService.createWorkout(exercise)

        then: "The workout should be created successfully"
        workout != null
        workout.getExercises().size() == 1
    }

    def "should add valid exercise to workout"() {
        given: "An existing workout with an exercise"
        def firstExerciseName = "Bench Press"
        def exercise1 = new Exercise(firstExerciseName, null, 5, 5, 225.0)
        def workoutService = new WorkoutService()
        def workout = workoutService.createWorkout(exercise1)

        when: "Adding a new exercise to the workout"
        def secondExerciseName = "Deadlift"
        def exercise2 = new Exercise(secondExerciseName, null, 5, 5, 495.0)
        workoutService.addExerciseToWorkout(workout, exercise2)

        then: "The workout should be updated successfully"
        workout.getExercises().size() == 2
        workout.getExercises()[0].getName() != workout.getExercises()[1].getName()

    }

    def "should delete exercise from workout"() {
        given: "A workout with multiple exercises"
        def exercise1 = new Exercise("Bench Press", null, 5, 5, 225.0)
        def exercise2 = new Exercise("Deadlift", null, 5, 5, 495.0)
        def workoutService = new WorkoutService()
        def workout = workoutService.createWorkout(exercise1)
        workoutService.addExerciseToWorkout(workout, exercise2)

        when: "Deleting an exercise from the workout"
        workoutService.removeExerciseFromWorkout(workout, exercise1)

        then: "The exercise should be removed successfully"
        workout.getExercises().size() == 1
        !workout.getExercises().contains(exercise1)
    }
}
