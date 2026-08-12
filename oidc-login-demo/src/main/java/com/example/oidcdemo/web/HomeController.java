package com.example.oidcdemo.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public HomeController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user,
                        @RequestParam(name = "error", required = false) String error,
                        Model model) {
        if (user != null) {
            model.addAttribute("user", user);
            return "profile";
        }

        List<ClientRegistration> registrations = new ArrayList<>();
        if (clientRegistrationRepository instanceof InMemoryClientRegistrationRepository repo) {
            repo.forEach(registrations::add);
        }
        model.addAttribute("registrations", registrations);
        model.addAttribute("loginError", error != null);
        return "index";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute("user", user);
        return "profile";
    }
}
