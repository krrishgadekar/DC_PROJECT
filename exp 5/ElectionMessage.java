import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Protocol Message framing for Quorum-based Split-Brain Preventing Election.
 * Communicates across distributed nodes over UDP datagram packets.
 */
public class ElectionMessage {

    public enum Type {
        HEARTBEAT,              // Leader -> Followers: maintains active lease & quorum proof
        HEARTBEAT_ACK,          // Follower -> Leader: confirms follower presence for leader quorum
        REQUEST_VOTE,           // Candidate -> Peers: solicits majority quorum votes
        VOTE_GRANT,             // Peer -> Candidate: grants vote for term
        VOTE_REJECT,            // Peer -> Candidate: rejects vote (stale term, already voted, etc.)
        COORDINATOR,            // New Leader -> All: announces successful majority election
        WORKLOAD_REQ,           // Client -> Node: submit task execution
        WORKLOAD_RESP,          // Node -> Client: success or fenced/split-brain rejection
        SET_PARTITION,          // Driver/CLI -> Node: dynamically isolate specific peers
        HEAL_PARTITION,         // Driver/CLI -> Node: restore connectivity to all peers
        STATUS_REQ,             // Driver/CLI -> Node: query node runtime state
        STATUS_RESP,            // Node -> Driver/CLI: state dump
        STEP_DOWN,              // Demote stale leader (higher term detected)
        SHUTDOWN                // Clean process termination
    }

    private final Type type;
    private final int senderId;
    private final int targetId;
    private final long term;             // Monotonic Election Epoch / Term
    private final String payload;        // Context-dependent payload
    private final String extra;          // Extra metadata (status, reachable count, etc.)

    public ElectionMessage(Type type, int senderId, int targetId, long term, String payload, String extra) {
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.term = term;
        this.payload = (payload == null) ? "" : payload;
        this.extra = (extra == null) ? "" : extra;
    }

    public Type getType() { return type; }
    public int getSenderId() { return senderId; }
    public int getTargetId() { return targetId; }
    public long getTerm() { return term; }
    public String getPayload() { return payload; }
    public String getExtra() { return extra; }

    /**
     * Serializes message to pipe-delimited string:
     * TYPE|senderId|targetId|term|b64(payload)|b64(extra)
     */
    public String serialize() {
        Base64.Encoder enc = Base64.getEncoder();
        String b64Payload = enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String b64Extra = enc.encodeToString(extra.getBytes(StandardCharsets.UTF_8));

        return String.join("|",
                type.name(),
                String.valueOf(senderId),
                String.valueOf(targetId),
                String.valueOf(term),
                b64Payload,
                b64Extra
        );
    }

    /**
     * Deserializes pipe-delimited string back to ElectionMessage.
     */
    public static ElectionMessage deserialize(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] tokens = raw.split("\\|", -1);
        if (tokens.length < 6) return null;

        try {
            Type t = Type.valueOf(tokens[0]);
            int sId = Integer.parseInt(tokens[1]);
            int tgId = Integer.parseInt(tokens[2]);
            long term = Long.parseLong(tokens[3]);

            Base64.Decoder dec = Base64.getDecoder();
            String payload = new String(dec.decode(tokens[4]), StandardCharsets.UTF_8);
            String extra = new String(dec.decode(tokens[5]), StandardCharsets.UTF_8);

            return new ElectionMessage(t, sId, tgId, term, payload, extra);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("ElectionMsg[%s | N%d->N%d | Term=%d | payload='%s' | extra='%s']",
                type, senderId, targetId, term, payload, extra);
    }
}
