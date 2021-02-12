package com.softhaxi.marves.core.controller.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.softhaxi.marves.core.domain.master.SystemParameter;
import com.softhaxi.marves.core.repository.master.SystemParameterRepository;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * SystemParameterController
 */
@Controller
public class SystemParameterController {

    private static final Logger logger = LoggerFactory.getLogger(SystemParameterController.class);

    @Autowired
    private SystemParameterRepository systemParameterRepository;

    @Value("${total.sysparam.perpage}")
    private int pageSize;

    @GetMapping("/sysparam")
    public String getAllSystemParameter(Model model, @RequestParam("paramCode") Optional<String> paramCode, @RequestParam("page") Optional<Integer> page){
        int currentPage = page.orElse(0);
        String strParamCode = paramCode.orElse("");
        Pageable paging = PageRequest.of(currentPage, pageSize, Sort.by("code"));
        
        Page<SystemParameter> pagedResult = new PageImpl<>(new ArrayList<>());
        List<SystemParameter> sysParamList = new ArrayList<>();

        try {

            if(!"".equals(strParamCode)){
                sysParamList = new ArrayList<>(systemParameterRepository.findSysParamByCode(
                    "%" + strParamCode.toLowerCase() + "%"));
            }else{
                sysParamList = systemParameterRepository.findAll();
            }

            if(!sysParamList.isEmpty()){
                int start = (int)paging.getOffset();
                int end = (start + paging.getPageSize()) > sysParamList.size() ? sysParamList.size() : (start + paging.getPageSize());
                pagedResult = new PageImpl<SystemParameter>(sysParamList.subList(start, end), paging, sysParamList.size());
            }
        
        } catch (Exception e) {
            logger.error("getAllSystemParameter - Error: " + e.getStackTrace());
        }
        
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startIndex", pageSize * currentPage);
        model.addAttribute("sysparamList", pagedResult);

        int totalPages = pagedResult.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "settings/sysparam/index";
    }
    

    @PostMapping("/sysparam/add")
    public String addNewSystemParameter(Model model, @ModelAttribute("sysParam") Optional<SystemParameter> systemParameter){
        SystemParameter sysParam = systemParameter.orElse(new SystemParameter());
        
        model.addAttribute("sysParam", sysParam);
        
        return "common/setting/sysparam-input";
    }

    @PostMapping("/sysparam/action")
    public String actionSystemParameter(Model model, @ModelAttribute("id") Optional<UUID> sysParamId, @RequestParam("btnAction") Optional<String> btnAction){
        
        Optional<SystemParameter> systemParameter = systemParameterRepository.findById(sysParamId.get());
        SystemParameter sysParam = systemParameter.orElse(new SystemParameter());

        model.addAttribute("sysParam", sysParam);
        if(btnAction.get().equals("delete")){
            return "common/setting/sysparam-delete-confirm";
        }
        return "common/setting/sysparam-input";
    }
    
    @PostMapping("/sysparam/confirm")
    public String confirmSaveSystemParameter(Model model, @ModelAttribute("sysParam") SystemParameter systemParameter, 
        @RequestParam("btnAction") Optional<String> btnAction, @RequestParam("errorMessage") Optional<String> errorMessage){
        
        if(btnAction.isPresent() && btnAction.get().equals("cancel")){
            return "redirect:/sysparam";
        }
        logger.debug("code: "+ systemParameter.getCode());
        logger.debug("isDeleted: "+ systemParameter.isDeleted());
        logger.debug("isEditable: "+ systemParameter.isEditable());
        logger.debug("isSystem: "+ systemParameter.isSystem());

        String failed = errorMessage.orElse("");
        if(!failed.equals("")){
            model.addAttribute("failed", failed);
        }
        model.addAttribute("sysParam", systemParameter);
        
        return "common/setting/sysparam-input-confirm";
    }

