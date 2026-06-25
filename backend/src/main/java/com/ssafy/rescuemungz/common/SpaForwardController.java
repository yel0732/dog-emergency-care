package com.ssafy.rescuemungz.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping(value = {"/", "/login", "/users", "/favorites", "/follow", "/followers", "/following", "/pets", "/records", "/videos", "/emergency", "/cases", "/cases/{id}", "/reports", "/hospitals", "/food-safety"})
    public String forward() {
        return "forward:/index.html";
    }
}
