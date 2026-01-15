import threading
import time


# ============================
# Node Class
# ============================
class Node:
    def __init__(self, name, is_active=True, latency_ms=100):
        self.name = name
        self.is_active = is_active
        self.latency_ms = latency_ms


# ============================
# Consistency Manager
# ============================
class ConsistencyManager:
    def __init__(self, replicas):
        self.replicas = replicas  # List of Node objects
        self.lock = threading.Lock()
        self.global_state = {}

    def execute_transaction(self, tx_id, key, value):
        """
        Strong Consistency (Synchronous Replication)
        """
        with self.lock:
            print(f"\n[TX {tx_id}] Initiating Transaction: {key} -> {value}")

            # Phase 1: Prepare
            acks = 0
            for node in self.replicas:
                if node.is_active:
                    time.sleep(node.latency_ms / 1000)
                    print(f"[TX {tx_id}] ACK from {node.name}")
                    acks += 1

            # Phase 2: Commit (Quorum)
            quorum = (len(self.replicas) // 2) + 1
            if acks >= quorum:
                self.global_state[key] = value
                print(f"[TX {tx_id}] SUCCESS: Quorum reached ({acks}/{len(self.replicas)})")
                return True
            else:
                print(f"[TX {tx_id}] ABORTED: Insufficient replicas")
                return False


# ============================
# MAIN EXECUTION (IMPORTANT)
# ============================
if __name__ == "__main__":

    # Define nodes_list FIRST
    nodes_list = [
        Node("Edge1", is_active=True, latency_ms=50),
        Node("Edge2", is_active=True, latency_ms=60),
        Node("Core1", is_active=True, latency_ms=80),
        Node("Cloud1", is_active=True, latency_ms=120)
    ]

    # Select replicas
    core_nodes = [nodes_list[2], nodes_list[3]]
    consistency_engine = ConsistencyManager(core_nodes)

    # Simulate concurrent transactions
    t1 = threading.Thread(
        target=consistency_engine.execute_transaction,
        args=("001", "balance", 500)
    )

    t2 = threading.Thread(
        target=consistency_engine.execute_transaction,
        args=("002", "balance", 600)
    )

    t1.start()
    t2.start()
    t1.join()
    t2.join()

    print("\nFinal Global State:", consistency_engine.global_state)
