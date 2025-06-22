package com.onelubo.strongnostr.service

import com.onelubo.strongnostr.model.Exercise
import com.onelubo.strongnostr.model.Workout
import com.onelubo.strongnostr.model.WorkoutExercise
import com.onelubo.strongnostr.model.WorkoutSet
import com.onelubo.strongnostr.repository.WorkoutRepository
import spock.lang.Specification

class WorkoutServiceSpec extends Specification {

    WorkoutService workoutService
    ExerciseService exerciseService
    WorkoutRepository workoutRepository

    def setup() {
        exerciseService = Mock(ExerciseService)
        workoutRepository = Mock(WorkoutRepository)
        workoutService = new WorkoutService(exerciseService, workoutRepository)
    }

    def "should create new workout with an existing exercise"() {
        given: "A valid exercise which already exists in the database"
        def exerciseName = "Lowbar Squats"
        def exerciseDescription = "A compound exercise for lower body strength"
        def equipment = "Barbell"
        def exercise = new Exercise(exerciseName, exerciseDescription, equipment)
        def savedExercise = new Exercise(exerciseName, exerciseDescription, equipment)
        savedExercise.setId(UUID.randomUUID().toString())
        def workoutSet = new WorkoutSet(100.0, 5)
        def expectedWorkout = new Workout()
        expectedWorkout.setId(UUID.randomUUID().toString())
        expectedWorkout.addExercise(new WorkoutExercise(savedExercise.getId(), savedExercise.getName(), savedExercise.getEquipment(), List.of(workoutSet),))
        exerciseService.addExercise(_ as Exercise) >> savedExercise

        when: "Creating a workout"
        def result = workoutService.createWorkout(exercise, workoutSet)

        then: "The existing exercise should be found in the database and used in the workout"
        1 * exerciseService.addExercise(exercise) >> savedExercise

        and: "The workout should be created successfully"
        result != null
        result.getId() == expectedWorkout.getId()
        result.getExercises().size() == 1
        result.getExercises()[0].exerciseId == savedExercise.getId()
        result.getExercises()[0].exerciseName == savedExercise.getName()
        result.getExercises()[0].getHeaviestSet() == workoutSet
        result.getExercises()[0].getTotalVolume() == workoutSet.getReps() * workoutSet.getWeight()

        and: "The workout should be saved in the database"
        1 * workoutRepository.save(_) >> expectedWorkout
    }

    def "should create a new workout with a new exercise"() {
        given: "A new exercise that does not exist in the database"
        def exerciseName = "Overhead Press"
        def exerciseDescription = "An upper body strength exercise"
        def equipment = "Barbell"
        def exercise = new Exercise(exerciseName, exerciseDescription, equipment)
        def newExercise = new Exercise(exerciseName, exerciseDescription, equipment)
        newExercise.setId(UUID.randomUUID().toString())
        def workoutSet = new WorkoutSet(60.0, 8)
        def expectedWorkout = new Workout()
        expectedWorkout.setId(UUID.randomUUID().toString())
        expectedWorkout.addExercise(new WorkoutExercise(newExercise.getId(), newExercise.getName(), newExercise.getEquipment(), List.of(workoutSet),))
        exerciseService.addExercise(_ as Exercise) >> newExercise

        when: "Creating a workout with a new exercise"
        def result = workoutService.createWorkout(exercise, workoutSet)

        then: "The new exercise should be created and saved in the database and used in the workout"
        1 * exerciseService.addExercise(exercise) >> newExercise

        then: "The workout should be created and saved successfully"
        result != null
        result.getId() == expectedWorkout.getId()
        result.getExercises().size() == 1
        result.getExercises()[0].getExerciseName() == exerciseName
        result.getExercises()[0].getHeaviestSet() == workoutSet
        result.getExercises()[0].getTotalVolume() == workoutSet.getReps() * workoutSet.getWeight()

        and: "The new workout should be saved in the database"
        1 * workoutRepository.save(_) >> expectedWorkout

    }

