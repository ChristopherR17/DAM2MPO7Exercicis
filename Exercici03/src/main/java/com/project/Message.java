package com.project;

public class Message {
    public enum Sender { USER, BOT }

    private final Sender sender;
    private final String text;
    private final String avatarResource; // path like "/assets/ieti.png"

    public Message(Sender sender, String text, String avatarResource) {
        this.sender = sender;
        this.text = text;
        this.avatarResource = avatarResource;
    }

    public Sender getSender() { return sender; }
    public String getText() { return text; }
    public String getAvatarResource() { return avatarResource; }
}
