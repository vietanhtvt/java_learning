package com.taskflow.repository;

import com.taskflow.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByProjectId(UUID projectId);

    Optional<Label> findByNameAndProjectId(String name, UUID projectId);

    boolean existsByNameAndProjectId(String name, UUID projectId);
}
