package com.example.quizapp.controller;

import com.example.quizapp.entity.Question;
import com.example.quizapp.entity.Quiz;
import com.example.quizapp.entity.Submission;
import com.example.quizapp.entity.User;
import com.example.quizapp.repository.QuestionRepository;
import com.example.quizapp.repository.QuizRepository;
import com.example.quizapp.repository.SubmissionRepository;
import com.example.quizapp.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/participant")
@PreAuthorize("hasRole('PARTICIPANT')")
public class ParticipantController {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantController.class);

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String participantDashboard(Model model) {
        logger.debug("Loading participant dashboard for user");
        try {
            List<Quiz> availableQuizzes = quizRepository.findAll();
            model.addAttribute("quizzes", availableQuizzes != null ? availableQuizzes : Collections.emptyList());
            return "participant/dashboard";
        } catch (Exception e) {
            logger.error("Failed to load participant dashboard: {}", e.getMessage(), e); // Log full stack trace
            model.addAttribute("error", "Unable to load dashboard. Please try again.");
            return "participant/dashboard";
        }
    }

    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model) {
        logger.debug("Loading quiz with id: {} for participant", id);
        Quiz quiz = quizRepository.findById(id).orElse(null);
        if (quiz == null) {
            logger.warn("Quiz not found for id: {}", id);
            model.addAttribute("error", "Quiz not found with ID: " + id);
            return "redirect:/participant/dashboard";
        }
        try {
            List<Question> questions = questionRepository.findByQuizId(id);
            if (questions == null) {
                logger.error("QuestionRepository returned null for quiz id: {}", id);
                model.addAttribute("error", "Internal error loading questions.");
                return "redirect:/participant/dashboard";
            }
            if (questions.isEmpty()) {
                logger.warn("No questions found for quiz id: {}", id);
                model.addAttribute("error", "No questions available for this quiz.");
                return "redirect:/participant/dashboard";
            }
            model.addAttribute("quiz", quiz);
            model.addAttribute("questions", questions);
            return "participant/quiz-play";
        } catch (Exception e) {
            logger.error("Failed to load quiz with id {}: {}", id, e.getMessage(), e); // Log full stack trace
            model.addAttribute("error", "Unable to load quiz. Please try again later.");
            return "redirect:/participant/dashboard";
        }
    }

    @PostMapping("/quiz/submit")
    public String submitQuiz(@RequestParam("quizId") Long quizId,
                             @RequestParam Map<String, String> answers,
                             Model model,
                             Authentication authentication) {
        logger.debug("Submitting quizId: {}, answers: {} for participant", quizId, answers);
        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) {
            logger.error("Quiz not found for id: {}", quizId);
            model.addAttribute("error", "Quiz not found.");
            return "redirect:/participant/dashboard";
        }

        Map<Long, String> userAnswers = new HashMap<>();
        answers.forEach((key, value) -> {
            if (key.startsWith("question_")) {
                try {
                    String questionIdStr = key.replace("question_", "");
                    Long questionId = Long.parseLong(questionIdStr);
                    if (questionRepository.existsById(questionId)) {
                        userAnswers.put(questionId, value);
                    } else {
                        logger.warn("Question ID {} not found", questionId);
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid question ID format: {}, error: {}", key, e.getMessage());
                }
            }
        });

        List<Question> questions;
        try {
            questions = questionRepository.findByQuizId(quizId);
            if (questions == null) {
                logger.error("QuestionRepository returned null for quizId: {}", quizId);
                model.addAttribute("error", "Internal error loading questions.");
                return "redirect:/participant/dashboard";
            }
            if (questions.isEmpty()) {
                logger.error("No questions found for quizId: {}", quizId);
                model.addAttribute("error", "No questions available to score.");
                return "redirect:/participant/dashboard";
            }
        } catch (Exception e) {
            logger.error("Failed to fetch questions for quizId {}: {}", quizId, e.getMessage(), e);
            model.addAttribute("error", "Unable to process quiz submission.");
            return "redirect:/participant/dashboard";
        }

        int score = 0;
        int totalQuestions = questions.size();
        for (Question question : questions) {
            String userAnswer = userAnswers.get(question.getId());
            if (userAnswer != null && userAnswer.equals(question.getCorrectAnswer())) {
                score++;
            }
        }

        try {
            Submission submission = new Submission();
            submission.setQuizId(quizId);
            Long userId = getUserIdFromAuth(authentication);
            submission.setUserId(userId != null ? userId : 0L); // Fallback to 0 if user not found
            submission.setScore(score);
            submission.setAttemptTime(LocalDateTime.now());
            submission.setAnswers(userAnswers.toString());
            submissionRepository.save(submission);
            logger.info("Submission saved for userId {}, quizId {}, score {}", userId, quizId, score);
        } catch (Exception e) {
            logger.error("Failed to save submission for quizId {}: {}", quizId, e.getMessage(), e);
            model.addAttribute("error", "Submission failed to save. Please try again.");
            return "redirect:/participant/dashboard";
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        model.addAttribute("submittedAnswers", userAnswers);
        model.addAttribute("score", score);
        model.addAttribute("totalQuestions", totalQuestions);
        logger.info("Quiz submitted, score: {}/{} for participant", score, totalQuestions);

        return "participant/result";
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}