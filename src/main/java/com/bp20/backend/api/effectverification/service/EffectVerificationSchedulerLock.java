package com.bp20.backend.api.effectverification.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@Profile("!mock")
@RequiredArgsConstructor
public class EffectVerificationSchedulerLock {

    private static final Logger log = LoggerFactory.getLogger(
            EffectVerificationSchedulerLock.class
    );
    private static final String LOCK_NAME = "bp20-effect-verification-scheduler";

    private final DataSource dataSource;

    public boolean runWithLock(Runnable task) {
        try (Connection connection = dataSource.getConnection()) {
            if (!acquire(connection)) {
                return false;
            }

            try {
                task.run();
                return true;
            } finally {
                release(connection);
            }
        } catch (SQLException exception) {
            log.error("Failed to manage effect verification scheduler lock", exception);
            return false;
        }
    }

    private boolean acquire(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT GET_LOCK(?, 0)"
        )) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    private void release(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT RELEASE_LOCK(?)"
        )) {
            statement.setString(1, LOCK_NAME);
            statement.executeQuery();
        } catch (SQLException exception) {
            log.warn("Failed to release effect verification scheduler lock", exception);
        }
    }
}
