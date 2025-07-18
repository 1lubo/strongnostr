package com.onelubo.strongnostr.rest;

import com.onelubo.strongnostr.dto.ExerciseSetDto;
import com.onelubo.strongnostr.dto.WorkoutResponse;
import com.onelubo.strongnostr.exception.WorkoutNotFoundException;
import com.onelubo.strongnostr.model.workout.Workout;
import com.onelubo.strongnostr.service.workout.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nostr/workout")
@PreAuthorize("hasRole('ROLE_USER')")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/{workoutId}")
    @PreAuthorize("@workoutService.isWorkoutOwner(authentication, #workoutId)")
    public ResponseEntity<?> getWorkoutById(@PathVariable("workoutId") String workoutId, Authentication authentication) {
        try {
            Workout workout = workoutService.getWorkoutById(workoutId);
            return ResponseEntity.ok(workout.toWorkoutResponse());
        } catch (WorkoutNotFoundException e) {
            return ResponseEntity.status(404).body(String.format("Workout with ID '%s' not found", workoutId));
        }
    }

    @PostMapping()
    public ResponseEntity<?> createWorkout(@Valid @RequestBody ExerciseSetDto workoutDto, BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList());
        }

        String userNPub = authentication.getName();

        Workout createdWorkout = workoutService.createWorkout(workoutDto.getExercise(), workoutDto.getWorkoutSet(), userNPub);
        return ResponseEntity.ok(createdWorkout.toWorkoutResponse());
    }

    @PostMapping("/addExercise/{workoutId}")
    public ResponseEntity<?> addExercise(@Valid @RequestBody ExerciseSetDto workoutDto,
                                               @PathVariable("workoutId") String workoutId) {
        try {
            Workout existingWorkout = workoutService.getWorkoutById(workoutId);
            Workout updatedWorkout = workoutService.addExerciseToWorkout(existingWorkout, workoutDto.getExercise(),
                                                       workoutDto.getWorkoutSet());
            return ResponseEntity.ok(updatedWorkout.toWorkoutResponse());

        } catch (WorkoutNotFoundException e) {
            return ResponseEntity.status(404).body("Workout with ID " + workoutId + " not found");
        }
    }

    @GetMapping()
    public ResponseEntity<List<WorkoutResponse>> getWorkouts(Authentication authentication, @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size){
        String userNPub = authentication.getName();
        List<Workout> workouts = workoutService.getWorkoutsByUser(userNPub, page, size);
        return ResponseEntity.ok(workouts.stream().map(Workout::toWorkoutResponse).toList());
    }
}
