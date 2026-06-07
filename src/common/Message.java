package common;

public class Message {

    private MessageType type;
    private String from;
    private String to;
    private String content;
    private long timestamp;

    public Message(MessageType type, String from, String to, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String encode() {
        return (type == null ? "" : type.name())
                + "|" + (from == null ? "" : from)
                + "|" + (to == null ? "" : to)
                + "|" + (content == null ? "" : content)
                + "|" + timestamp;
    }

    public static Message decode(String line) {
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("message line is null or empty");
        }

        // timestamp is always the last field (no | in a long value)
        int lastPipe = line.lastIndexOf('|');
        if (lastPipe < 0) {
            throw new IllegalArgumentException("invalid message format: " + line);
        }
        String timestampStr = line.substring(lastPipe + 1);
        String rest = line.substring(0, lastPipe);

        // split the rest into at most 4 parts; content (4th part) may contain |
        String[] parts = rest.split("\\|", 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "invalid message format, expected type|from|to|content: " + line);
        }
        // parts[3] (content) is optional
        String content = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;

        MessageType type = MessageType.valueOf(parts[0]);
        String from = parts[1].isEmpty() ? null : parts[1];
        String to = parts[2].isEmpty() ? null : parts[2];
        Message msg = new Message(type, from, to, content);
        try {
            msg.setTimestamp(Long.parseLong(timestampStr));
        } catch (NumberFormatException e) {
            // keep the default current time
        }
        return msg;
    }

    @Override
    public String toString() {
        return "Message{type=" + type
                + ", from='" + from + '\''
                + ", to='" + to + '\''
                + ", content='" + content + '\''
                + ", timestamp=" + timestamp + '}';
    }
}
