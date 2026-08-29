package ru.nyansus.mc.ethos;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void localizedMessagesRenderMiniMessageFormatting() {
        String english = plugin.getMessages().get("en", "title.list-header");
        String russian = plugin.getMessages().get("ru", "title.list-header");

        assertFalse(english.contains("<dark_gray>"));
        assertFalse(russian.contains("<dark_gray>"));
        assertTrue(english.contains("\u00a78"));
        assertTrue(russian.contains("\u00a78"));
    }
}
