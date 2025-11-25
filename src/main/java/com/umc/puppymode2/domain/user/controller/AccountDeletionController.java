package com.umc.puppymode2.domain.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountDeletionController {

    @GetMapping("/account-deletion")
    public String accountDeletionPage() {
        return "account-deletion";
    }
}