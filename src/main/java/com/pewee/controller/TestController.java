package com.pewee.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pewee.service.schedule.ScheduleService;
@RestController
@RequestMapping("/test")
public class TestController {
	
	@Autowired
    private ScheduleService scheduleService;
    
    
    @PostMapping("/schedule")
    public String schedule() {
    	scheduleService.settingStatusAndDeleteDelayLogicFileForSplit();
    	return "OK";
    }
	
}
