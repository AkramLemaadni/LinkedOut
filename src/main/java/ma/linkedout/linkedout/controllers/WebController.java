package ma.linkedout.linkedout.controllers;

import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.UserRole;
import ma.linkedout.linkedout.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class WebController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
    private final UserService userService;

    @GetMapping
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            logger.info("Authenticated user {} accessing login page, redirecting to dashboard", auth.getName());
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }
        try {
            User user = userService.getCurrentUser();
            model.addAttribute("user", user);
            if (user.getRole() == UserRole.ADMIN) {
                return "redirect:/admin/dashboard";
            } else if (user.getRole() == UserRole.RECRUITER) {
                // model.addAttribute("dashboardType", "recruiter");
            } else if (user.getRole() == UserRole.CANDIDATE) {
                // model.addAttribute("dashboardType", "candidate");
            }
        } catch (Exception e) {
            model.addAttribute("username", auth.getName());
        }
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            logger.info("Unauthenticated user accessing profile, redirecting to login");
            return "redirect:/login";
        }
        
        return "profile";
    }

    @GetMapping("/job/{id}")
    public String jobDetails(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Add job ID to model for client-side API call
        model.addAttribute("jobId", id);
        
        // Add user authentication status
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
        model.addAttribute("isAuthenticated", isAuthenticated);
        
        if (isAuthenticated) {
            try {
                User user = userService.getCurrentUser();
                model.addAttribute("userRole", user.getRole());
                model.addAttribute("userId", user.getId());
            } catch (Exception e) {
                logger.error("Error retrieving user details for job details page: {}", e.getMessage());
            }
        }
        
        return "job-details";
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @GetMapping("/create-job-offer")
    public String createJobOfferPage() {
        return "create-job-offer";
    }

    @GetMapping("/job-offers")
    public String showJobOffersPage() {
        logger.info("Accessing job offers page");
        return "job-offers";
    }

    @GetMapping("/recruiter/applications-list")
    public String showApplicationsListPage(Long jobOfferId, Model model) {
        model.addAttribute("jobOfferId", jobOfferId);
        return "applications-list";
    }
} 