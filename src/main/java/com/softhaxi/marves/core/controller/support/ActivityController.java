package com.softhaxi.marves.core.controller.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;
import com.softhaxi.marves.core.service.account.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ActivityController {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    protected UserService userService;

    @Value("${total.activity.perpage}")
    private int pageSize;

    @GetMapping("/employment/activity")
    public String getActivity(Model model, @RequestParam("page") Optional<Integer> page, @RequestParam("search") Optional<String> username) {
        int currentPage = page.orElse(0);
        String strUserName = username.orElse("");
        Pageable paging = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "actionTime"));
        
        Page<ActivityLog> pagedResult = new PageImpl<>(new ArrayList<>());
        
        if(!"".equals(strUserName)){
            Optional<User> user = userService.findByUsername(strUserName);
            model.addAttribute("search", strUserName);
            if(!user.isEmpty()){
                List<ActivityLog> activityLogs = activityLogRepository.findActivityLogByUserName(user.get());
                int start = (int)paging.getOffset();
                int end = (start + paging.getPageSize()) > activityLogs.size() ? activityLogs.size() : (start + paging.getPageSize());
                pagedResult = new PageImpl<ActivityLog>(activityLogs.subList(start, end), paging, activityLogs.size());
            }
        }else{
            pagedResult=activityLogRepository.findAll(paging);
        }
        
        
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startIndex", pageSize * currentPage);
        model.addAttribute("activities", pagedResult);

        int totalPages = pagedResult.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);

        }

        return "common/activity";
    }

    public String detail() {

        return "activity/detail";
    }
}
