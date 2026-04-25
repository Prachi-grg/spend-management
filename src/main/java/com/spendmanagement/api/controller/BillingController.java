package com.spendmanagement.api.controller;

import com.spendmanagement.api.dto.BillingRecordResponse;
import com.spendmanagement.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/{teamId}/billing")
    public BillingRecordResponse getBilling(
            @PathVariable UUID teamId,
            @RequestParam String period
    ) {
        return billingService.getBillingRecord(teamId, period);
    }
}
