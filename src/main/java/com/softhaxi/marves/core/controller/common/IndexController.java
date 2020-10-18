package com.softhaxi.marves.core.controller.common;

import org.springframework.beans.factory.annotation.Value;
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
	public String index(Model model) {
		if(comingSoonFlag.equals("Y")) {
			model.addAttribute("date", comingSoonDate);
			return "common/coming";
		}
		return "common/index";
	}
}
