package ru.nyansus.mc.domya_fate;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DomyaFateTest {

    private ServerMock server;
    private DomyaFate plugin;

    @Before
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DomyaFate.class);
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
