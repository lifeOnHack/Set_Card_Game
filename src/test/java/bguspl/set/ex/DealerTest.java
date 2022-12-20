package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerTest {
    
    private Dealer dealer;
    private Config config;
    @Mock
    private Table table;
    @Mock
    private Player[] players;
    @Mock
    private Logger logger;
    @Mock
    private UserInterface ui;
    @Mock
    private Util util;
    

    @BeforeEach
    void setUp(){
        config = new Config(logger, "");
        Env env = new Env(logger, config, ui, util);
        dealer = new Dealer(env, table, players);
    }

    @Test
    void addCheckReq_ab(){
        
    }
}
