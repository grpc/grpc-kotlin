package io.grpc.testing.integration

import com.squareup.okhttp.ConnectionSpec
import io.grpc.testing.integration.Messages.SimpleRequest
import io.grpc.testing.integration.Messages.SimpleResponse
import io.grpc.testing.integration.Messages.EchoStatus
import io.grpc.ManagedChannel
import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyServerBuilder
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.okhttp.internal.Platform
import io.netty.handler.ssl.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import java.net.InetSocketAddress


@RunWith(JUnit4::class)
class CoroutineClientRetryTest: AbstractCoroutinesInteropTest() {

    override val serverBuilder: ServerBuilder<*>?

    init {
        serverBuilder = try {
            var sslProvider = SslContext.defaultServerProvider()
            if (sslProvider == SslProvider.OPENSSL && !OpenSsl.isAlpnSupported()) {
                // OkHttp only supports Jetty ALPN on OpenJDK. So if OpenSSL doesn't support ALPN, then we
                // are forced to use Jetty ALPN for Netty instead of OpenSSL.
                sslProvider = SslProvider.JDK
            }
            val contextBuilder = SslContextBuilder
                .forServer(TestUtils.loadCert("server1.pem"), TestUtils.loadCert("server1.key"))
            GrpcSslContexts.configure(contextBuilder, sslProvider)
            contextBuilder.ciphers(TestUtils.preferredTestCiphers(), SupportedCipherSuiteFilter.INSTANCE)
            NettyServerBuilder.forPort(0)
                .flowControlWindow(65 * 1024)
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .sslContext(contextBuilder.build())
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun loadConscrypt() {
            // Load conscrypt if it is available. Either Conscrypt or Jetty ALPN needs to be available for
            // OkHttp to negotiate.
            TestUtils.installConscryptIfAvailable()
        }
    }

    @Test
    fun retry() {
        runBlocking {
            val errorCode = 2
            val errorMessage = "test status message"
            val responseStatus = EchoStatus.newBuilder()
                .setCode(errorCode)
                .setMessage(errorMessage)
                .build()
            val simpleRequest = SimpleRequest.newBuilder()
                .setResponseStatus(responseStatus)
                .build()

            try {
                stub.unaryCall(simpleRequest)
            } catch (e: StatusException) {
                Assert.assertEquals(Status.UNKNOWN.code, e.status.code)
                Assert.assertEquals(errorMessage, e.status.description)
            }
        }
    }

    private fun createChannelBuilder(): OkHttpChannelBuilder? {
        val port = (listenAddress as InetSocketAddress).port
        val builder = OkHttpChannelBuilder.forAddress("localhost", port)
            .maxInboundMessageSize(MAX_MESSAGE_SIZE)
            .connectionSpec(ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .build())
            .enableRetry()
            .defaultServiceConfig(getRetryingServiceConfig())
            .overrideAuthority(Util.authorityFromHostAndPort(
                TestUtils.TEST_SERVER_HOST, port))
        try {
            builder.sslSocketFactory(TestUtils.newSslSocketFactoryForCa(Platform.get().provider,
                TestUtils.loadCert("ca.pem")))
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException(e)
        }
        return builder
    }

    override fun createChannel(): ManagedChannel {
        return createChannelBuilder()!!.build()
    }

    private fun getRetryingServiceConfig(): Map<String, Any> {
        val config = hashMapOf<String, Any>()

        val name = mutableListOf<Map<String, Any>>()
        name.add(
            mapOf(
                "service" to "grpc.testing.TestService",
                "method" to "UnaryCall"
            )
        )

        val retryPolicy = hashMapOf<String, Any>()
        retryPolicy["maxAttempts"] = 5.0
        retryPolicy["initialBackoff"] = "0.5s"
        retryPolicy["maxBackoff"] = "30s"
        retryPolicy["backoffMultiplier"] = 2.0
        retryPolicy["retryableStatusCodes"] = listOf("UNKNOWN")

        val methodConfig = mutableListOf<Map<String, Any>>()
        val serviceConfig = hashMapOf<String, Any>()

        serviceConfig["name"] = name
        serviceConfig["retryPolicy"] = retryPolicy

        methodConfig.add(serviceConfig)

        config["methodConfig"] = methodConfig

        return config
    }
}
