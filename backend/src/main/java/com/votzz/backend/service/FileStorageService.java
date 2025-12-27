package com.votzz.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
public class FileStorageService {
    
    // @Autowired private S3Template s3Template; // Descomentar quando configurar AWS

    public String uploadFile(MultipartFile file) {
        // Gera um nome único para o arquivo
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        /* LÓGICA AWS S3 (Futuro):
           try {
               s3Template.upload("nome-do-bucket", fileName, file.getInputStream());
           } catch (IOException e) {
               throw new RuntimeException("Erro ao fazer upload", e);
           }
        */
        
        // Retorna uma URL simulada por enquanto
        return "https://s3.amazonaws.com/votzz-bucket/" + fileName;
    }
}