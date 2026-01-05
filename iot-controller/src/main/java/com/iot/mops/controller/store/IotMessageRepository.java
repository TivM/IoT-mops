package com.iot.mops.controller.store;

import com.iot.mops.common.dto.IotMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IotMessageRepository extends MongoRepository<IotMessage, String> {
}
