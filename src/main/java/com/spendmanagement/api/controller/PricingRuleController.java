package com.spendmanagement.api.controller;

import com.spendmanagement.api.dto.PricingRuleRequest;
import com.spendmanagement.api.dto.PricingRuleResponse;
import com.spendmanagement.service.PricingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PricingRuleResponse createRule(@Valid @RequestBody PricingRuleRequest request) {
        return PricingRuleResponse.from(pricingRuleService.createRule(request));
    }

    @GetMapping
    public List<PricingRuleResponse> listActiveRules() {
        return pricingRuleService.getActiveRules().stream()
                .map(PricingRuleResponse::from)
                .toList();
    }
}
