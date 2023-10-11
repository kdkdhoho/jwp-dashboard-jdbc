package com.techcourse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.techcourse.config.DataSourceConfig;
import com.techcourse.dao.UserDao;
import com.techcourse.dao.UserHistoryDao;
import com.techcourse.domain.User;
import com.techcourse.support.jdbc.init.DatabasePopulatorUtils;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class UserServiceTest {

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private UserDao userDao;

    @BeforeEach
    void setUp() {
        dataSource = DataSourceConfig.getInstance();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.userDao = new UserDao(jdbcTemplate);

        DatabasePopulatorUtils.execute(DataSourceConfig.getInstance());
        final var user = new User("gugu", "password", "hkkang@woowahan.com");
        userDao.insert(user);
    }

    @Test
    void testChangePassword() {
        // given
        final var userHistoryDao = new UserHistoryDao(jdbcTemplate);
        final var userService = new AppUserService(userDao, userHistoryDao);

        final var newPassword = "qqqqq";
        final var createBy = "gugu";

        // when
        userService.changePassword(1L, newPassword, createBy);

        // then
        final var actual = userService.findById(1L);
        assertThat(actual.getPassword()).isEqualTo(newPassword);
    }

    @Test
    void testTransactionRollback() {
        // given
        final var userHistoryDao = new MockUserHistoryDao(jdbcTemplate);
        final var appUserService = new AppUserService(userDao, userHistoryDao);
        final var txUserService = new TxUserService(appUserService, dataSource);

        final var newPassword = "newPassword";
        final var createBy = "gugu";

        // then
        assertThrows(DataAccessException.class,
                () -> txUserService.changePassword(1L, newPassword, createBy));

        final var actual = appUserService.findById(1L);
        assertThat(actual.getPassword()).isNotEqualTo(newPassword);
    }
}
