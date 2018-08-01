package pl.oskarpolak.oirc.models;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@EnableWebSocket
@Component
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    private Set<User> users = new HashSet<>();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(this, "/chat")
                                .setAllowedOrigins("*");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        users.add(new User(session));

        User connectedUser = findUserBySession(session);
        connectedUser.getSession().sendMessage(new TextMessage("Witaj na naszym czacie!"));
        connectedUser.getSession().sendMessage(new TextMessage("Twoja piersza wiadomość będzie Twoim nickiem"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User sender = findUserBySession(session);
        if(sender.getNickname() == null){
            sender.setNickname(message.getPayload());
            sender.getSession().sendMessage(new TextMessage("Ustawiliśmy Twój nick"));
            return;
        }
        sendMessageToAllUsers(createMessageWithSenderNickname(message, sender));
    }

    private TextMessage createMessageWithSenderNickname(TextMessage message, User sender) {
        return new TextMessage(sender.getNickname() + ": " + message.getPayload());
    }

    private User findUserBySession(WebSocketSession session) {
        return  users.stream()
                .filter(s -> s.getSession().getId().equals(session.getId()))
                .findAny()
                .orElseThrow(IllegalStateException::new);
    }

    private void sendMessageToAllUsers(TextMessage message) throws IOException {
        for (User user : users) {
            user.getSession().sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        users.stream()
                .filter(s -> s.getSession().getId().equals(session.getId()))
                .findAny()
                .ifPresent(s -> users.remove(s));
    }
}
