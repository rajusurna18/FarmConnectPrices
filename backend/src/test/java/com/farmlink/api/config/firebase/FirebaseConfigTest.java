package com.farmlink.api.config.firebase;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.firebase.project-id=farmconnectprices",
    "app.firebase.use-emulator=true"
})
class FirebaseConfigTest {

    @Autowired
    private FirebaseProperties firebaseProperties;

    @Autowired(required = false)
    private FirebaseApp firebaseApp;

    @Autowired(required = false)
    private Firestore firestore;

    @Test
    void firebasePropertiesShouldLoadDefaults() {
        assertNotNull(firebaseProperties);
        assertEquals("farmconnectprices", firebaseProperties.getProjectId());
        assertTrue(firebaseProperties.isUseEmulator());
    }

    @Test
    void firebasePropertiesPrivateKeyFormattingShouldReplaceEscapedNewlines() {
        FirebaseProperties props = new FirebaseProperties();
        props.setPrivateKey("HEADER\\nLINE1\\nLINE2\\nFOOTER");
        assertEquals("HEADER\nLINE1\nLINE2\nFOOTER", props.getFormattedPrivateKey());
    }

    @Test
    void firebaseAppAndFirestoreBeansShouldBeInitializedInEmulatorMode() {
        assertNotNull(firebaseApp, "FirebaseApp bean should be initialized");
        assertNotNull(firestore, "Firestore bean should be initialized");
        assertEquals("farmconnectprices", firebaseApp.getOptions().getProjectId());
    }
}