    def "should add valid exercise to workout"() {
        given: "An existing workout with an exercise"
        def firstExerciseName = "Bench Press"
        def firstExerciseDescription = "A compound exercise for upper body strength"
        def equipment = "Barbell"
        def exercise1 = new Exercise(firstExerciseName, firstExerciseDescription, equipment)
        exercise1.setId(UUID.randomUUID().toString())
        def secondExerciseName = "Deadlift"
        def secondExerciseDescription = "A compound exercise for lower body strength"
        def exercise2 = new Exercise(secondExerciseName, secondExerciseDescription, equipment)
        def newExercise = new Exercise(secondExerciseName, secondExerciseDescription, equipment)
        newExercise.setId(UUID.randomUUID().toString())
        def set = new WorkoutSet(80.0, 5)
        def existingWorkout = new Workout()
        existingWorkout.addExercise(new WorkoutExercise(exercise1.getId(), exercise1.getName(), exercise1.getEquipment(), List.of(new WorkoutSet(80.0, 5)),))
        existingWorkout.setId(UUID.randomUUID().toString())
        exerciseService.addExercise(_ as Exercise) >> newExercise

        when: "Adding a new exercise to the workout"
        workoutService.addExerciseToWorkout(existingWorkout, exercise2, set)

        then: "the new exercise should be created and saved in the database and used in the workout"
        1 * exerciseService.addExercise(exercise2) >> newExercise

        then: "The workout should be updated successfully"
        existingWorkout.getExercises().size() == 2
        existingWorkout.getExercises()[0].getExerciseName() != existingWorkout.getExercises()[1].getExerciseName()

        and: "The workout should be saved in the database"
        1 * workoutRepository.save(existingWorkout) >> existingWorkout

    }

    def "should remove existing WorkoutExercise from workout"() {
        given: "A workout with multiple exercises"
        def exercise1 = new Exercise("Bench Press", null, "Barbell")
        def exercise2 = new Exercise("Deadlift", null, "Barbell")
        exercise1.setId(UUID.randomUUID().toString())
        exercise2.setId(UUID.randomUUID().toString())
        def workoutExercise1 = new WorkoutExercise(exercise1.getId(), exercise1.getName(),exercise1.getEquipment(), List.of(new WorkoutSet(80.0, 5)),)
        def workoutExercise2 = new WorkoutExercise(exercise2.getId(), exercise2.getName(),exercise2.getEquipment(), List.of(new WorkoutSet(100.0, 3)),)
        def workout = new Workout()
        workout.addExercise(workoutExercise1)
        workout.addExercise(workoutExercise2)
        workout.setId(UUID.randomUUID().toString())


        when: "Removing an exercise from the workout"
        workoutService.removeExerciseFromWorkout(workout, exercise1)

        then: "The exercise should be removed successfully"
        workout.getExercises().size() == 1
        !workout.getExercises().contains(exercise1)

        and: "The workout should be saved in the database"
        1 * workoutRepository.save(workout) >> workout
    }

    def "should add a set of the same exercise to an existing WorkoutExercise"() {
        given: "A workout with an existing exercise"
        def exerciseName = "Bench Press"
        def exerciseDescription = "A compound exercise for upper body strength"
        def equipment = "Barbell"
        def exerciseId = UUID.randomUUID().toString()
        def workoutId = UUID.randomUUID().toString()
        def exercise = new Exercise(exerciseName, exerciseDescription, equipment)
        exercise.setId(exerciseId)
        def workoutSet = new WorkoutSet(80.0, 5)
        def workoutExercise = new WorkoutExercise(exercise.getId(), exercise.getName(), exercise.getEquipment(), List.of(workoutSet),)
        def workout = new Workout()
        workout.addExercise(workoutExercise)
        workout.setId(workoutId)
        def identicalExercise = new Exercise(exerciseName, exerciseDescription, equipment)
        def newSet = new WorkoutSet(85.0, 3)
        def updatedWorkout = new Workout()
        def updatedWorkoutExercise = new WorkoutExercise(exercise.getId(), exercise.getName(), exercise.getEquipment(), List.of(workoutSet, newSet),)
        updatedWorkout.addExercise(updatedWorkoutExercise)
        updatedWorkout.setId(workoutId)
        exerciseService.addExercise(_ as Exercise) >> exercise

        when: "Adding a new set to the existing WorkoutExercise"
        workoutService.addExerciseToWorkout(workout, identicalExercise, newSet)


        then: "The set should be added successfully"
        workout.getExercises().size() == 1
        workout.getExercises()[0].getTotalSets() == 2
        workout.getExercises()[0].getTotalReps() == workoutSet.getReps() + newSet.getReps()
        workout.getExercises()[0].getTotalVolume() == (workoutSet.getWeight() * workoutSet.getReps()) + (newSet.getWeight() * newSet.getReps())

        and: "The workout should be saved in the database"
        1 * workoutRepository.save(_ as Workout) >> updatedWorkout
    }
}
