package com.example.quizapp.controller;

import com.example.quizapp.entity.Quiz;
import com.example.quizapp.entity.Question;
import com.example.quizapp.repository.QuizRepository;
import com.example.quizapp.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        logger.debug("Loading admin dashboard for authenticated ADMIN user");
        try {
            List<Quiz> quizzes = quizRepository.findAll();
            if (quizzes == null) {
                quizzes = new ArrayList<>();
                logger.warn("No quizzes found in repository");
            }
            model.addAttribute("quizzes", quizzes);
            // Check for existing error message and log if present
            String error = (String) model.asMap().get("error");
            if (error != null) {
                logger.info("Displaying error on dashboard: {}", error);
            }
            return "admin/quiz-list";
        } catch (Exception e) {
            logger.error("Failed to load admin dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Unable to load dashboard due to an internal error. Please try again later.");
            return "admin/quiz-list";
        }
    }

    @GetMapping("/quiz/new")
    public String newQuiz(Model model) {
        logger.debug("Loading new quiz form for ADMIN user");
        model.addAttribute("quiz", new Quiz());
        model.addAttribute("isNewQuiz", true); // Flag for new quiz form
        return "admin/quiz-form";
    }

    @PostMapping("/quiz/save")
    public String saveQuiz(@ModelAttribute Quiz quiz, Model model) {
        logger.debug("Saving new quiz: {}", quiz);
        try {
            if (quiz.getId() != null && quizRepository.existsById(quiz.getId())) {
                logger.warn("Quiz with id {} already exists, updating instead", quiz.getId());
            }
            quizRepository.save(quiz);
            logger.info("Quiz saved successfully with id: {}", quiz.getId());
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            logger.error("Failed to save quiz: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to save quiz due to an internal error. Please try again.");
            model.addAttribute("quiz", quiz); // Preserve form data
            model.addAttribute("isNewQuiz", true);
            return "admin/quiz-form";
        }
    }

    @GetMapping("/quiz/{id}/questions")
    public String manageQuestions(@PathVariable Long id, Model model) {
        logger.debug("Loading questions for quiz id: {} for ADMIN user", id);
        Quiz quiz = quizRepository.findById(id).orElse(null);
        if (quiz == null) {
            logger.warn("Quiz not found for id: {}", id);
            model.addAttribute("error", "The requested quiz could not be found.");
            return "redirect:/admin/dashboard";
        }
        try {
            model.addAttribute("quiz", quiz);
            List<Question> questions = questionRepository.findByQuizId(id);
            if (questions == null) {
                questions = new ArrayList<>();
                logger.warn("No questions found for quiz id: {}", id);
            }
            model.addAttribute("questions", questions);
            Question newQuestion = new Question();
            newQuestion.setQuizId(id);
            model.addAttribute("question", newQuestion);
            model.addAttribute("isNewQuiz", false); // Flag for question management
            return "admin/quiz-form";
        } catch (Exception e) {
            logger.error("Failed to load questions for quiz id {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Unable to load questions due to an internal error. Please try again.");
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/question/save")
    public String saveQuestion(@ModelAttribute Question question, Model model) {
        logger.debug("Received question for saving: {}", question);

        if (question.getQuizId() == null) {
            logger.error("quizId is null, aborting save");
            model.addAttribute("error", "Quiz ID is missing. Please try again.");
            return "redirect:/admin/dashboard";
        }

        // ✅ FIX: Check if quiz exists using quizId, not questionId
        if (!quizRepository.existsById(question.getQuizId())) {
            logger.error("Quiz with id {} does not exist", question.getQuizId());
            model.addAttribute("error", "The associated quiz could not be found.");
            return "redirect:/admin/dashboard";
        }

        try {
            questionRepository.save(question);
            logger.info("Question saved successfully with id: {}", question.getId());
            return "redirect:/admin/quiz/" + question.getQuizId() + "/questions";
        } catch (Exception e) {
            logger.error("Failed to save question: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to save question due to an internal error. Please try again.");

            Quiz quiz = quizRepository.findById(question.getQuizId()).orElse(null);
            if (quiz != null) {
                model.addAttribute("quiz", quiz);
                model.addAttribute("questions", questionRepository.findByQuizId(question.getQuizId()));
                model.addAttribute("question", question); // Preserve form data
                model.addAttribute("isNewQuiz", false);
                return "admin/quiz-form";
            }

            return "redirect:/admin/dashboard";
        }
    }

}