package com.digicore.eventapi.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class BookingIdGenerator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        Long next = (Long) session
                .createNativeQuery("SELECT nextval('booking_id_seq')", Long.class)
                .getSingleResult();
        return String.format("BK-%04d", next);
    }
}
