package com.lektralabs.thrones.pallbearer.datetime.jaxrs;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import org.joda.time.DateTime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class DateTimeParameterConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (DateTime.class.equals(rawType)) {
            final DateTimeParameterConverter dateTimeParameterConverter = new DateTimeParameterConverter();
            return (ParamConverter<T>) dateTimeParameterConverter;
        }
        return null;
    }

}