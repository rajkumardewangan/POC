package com.example.salon.controller;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.UserRole;
import com.example.salon.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private final AppUserService appUserService;

    public LoginController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") != null) {
            session.invalidate();
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String phone,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        AppUser user = appUserService.findByPhone(phone).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "No profile exists for that mobile number. Ask an admin to create one.");
            return "redirect:/login";
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute("currentUser", user);
        redirectAttributes.addFlashAttribute("message", "Welcome, " + user.getName() + "!");
        return user.getRole() == UserRole.ADMIN ? "redirect:/" : "redirect:/appointments/calendar";
    }
}
