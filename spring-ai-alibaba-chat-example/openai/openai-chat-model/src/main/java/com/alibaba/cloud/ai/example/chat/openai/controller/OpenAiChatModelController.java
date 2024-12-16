/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.chat.openai.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @Author: wst
 * @Date: 2024-12-16
 */

@RestController
@RequestMapping("/openai/chat-model")
public class OpenAiChatModelController {

    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。";
    private static final String JSON_OUTPUT_PROMPT = "how can I solve 8x + 7 = -23";

    private final ChatModel openAiChatModel;

    public OpenAiChatModelController(ChatModel chatModel) {
        this.openAiChatModel = chatModel;
    }

    /**
     * 最简单的使用方式，没有任何 LLMs 参数注入。
     *
     * @return String types.
     */
    @GetMapping("/simple/chat")
    public String simpleChat() {

        return openAiChatModel.call(new Prompt(DEFAULT_PROMPT)).getResult().getOutput().getContent();
    }

    /**
     * Stream 流式调用。可以使大模型的输出信息实现打字机效果。
     *
     * @return Flux<String> types.
     */
    @GetMapping("/stream/chat")
    public String streamChat(HttpServletResponse response) {

        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");
        StringBuilder res = new StringBuilder();

        Flux<ChatResponse> stream = openAiChatModel.stream(new Prompt(DEFAULT_PROMPT));
        stream.toStream().toList().forEach(resp -> {
            res.append(resp.getResult().getOutput().getContent());
        });
        return res.toString();
    }

    /**
     * 使用编程方式自定义 LLMs ChatOptions 参数， {@link org.springframework.ai.openai.OpenAiChatOptions}
     * 优先级高于在 application.yml 中配置的 LLMs 参数！
     */
    @GetMapping("/custom/chat")
    public String customChat() {

        OpenAiChatOptions customOptions = OpenAiChatOptions.builder()
                .withTopP(0.7)
                .withModel("gpt-4o")
                .withMaxTokens(1000)
                .withTemperature(0.8)
                .build();

        return openAiChatModel.call(new Prompt(DEFAULT_PROMPT, customOptions)).getResult().getOutput().getContent();
    }

    /**
     * JSON mode：通过设置 response_format 参数为 JSON 类型，使大模型返回标准的 JSON 格式数据。
     *
     * @return JSON String.
     */
    @GetMapping("/custom/chat/json-mode")
    public String jsonChat() {

        String jsonSchema = """
                {
                    "type": "object",
                    "properties": {
                        "steps": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "explanation": { "type": "string" },
                                    "output": { "type": "string" }
                                },
                                "required": ["explanation", "output"],
                                "additionalProperties": false
                            }
                        },
                        "final_answer": { "type": "string" }
                    },
                    "required": ["steps", "final_answer"],
                    "additionalProperties": false
                }
                """;

        OpenAiChatOptions customOptions = OpenAiChatOptions.builder()
                .withTopP(0.7)
                .withModel("gpt-4o")
                .withTemperature(0.4)
                .withMaxTokens(4096)
                .withResponseFormat(new OpenAiApi.ChatCompletionRequest.ResponseFormat(OpenAiApi.ChatCompletionRequest.ResponseFormat.Type.JSON_SCHEMA, jsonSchema))
                .build();

        return openAiChatModel.call(new Prompt(JSON_OUTPUT_PROMPT, customOptions)).getResult().getOutput().getContent();
    }

}