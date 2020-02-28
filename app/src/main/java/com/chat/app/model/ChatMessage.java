package com.chat.app.model;

import com.google.firebase.database.ServerValue;

import java.util.Map;

public class ChatMessage {
    private String chatText, chatSender, roomId;
    private String chatSenderUid;
    public long timeStamp;
    public boolean seen = false;
    private String type = null;

    public ChatMessage() {
    }


    public String getChatText() {
        return chatText;
    }

    public void setChatText(String chatText) {
        this.chatText = chatText;
    }

    public String getChatSender() {
        return chatSender;
    }

    public void setChatSender(String chatSender) {
        this.chatSender = chatSender;
    }

    public String getChatSenderUid() {
        return chatSenderUid;
    }

    public void setChatSenderUid(String chatSenderUid) {
        this.chatSenderUid = chatSenderUid;
    }


    /*public long getTime() {
        return timeStamp;
    }

    public void setTime(long time) {
        this.timeStamp = time;
    }*/

    public Map<String, String> getTimeStamp() {
        return ServerValue.TIMESTAMP;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }



    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