    @PostMapping("/sysparam/delete")
    public String deleteSystemParamter(Model model, @ModelAttribute ("sysParam")Optional<SystemParameter> systemParameter, 
    @RequestParam("btnAction") Optional<String> btnAction){
        String errorMessage = "";
        SystemParameter sysParam = systemParameter.orElse(new SystemParameter());

        if(btnAction.isPresent() && btnAction.get().equals("cancel")){
            return "redirect:/sysparam";
        }

        if(systemParameterRepository.existsById(sysParam.getId())){
            try{
                Optional<SystemParameter> optSysParamToUpdate = systemParameterRepository.findById(sysParam.getId());
                SystemParameter sysParamToDelete = optSysParamToUpdate.orElse(new SystemParameter());
                sysParamToDelete.setIsDeleted(true);
                systemParameterRepository.save(sysParamToDelete);
            }catch(Exception e){
                logger.error("deleteSystemParamter - Failed to delete: "+e.getStackTrace());
                errorMessage = e.getMessage();
            }
            
        }else{
            model.addAttribute("failed", "Failed to delete System Parameter: " + sysParam.getCode() + "(" + sysParam.getName() + "). Parameter Code not found.");
        }

        if(!errorMessage.isEmpty()){
            model.addAttribute("failed", "Failed to delete System Parameter: " + sysParam.getCode() + "(" + sysParam.getName() + "). Error: " + errorMessage);
        }else{
            model.addAttribute("success", "Successfully delete System Parameter: " + sysParam.getCode() + "(" + sysParam.getName() + ")");
        }

        model.addAttribute("sysParam", sysParam);
        
        return "common/setting/sysparam-delete-result";
    }

    @PostMapping("/sysparam/submit")
    public String submitNewSystemParameter(Model model, @ModelAttribute("sysParam") Optional<SystemParameter> systemParameter, 
        @RequestParam("btnAction") Optional<String> btnAction){
        
        SystemParameter sysParam = systemParameter.orElse(new SystemParameter());
        logger.debug("System Parameter: " + sysParam);
        String errorMessage = "";
        String successMessage = "";
        
        if(btnAction.isPresent()){
            if(btnAction.get().equals("cancel")){
                return "redirect:/sysparam";
            }
            if(btnAction.get().equals("back")){
                return addNewSystemParameter(model, systemParameter);
            }
        }
        logger.debug("UUID: " + sysParam.getId());
        try{
            if(null!=sysParam.getId()){
                Optional<SystemParameter> optSysParamToUpdate = systemParameterRepository.findById(sysParam.getId());
                SystemParameter sysParamToUpdate = optSysParamToUpdate.orElse(new SystemParameter());
                sysParamToUpdate.setCode(sysParam.getCode());
                sysParamToUpdate.setName(sysParam.getName());
                sysParamToUpdate.setAdditionalInfo(sysParam.getAdditionalInfo());
                sysParamToUpdate.setValue(sysParam.getValue());
                sysParamToUpdate.setRegex(sysParam.getRegex());
                sysParamToUpdate.setDecription(sysParam.getDecription());
                systemParameterRepository.save(sysParamToUpdate);
                successMessage = "System Parameter has been updated successfully";
            }
            else{
                systemParameterRepository.save(sysParam);
                successMessage = "New System Parameter has been added successfully updated";
            }

            model.addAttribute("sysParam", sysParam);
        }catch(Exception e){
            logger.error("submitNewSystemParameter - Error: ");
            e.printStackTrace();
            errorMessage = "Error: "+e.getMessage();
        }finally{
            if(!errorMessage.isEmpty()){
                model.addAttribute("failed", errorMessage);
                return "common/setting/sysparam-input";
            }else{
                model.addAttribute("success", successMessage);
            }
        }
        
        return "common/setting/sysparam-input-result";
    }

    @PostMapping("/sysparam/done")
    public String saveSystemParameter(Model model, @RequestParam("btnAction") Optional<String> btnAction){
        
        if(btnAction.isPresent() && btnAction.get().equals("done")){
            return "redirect:/sysparam";
        }
        
        return "common/setting/sysparam-input-result";
    }
}