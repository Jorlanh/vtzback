package com.votzz.backend.controller;

import com.votzz.backend.dto.CreateOrderRequest;
import com.votzz.backend.dto.OrderDTO;
import com.votzz.backend.dto.SignOrderRequest;
import com.votzz.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> listOrders() {
        return ResponseEntity.ok(orderService.listMyOrders());
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    // --- ENDPOINT DE EDIÇÃO (CONFIRMADO) ---
    // Recebe o ID na URL e o Objeto com os dados atualizados no corpo
    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable UUID id, @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PutMapping("/{id}/sign")
    public ResponseEntity<OrderDTO> signOrder(@PathVariable UUID id, @RequestBody SignOrderRequest request) {
        return ResponseEntity.ok(orderService.signOrder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint para notificação via backend (Push/Email seguro)
    @PostMapping("/{id}/notify-email")
    public ResponseEntity<Void> sendNotificationEmail(@PathVariable UUID id) {
        orderService.sendNotificationEmail(id);
        return ResponseEntity.ok().build();
    }
}