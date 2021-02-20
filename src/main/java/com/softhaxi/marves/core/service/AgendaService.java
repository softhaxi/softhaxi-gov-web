package com.softhaxi.marves.core.service;

import java.util.*;

import com.softhaxi.marves.core.domain.employee.Invitation;

import org.springframework.stereotype.Service;

/**
 * AgendaService
 */
@Service
public class AgendaService {

    public void setMember(Collection<Invitation> invitations){
        if(invitations != null) {
            invitations.forEach((invitation) -> {
                Set<Map<String, Object>> members = new HashSet<>();
                invitation.getInvitees().forEach((member) -> {
                    if(!member.isDeleted()) {
                        Map<String, Object> temp = new HashMap<>();
                        temp.put("email", member.getUser().getEmail());
                        temp.put("id", member.getUser().getId());
                        temp.put("fullName", member.getUser().getProfile() != null ? member.getUser().getProfile().getFullName() : "");
                        temp.put("response", member.getResponse());
                        temp.put("organizer", member.getOrganizer());
                        members.add(temp);
                    }
                });
                invitation.setMembers(members);
            });
        }
    }
    
}