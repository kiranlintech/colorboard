package com.example.colorboard.service;
import com.example.colorboard.model.Task;
import com.example.colorboard.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class TaskService {
  private final TaskRepository repo;
  public TaskService(TaskRepository repo){this.repo=repo;}
  public List<Task> all(){return repo.findAll();}
  public Task create(Task t){return repo.save(t);}
  public Task update(Long id, Task input){
    Task t=repo.findById(id).orElseThrow();
    if(input.getTitle()!=null)t.setTitle(input.getTitle());
    if(input.getDescription()!=null)t.setDescription(input.getDescription());
    if(input.getStatus()!=null)t.setStatus(input.getStatus());
    if(input.getColor()!=null)t.setColor(input.getColor());
    return repo.save(t);
  }
  public void delete(Long id){repo.deleteById(id);}
}
