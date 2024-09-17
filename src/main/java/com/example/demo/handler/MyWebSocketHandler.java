package com.example.demo.handler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;


@Component

public class MyWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private  static List<UnoCard> deck = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(MyWebSocketHandler.class);
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private Map<WebSocketSession, String> name = new HashMap<>();
    private Map<WebSocketSession, List<UnoCard>> playersCard =  new HashMap<>();

    
    
    @JsonSerialize
    private static class UnoCard {
        public String color;
        public int number;



        public boolean isOwn;

        UnoCard(){

        }


        UnoCard(String color, int number, boolean isOwn) {
            this.color = color;
            this.number = number;
            this.isOwn = isOwn;
        }
        // Getters and setters

        public void setIsOwn(boolean own) {
            isOwn = own;
        }

        public boolean getIsOwn() {
            return isOwn;
        }
        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected:");
        if(sessions.isEmpty()){
            ObjectMapper mapper = new ObjectMapper();
            List<UnoCard>newList = new ArrayList<>();
            newList.add(new UnoCard("your turn now.",-1,false));
            String json = mapper.writeValueAsString(newList);
            session.sendMessage(new TextMessage(json));
        }
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("Received card: " + payload);
        try {
            UnoCard card = objectMapper.readValue(payload, UnoCard.class);
            List<UnoCard> currCard = playersCard.get(session);
            for(UnoCard card1:currCard){
                if(card1.getColor().equals(card.getColor()) && card1.getNumber()==card.getNumber() ){
                    currCard.remove(card1);
                    break;
                }
            }
            card.isOwn=false;
            WebSocketSession newSession = null;
            int x=0;
            for(WebSocketSession s:sessions){
                if(x==1){
                    newSession=s;
                    break;
                }
                if(s.equals(session))x=1;
            }
            for(WebSocketSession s:sessions){
                if(Objects.nonNull(newSession))break;
                newSession = s;
                break;
            }
            log.info(name.get(newSession)+"turn");
            ObjectMapper mapper = new ObjectMapper();
            List<UnoCard>newList = new ArrayList<>();
            newList.add(new UnoCard("your turn now.",-1,false));
            String json = mapper.writeValueAsString(newList);
            newSession.sendMessage(new TextMessage(json));
            broadcast(card);
            log.info("size:"+currCard.size());
            if(currCard.isEmpty()){
                log.info("heret:"+currCard.size());
                broadcastJoin(name.get(session)+" won the game !! ");
            }

        }
        catch (Exception e){
            if(payload.contains("draw card")){
                ObjectMapper mapper = new ObjectMapper();
                if(deck.isEmpty()){
                    createDeck();
                    shuffleDeck();
                }
                UnoCard newCard = new UnoCard(deck.get(0).color, deck.get(0).number, deck.get(0).isOwn);
                deck.remove(0);
                playersCard.get(session).add(newCard);
                String json = mapper.writeValueAsString(playersCard.get(session));
                session.sendMessage(new TextMessage(json));

                WebSocketSession newSession = null;
                int x=0;
                for(WebSocketSession s:sessions){
                    if(x==1){
                        newSession=s;
                        break;
                    }
                    if(s.equals(session))x=1;
                }
                for(WebSocketSession s:sessions){
                    if(Objects.nonNull(newSession))break;
                    newSession = s;
                    break;
                }
                log.info(name.get(newSession)+"turn");
                List<UnoCard>newList = new ArrayList<>();
                newList.add(new UnoCard("your turn now.",-1,false));
                String json1 = mapper.writeValueAsString(newList);
                newSession.sendMessage(new TextMessage(json1));

                return;
            }
            if(payload.contains("claim UNO")){
                broadcastJoin(name.get(session) + " claimed uno.");
                return;
            }
            if (name.containsKey(session)) {
                log.info(message.getPayload() + " already.");
                return;
            }


            name.put(session, payload);
            broadcastJoin(payload + " joined.");
            sendUnoCards(session);
        }
    }

    private void sendUnoCards(WebSocketSession session) {
        List<UnoCard> cards = new ArrayList<>();
        for(int i=0;i<7;i++){
            cards.add(deck.get(i));
            deck.remove(i);
        }
        playersCard.put(session, cards);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(cards);
            session.sendMessage(new TextMessage(json));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        name.remove(session);
    }

    public void broadcast(UnoCard card) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                List<UnoCard>newList = new ArrayList<>();
                newList.addAll(playersCard.get(session));
                newList.add(card);
                String json = mapper.writeValueAsString(newList);
                session.sendMessage(new TextMessage(json));
            }
        }
    }
    
    public void broadcastJoin(String messege) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<UnoCard>newList = new ArrayList<>();
        newList.add(new UnoCard(messege,-1,false));
        String json = mapper.writeValueAsString(newList);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    public void createDeck() {
        String[] COLORS = {"Red", "Green", "Blue", "Yellow"};
        int CARDS_PER_COLOR = 25;
        int TOTAL_CARDS = 100;
        for (String color : COLORS) {
            for (int i = 0; i < CARDS_PER_COLOR; i++) {
                int number = i % 10;
                deck.add(new UnoCard(color, number, true));
            }
        }
    }

    public static void shuffleDeck() {
        Collections.shuffle(deck);
    }
    



}
