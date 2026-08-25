package com.example.colorboard.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="tasks")
public class Task {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;
  @Column(nullable=false) private String title;
  private String description;
  @Column(nullable=false) private String status;
  private String color;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @PrePersist void created(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
  @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
  public Task(){}
  public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;}
  public String getStatus(){return status;} public String getColor(){return color;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
  public void setId(Long id){this.id=id;} public void setTitle(String v){this.title=v;} public void setDescription(String v){this.description=v;}
  public void setStatus(String v){this.status=v;} public void setColor(String v){this.color=v;} public void setCreatedAt(LocalDateTime v){this.createdAt=v;} public void setUpdatedAt(LocalDateTime v){this.updatedAt=v;}
}
