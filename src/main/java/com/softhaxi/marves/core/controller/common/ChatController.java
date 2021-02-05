package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ChatController
 */
@Controller
public class ChatController {

    Logger logger = LoggerFactory.getLogger(ChatController.class);

    @GetMapping("/create-chat")
    public String createChat(Model model){

        return "common/create-chat.html";
    }
    
    @PostMapping("/download-chat")
    public String downloadChat(Model model, @RequestParam("name") String name, @RequestParam("fromdate") String fromDate, @RequestParam("todate") String toDate){
        try {
            LocalDate fromDate2 = LocalDate.parse(fromDate);
            LocalDate toDate2 = LocalDate.parse(toDate);
            if(fromDate2.isAfter(toDate2)){
                model.addAttribute("errorMessage", "invalidDateOrder");
            }
        }
        catch (Exception e) {
            if(e instanceof DateTimeParseException){
                model.addAttribute("errorMessage", "invalidDateParse");
            }
            e.printStackTrace();
        }
        
        return "common/create-chat.html";
    }
}