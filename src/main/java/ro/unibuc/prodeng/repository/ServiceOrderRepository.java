package ro.unibuc.prodeng.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.model.ServiceOrderEntity;

@Repository
public interface ServiceOrderRepository extends MongoRepository<ServiceOrderEntity, String> {
    List<ServiceOrderEntity> findByStatus(OrderStatus status);
    List<ServiceOrderEntity> findByMechanicIdAndStatus(String mechanicId, OrderStatus status);
}
