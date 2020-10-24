package com.softhaxi.marves.core.controller.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

	@GetMapping("/")
	public String index(Model model, String error, String logout) {
		if (comingSoonFlag.equals("Y")) {
			model.addAttribute("date", comingSoonDate);
			return "common/coming";
		}
		if (error != null)
            model.addAttribute("errorMsg", "error.invalid.credential");

        if (logout != null)
            model.addAttribute("msg", "info.logout.success");

		return "common/index";
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		model.addAttribute("username", auth.getName());
		return "common/main";
	}

	/*@PostMapping("/login")
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
	}*/
}
