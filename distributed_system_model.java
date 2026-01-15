import java.util.*;
import java.util.concurrent.*;

/* =========================================================
 * 1. Message Structure with Priority & Event Ordering
 * ========================================================= */
class Message {
    enum Type { RPC, PREPARE, COMMIT, DATA_SYNC, RECOVERY }

    final String senderId;
    final Type type;
    final long timestamp;
    final int priority;
    final Object payload;

    public Message(String senderId, Type type, int priority, Object payload) {
        this.senderId = senderId;
        this.type = type;
        this.priority = priority;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
}

/* =========================================================
 * 2. Abstract Node Definition
 * ========================================================= */
abstract class Node {
    protected final String id;
    protected final String layer;
    protected final int latency;
    protected final double packetLoss;
    protected final String failureType;
    protected boolean isRunning = true;

    public Node(String id, String layer, int latency, double packetLoss, String failureType) {
        this.id = id;
        this.layer = layer;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.failureType = failureType;
    }

    public abstract void handleMessage(Message msg);

    protected boolean shouldDropPacket() {
        return Math.random() < (packetLoss / 100.0);
    }
}

/* =========================================================
 * 3. Edge Node
 * ========================================================= */
class EdgeNode extends Node {

    private final PriorityQueue<Message> queue =
            new PriorityQueue<>(Comparator.comparingInt(m -> m.priority));

    public EdgeNode(String id, int latency, double loss, String failureType) {
        super(id, "Edge", latency, loss, failureType);
    }

    @Override
    public void handleMessage(Message msg) {
        if (!isRunning) {
            System.out.println("[" + id + "] Node is down.");
            return;
        }

        if ("Omission".equals(failureType) && shouldDropPacket()) {
            System.out.println("[" + id + "] Packet omitted.");
            return;
        }

        queue.add(msg);
        process();
    }

    private void process() {
        while (!queue.isEmpty()) {
            Message m = queue.poll();
            System.out.println("[" + id + "] Processing " + m.type +
                    " | Priority=" + m.priority +
                    " | Payload=" + m.payload);
        }
    }
}

/* =========================================================
 * 4. Core Node (2PC + Byzantine + Crash)
 * ========================================================= */
class CoreNode extends Node {

    private int load = 0;

    public CoreNode(String id, int latency, double loss, String failureType) {
        super(id, "Core", latency, loss, failureType);
    }

    @Override
    public void handleMessage(Message msg) {
        if (!isRunning) {
            System.out.println("[" + id + "] Node crashed.");
            return;
        }

        if ("Crash".equals(failureType) && ++load > 4) {
            isRunning = false;
            System.out.println("[" + id + "] CRASH FAILURE triggered.");
            return;
        }

        Message processed = msg;

        if ("Byzantine".equals(failureType)) {
            processed = new Message(
                    msg.senderId,
                    msg.type,
                    msg.priority,
                    "CORRUPTED_" + msg.payload
            );
            System.out.println("[" + id + "] Byzantine fault injected.");
        }

        if (processed.type == Message.Type.RPC) {
            System.out.println("[" + id + "] RPC handled from " +
                    processed.senderId + " | Payload=" + processed.payload);
        }

        if (processed.type == Message.Type.PREPARE) {
            System.out.println("[" + id + "] 2PC PREPARE → VOTE COMMIT | Payload=" +
                    processed.payload);
        }
    }
}

/* =========================================================
 * 5. Network Simulator
 * ========================================================= */
class Network {

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);

    private final Map<String, Node> nodes = new HashMap<>();

    public void addNode(Node node) {
        nodes.put(node.id, node);
    }

    public void sendMessage(String targetId, Message msg) {
        Node target = nodes.get(targetId);
        if (target == null) return;

        scheduler.schedule(
                () -> target.handleMessage(msg),
                target.latency,
                TimeUnit.MILLISECONDS
        );
    }

    public void shutdown() {
        scheduler.shutdown();
        System.out.println("[Network] Shutdown complete.");
    }
}

/* =========================================================
 * 6. MAIN CLASS (Programiz Requirement)
 * ========================================================= */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        Network network = new Network();

        EdgeNode edge1 = new EdgeNode("Edge1", 12, 0.2, "Crash");
        CoreNode core1 = new CoreNode("Core1", 8, 0.1, "Byzantine");
        CoreNode core2 = new CoreNode("Core2", 10, 0.2, "Crash");

        network.addNode(edge1);
        network.addNode(core1);
        network.addNode(core2);

        System.out.println("=== Distributed System Simulation Started ===");

        network.sendMessage("Edge1",
                new Message("User", Message.Type.RPC, 1, "Initial_Request"));

        network.sendMessage("Core1",
                new Message("Edge1", Message.Type.RPC, 2, "Forwarded_RPC"));

        network.sendMessage("Core1",
                new Message("Edge1", Message.Type.PREPARE, 3, "TX_001"));

        network.sendMessage("Core2",
                new Message("Edge1", Message.Type.PREPARE, 3, "TX_001"));

        Thread.sleep(3000);

        network.shutdown();
        System.out.println("=== Simulation Completed ===");
    }
}
