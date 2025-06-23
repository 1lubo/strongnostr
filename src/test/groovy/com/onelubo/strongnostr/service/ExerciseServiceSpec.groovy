package com.onelubo.strongnostr.service

import com.onelubo.strongnostr.model.workout.Exercise
import com.onelubo.strongnostr.repository.ExerciseRepository
import com.onelubo.strongnostr.service.workout.ExerciseService
import spock.lang.Specification
import spock.lang.Unroll

class ExerciseServiceSpec extends Specification {

    ExerciseService exerciseService
    ExerciseRepository exerciseRepository

    def setup() {
        exerciseRepository = Mock(ExerciseRepository)
        exerciseService = new ExerciseService(exerciseRepository)
    }

    @Unroll
    def "should create exercise with valid details"() {
        when: "Creating an exercise"
        def result = exerciseService.createExercise(exerciseName, exerciseDescription, equipment)

        then: "The exercise should be created successfully"
        result != null
        result.getName() == exerciseName
        result.getDescription() == exerciseDescription
        result.getEquipment() == equipment

        and: "The exercise should be saved in the database"
        1 * exerciseRepository.save(exercise) >> expectedResult

        where:
        exerciseName | exerciseDescription                             | equipment | exercise                                                                         | expectedResult
        "Push Up"    | "A bodyweight exercise for upper body strength" | "None"    | new Exercise("Push Up", "A bodyweight exercise for upper body strength", "None") | createExerciseWithId("Push Up", "A bodyweight exercise for upper body strength", "None", UUID.randomUUID().toString())
        "Squat"      | "A compound exercise for lower body strength"   | null      | new Exercise("Squat", "A compound exercise for lower body strength", null)       | createExerciseWithId("Squat", "A compound exercise for lower body strength", null, UUID.randomUUID().toString())
        "Deadlift"   | null                                            | null      | new Exercise("Deadlift", null, null)                                             | createExerciseWithId("Deadlift", null, null, UUID.randomUUID().toString())
    }

    def "should update existing exercise"() {
        given: "An existing exercise"
        def exerciseId = UUID.randomUUID().toString()
        def existingExercise = new Exercise("Old Exercise", "Old Description", "Old Equipment")
        existingExercise.setId(exerciseId)
        exerciseRepository.findById(exerciseId) >> Optional.of(existingExercise)

        and: "New details for the exercise"
        def newExerciseName = "Updated Exercise"
        def newExerciseDescription = "Updated Description"
        def newEquipment = "Updated Equipment"

        when: "Updating the exercise"
        exerciseService.updateExercise(exerciseId, newExerciseName, newExerciseDescription, newEquipment)

        then: "The exercise should be updated successfully and saved in the database"
        1 * exerciseRepository.save({ Exercise e ->
            e.getName() == newExerciseName &&
                    e.getDescription() == newExerciseDescription &&
                    e.getEquipment() == newEquipment
        })
    }

    def "should delete exercise by ID"() {
        given: "An existing exercise"
        def exerciseId = UUID.randomUUID().toString()
        def existingExercise = new Exercise("Exercise to Delete", "Description", "Equipment")
        existingExercise.setId(exerciseId)
        exerciseRepository.findById(exerciseId) >> Optional.of(existingExercise)

        when: "Deleting the exercise"
        exerciseService.deleteExercise(existingExercise)

        then: "The exercise should be deleted successfully"
        1 * exerciseRepository.delete(existingExercise)
    }

    Exercise createExerciseWithId(String name, String description, String equipment, String id) {
        def ex = new Exercise(name, description, equipment)
        ex.setId(id)
        return ex
    }
}
