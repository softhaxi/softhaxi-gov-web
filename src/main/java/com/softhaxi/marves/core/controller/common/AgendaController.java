package com.softhaxi.marves.core.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * AgendaController
 */
@Controller
public class AgendaController {

    @GetMapping("/agenda")
    public String getAgenda(Model model){

        return "common/agenda/agenda-bak";
    }
}