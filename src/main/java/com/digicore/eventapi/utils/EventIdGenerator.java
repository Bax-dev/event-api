package com.digicore.eventapi.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class EventIdGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        Long next = (Long) session
                .createNativeQuery("SELECT nextval('event_id_seq')", Long.class)
                .getSingleResult();
        return String.format("EVT-%04d", next);
    }
}
