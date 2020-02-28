package com.chat.app.model;

public class Room {
    private String room_id,room_created_by,room_sender_id,room_reciever_id;
    private String senderName, recieverName, recieverPhoto;
    private long room_created_at;

    public Room()
    {}

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public String getRoom_created_by() {
        return room_created_by;
    }

    public void setRoom_created_by(String room_created_by) {
        this.room_created_by = room_created_by;
    }

    public long getRoom_created_at() {
        return room_created_at;
    }

    public void setRoom_created_at(long room_created_at) {
        this.room_created_at = room_created_at;
    }

    public String getRoom_sender_id() {
        return room_sender_id;
    }

    public void setRoom_sender_id(String room_sender_id) {
        this.room_sender_id = room_sender_id;
    }

    public String getRoom_reciever_id() {
        return room_reciever_id;
    }

    public void setRoom_reciever_id(String room_reciever_id) {
        this.room_reciever_id = room_reciever_id;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getRecieverName() {
        return recieverName;
    }

    public void setRecieverName(String recieverName) {
        this.recieverName = recieverName;
    }

    public String getRecieverPhoto() {
        return recieverPhoto;
    }

    public void setRecieverPhoto(String recieverPhoto) {
        this.recieverPhoto = recieverPhoto;
    }


}
