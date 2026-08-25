package com.example.colorboard.controller;
import com.example.colorboard.model.Task;
import com.example.colorboard.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin
public class TaskController {
  private final TaskService service;
  public TaskController(TaskService service){this.service=service;}
  @GetMapping public List<Task> all(){return service.all();}
  @PostMapping public Task create(@RequestBody Task t){return service.create(t);}
  @PutMapping("/{id}") public Task update(@PathVariable Long id,@RequestBody Task t){return service.update(id,t);}
  @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){service.delete(id);return ResponseEntity.noContent().build();}
  @GetMapping("/health") public String health(){return "ColorBoard backend is healthy";}
}
