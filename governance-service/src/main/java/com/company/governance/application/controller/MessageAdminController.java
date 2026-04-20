package com.company.governance.application.controller;

import com.company.governance.application.controller.vo.MessageRetryResultVO;
import com.company.governance.application.controller.vo.MessageStatsVO;
import com.company.governance.application.service.MessageAdminApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance/admin/messages")
public class MessageAdminController {

    private final MessageAdminApplicationService messageAdminApplicationService;

    public MessageAdminController(MessageAdminApplicationService messageAdminApplicationService) {
        this.messageAdminApplicationService = messageAdminApplicationService;
    }

    @PostMapping("/retry")
    public MessageRetryResultVO retryFailedMessages() {
        return messageAdminApplicationService.retryFailedMessages();
    }

    @GetMapping("/stats")
    public MessageStatsVO getMessageStats() {
        return messageAdminApplicationService.getMessageStats();
    }
}
