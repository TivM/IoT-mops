package com.iot.mops.ruleengine.store;

import com.iot.mops.common.dto.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AlertRepository extends MongoRepository<Alert, String> {
}
