import java.util.*;
import java.util.concurrent.*;

class Transaction {
    String id;
    long timestamp;
    int priority; // Higher weight nodes get higher priority

    public Transaction(String id, int priority) {
        this.id = id;
        this.timestamp = System.currentTimeMillis();
        this.priority = priority;
    }
}

class DeadlockDetector {
    // Maps Transaction ID -> Transaction it is waiting for
    private final Map<String, String> waitForGraph = new ConcurrentHashMap<>();
    private final long timeoutMillis = 2000;

    public void requestResource(Transaction requester, String holderId) {
        if (holderId == null) return;

        System.out.println("[Detector] " + requester.id + " is waiting for " + holderId);
        waitForGraph.put(requester.id, holderId);

        // Initiate Edge-Chasing (Probe)
        if (detectCycle(requester.id, holderId, new HashSet<>())) {
            resolveDeadlock(requester, holderId);
        }
    }

    private boolean detectCycle(String startId, String currentId, Set<String> visited) {
        if (startId.equals(currentId)) return true;
        if (currentId == null || !visited.add(currentId)) return false;

        return detectCycle(startId, waitForGraph.get(currentId), visited);
    }

    private void resolveDeadlock(Transaction requester, String holderId) {
        System.out.println("!!! DEADLOCK DETECTED involving " + requester.id + " !!!");

        // Resolution Strategy: Wait–Die (Priority-based)
        if (requester.priority < 5) {
            System.out.println("[Recovery] Aborting Transaction " + requester.id +
                    " to release locks.");
            waitForGraph.remove(requester.id);
        } else {
            System.out.println("[Recovery] High-priority transaction preserved. Forcing timeout on "
                    + holderId);
        }
    }
}

/* =========================================================
 * PROGRAMIZ ENTRY POINT (MUST BE Main)
 * ========================================================= */
public class Main {
    public static void main(String[] args) {

        DeadlockDetector detector = new DeadlockDetector();

        // Transactions on bottleneck nodes (Task k)
        Transaction txCore1 = new Transaction("TX_CORE1", 8); // High priority
        Transaction txEdge2 = new Transaction("TX_EDGE2", 2); // Low priority

        System.out.println("--- Simulating Cyclic Dependency between Core and Edge ---");

        // Cycle:
        // TX_CORE1 → TX_EDGE2
        // TX_EDGE2 → TX_CORE1
        detector.requestResource(txCore1, "TX_EDGE2");
        detector.requestResource(txEdge2, "TX_CORE1"); // Triggers detection
    }
}
