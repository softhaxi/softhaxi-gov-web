package com.softhaxi.marves.core.controller.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Map.entry;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.softhaxi.marves.core.domain.account.User;
import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.domain.messaging.Notification;
import com.softhaxi.marves.core.domain.messaging.NotificationStatus;
import com.softhaxi.marves.core.model.request.NotificationRequest;
import com.softhaxi.marves.core.repository.account.UserRepository;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationRepository;
import com.softhaxi.marves.core.repository.messaging.NotificationStatusRepository;
import com.softhaxi.marves.core.service.employee.EmployeeDivisionService;
import com.softhaxi.marves.core.service.message.MessageService;
import com.softhaxi.marves.core.util.PagingUtil;

import org.apache.groovy.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

/**
 * Notification
 */
@Controller
@RequestMapping("/notification")
public class NotificationController {

    Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private NotificationStatusRepository statusRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private SystemParameterRepository parameterRepo;

    @Autowired
    @Qualifier("oneSignalRestTemplate")
    private RestTemplate restTemplate;

    @Value("${onesignal.app.id}")
    private String appId;

    @Value("${onesignal.notification.endpoint}")
    private String notificationEndPoint;

    @Autowired
    private EmployeeDivisionService divisionService;

    @Autowired
    private MessageService messageService;

