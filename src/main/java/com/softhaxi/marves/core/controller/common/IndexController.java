package com.softhaxi.marves.core.controller.common;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main controller
 * 
 * https://www.appsdeveloperblog.com/reading-application-properties-spring-boot/
 * https://jsfiddle.net/kingBethal/2apo3e6x/9/
 * 
 * @author Raja Sihombing
 * @since 1
 */
@Controller
public class IndexController {

	//private final static Logger logger = LoggerFactory.getLogger(IndexController.class);

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

		LocalDateTime time = LocalDateTime.now();
		System.out.println(time.getHour());

		if(time.getHour() >= 18 || time.getHour() <= 5)
		model.addAttribute("bgImage", "darknight");
		else
		model.addAttribute("bgImage", "daylight");

		return "auth/login";
	}
}
