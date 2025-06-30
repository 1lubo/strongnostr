package com.onelubo.strongnostr.model

import com.onelubo.strongnostr.model.workout.Workout
import com.onelubo.strongnostr.model.workout.WorkoutExercise
import spock.lang.Specification

class WorkoutSpec extends Specification {
    
    final String VALID_NPUB = "npub1examplevalidpubkey1234567890abcdefg"

    def "calculateMetrics sets all metrics to zero or null when exercises list is empty"() {
        given:
        def workout = new Workout(VALID_NPUB)
        workout.exercises = []

        when:
        workout.calculateMetrics()

        then:
        workout.getTotalVolume() == 0.0
        workout.getTotalSets() == 0
        workout.getTotalReps() == 0
        workout.getAverageRpe() == null
    }

    def "calculateMetrics calculates correct metrics for single exercise with all values"() {
        given:
        def exercise = Mock(WorkoutExercise) {
            getTotalVolume() >> 100.0
            getTotalSets() >> 3
            getTotalReps() >> 24
            getAverageRpe() >> 8.0
            getTotalRestTimeSeconds() >> 180
        }
        def workout = new Workout(VALID_NPUB)
        workout.exercises = [exercise]

        when:
        workout.calculateMetrics()

        then:
        workout.getTotalVolume() == 100.0
        workout.getTotalSets() == 3
        workout.getTotalReps() == 24
        workout.getAverageRpe() == 8.0
        workout.getDurationSeconds() == 180
    }

    def "calculateMetrics averages RPE only for exercises with non-null RPE"() {
        given:
        def exercise1 = Mock(WorkoutExercise) {
            getTotalVolume() >> 50.0
            getTotalSets() >> 2
            getTotalReps() >> 10
            getAverageRpe() >> 7.0
            getTotalRestTimeSeconds() >> 60
        }
        def exercise2 = Mock(WorkoutExercise) {
            getTotalVolume() >> 70.0
            getTotalSets() >> 4
            getTotalReps() >> 20
            getAverageRpe() >> null
            getTotalRestTimeSeconds() >> 120
        }
        def exercise3 = Mock(WorkoutExercise) {
            getTotalVolume() >> 30.0
            getTotalSets() >> 1
            getTotalReps() >> 5
            getAverageRpe() >> 9.0
            getTotalRestTimeSeconds() >> 30
        }
        def workout = new Workout(VALID_NPUB)
        workout.exercises = [exercise1, exercise2, exercise3]

        when:
        workout.calculateMetrics()

        then:
        workout.getTotalVolume() == 150.0
        workout.getTotalSets() == 7
        workout.getTotalReps() == 35
        workout.getAverageRpe() == 8.0
        workout.getDurationSeconds() == 210
    }

    def "calculateMetrics sets averageRpe to zero if all exercises have null RPE"() {
        given:
        def exercise1 = Mock(WorkoutExercise) {
            getTotalVolume() >> 10.0
            getTotalSets() >> 1
            getTotalReps() >> 5
            getAverageRpe() >> null
            getTotalRestTimeSeconds() >> 10
        }
        def exercise2 = Mock(WorkoutExercise) {
            getTotalVolume() >> 20.0
            getTotalSets() >> 2
            getTotalReps() >> 10
            getAverageRpe() >> null
            getTotalRestTimeSeconds() >> 20
        }
        def workout = new Workout()
        workout.exercises = [exercise1, exercise2]

        when:
        workout.calculateMetrics()

        then:
        workout.getTotalVolume() == 30.0
        workout.getTotalSets() == 3
        workout.getTotalReps() == 15
        workout.getAverageRpe() == 0.0
        workout.getDurationSeconds() == 30
    }
}