    private Model getTablePagination(Model model, int page, String month, String year) {
        LocalDate now = LocalDate.now();
        LocalDate from = now.with(TemporalAdjusters.firstDayOfMonth());
        if (year != null && month != null)
            from = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), 1);
        LocalDate to = null;
        if (now.getYear() == from.getYear() && now.getMonthValue() == from.getMonthValue()) {
            to = now.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        } else {
            to = from.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        }
        List<Notification> notifications = (List<Notification>) notificationRepo.findAllBySourceAndDateRange("WEB", 
            from.atStartOfDay(ZoneId.systemDefault()), to.atStartOfDay(ZoneId.systemDefault()));

        int pageSize = Integer.parseInt(
                parameterRepo.findByCode("PAGINATION_PAGE_SIZE").orElse(new SystemParameter().value("10")).getValue());
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Notification> pagination = new PageImpl<>(new LinkedList<>());
        if (null != notifications && notifications.size() > 0) {
            int start = (int) pageable.getOffset();
            int end = (start + pageable.getPageSize()) > notifications.size() ? notifications.size()
                    : (start + pageable.getPageSize());
            pagination = new PageImpl<Notification>((notifications).subList(start, end), pageable, notifications.size());
        }
        
        model.addAttribute("month", from.getMonthValue());
        model.addAttribute("year", from.getYear());
        model.addAttribute("dateDisplay",
                from.format(DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(new Locale("in", "ID"))));
        model.addAttribute("currentPage", page + 1);
        model.addAttribute("startIndex", pageSize * page);
        model.addAttribute("data", pagination);
        int[] pages = PagingUtil.generatePages(pagination.getTotalPages(), pagination.getNumber());
        // System.out.println(pages);

        model.addAttribute("pages", pages);

        return model;
    }

    @GetMapping()
    public String index(Model model, 
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "year", required = false) String year) {
        model = getTablePagination(model, page - 1, month, year);

        var divisions = divisionService.findAll();
        // logger.debug(divisions.toString());
        model.addAttribute("divisions", divisions);
        
        return "notification/index";
    }

    @PostMapping()
    public String post(Model model, @RequestBody NotificationRequest request) {
        // logger.debug(request.toString());
        Notification notification = new Notification()
            .assignee(request.getRecipientGroup().toUpperCase().trim())
            .category(request.getSubject().toUpperCase())
            .source("WEB")
            .deepLink("core://marves.dev/notification")
            .uri("/notification");
        notification.setDateTime(ZonedDateTime.now());
        notification.setContent(request.getMessage().trim());

        Collection<User> users = null;
        if(request.getRecipientGroup().equalsIgnoreCase("ALL")) {
            notification.setLevel("PUBLIC");
            users = userRepo.findAllActiveMobileUser();
        } else {
            notification.setLevel("SPECIFIC");
            String[] divisionArray = request.getDivision().split("\\|");
            // logger.debug("division..." + divisionArray.toString());
            notification.setAssigneeName(divisionArray[1]);
            Collection<Map<?, ?>> employees = divisionService.findEmployeeByDivision(divisionArray[0]);
            Collection<String> emails = new LinkedList<>();
            if (employees != null && !employees.isEmpty()) {
                for (Map<?, ?> employee : employees) {
                    emails.add((String) employee.get("email"));
                }
                users = userRepo.findAllByEmails(emails);
            }
        }

        Collection<String> oneSignalIds = new LinkedList<>();
        Collection<NotificationStatus> oneSignalStatuses = new LinkedList<>();
        Collection<NotificationStatus> statuses = new LinkedList<>();
        users.forEach((user) -> {
            if (user.getStatus().equalsIgnoreCase("active") && user.getOneSignalId() != null
                    && !user.getOneSignalId().isEmpty()) {
                oneSignalStatuses.add(new NotificationStatus(notification, user, false, false));
                oneSignalIds.add(user.getOneSignalId());
            } else {
                statuses.add(new NotificationStatus(notification, user, false, false));
            }
        });

        notificationRepo.save(notification);
        statusRepo.saveAll(statuses);

        if (oneSignalIds != null && !oneSignalIds.isEmpty()) {
            Map<String, Object> body = new HashMap<>(Map.ofEntries(
                    entry("headings", Map.of("en", notification.getCategoryDisplay())),
                    entry("contents",
                            Map.of("en", notification.getContent())),
                    entry("data",
                            Map.of("deepLink", notification.getDeepLink(), "view", "detail", "refId",
                                    notification.getId())),
                    entry("include_player_ids", oneSignalIds), entry("small_icon", "ic_stat_marves"),
                    entry("android_accent_color", "FF19A472"),
                    entry("android_channel_id", "066ee9a7-090b-4a42-b084-0dcbbeb7f158"),
                    entry("android_group", notification.getCategory().toLowerCase())));
            messageService.sendPushNotification(notification, oneSignalStatuses, body);
        }

        // Map<String, Object> map = new HashMap<>();
        // map.put("id", notification.getId());
        // map.put("status", "submitted");

        model = getTablePagination(model, 0, String.valueOf(LocalDate.now().getMonthValue()), 
            String.valueOf(LocalDate.now().getYear()));

        return "notification/table";
    }

    @PostMapping("/save-notification")
    public String saveNotification(Model model, @ModelAttribute("notification") Notification notification) {
        String subscriber = "";
        logger.debug("Assignee: " + notification.getAssignee());
        if (null == notification.getAssignee() || notification.getAssignee().equals("")) {
            subscriber = "Maritim Users";
        } else {
            Optional<User> user = userRepo.findUserByEmail(notification.getAssignee());
            if (user.isPresent()) {
                notification.setUser(user.get());
                notification.setDateTime(ZonedDateTime.now(ZoneId.systemDefault()));
                notificationRepo.save(notification);
            } else {
                model.addAttribute("errorMessage",
                        "Email " + notification.getAssignee() + " is not attached to any user");
            }
        }

        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Type", "application/json; charset=utf-8");

        Map<?, ?> body = new HashMap<>();
        if (!subscriber.equals("")) {
            body = Maps.of("app_id", appId, "included_segments", Arrays.asList(subscriber), "headings",
                    ImmutableMap.of("en", notification.getCategory()), "contents",
                    ImmutableMap.of("en", notification.getContent()));
        } else {
            body = Maps.of("app_id", appId, "include_player_ids",
                    Arrays.asList(notification.getUser().getOneSignalId()), "headings",
                    ImmutableMap.of("en", notification.getCategory()), "contents",
                    ImmutableMap.of("en", notification.getContent()));
        }

        HttpEntity<Map<?, ?>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<?> response = restTemplate.postForEntity(notificationEndPoint, entity, Map.class);

        logger.info("[sendNotification] Result....{}", response.getBody());

        List<Notification> notifications = notificationRepo.findAll();

        model.addAttribute("notifications", notifications);
        return "common/notification-list.html";
    }

    @GetMapping("/find-user-email")
    public @ResponseBody String findUserByName(Model model, @RequestParam("email") Optional<String> email) {
        String strEmail = email.orElse("");

        List<User> users = userRepo.findUserByUsernameLike(strEmail);

        List<Map<String, String>> userList = new ArrayList<>();
        users = users.stream().limit(10).collect(Collectors.toList());
        String json = "";

        try {
            Map<String, String> userMap = new HashMap<>();
            for (User user : users) {
                userMap = new HashMap<>();
                userMap.put("email", user.getEmail());
                userList.add(userMap);
            }

            Gson gson = new Gson();
            json = gson.toJson(userList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }
}