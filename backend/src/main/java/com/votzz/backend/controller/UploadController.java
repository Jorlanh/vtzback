package com.votzz.backend.controller;

import com.votzz.backend.service.FileStorageService; // <--- IMPORTA O SERVIÇO CERTO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Autowired
    private FileStorageService fileStorageService; // <--- INJETA O SERVIÇO CERTO

    @PostMapping("/assemblies")
    public ResponseEntity<Map<String, String>> uploadAssemblyFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("O arquivo não pode estar vazio.");
            }

            System.out.println("Recebendo upload: " + file.getOriginalFilename()); // Log para debug

            // CORRIGIDO: Passando a pasta "assemblies"
            String fileUrl = fileStorageService.uploadFile(file, "assemblies");

            return ResponseEntity.ok(Map.of("url", fileUrl));
            
        } catch (Exception e) {
            e.printStackTrace(); // Mostra o erro no console do Java se falhar
            return ResponseEntity.internalServerError().body(Map.of("error", "Falha no upload: " + e.getMessage()));
        }
    }
}