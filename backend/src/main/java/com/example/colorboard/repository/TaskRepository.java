package com.example.colorboard.repository;
import com.example.colorboard.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TaskRepository extends JpaRepository<Task,Long> {}
