package com.votzz.backend.service;

import com.votzz.backend.core.security.SecurityUtils;
import com.votzz.backend.domain.Order;
import com.votzz.backend.domain.Tenant;
import com.votzz.backend.domain.User;
import com.votzz.backend.domain.enums.OrderStatus;
import com.votzz.backend.dto.CreateOrderRequest;
import com.votzz.backend.dto.OrderDTO;
import com.votzz.backend.dto.SignOrderRequest;
import com.votzz.backend.repository.OrderRepository;
import com.votzz.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService; 
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<OrderDTO> listMyOrders() {
        User currentUser = SecurityUtils.getCurrentUser();
        
        if (currentUser.getTenant() == null) {
             throw new RuntimeException("Usuário não vinculado a um condomínio.");
        }
        
        UUID tenantId = currentUser.getTenant().getId();

        if (isStaff(currentUser)) {
            return orderRepository.findByTenantIdOrderByArrivalDateDesc(tenantId)
                    .stream().map(OrderDTO::new).collect(Collectors.toList());
        } else {
            return orderRepository.findByTenantIdAndResidentIdOrderByArrivalDateDesc(tenantId, currentUser.getId())
                    .stream().map(OrderDTO::new).collect(Collectors.toList());
        }
    }

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Tenant currentTenant = tenantRepository.findById(currentUser.getTenant().getId())
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

        Order order = Order.builder()
                .tenantId(currentTenant.getId())
                .trackingCode(dto.getTrackingCode())
                .origin(dto.getOrigin())
                .recipientName(dto.getRecipientName())
                .arrivalDate(dto.getArrivalDate() != null ? dto.getArrivalDate() : LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .residentId(dto.getResidentId())
                .residentName(dto.getResidentName())
                .unit(dto.getUnit())
                .block(dto.getBlock())
                .residentEmail(dto.getResidentEmail())
                .residentCpf(dto.getResidentCpf())
                .residentWhatsapp(dto.getResidentWhatsapp())
                .build();

        Order savedOrder = orderRepository.save(order);

        String details = String.format("Nova encomenda %s recebida para %s (Unid: %s)", 
                savedOrder.getTrackingCode(), savedOrder.getResidentName(), savedOrder.getUnit());
        
        auditService.log(currentUser, currentTenant, "REGISTRO_ENCOMENDA", details, "ENCOMENDA");

        // Opcional: Enviar e-mail automaticamente ao criar
        // sendNotificationEmail(savedOrder.getId());

        return new OrderDTO(savedOrder);
    }

    @Transactional
    public OrderDTO updateOrder(UUID id, CreateOrderRequest dto) {
        User currentUser = SecurityUtils.getCurrentUser();

        if (!isStaff(currentUser)) {
            throw new RuntimeException("Permissão negada: Apenas funcionários podem editar encomendas.");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encomenda não encontrada"));

        if (!order.getTenantId().equals(currentUser.getTenant().getId())) {
            throw new RuntimeException("Acesso negado.");
        }

        order.setTrackingCode(dto.getTrackingCode());
        order.setOrigin(dto.getOrigin());
        order.setRecipientName(dto.getRecipientName());
        
        if (dto.getArrivalDate() != null) {
            order.setArrivalDate(dto.getArrivalDate());
        }

        if (dto.getResidentId() != null && !dto.getResidentId().equals(order.getResidentId())) {
            order.setResidentId(dto.getResidentId());
            order.setResidentName(dto.getResidentName());
            order.setUnit(dto.getUnit());
            order.setBlock(dto.getBlock());
            order.setResidentEmail(dto.getResidentEmail());
            order.setResidentWhatsapp(dto.getResidentWhatsapp());
            order.setResidentCpf(dto.getResidentCpf());
        }

        Order savedOrder = orderRepository.save(order);

        Tenant tenant = tenantRepository.findById(order.getTenantId()).orElse(null);
        auditService.log(currentUser, tenant, "EDICAO_ENCOMENDA", 
            "Encomenda " + order.getTrackingCode() + " editada.", "ENCOMENDA");

        return new OrderDTO(savedOrder);
    }

    @Transactional
    public OrderDTO signOrder(UUID orderId, SignOrderRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Encomenda não encontrada"));

        if (!order.getTenantId().equals(currentUser.getTenant().getId())) {
            throw new RuntimeException("Acesso negado.");
        }

        if ("RESIDENT".equalsIgnoreCase(request.getType())) {
            if (!isStaff(currentUser) && !order.getResidentId().equals(currentUser.getId())) {
                 throw new RuntimeException("Você não é o destinatário desta encomenda.");
            }
            order.setResidentSignatureDate(LocalDateTime.now());
            order.setResidentSignatureName(currentUser.getNome());
        } 
        else if ("STAFF".equalsIgnoreCase(request.getType())) {
            if (!isStaff(currentUser)) {
                throw new RuntimeException("Apenas funcionários podem confirmar a entrega.");
            }
            order.setStaffSignatureDate(LocalDateTime.now());
            order.setStaffSignatureName(currentUser.getNome());
        }

        if (order.getResidentSignatureDate() != null && order.getStaffSignatureDate() != null) {
            order.setStatus(OrderStatus.ARCHIVED);
        }

        Order savedOrder = orderRepository.save(order);

        Tenant tenant = tenantRepository.findById(order.getTenantId()).orElse(null);
        String details = String.format("Encomenda %s assinada por %s (%s)", 
                order.getTrackingCode(), currentUser.getNome(), request.getType());

        auditService.log(currentUser, tenant, "ASSINATURA_ENCOMENDA", details, "ENCOMENDA");

        return new OrderDTO(savedOrder);
    }

    @Transactional
    public void deleteOrder(UUID id) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (!isStaff(currentUser)) throw new RuntimeException("Acesso negado.");
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Encomenda não encontrada"));
                
        if (!order.getTenantId().equals(currentUser.getTenant().getId())) {
            throw new RuntimeException("Acesso negado.");
        }
        
        String trackingInfo = order.getTrackingCode();
        Tenant tenant = tenantRepository.findById(order.getTenantId()).orElse(null);
        
        orderRepository.delete(order);

        auditService.log(currentUser, tenant, "EXCLUSAO_ENCOMENDA", 
                "Encomenda " + trackingInfo + " removida do sistema.", "ENCOMENDA");
    }

    // --- CORREÇÃO: ENVIO SEGURO VIA HTML ---
    public void sendNotificationEmail(UUID id) {
        User currentUser = null;
        try { currentUser = SecurityUtils.getCurrentUser(); } catch (Exception e) {}

        Order order = orderRepository.findById(id).orElseThrow();
        Tenant tenant = tenantRepository.findById(order.getTenantId()).orElse(null);
        
        String subject = "📦 Chegou Encomenda: " + order.getTrackingCode();
        
        // Formatação de Data
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String arrival = order.getArrivalDate() != null ? order.getArrivalDate().format(fmt) : "Hoje";

        // Template HTML Bonito
        String htmlBody = String.format(
            "<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto;'>" +
            "<h2 style='color: #2563eb;'>Nova Encomenda na Portaria</h2>" +
            "<p>Olá <strong>%s</strong>,</p>" +
            "<p>Informamos que chegou uma nova encomenda para a sua unidade.</p>" +
            "<div style='background: #f0f9ff; padding: 15px; border-radius: 8px; border-left: 4px solid #2563eb; margin: 20px 0;'>" +
            "<p><strong>Origem:</strong> %s</p>" +
            "<p><strong>Rastreio/Cód:</strong> %s</p>" +
            "<p><strong>Data de Chegada:</strong> %s</p>" +
            "</div>" +
            "<p>Por favor, compareça à portaria para retirada apresentando um documento.</p>" +
            "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'/>" +
            "<p style='font-size: 12px; color: #666;'>Atenciosamente,<br>Administração do Condomínio.</p>" +
            "</div>",
            order.getResidentName(),
            order.getOrigin(),
            order.getTrackingCode(),
            arrival
        );

        // CORREÇÃO AQUI: Chama sendHtmlEmail em vez de sendSimpleEmail
        emailService.sendHtmlEmail(order.getResidentEmail(), subject, htmlBody);

        if (currentUser != null) {
            auditService.log(currentUser, tenant, "NOTIFICACAO_EMAIL", 
                "Email enviado para " + order.getResidentEmail(), "ENCOMENDA");
        }
    }

    private boolean isStaff(User user) {
        String role = String.valueOf(user.getRole()); 
        return role.equals("SINDICO") || role.equals("ADM_CONDO") || role.equals("MANAGER") || role.equals("PORTEIRO");
    }
}