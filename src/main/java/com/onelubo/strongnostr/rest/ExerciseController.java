package com.onelubo.strongnostr.rest;

import com.onelubo.strongnostr.model.workout.Exercise;
import com.onelubo.strongnostr.service.workout.ExerciseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nostr/exercise")
@PreAuthorize("hasRole('ROLE_USER')")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @PostMapping
    private ResponseEntity<Exercise> createNewExercise(@Valid @RequestBody Exercise exercise, Authentication authentication) {
        String userNPub = authentication.getName();
        exercise.setCreatedByUserId(userNPub);

        Exercise createdExercise = exerciseService.findOrCreateExercise(exercise);
        return ResponseEntity.ok(createdExercise);

    }
}
