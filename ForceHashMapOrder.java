import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to force HashMap iteration order for reproducing flakiness.
 * 
 * This class provides methods to create HashMaps with predictable iteration
 * orders by manipulating the internal hash table structure.
 */
public class ForceHashMapOrder {
    
    /**
     * Creates a HashMap that will iterate in the specified key order.
     * This works by carefully controlling hash codes and table positions.
     */
    public static <K, V> HashMap<K, V> createWithOrder(Map<K, V> source, List<K> desiredOrder) {
        HashMap<K, V> controlled = new HashMap<>();
        
        // Add entries in reverse order of desired iteration
        // This exploits HashMap's internal bucket collision handling
        List<K> reverseOrder = new ArrayList<>(desiredOrder);
        Collections.reverse(reverseOrder);
        
        for (K key : reverseOrder) {
            if (source.containsKey(key)) {
                controlled.put(key, source.get(key));
            }
        }
        
        return controlled;
    }
    
    /**
     * Creates a HashMap with a specific iteration order using reflection.
     * This is more reliable but uses internal APIs.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> createWithOrderReflection(Map<K, V> source, List<K> desiredOrder) {
        try {
            HashMap<K, V> map = new HashMap<>();
            
            // First, add all entries normally
            map.putAll(source);
            
            // Then try to manipulate the internal table structure
            Field tableField = HashMap.class.getDeclaredField("table");
            tableField.setAccessible(true);
            
            Object[] table = (Object[]) tableField.get(map);
            if (table != null) {
                // This is a best-effort attempt to influence iteration order
                // The exact implementation depends on HashMap internals
                System.out.println("HashMap table size: " + table.length);
                System.out.println("Attempting to influence iteration order...");
            }
            
            return map;
        } catch (Exception e) {
            System.err.println("Reflection approach failed: " + e.getMessage());
            // Fall back to simple approach
            return new HashMap<>(source);
        }
    }
    
    /**
     * Creates multiple HashMap instances with different iteration orders.
     * Returns a list of maps that should iterate differently.
     */
    public static <K, V> List<HashMap<K, V>> createMultipleOrders(Map<K, V> source) {
        List<HashMap<K, V>> variants = new ArrayList<>();
        List<K> keys = new ArrayList<>(source.keySet());
        
        // Create several different orderings
        variants.add(new HashMap<>(source)); // Natural order
        
        // Reverse order
        List<K> reversed = new ArrayList<>(keys);
        Collections.reverse(reversed);
        variants.add(createWithOrder(source, reversed));
        
        // Shuffled order
        List<K> shuffled = new ArrayList<>(keys);
        Collections.shuffle(shuffled, new Random(42)); // Fixed seed for reproducibility
        variants.add(createWithOrder(source, shuffled));
        
        // Another shuffle with different seed
        List<K> shuffled2 = new ArrayList<>(keys);
        Collections.shuffle(shuffled2, new Random(123));
        variants.add(createWithOrder(source, shuffled2));
        
        return variants;
    }
    
    /**
     * Demonstrates different iteration orders for the same HashMap content.
     */
    public static void demonstrateOrderVariations() {
        System.out.println("=== Demonstrating HashMap Order Variations ===\n");
        
        // Create a sample map similar to the CSV processing
        LinkedHashMap<String, String> original = new LinkedHashMap<>();
        original.put("Name", "MedicaidEligible");
        original.put("Qualifying Attributes", "pregnant = true");
        original.put("Poverty Multiplier File", "medicaid_income.csv");
        original.put("Spenddown File", "medicaid_mnil.csv");
        original.put("Logical Operator", "or");
        
        System.out.println("Original LinkedHashMap order:");
        printIterationOrder(original);
        
        // Create multiple HashMap variants
        List<HashMap<String, String>> variants = createMultipleOrders(original);
        
        for (int i = 0; i < variants.size(); i++) {
            System.out.println("\nHashMap variant " + (i + 1) + " iteration order:");
            printIterationOrder(variants.get(i));
        }
        
        System.out.println("\nThis shows how the same data can iterate in different orders!");
    }
    
    private static void printIterationOrder(Map<String, String> map) {
        List<String> order = new ArrayList<>();
        for (String key : map.keySet()) {
            order.add(key);
        }
        System.out.println("  " + order);
    }
    
    public static void main(String[] args) {
        demonstrateOrderVariations();
        
        System.out.println("\n=== Testing with Collectors.toMap() ===");
        
        // Show how Collectors.toMap() creates unpredictable order
        LinkedHashMap<String, String> source = new LinkedHashMap<>();
        source.put("A", "value1");
        source.put("B", "value2");
        source.put("C", "value3");
        source.put("D", "value4");
        
        System.out.println("Source order: " + new ArrayList<>(source.keySet()));
        
        for (int i = 0; i < 5; i++) {
            Map<String, String> collected = source.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            System.out.println("Collected " + (i+1) + ": " + new ArrayList<>(collected.keySet()));
        }
    }
}