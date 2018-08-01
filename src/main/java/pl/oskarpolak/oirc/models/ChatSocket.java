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
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@EnableWebSocket
@Component
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    private Set<User> users = new HashSet<>();
    private Queue<TextMessage> messages = new ArrayDeque<>();

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
            if(!isNicknameFree(message.getPayload())){
                sender.getSession().sendMessage(new TextMessage("Nick zajęty, wybierz inny"));
                return;
            }
            sender.setNickname(message.getPayload());
            sender.getSession().sendMessage(new TextMessage("Ustawiliśmy Twój nick"));

            sendAllLastMessagesToSender(sender);
            sendMessageToAllWithoutSender(sender, new TextMessage(sender.getNickname() + ", właśnie się zalogował"));
            return;
        }

        TextMessage messageToSend = createMessageWithSenderNickname(message, sender);
        addToMessageList(messageToSend);
        sendMessageToAllUsers(messageToSend);
    }

    private void sendMessageToAllWithoutSender(User sender, TextMessage textMessage){
        users.stream()
                .filter(s -> !s.getSession().getId().equals(sender.getSession().getId()))
                .forEach(s -> {
                    try {
                        s.getSession().sendMessage(textMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }



    private void sendAllLastMessagesToSender(User sender) throws IOException {
        for (TextMessage message : messages) {
            sender.getSession().sendMessage(message);
        }
    }

    private void addToMessageList(TextMessage messageToSend) {
        if(messages.size() >= 10){
            messages.remove();
        }
        messages.add(messageToSend);
    }

    private boolean isNicknameFree(String nickname) {
         return users.stream().noneMatch(s -> s.getNickname() != null && s.getNickname().equals(nickname));
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
        User sender = findUserBySession(session);
        sendMessageToAllWithoutSender(sender, new TextMessage(sender.getNickname() + ", właśnie wyszedł"));


        users = users.stream()
                .filter(s -> !s.getSession().getId().equals(session.getId()))
                .collect(Collectors.toSet());
        //albo to co wyzej
        //albo zastanowic sie dobrze nad przeslonieciem metody hashcode jeszcze raz i equals
    }
}
