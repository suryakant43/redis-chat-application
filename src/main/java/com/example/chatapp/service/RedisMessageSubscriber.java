/** Redis message listener that receives real-time messages published to chatroom channels. */
package com.example.chatapp.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class RedisMessageSubscriber implements MessageListener {

    @Getter
    private final ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<>();

    /**
     * Callback triggered when a message is published to any subscribed Redis channel.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("Real-time message received on channel '{}': {}", channel, body);
        receivedMessages.add(body);
    }
}
