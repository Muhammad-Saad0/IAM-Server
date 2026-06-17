package com.example.iam.auth.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginPageController {
    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("loginError", error != null);
        model.addAttribute("logoutSuccess", logout != null);
        addCsrfAttributes(request, model);

        return "login";
    }

    private void addCsrfAttributes(HttpServletRequest request, Model model) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            return;
        }

        model.addAttribute("csrfParameterName", csrfToken.getParameterName());
        model.addAttribute("csrfToken", csrfToken.getToken());
    }
}
