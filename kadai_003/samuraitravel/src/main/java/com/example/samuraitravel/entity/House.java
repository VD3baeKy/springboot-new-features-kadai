package com.example.samuraitravel.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;

@Getter
@Entity
@Table(name = "houses")
@Data
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "image_name")
    private String imageName;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private Integer price;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;
    
    
    @Override
    public String toString() {  
	    return "House{id='" + id + 
	    		",' name='" + name + 
	    		"', description='" + description +
	    		"', price='" + price + 
	    		"', capacity='" + capacity + 
	    		"', postalCode='" + postalCode + 
	    		"', address='" + address + 
	    		"', phoneNumber='" + phoneNumber + 
	    		"', image_name='" + imageName + 
	    		"', created_at='" + createdAt + 
	    		"', updated_at='" + updatedAt + 
	    		"'}"; // 必要なフィールドのみを表示
    }
    
}
