package org.example.database.gpt;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.example.utils.ExecutorServiceUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GPTTranslate {
    private static final TypeToken<HashMap<Integer, String>> TYPE_TOKEN = new TypeToken<>() {};
    private static final String GPT_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final OkHttpClient client;
    private final String openAPIKey = "sk-<your key here>";


    public GPTTranslate() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(100);
        dispatcher.setMaxRequestsPerHost(100);

        this.client = new OkHttpClient().newBuilder()
            .callTimeout(TIMEOUT)
            .connectTimeout(TIMEOUT)
            .readTimeout(TIMEOUT)
            .writeTimeout(TIMEOUT)
            .dispatcher(dispatcher)
            .build();
    }

    public CompletableFuture<Map<Integer, String>> translateStringTable(Map<Integer, String> bufferStringTable) throws IOException {
        var messageUser = new Gson().toJson(bufferStringTable);
        var futureResult = new CompletableFuture<Map<Integer, String>>();

        doRequest(messageUser).thenAcceptAsync((response) -> {
            try {
                var stringTableTranslates = getStringTableTranslates(response);

                if (stringTableTranslates.size() != bufferStringTable.size()) {
                    throw new RuntimeException("String table size not equals");
                }

                futureResult.complete(stringTableTranslates);
            } catch (MessageTemplate.RateLimitException e) {
                log.error("Rate limit exception");
                // try again in 1 minute
                ExecutorServiceUtils.SCHEDULER.schedule(() -> {
                    try {
                        futureResult.complete(translateStringTable(bufferStringTable).get());
                    } catch (Exception ex) {
                        futureResult.completeExceptionally(ex);
                    }
                }, 1, TimeUnit.MINUTES);

            } catch (Exception e) {
                log.error("Error while translating string table", e);
                futureResult.completeExceptionally(e);
            }

        }, ExecutorServiceUtils.EXECUTOR);

        return futureResult;
    }


    private CompletableFuture<Response> doRequest(String text) {
        Request.Builder builder = new Request.Builder();

        builder.url(GPT_URL);
        builder.header("Authorization", "Bearer " + openAPIKey);


        JsonObject body = MessageTemplate.templateBody(text);
        builder.post(RequestBody.create(body.toString(), MediaType.parse("application/json")));

        Request request = builder.build();

        try {
            CompletableFuture<Response> future = new CompletableFuture<>();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    future.complete(response);
                }
            });

            return future;
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private HashMap<Integer, String> getStringTableTranslates(@NotNull Response response) {
        if (response.body() == null) {
            throw new RuntimeException("Response body is null");
        }

        String bodyText = "";

        try {
            bodyText = response.body().string();

            JsonObject jsonObject = new Gson().fromJson(bodyText, JsonObject.class);


            if (jsonObject.has("error")) {
                String error = jsonObject.get("error").getAsString();
                String code = jsonObject.get("code").getAsString();

                if (code.equals("rate_limit_exceeded")) {
                    throw new MessageTemplate.RateLimitException(error);
                } else {
                    throw new RuntimeException("Error in get string table translates: " + error);
                }
            }

            JsonArray choices = jsonObject.getAsJsonArray("choices");

            // first choice
            JsonObject choice = choices.get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");

            // get first content
            String content = message.get("content").getAsString();

            // convert to object content to hashmap
            return new Gson().fromJson(content, TYPE_TOKEN.getType());
        } catch (Exception e) {
            throw new RuntimeException("Error in get string table translates: " + bodyText, e);
        }

    }
}

class MessageTemplate {
    public final static String SYSTEM_MESSAGE = """
    API Translate GPT: 1.0.2
    
    Resume:
        Translate Resource from game Zelda Breath of the Wild
            - Need be of english to brazilian portuguese.
            - Keep the escapes of the original escapes like \\u0000, \\u000A, ETC.
            - The translation must be from English into Brazilian Portuguese.
    """;

    public static JsonObject templateBody(String text) {
        JsonObject body = new JsonObject();

        body.addProperty("model", "gpt-3.5-turbo-0613");
        body.addProperty("temperature", 0.7);

        JsonObject systemMessaga = new JsonObject();
        systemMessaga.addProperty("role", "system");
        systemMessaga.addProperty("content", SYSTEM_MESSAGE);

        JsonObject userMessageBody = new JsonObject();
        userMessageBody.addProperty("role", "user");
        userMessageBody.addProperty("content", text);

        JsonArray messages = new JsonArray();
        messages.add(systemMessaga);
        messages.add(userMessageBody);

        body.add("messages", messages);

        return body;
    }

    static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
