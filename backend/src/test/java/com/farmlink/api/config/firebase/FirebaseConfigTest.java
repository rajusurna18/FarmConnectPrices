package com.farmlink.api.config.firebase;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
        assertEquals("farmconnectprices-dev", firebaseProperties.getProjectId());
        assertFalse(firebaseProperties.isUseEmulator());
    }

    @Test
    void firebasePropertiesPrivateKeyFormattingShouldReplaceEscapedNewlines() {
        FirebaseProperties props = new FirebaseProperties();
        props.setPrivateKey("HEADER\\nLINE1\\nLINE2\\nFOOTER");
        assertEquals("HEADER\nLINE1\nLINE2\nFOOTER", props.getFormattedPrivateKey());
    }

    @Test
    void firebaseAppAndFirestoreBeansShouldBeInitialized() {
        assertNotNull(firebaseApp, "FirebaseApp bean should be initialized");
        assertNotNull(firestore, "Firestore bean should be initialized");
        assertEquals("farmconnectprices-dev", firebaseApp.getOptions().getProjectId());
    }
}
