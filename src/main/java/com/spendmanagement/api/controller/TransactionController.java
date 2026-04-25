package com.spendmanagement.api.controller;

import com.spendmanagement.api.dto.TransactionRequest;
import com.spendmanagement.api.dto.TransactionResponse;
import com.spendmanagement.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse submit(@Valid @RequestBody TransactionRequest request) {
        return transactionService.submit(request);
    }

    @PutMapping("/{id}/reverse")
    public TransactionResponse reverse(@PathVariable UUID id) {
        return transactionService.reverse(id);
    }
}
