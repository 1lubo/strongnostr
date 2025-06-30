package com.onelubo.strongnostr.dto;

import com.onelubo.strongnostr.model.workout.WorkoutExercise;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Workout response with computed metrics")
public class WorkoutResponse {

    @Schema(description = "Workout ID", example = "workout12345")
    private String id;

    @Schema(description = "User NPub (Nostr public key)")
    private String userNPub;

    @Schema(description = "Workout date in ISO format", example = "2023-10-01T12:00:00Z")
    private String workoutDate;

    @Schema(description = "List of exercises in the workout")
    private List<WorkoutExerciseResponse> exercises;

    @Schema(description = "Total volume (weight * reps) lifted for all exercises", example = "1500.0")
    private Double totalVolume;

    @Schema(description = "Total number of sets performed", example = "20")
    private Integer totalSets;

    @Schema(description = "Total number of repetitions performed", example = "100")
    private Integer totalReps;

    @Schema(description = "Average Rate of Perceived Exertion (RPE) for the workout", example = "7.5")
    private Double averageRpe;

    @Schema(description = "Duration of the workout in seconds", example = "3600")
    private Integer durationSeconds;

    @Schema(description = "Additional notes or comments about the workout")
    private String notes;

    @Schema(description = "Indicates if the workout is public or private", example = "true")
    private Boolean isPublic;

    public WorkoutResponse(String id, String userNPub, String workoutDate, List<WorkoutExerciseResponse> exercises, Double totalVolume, Integer totalSets, Integer totalReps, Double averageRpe, Integer durationSeconds, String notes, Boolean isPublic) {
        this.id = id;
        this.userNPub = userNPub;
        this.workoutDate = workoutDate;
        this.exercises = exercises;
        this.totalVolume = totalVolume;
        this.totalSets = totalSets;
        this.totalReps = totalReps;
        this.averageRpe = averageRpe;
        this.durationSeconds = durationSeconds;
        this.notes = notes;
        this.isPublic = isPublic;
    }
}
