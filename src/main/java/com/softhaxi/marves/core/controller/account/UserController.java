package com.softhaxi.marves.core.controller.account;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.softhaxi.marves.core.domain.access.Role;
import com.softhaxi.marves.core.domain.access.UserRole;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.model.request.UserRequest;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.access.RoleRepository;
import com.softhaxi.marves.core.repository.access.UserRoleRepository;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.util.PagingUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private UserRoleRepository userRoleRepo;

    @Autowired
    private SystemParameterRepository parameterRepo;

    private Model getTablePagination(Model model, int page, String name) {
        List<User> users = (List<User>) userRepo.findAllNonSuperAdminUsers();
        // System.out.println(users);
        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<User> pagination = new PageImpl<>(new LinkedList<>());
        if (null != users && users.size() > 0) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > users.size() ? users.size() : (start + pageable.getPageSize());
            pagination = new PageImpl<User>((users).subList(start, end), pageable, users.size());
        }

        model.addAttribute("currentPage", page);
        model.addAttribute("startIndex", pageSize * (page - 1));
        model.addAttribute("data", pagination);
        // System.out.println(pagination.getContent());
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());
        // System.out.println(pages);

        model.addAttribute("pages", pages);

        return model;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(Model model, @RequestParam(name = "name", required = false) String name) {
        if(name == null)
            name = "";
        
        List<User> users = userRepo.findUserByUsernameLike(name.toUpperCase());
        List<Map<?, ?>> data = new LinkedList<>();
        
        try {
            Map<String, String> userMap = new HashMap<>();
            for (User user : users) {
                userMap = new HashMap<>();
                userMap.put("value", user.getId().toString());
                userMap.put("label", user.getProfile().getFullName());
                data.add(userMap);
            }
            return new ResponseEntity<>(
                new GeneralResponse(
                    HttpStatus.OK.value(),
                    HttpStatus.OK.getReasonPhrase(),
                    data
                ),
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    e.getMessage()
                ),
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PreAuthorize("hasAuthority('SADMIN')")
    @GetMapping()
    public String index(Model model, @RequestParam(name = "page", required = false, defaultValue = "1") int page) {
        model = getTablePagination(model, page, null);
        return "account/user/index";
    }

    private Model getRoles(Model model, User user) {
        Collection<UserRole> assignedRoles = userRoleRepo.findByUser(user);

        List<UUID> ids = new LinkedList<>();
        boolean asAdmin = false;
        boolean asOperator = false;
        for (UserRole row : assignedRoles) {
            if (row.getRole().getName().equalsIgnoreCase("ADMIN")) 
                asAdmin = true;
            if(row.getRole().getName().equalsIgnoreCase("OPERATOR"))
                asOperator = true;
            ids.add(row.getRole().getId());
        }
        
        List<Role> roles = (List<Role>) roleRepo.findAllNonSuperAdminExcept(ids);

        if(asAdmin)
            roles.removeIf(role -> role.getName().equalsIgnoreCase("OPERATOR"));
        if(asOperator)
            roles.removeIf(role -> role.getName().equalsIgnoreCase("ADMIN"));

        model.addAttribute("assignedRoles", assignedRoles);
        model.addAttribute("roles", roles);

        return model;
    }

    @PreAuthorize("hasAuthority('SADMIN')")
    @GetMapping("/action")
    public String action(Model model, @RequestParam(name = "id") String id,
            @RequestParam(name = "action", required = false, defaultValue = "detail") String action) {

        User user = userRepo.findById(UUID.fromString(id)).orElseThrow();
        model.addAttribute("data", user);
        model = getRoles(model, user);

        return "account/user/detail";
    }

    @PreAuthorize("hasAuthority('SADMIN')")
    @PostMapping("/role")
    public String role(Model model, @RequestBody UserRequest request) {

        User user = userRepo.findById(UUID.fromString(request.getId())).orElseThrow();
        if(request.getAction().equals("add")) {
            Role newRole = roleRepo.findById(UUID.fromString(request.getRoleId())).orElseThrow();

            List<UUID> ids = new LinkedList<>();
            boolean exist = false;
            for (UserRole userRole : user.getRoles()) {
                if (newRole.equals(userRole.getRole())) {
                    model.addAttribute("error", "Level akses sudah ada");
                    exist = true;
                }
                ids.add(userRole.getRole().getId());
            }

            if (!exist) {
                UserRole userRole = new UserRole(user, newRole, true);
                user.getRoles().add(userRole);
                userRoleRepo.save(userRole);

                model.addAttribute("message", "Level akses berhasil ditambahkan");
            }
        } else if(request.getAction().equals("delete")) {
            System.out.println(request.toString());
            UserRole role = userRoleRepo.findById(UUID.fromString(request.getRoleId())).orElseThrow();
            userRoleRepo.delete(role);
        }

        model = getRoles(model, user);

        return "account/user/role";
    }

}
