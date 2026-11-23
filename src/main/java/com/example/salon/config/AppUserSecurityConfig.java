package com.example.salon.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.salon.entity.AppUser;
import com.example.salon.entity.UserRole;

@Configuration
public class AppUserSecurityConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AppUserAccessInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/users/login",
                        "/users/logout",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/h2-console/**",
                        "/error",
                        "/webhooks/whatsapp",
                        "/favicon.ico"
                );
    }

    private static class AppUserAccessInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String uri = request.getRequestURI();
            if (uri == null || uri.isBlank()) {
                return true;
            }

            if (uri.equals(request.getContextPath() + "/users/logout")) {
                return true;
            }

            HttpSession session = request.getSession(false);
            AppUser currentUser = session != null ? (AppUser) session.getAttribute("currentUser") : null;

            if (currentUser == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }

            if (currentUser.getRole() == UserRole.ADMIN) {
                return true;
            }

            if (uri.equals(request.getContextPath() + "/")
                    || uri.startsWith(request.getContextPath() + "/customers")
                    || uri.startsWith(request.getContextPath() + "/barbers")
                    || uri.startsWith(request.getContextPath() + "/services")
                    || uri.startsWith(request.getContextPath() + "/attendances")
                    || uri.startsWith(request.getContextPath() + "/reports")
                    || uri.startsWith(request.getContextPath() + "/users")
                    || uri.startsWith(request.getContextPath() + "/h2-console")) {
                response.sendRedirect(request.getContextPath() + "/appointments/calendar");
                return false;
            }

            if (uri.startsWith(request.getContextPath() + "/appointments")
                    || uri.startsWith(request.getContextPath() + "/appointments/lookup-customer")) {
                return true;
            }

            response.sendRedirect(request.getContextPath() + "/appointments/calendar");
            return false;
        }
    }
}
