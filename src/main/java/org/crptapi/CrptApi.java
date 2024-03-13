package org.crptapi;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Semaphore semaphore;
    private final OkHttpClient httpClient = new OkHttpClient();

    public CrptApi(long timeInterval, TimeUnit timeUnit, int requestLimit) {
        long intervalInMillis = timeUnit.toMillis(timeInterval);
        this.semaphore = new Semaphore(requestLimit);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(this::releaseSemaphore, 0, intervalInMillis, TimeUnit.MILLISECONDS);
    }

    private void releaseSemaphore() {
        semaphore.release(semaphore.availablePermits());
    }

    public void createDocument(Document document, String signature) {
        try {
            if (semaphore.tryAcquire()) {
                String jsonBody = objectMapper.writeValueAsString(document);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .addHeader("Signature", signature)
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("Ошибка создания запроса: " + response.code() + " " + response.message());
                    } else {
                        System.out.println("Запрос успешно сформирован");
                    }
                }
            } else {
                System.err.println("Количество запросов превышено");
            }
        } catch (JsonProcessingException e) {
            System.err.println("Ошибка сериализации в JSON: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Ошибка выполнения HTTP запроса: " + e.getMessage());
        }
    }

    @Data
    static class Document {
        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("importRequest")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("production_type")
        private String productionType;

        @JsonProperty("products")
        private Product[] products;

        @JsonProperty("reg_date")
        private String regDate;

        @JsonProperty("reg_number")
        private String regNumber;

        @Data
        static class Description {
            @JsonProperty("participantInn")
            private String participantInn;
        }

        @Data
        static class Product {
            @JsonProperty("certificate_document")
            private String certificateDocument;

            @JsonProperty("certificate_document_date")
            private String certificateDocumentDate;
        }
    }

    public static void main(String[] args) {

        int reqestLimit = 10;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        Long timeInterval = 1L;

        CrptApi api = new CrptApi(timeInterval, timeUnit, reqestLimit);
        CrptApi.Document document = new CrptApi.Document();

        String signature = "Signature";

        api.createDocument(document, signature);
    }
}
