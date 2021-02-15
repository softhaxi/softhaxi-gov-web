package com.softhaxi.marves.core.restful.employee;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.service.employee.EmployeeInfoService;
import com.softhaxi.marves.core.service.employee.EmployeeVitaeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
@SuppressWarnings("unchecked")
public class EmployeeRestful {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeRestful.class);

    @Autowired
    private EmployeeVitaeService employeeVitaeService;

    @Autowired
    private EmployeeInfoService employeeInfoService;

    @Autowired
    private UserRepository userRepo;

    @GetMapping()
    public ResponseEntity<?> index(
        @RequestParam(value="page", defaultValue = "1", required = false) int page,
        @RequestParam(value="q", required=false) String q) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        List<Map<?, ?>> data = (List<Map<?, ?>>) employeeInfoService.getEmployeeList();
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }
        int index = IntStream.range(0, data.size())
            .filter(item -> {
                String email = (String) data.get(item).get("email");
                if(email == null) 
                    return false;
                return email.equalsIgnoreCase(user.getEmail());
            })
            .findFirst().orElse(-1);
        data.remove(index);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(value="q", required = true) String keyword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        List<Map<?, ?>> data = (List<Map<?, ?>>) employeeInfoService.findEmployeeList(keyword);
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }
        // int index = IntStream.range(0, data.size())
        //     .filter(item -> {
        //         String email = (String) data.get(item).get("email");
        //         if(email == null) 
        //             return false;
        //         return email.equalsIgnoreCase(user.getEmail());
        //     })
        //     .findFirst().orElse(-1);
        // data.remove(index);

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/personalInfo")
    public ResponseEntity<?> personalInfo(@RequestParam(required = false) String payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getPersonalInfo(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/education")
    public ResponseEntity<?> education() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getEducations(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/job")
    public ResponseEntity<?> job() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getJobs(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/reward")
    public ResponseEntity<?> reward() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getRewards(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/performance")
    public ResponseEntity<?> performance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getPerformances(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/oversea")
    public ResponseEntity<?> oversea() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getOverseas(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/family")
    public ResponseEntity<?> family() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getFamilies(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/organization")
    public ResponseEntity<?> organization() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getOrganizations(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }

    @GetMapping("/otherInfo")
    public ResponseEntity<?> otherInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) employeeVitaeService.getOtherInfos(user.getEmail().toLowerCase().trim());
        if(data == null) {
            return new ResponseEntity<>(
                new ErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "invalid data response"
                ),
                HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                data
            ),
            HttpStatus.OK   
        );
    }
}
