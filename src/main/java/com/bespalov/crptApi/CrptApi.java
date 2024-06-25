package com.bespalov.crptApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class App {
    public static void main(String[] args) throws JsonProcessingException {
        // Создаем экземпляр CrptApi
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10, 5);

        // Создаем пример документа и подписи
        Document sampleDocument = Document.builder()
                .description(new Document.Description("participantInn"))
                .docId("docId")
                .docStatus("docStatus")
                .docType("docType")
                .importRequest(true)
                .ownerInn("ownerInn")
                .participantInn("participantInn")
                .producerInn("producerInn")
                .productionDate("productionDate")
                .productionType("productionType")
                .products(List.of(Document.Product.builder()
                        .certificateDocument("certificateDocument")
                        .certificateDocumentDate("certificateDocumentDate")
                        .certificateDocumentNumber("certificateDocumentNumber")
                        .ownerInn("ownerInn")
                        .producerInn("producerInn")
                        .productionDate("productionDate")
                        .tnvedCode("tnvedCode")
                        .uitCode("uitCode")
                        .uituCode("uituCode")
                        .build()))
                .regDate("regDate")
                .regNumber("regNumber")
                .build();
        String signature = "signature";
        // Вызываем метод для создания документа
        crptApi.createDocument(sampleDocument, signature);
    }
}

public final class CrptApi {
    private final Semaphore semaphore; // Семафор для управления лимитом запросов
    private final HttpClient httpClient; // HTTP клиент для отправки запросов
    private final int requestLimit; // Ограничение на количество запросов
    private final long duration; // Количество времени ограничения на количество запросов
    private final TimeUnit timeUnit; // Единица времени ограничения на количество запросов


    public CrptApi(TimeUnit timeUnit, long duration, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        this.duration = duration;
        this.timeUnit = timeUnit;
        httpClient = HttpClient.newBuilder().build();
        this.requestLimit = requestLimit;
        updateLimit();
    }

    // Метод для обновления лимита запросов в указанном времени
    private void updateLimit() {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(timeUnit.toMillis(duration));
                    semaphore.release(requestLimit);
                }
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }).start();
    }

    public void createDocument(Document document, String signature) throws JsonProcessingException {
        try {
            semaphore.acquire();
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonDocument = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(System.out::println);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

@Data
@AllArgsConstructor
@Builder
class Document {
    private Description description;
    private String docId;
    private String docStatus;
    private String docType;
    private boolean importRequest;
    private String ownerInn;
    private String participantInn;
    private String producerInn;
    private String productionDate;
    private String productionType;
    private List<Product> products;
    private String regDate;
    private String regNumber;

    @Data
    @AllArgsConstructor
    static class Description {
        private String participantInn;
    }

    @Data
    @AllArgsConstructor
    @Builder
    static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }
}


