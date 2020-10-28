package com.softhaxi.marves.core.controller.common;

import java.util.List;

import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;
import com.softhaxi.marves.core.service.employee.AbsenceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

	@Value("${coming.soon.flag}")
	private String comingSoonFlag;

	@Value("${coming.soon.end}")
	private String comingSoonDate;

	@Autowired
	private ActivityLogRepository activityLogRepo;

	@Autowired
	private AbsenceService absenceService;

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
		model.addAttribute("latestUpdated", 
			activityLogRepo.findAll(PageRequest.of(0, 5, 
				Sort.by(Sort.Direction.DESC, "actionTime"))));
		List<?> weekly = absenceService.getDailyAbsenceCountWeekly();
		model.addAttribute("weeklyDate", weekly.get(0));
		model.addAttribute("weeklyWFO", weekly.get(1));
		model.addAttribute("weeklyWFH", weekly.get(2));
		return "common/dashboard";
	}
}
