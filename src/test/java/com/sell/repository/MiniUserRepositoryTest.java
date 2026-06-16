package com.sell.repository;

import com.sell.dataobject.MiniUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MiniUserRepositoryTest {

    @Autowired
    private MiniUserRepository repo;

    @Test
    public void saveAndFindByToken() {
        MiniUser u = new MiniUser();
        u.setOpenid("o_test_1");
        u.setSessionKey("sk");
        u.setToken("tok-123");
        u.setTokenExpireAt(new Date(System.currentTimeMillis() + 86400000L));
        repo.save(u);

        MiniUser found = repo.findByToken("tok-123").orElse(null);
        assertNotNull(found);
        assertEquals("o_test_1", found.getOpenid());
        assertEquals("sk", found.getSessionKey());
    }

    @Test
    public void findByToken_unknown_empty() {
        assertFalse(repo.findByToken("no-such").isPresent());
    }
}
