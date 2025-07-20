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
    private UserRepository userRepository; // ✅ Inject UserRepository

    @GetMapping("/dashboard")
    public String participantDashboard(Model model) {
        logger.debug("Loading participant dashboard for user");
        try {
            List<Quiz> availableQuizzes = quizRepository.findAll();
            model.addAttribute("quizzes", availableQuizzes != null ? availableQuizzes : List.of());
            return "participant/dashboard";
        } catch (Exception e) {
            logger.error("Failed to load participant dashboard: {}", e.getMessage());
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
            model.addAttribute("error", "Quiz not found.");
            return "redirect:/participant/dashboard";
        }
        try {
            model.addAttribute("quiz", quiz);
            model.addAttribute("questions", questionRepository.findByQuizId(id));
            return "/participant/quiz-play";
        } catch (Exception e) {
            logger.error("Failed to load quiz with id {}: {}", id, e.getMessage());
            model.addAttribute("error", "Unable to load quiz. Please try again.");
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
                    logger.warn("Invalid question ID format: {}", key, e);
                }
            }
        });

        List<Question> questions;
        try {
            questions = questionRepository.findByQuizId(quizId);
        } catch (Exception e) {
            logger.error("Failed to fetch questions for quizId {}: {}", quizId, e.getMessage());
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

        // ✅ Save submission to database
        try {
            Submission submission = new Submission();
            submission.setQuizId(quizId);

            Long userId = getUserIdFromAuth(authentication); // Corrected version
            submission.setUserId(userId != null ? userId : 0L); // fallback

            submission.setScore(score);
            submission.setAttemptTime(LocalDateTime.now());
            submission.setAnswers(userAnswers.toString());

            submissionRepository.save(submission);
            logger.info("Submission saved for userId {}, quizId {}, score {}", userId, quizId, score);
        } catch (Exception e) {
            logger.error("Failed to save submission: {}", e.getMessage());
        }

        // ✅ Show result page
        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);
        model.addAttribute("submittedAnswers", userAnswers);
        model.addAttribute("score", score);
        model.addAttribute("totalQuestions", totalQuestions);
        logger.info("Quiz submitted, score: {}/{} for participant", score, totalQuestions);

        return "participant/result";
    }

    // ✅ Extract userId from Authentication using UserRepository
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}
