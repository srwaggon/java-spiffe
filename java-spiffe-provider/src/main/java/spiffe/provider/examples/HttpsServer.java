package spiffe.provider.examples;

import lombok.val;
import spiffe.exception.SocketEndpointAddressException;
import spiffe.provider.SpiffeSslContextFactory;
import spiffe.provider.SpiffeSslContextFactory.SslContextOptions;
import spiffe.workloadapi.X509Source;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Example of a simple HTTPS Server backed by the Workload API to get the X509 certificates
 * and trusted bundles.
 * <p>
 * The purpose of this class is to show the use of the {@link SpiffeSslContextFactory} to create
 * a {@link SSLContext} that uses X509-SVID provided by a Workload API. The SSLContext uses the
 * {@link spiffe.provider.SpiffeKeyManager} and {@link spiffe.provider.SpiffeTrustManager} for
 * providing certificates and doing chain and SPIFFE ID validation.
 * To run this example, Spire should be running, SPIFFE_ENDPOINT_SOCKET env variable should be
 * defined, and a property ssl.spiffe.accept should be defined in the java.security having a
 * spiffe id from a client workload.
 */
public class HttpsServer {

    int port;

    public static void main(String[] args) {
        HttpsServer httpsServer = new HttpsServer(4000);
        try {
            httpsServer.run();
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error starting HttpsServer");
        }
    }

    HttpsServer(int port ) {
        this.port = port;
    }

    void run() throws IOException, KeyManagementException, NoSuchAlgorithmException {
        X509Source x509Source = null;
        try {
            x509Source = X509Source.newSource();
        } catch (SocketEndpointAddressException e) {
            throw new RuntimeException(e);
        }

        val sslContextOptions = SslContextOptions
                .builder()
                .x509Source(x509Source)
                .build();
        SSLContext sslContext = SpiffeSslContextFactory.getSslContext(sslContextOptions);

        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

        // Server will validate Client chain and SPIFFE ID
        sslServerSocket.setNeedClientAuth(true);

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        new WorkloadThread(sslSocket, x509Source).start();
    }
}


