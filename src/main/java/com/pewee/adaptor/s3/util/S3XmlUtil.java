package com.pewee.adaptor.s3.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3 XML 序列化与反序列化工具类
 */
public class S3XmlUtil {

    private static final ConcurrentHashMap<Class<?>, JAXBContext> CONTEXT_CACHE = new ConcurrentHashMap<>();

    private static JAXBContext getContext(Class<?> clazz) {
        return CONTEXT_CACHE.computeIfAbsent(clazz, c -> {
            try {
                return JAXBContext.newInstance(c);
            } catch (Exception e) {
                throw new RuntimeException("Failed to init JAXBContext for " + c.getName(), e);
            }
        });
    }

    public static String toXml(Object obj) {
        if (obj == null) {
            return "";
        }
        try {
            JAXBContext context = getContext(obj.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            marshaller.marshal(obj, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to marshal XML", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromXml(InputStream is, Class<T> clazz) {
        if (is == null) {
            return null;
        }
        try {
            JAXBContext context = getContext(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (T) unmarshaller.unmarshal(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmarshal XML", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromXml(String xml, Class<T> clazz) {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }
        try {
            JAXBContext context = getContext(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (T) unmarshaller.unmarshal(new StringReader(xml));
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmarshal XML", e);
        }
    }
}
