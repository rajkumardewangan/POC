package com.example.salon.controller;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.Barber;
import com.example.salon.entity.UserRole;
import com.example.salon.service.AppUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser(HttpSession session) {
        return (AppUser) session.getAttribute("currentUser");
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/manage")
    public String manageUsers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/appointments/calendar";
        }

        model.addAttribute("users", appUserService.findAll());
        return "users/manage";
    }

    @GetMapping("/manage/new")
    public String createUserForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/appointments/calendar";
        }
        model.addAttribute("user", new AppUser());
        model.addAttribute("staffProfile", new Barber());
        model.addAttribute("userRoles", UserRole.values());
        return "users/create";
    }

    @PostMapping("/manage")
    public String createUser(@RequestParam String name,
                             @RequestParam String phone,
                             @RequestParam UserRole role,
                             @ModelAttribute("staffProfile") Barber staffProfile,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/appointments/calendar";
        }

        try {
            AppUser user = appUserService.createUser(name, phone, role, staffProfile);
            redirectAttributes.addFlashAttribute("message", "Profile created for " + user.getName() + ".");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/users/manage";
    }

    @GetMapping("/manage/{id}/edit")
    public String editUserForm(@PathVariable Long id, HttpSession session, Model model,
                               RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/appointments/calendar";
        }
        try {
            AppUser user = appUserService.findById(id);
            model.addAttribute("user", user);
            model.addAttribute("staffProfile", user.getBarber() == null ? new Barber() : user.getBarber());
            model.addAttribute("userRoles", UserRole.values());
            return "users/edit";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/users/manage";
        }
    }

    @PostMapping("/manage/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam String phone,
                             @RequestParam UserRole role,
                             @ModelAttribute("staffProfile") Barber staffProfile,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/appointments/calendar";
        }

        try {
            AppUser updatedUser = appUserService.updateUser(id, name, phone, role, staffProfile);
            if (currentUser.getId().equals(updatedUser.getId())) {
                session.setAttribute("currentUser", updatedUser);
            }
            redirectAttributes.addFlashAttribute("message", "Profile updated for " + updatedUser.getName() + ".");
            return currentUser.getId().equals(updatedUser.getId()) && updatedUser.getRole() == UserRole.REGULAR
                    ? "redirect:/appointments/calendar"
                    : "redirect:/users/manage";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/users/manage/" + id + "/edit";
        }
    }

    @PostMapping("/manage/{id}/delete")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/appointments/calendar";
        }
        if (currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete your own profile while signed in.");
            return "redirect:/users/manage";
        }

        try {
            appUserService.deleteUser(id);
            redirectAttributes.addFlashAttribute("message", "Profile deleted.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/users/manage";
    }

    private boolean isAdmin(HttpSession session) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }
}
