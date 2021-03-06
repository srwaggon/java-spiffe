package io.spiffe.provider;

import io.spiffe.exception.SocketEndpointAddressException;
import io.spiffe.exception.X509SourceException;
import io.spiffe.provider.exception.SpiffeProviderException;
import io.spiffe.svid.x509svid.X509SvidSource;
import io.spiffe.workloadapi.DefaultX509Source;
import io.spiffe.workloadapi.X509Source;
import lombok.NonNull;
import lombok.val;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import java.security.KeyStore;

/**
 * Implementation of a {@link KeyManagerFactorySpi} to create a {@link KeyManager} that is backed by the Workload API.
 * <p>
 * The Java Security API will call <code>engineGetKeyManagers()</code> to get an instance of a KeyManager.
 * This KeyManager instance is injected with an {@link DefaultX509Source} to obtain the latest X.509 SVIDs updates
 * from the Workload API.
 *
 * @see SpiffeSslContextFactory
 * @see X509SvidSource
 * @see X509SourceManager
 * @see SpiffeSslContextFactory
 */
public final class SpiffeKeyManagerFactory extends KeyManagerFactorySpi {

    /**
     * Default method for creating the KeyManager, uses an {@link DefaultX509Source} instance
     * that is handled by the Singleton {@link X509SourceManager}
     *
     * @throws SpiffeProviderException in case there is an error setting up the X.509 source
     */
    @Override
    protected KeyManager[] engineGetKeyManagers() {
        val x509Source = getX509Source();
        val spiffeKeyManager = new SpiffeKeyManager(x509Source);
        return new KeyManager[]{spiffeKeyManager};
    }

    /**
     * Creates a new key manager and initializes it with the given X.509 SVID source.
     *
     * @param x509SvidSource an instance of a {@link X509SvidSource}
     * @return an array with an instance of a {@link KeyManager}
     */
    public KeyManager[] engineGetKeyManagers(@NonNull final X509SvidSource x509SvidSource) {
        val spiffeKeyManager = new SpiffeKeyManager(x509SvidSource);
        return new KeyManager[]{spiffeKeyManager};
    }

    @Override
    protected void engineInit(final KeyStore keyStore, final char[] chars) {
        //no implementation needed
    }

    @Override
    protected void engineInit(final ManagerFactoryParameters managerFactoryParameters) {
        //no implementation needed
    }

    private X509Source getX509Source() {
        try {
            return X509SourceManager.getX509Source();
        } catch (X509SourceException e) {
            throw new SpiffeProviderException("The X.509 source could not be created", e);
        } catch (SocketEndpointAddressException e) {
            throw new SpiffeProviderException("The Workload API Socket endpoint address configured is not valid", e);
        }
    }
}
