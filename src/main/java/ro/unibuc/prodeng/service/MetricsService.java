package ro.unibuc.prodeng.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.repository.PartRepository;
import ro.unibuc.prodeng.repository.ServiceOrderRepository;

@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final Counter userCreatedCounter;
    private final Timer userLookupTimer;

    public MetricsService(MeterRegistry registry,
                          ServiceOrderRepository serviceOrderRepository,
                          PartRepository partRepository) {
        this.registry = registry;

        // 1. Business: total user registrations
        this.userCreatedCounter = Counter.builder("app_users_created_total")
                .description("Total number of users created")
                .tag("type", "business")
                .register(registry);

        // 2. Performance: user lookup duration
        this.userLookupTimer = Timer.builder("app_user_lookup_duration_seconds")
                .description("Time taken to look up a user by ID")
                .register(registry);

        // 4. Resource: live count of IN_PROGRESS service orders
        Gauge.builder("app_service_orders_active", serviceOrderRepository,
                        repo -> repo.findByStatus(OrderStatus.IN_PROGRESS).size())
                .description("Number of currently active (IN_PROGRESS) service orders")
                .register(registry);

        // 5. Domain-specific: total parts across all inventory
        Gauge.builder("app_parts_total_stock", partRepository,
                        repo -> repo.findAll().stream().mapToInt(p -> p.availableStock()).sum())
                .description("Total number of parts in stock across all inventory")
                .register(registry);
    }

    public void recordUserCreated() {
        userCreatedCounter.increment();
    }

    public Timer getUserLookupTimer() {
        return userLookupTimer;
    }

    // 3. Error: errors by type (dynamically tagged so each type gets its own time series)
    public void recordError(String errorType) {
        registry.counter("app_errors_total", "type", errorType).increment();
    }
}
