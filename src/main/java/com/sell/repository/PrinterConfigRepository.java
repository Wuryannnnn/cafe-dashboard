package com.sell.repository;

import com.sell.dataobject.PrinterConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrinterConfigRepository extends JpaRepository<PrinterConfig, String> {

    List<PrinterConfig> findByStationAndEnabledTrue(Integer station);

    List<PrinterConfig> findByEnabledTrue();
}
