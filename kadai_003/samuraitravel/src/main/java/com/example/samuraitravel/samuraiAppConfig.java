package com.example.samuraitravel;

import org.springframework.context.annotation.Configuration;

import com.example.samuraitravel.entity.House;
import com.example.samuraitravel.entity.Loves;
import com.example.samuraitravel.repository.FaboriteHouseDTO;

@Configuration
public class samuraiAppConfig {

    
    public FaboriteHouseDTO faboriteHouseDTO(Loves loves, House house) {
        return new FaboriteHouseDTO(loves, house);
    }
}
