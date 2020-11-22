package com.softhaxi.marves.core.restful.employement;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.model.response.ErrorResponse;
import com.softhaxi.marves.core.model.response.GeneralResponse;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.service.employee.MarvesHRService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeRestful {

    @Value("${marves.hr.url}")
    private String marvesHrUrl;

    @Autowired
    private MarvesHRService marvesHrService;

    @Autowired
    private UserRepository userRepo;

    private List<?> needUpdateUrlKey = List.of("FOTO", "FOTO_THUMB", "KARPEG_FILE",
        "KTP_FILE", "TASPEN_FILE"
    );

    @GetMapping("/drh")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getVitae() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findById(UUID.fromString(auth.getPrincipal().toString())).orElse(null);

        Map<?, ?> data = (Map<?, ?>) marvesHrService.getPersonalInfo(user.getEmail().toLowerCase().trim());
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
        Map<String, Object> result = (Map<String, Object>) ((List<?>) data.get("result")).get(0);
        needUpdateUrlKey.forEach(item -> updateUrl(result, item.toString()));
        return new ResponseEntity<>(
            new GeneralResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                result
            ),
            HttpStatus.OK   
        );
    }

    private void updateUrl(Map<String, Object> map, String key) {
        if(map.containsKey(key)) {
            map.put(key, String.format("%s%s", marvesHrUrl, map.get(key)));
        }
    }
}
