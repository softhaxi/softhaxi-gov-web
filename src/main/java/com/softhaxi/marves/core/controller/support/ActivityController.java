package com.softhaxi.marves.core.controller.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.logging.ActivityLog;
import com.softhaxi.marves.core.repository.logging.ActivityLogRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.service.account.UserService;
import com.softhaxi.marves.core.util.PagingUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/activity")
public class ActivityController {

    Logger logger = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private SystemParameterRepository parameterRepo;

    @Autowired
    protected UserService userService;

    @Value("${total.activity.perpage}")
    private int pageSize;

    @GetMapping
    public String index(Model model, @RequestParam("page") Optional<Integer> page, @RequestParam("search") Optional<String> username, @RequestParam("action") Optional<String> action) {
        int currentPage = page.orElse(0);
        String strUserName = username.orElse("");
        String strAction = action.orElse("");
        Pageable paging = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "actionTime"));
        
        Page<ActivityLog> pagedResult = new PageImpl<>(new ArrayList<>());
        List<ActivityLog> activityLogs = new ArrayList<>();
        
        if(!"".equals(strUserName)){
            Optional<User> user = userService.findByUsername(strUserName);
            model.addAttribute("search", strUserName);
            if(!user.isEmpty()){
                activityLogs = activityLogRepository.findActivityLogByUserName(user.get(), strAction);
            }
        }else{
                activityLogs = activityLogRepository.findActivityLogByAction(strAction);
        }

        if(null!=activityLogs && activityLogs.size()>0){
            int start = (int)paging.getOffset();
            int end = (start + paging.getPageSize()) > activityLogs.size() ? activityLogs.size() : (start + paging.getPageSize());
            pagedResult = new PageImpl<ActivityLog>(activityLogs.subList(start, end), paging, activityLogs.size());
        }
    
        model.addAttribute("action", strAction);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startIndex", pageSize * currentPage);
        model.addAttribute("activities", pagedResult);

        int[] pages = PagingUtil.generatePages(pagedResult.getTotalPages(), pagedResult.getNumber());

        model.addAttribute("pages", pages);

        return "activity/index";
    }

    public String detail() {

        return "activity/detail";
    }
}
