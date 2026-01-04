package com.votzz.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // Lê a URL do application.properties
    @Value("${votzz.storage.public-url}")
    private String storagePublicUrl;

    // Diretório local onde os arquivos serão salvos
    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório para upload de arquivos.", ex);
        }
    }

    /**
     * Salva o arquivo localmente e retorna uma URL (configurada no properties).
     */
    public String storeFile(MultipartFile file) {
        // 1. Gera um nome único para evitar conflito (UUID + Nome Original)
        String originalName = file.getOriginalFilename();
        if (originalName == null) originalName = "arquivo_sem_nome.pdf";
        
        // Remove caracteres perigosos do nome
        String cleanName = originalName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
        String fileName = UUID.randomUUID().toString() + "_" + cleanName;

        try {
            // 2. Salva o arquivo na pasta 'uploads' (Local)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            /* * --- LÓGICA FUTURA PARA AWS S3 ---
             * * s3Template.upload("votzz-bucket", fileName, file.getInputStream());
             * * Nesse caso, a URL base no properties seria a do bucket S3
             */

            // 3. Retorna a URL montada dinamicamente com base no properties
            // Garante que a URL base termina com / antes de concatenar
            String baseUrl = storagePublicUrl.endsWith("/") ? storagePublicUrl : storagePublicUrl + "/";
            return baseUrl + fileName;

        } catch (IOException ex) {
            throw new RuntimeException("Não foi possível armazenar o arquivo " + fileName + ". Tente novamente!", ex);
        }
    }
}