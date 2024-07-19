package com.mycompany.truemarkapi;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;

/**
 *
 * @author pablo
 */

// Main default class
public class CtprApi extends Thread
{
    private final long secondsToRequest;
    private final int requestLimit;
    private HttpPost httpPost;
    private CloseableHttpClient client;
    private ArrayList<String> requestList;
    private Long firstRequestTime;

    @SneakyThrows
    @Override
    public void run()
    {
        connect();
        sendRequest();
    }

    // @param TimeUnit timeUnit, int requestLimit
    public CtprApi(long seconds, int requestLimit) throws Exception
    {
        this.secondsToRequest = seconds;
        this.requestLimit = requestLimit;
    }

    // Возможно убрать синхронизацию
    public synchronized void connect()
    {
        // Установка max количества соединений
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(1);
        connManager.setDefaultMaxPerRoute(1);
        // .....................................................................................

        this.httpPost = new HttpPost("http://localhost:8080/source");
        this.client = HttpClients.custom()
                .setConnectionManager(connManager)
                .build();
        // установка хедеров для json формата
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        requestList = new ArrayList<>(requestLimit);
    }
    // 5 second - > 5 times sendRequest()

    // Проверка списка запросов
    @SneakyThrows
    public boolean ifRequestListNotFilled()
    {
        // Если разница во времени между первым запросом и текущим больше или равно условию, то обновляем список запросов
        long currentSeconds = Instant.now().getEpochSecond();
        if (Math.abs(currentSeconds-firstRequestTime) >= secondsToRequest)
            requestList.clear();

        // Если список запросов не полный, то даем добро на новый запрос
        if (requestList.size() != requestLimit) {
            return true;
        }
        // Если размер списка запросов равен условию количества запросов,
        // то засыпляем потом на разницу в секундах до конца периода и обновляем список запросов
        else if (requestList.size() == requestLimit)
        {

            currentSeconds = Instant.now().getEpochSecond();
            Thread.sleep(1000 * Math.abs(currentSeconds - firstRequestTime - secondsToRequest));
            requestList.clear();
            return true;
        }

        return false;
    }

    // Отправка запроса
    public void sendRequest()
    {
        if (requestList.isEmpty())
        {
            firstRequestTime = Instant.now().getEpochSecond(); // Первый запрос: фиксируем время запроса
        }

        if (ifRequestListNotFilled())
        {
            requestList.add("request"); // Проверка условия и добавление в список запросов
        }

        try {
            // Создание объекта JSON документа и его сериализация
            Document document = new Document();
            Gson gson = new Gson();
            String jsonDoc = gson.toJson(document);
            //........................................................................

            // Создание тела запроса
            StringEntity entity = new StringEntity(jsonDoc);
            httpPost.setEntity(entity);

            try {
                // отправка запроса и получение ответа
                CloseableHttpResponse response = this.client.execute(httpPost);

                // вывод статус кода
                System.out.println(response.getStatusLine().getStatusCode());

                // получение строки json в качестве ответа
                String responseJSON = EntityUtils.toString(response.getEntity());

                // вывод ответа в виде json
                System.out.println(responseJSON);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
}
    // ============================================================================
    // POJO-data классы документа
    @AllArgsConstructor
    public static class Description{
        private final String participantInn = "string";
    }

    @AllArgsConstructor
    public static class Product{
        private final String certificate_document = "string";
        private final String certificate_document_date = "string";
        private final String certificate_document_number = "string";
        private final String owner_inn = "string";
        private final String producer_inn = "string";
        private final String production_date = LocalDateTime.now().toString();
        private final String tnved_code = "string";
        private final String uit_code = "string";
        private final String uitu_code = "string";
    }

    @AllArgsConstructor
    public static class Document {
        private final Description description = new Description();
        private final String doc_id = "string";
        private final String doc_status = "string";
        private final String doc_type = "LP_INTRODUCE_GOODS";
        private final boolean importRequest = false;
        private final String owner_inn = "string";
        private final String participant_inn = "string";
        private final String producer_inn = "string";
        private final String production_date = LocalDateTime.now().toString();
        private final String production_type = "string";
        private final ArrayList<Product> products = new ArrayList<>(2);

        private final String reg_date = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        private final String reg_number = "string";

    }
    // ============================================================================

    // Точка входа
   public static void main(String[] args) throws Exception
   {
        CtprApi api = new CtprApi(5, 2);
        api.start();
   }
}
