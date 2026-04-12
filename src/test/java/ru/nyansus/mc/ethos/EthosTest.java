package ru.nyansus.mc.ethos;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class EthosTest {

    private ServerMock server;
    private Ethos plugin;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Ethos.class);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void pluginLoads() {
        assertNotNull(plugin);
        assertNotNull(plugin.getMessages());
    }
}
