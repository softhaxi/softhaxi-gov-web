package com.softhaxi.marves.core.controller.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.employee.Employee;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.employee.EmployeeRepository;
import com.softhaxi.marves.core.service.account.UserService;

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
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Raja Sihombing
 * @since 1
 */
@Controller
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    protected EmployeeRepository employeeRepository;

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserRepository userRepository;

    @Value("${total.activity.perpage}")
    private int pageSize;

    @GetMapping("/employment")
    public String employment(Model model,@RequestParam("page") Optional<Integer> page, @RequestParam("search") Optional<String> username) {
        int currentPage = page.orElse(0);
        String strUserName = username.orElse("");
        List<Employee> employeeList = new ArrayList<>();
        List<User> userList = new ArrayList<>();

        Pageable paging = PageRequest.of(currentPage, pageSize, Sort.by("employeeNo"));
        Page<Employee> pagedResult =  new PageImpl<>(new ArrayList<>());

        if ("".equals(strUserName)) {
            userList = new ArrayList<>(userRepository.findAllNonAdminUsers());
        } else {
            Optional<User> optUser = userRepository.findByUsername(strUserName.toUpperCase());
            if (optUser.isPresent()) {
                userList.add(optUser.get());
            }
        }
        if (!userList.isEmpty()) {
            for (User user : userList) {
                Optional<Employee> employee = employeeRepository.findEmployeeByUserName(user);
                if(employee.isPresent()){
                    employeeList.add(employee.get());
                }
            }
            if(!employeeList.isEmpty()){
                int start = (int)paging.getOffset();
                int end = (start + paging.getPageSize()) > employeeList.size() ? employeeList.size() : (start + paging.getPageSize());
                pagedResult = new PageImpl<Employee>(employeeList.subList(start, end), paging, employeeList.size());
            }
        }
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startIndex", pageSize * currentPage);
        model.addAttribute("employees", pagedResult);

        int totalPages = pagedResult.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        return "common/employee";
    }
}
