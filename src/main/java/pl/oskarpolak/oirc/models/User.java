package pl.oskarpolak.oirc.models;


import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class User {
    private String nickname;
    private WebSocketSession session;

    public User(WebSocketSession session){
        this.session = session;
    }
}
