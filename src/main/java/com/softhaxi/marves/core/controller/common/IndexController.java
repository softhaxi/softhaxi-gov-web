package com.softhaxi.marves.core.controller.common;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Main controller
 * 
 * https://www.appsdeveloperblog.com/reading-application-properties-spring-boot/
 * 
 * @author Raja Sihombing
 * @since 1
 */
@Controller
public class IndexController {

	@Value("${coming.soon.flag}")
	private String comingSoonFlag;

	@Value("${coming.soon.end}")
	private String comingSoonDate;

	@Autowired
	private UserService userService;

	@GetMapping("/")
	public String index(Model model) {
		if (comingSoonFlag.equals("Y")) {
			model.addAttribute("date", comingSoonDate);
			return "common/coming";
		}
		return "common/index";
	}

	@PostMapping("/login")
	public String login(Model model, @ModelAttribute("user") User user) {
		try {
			User loginUser = userService.findUserByUsernameAndPassword(user.getUsername(), user.getPassword());
			model.addAttribute("user", loginUser);
			return "common/main";
		} catch (UsernameNotFoundException unfe) {
			System.out.println(unfe.getMessage());
			model.addAttribute("errorMessage", "Invalid Username/Password");
			return "common/index";
		}

	}

	@PostMapping("/logout")
	public String logout(Model model) {
		return "common/index";
	}
}
