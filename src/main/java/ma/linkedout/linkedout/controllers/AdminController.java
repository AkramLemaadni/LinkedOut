package ma.linkedout.linkedout.controllers;

import lombok.RequiredArgsConstructor;
import ma.linkedout.linkedout.entities.User;
import ma.linkedout.linkedout.entities.JobOffer;
import ma.linkedout.linkedout.entities.JobOfferStatus;
import ma.linkedout.linkedout.services.UserService;
import ma.linkedout.linkedout.services.JobOfferService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final JobOfferService jobOfferService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin-dashboard";
    }

    @GetMapping("/users/{id}/change-password")
    public String showChangePasswordPage(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        return "admin-change-password";
    }

    @PostMapping("/users/{id}/change-password")
    public String changeUserPassword(@PathVariable Long id, @RequestParam String newPassword, Model model) {
        userService.changeUserPassword(id, newPassword);
        model.addAttribute("success", true);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/offers")
    public String showOffersModerationPage(Model model) {
        List<JobOffer> offers = jobOfferService.getAllJobOffers();
        model.addAttribute("offers", offers);
        return "admin-offers";
    }

    @PostMapping("/offers/{id}/accept")
    public String acceptOffer(@PathVariable Long id) {
        jobOfferService.updateJobOfferStatus(id, JobOfferStatus.ACCEPTED);
        return "redirect:/admin/offers";
    }

    @PostMapping("/offers/{id}/reject")
    public String rejectOffer(@PathVariable Long id) {
        jobOfferService.updateJobOfferStatus(id, JobOfferStatus.REJECTED);
        return "redirect:/admin/offers";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(
            @PathVariable Long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            Model model) {
        try {
            userService.updateUserInfo(id, firstName, lastName, email, phone);
            model.addAttribute("success", "Utilisateur mis à jour avec succès.");
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/{id}/toggle-activation")
    public String toggleUserActivation(@PathVariable Long id, Model model) {
        try {
            userService.toggleUserActivation(id);
            model.addAttribute("success", "Statut de l'utilisateur mis à jour.");
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la mise à jour du statut.");
        }
        return "redirect:/admin/dashboard";
    }
}
