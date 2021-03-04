package org.apache.sis.storage;

import org.apache.sis.internal.storage.URIDataStore;
import org.opengis.parameter.ParameterDescriptorGroup;

public class FailingProvider extends DataStoreProvider {

    private static final String NAME = "ALWAYS_FAIL";

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return URIDataStore.Provider.descriptor(NAME);
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        throw new SystematicFailure("I always fail. I'm here to ensure Datastore discovery is robust to arbitrary provider errors");
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("Not meant to be supported");
    }

    public static class SystematicFailure extends RuntimeException {
        SystematicFailure(String message) {
            super(message);
        }
    }
}
