package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.model.CarEntity;
import ro.unibuc.prodeng.request.CreateCarRequest;
import ro.unibuc.prodeng.service.AutoServiceCatalogService;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    @Autowired
    private AutoServiceCatalogService catalogService;

    @PostMapping
    public ResponseEntity<CarEntity> createCar(@Valid @RequestBody CreateCarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCar(request));
    }

    @GetMapping
    public ResponseEntity<List<CarEntity>> getAllCars() {
        return ResponseEntity.ok(catalogService.getAllCars());
    }

    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<CarEntity>> getCarsByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(catalogService.getCarsByClientId(clientId));
    }
}
