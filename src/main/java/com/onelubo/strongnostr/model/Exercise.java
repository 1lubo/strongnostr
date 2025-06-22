package com.onelubo.strongnostr.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.Objects;

@Document(collection = "exercises")
public class Exercise {
    @Id
    private String id;

    @NotBlank(message = "Exercise name cannot be blank")
    @TextIndexed(weight = 10)
    private String name;

    @TextIndexed(weight = 5)
    private String description;

    private String equipment;

    private boolean isCustom = false;

    private String createdByUserId;

    @CreatedDate
    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;

    public Exercise() {}

    public Exercise(String name, String description, String equipment, boolean isCustom, String createdByUserId) {
        this.name = name;
        this.description = description;
        this.equipment = equipment;
        this.isCustom = isCustom;
        this.createdByUserId = createdByUserId;
    }

    public Exercise(String name, String description, String equipment) {
        this.name = name;
        this.description = description;
        this.equipment = equipment;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Exercise exercise = (Exercise) object;
        return Objects.equals(name, exercise.name) && Objects.equals(description, exercise.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, equipment, createdByUserId);
    }

    @Override
    public String toString() {
        return "Exercise{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", equipment='" + equipment + '\'' +
                '}';
    }
}
