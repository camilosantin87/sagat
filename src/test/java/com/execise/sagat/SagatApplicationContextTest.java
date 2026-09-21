package com.execise.sagat;

import com.execise.sagat.notification.controller.NotificationController;
import com.execise.sagat.notification.entity.Notification;
import com.execise.sagat.notification.repository.NotificationRepository;
import com.execise.sagat.notification.services.NotificationDispatcher;
import com.execise.sagat.notification.services.NotificationService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SagatApplicationContextTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void loadsTheApplicationAndItsCoreBeans() {
        assertNotNull(context);
        assertNotNull(context.getBean(NotificationController.class));
        assertNotNull(context.getBean(NotificationService.class));
        assertNotNull(context.getBean(NotificationDispatcher.class));
        assertNotNull(context.getBean(NotificationRepository.class));
    }

    @Test
    void registersNotificationAsJpaEntity() {
        assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                .anyMatch(entity -> entity.getJavaType().equals(Notification.class)));
    }
}
