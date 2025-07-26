package com.example.ShopifyLearn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/main")
public class MainController {

    @GetMapping("/homePage")
    public String getHomePage(
            @RequestParam String shopName,
            @RequestParam String domain,
            Model model
    ) {
        model.addAttribute("shopName", shopName);
        model.addAttribute("domain", domain);
            return "HomePage";
    }

    @GetMapping("/error")
    public String getErrorPage() {
        return "ErrorPage";
    }

}
