import time
import random
from dataclasses import dataclass

# --- 1. CONFIGURATION & CONSTANTS ---
# These constants define how aggressively the PID controller reacts.
KP = 0.5   # Proportional: Reacts to current error
KI = 0.1   # Integral: Reacts to accumulated past error
KD = 0.05  # Derivative: Predicts future error based on current trend

@dataclass
class NodeMetrics:
    cpu: float
    memory_usage: float
    locks: int

# --- 2. MOCK FUNCTIONS ---
# In a real app, these would call an API or system library.
def get_node_metrics(node_name):
    """Simulates fetching real-time system metrics."""
    return NodeMetrics(
        cpu=random.uniform(0.5, 0.95),      # Random CPU between 50% and 95%
        memory_usage=random.uniform(0.6, 0.95), 
        locks=random.randint(0, 5)          # Simulating lock contention
    )

def drop_non_essential_tasks(priority_threshold):
    print(f"EMERGENCY: Dropping tasks with priority < {priority_threshold}")

def trigger_backpressure_to_edge(rate):
    print(f"BACKPRESSURE: Throttling edge ingress to {rate:.2%}")

def update_admission_control(multiplier):
    print(f"NORMAL OPS: Admission rate set to {multiplier:.2%}")

# --- 3. THE PID CONTROLLER ---
class PIDController:
    def __init__(self, target_cpu=0.70):
        self.target = target_cpu
        self.integral = 0
        self.prev_error = 0

    def calculate_throttle_rate(self, current_cpu, current_locks):
        # Penalty for lock contention (e.g., each lock adds 5% virtual load)
        effective_load = current_cpu + (current_locks * 0.05)
        error = effective_load - self.target
        
        self.integral += error
        derivative = error - self.prev_error
        
        # PID Formula: adjustment to the Transaction Rate
        adjustment = (KP * error) + (KI * self.integral) + (KD * derivative)
        self.prev_error = error
        
        # Returns a multiplier: 1.0 is full speed, 0.1 is 10% speed
        return max(0.1, min(1.0, 1.0 - adjustment))

# --- 4. EXECUTION LOOP ---
def monitor_and_optimize():
    # Initialize the controller instance
    controller = PIDController(target_cpu=0.70)
    
    print("Monitoring started. Press Ctrl+C to exit...\n")
    
    try:
        while True:
            metrics = get_node_metrics("Cloud1")
            multiplier = controller.calculate_throttle_rate(metrics.cpu, metrics.locks)
            
            # Logic for load shedding vs gradual adjustment
            if metrics.cpu > 0.85 or metrics.memory_usage > 0.90:
                # Fixed: passing a constant 3 instead of undefined 'priority'
                drop_non_essential_tasks(3) 
                trigger_backpressure_to_edge(multiplier * 0.5)
            else:
                update_admission_control(multiplier)
            
            # FIXED: 100ms is written as 0.1 seconds in Python
            time.sleep(0.1) 
            
    except KeyboardInterrupt:
        print("\nMonitoring stopped by user.")

if __name__ == "__main__":
    monitor_and_optimize()
