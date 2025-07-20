package com.example.quizapp.repository;
import com.example.quizapp.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SubmissionRepository extends JpaRepository<Submission, Long> {}