package io.grpc.examples.helloworld

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = URL(resources.getString(R.string.server_url))
        val greeterService = GreeterService(url) // todo: better resource open & closing

        setContent {
            Surface(color = MaterialTheme.colors.background) {
                Greeter(greeterService)
            }
        }
    }
}

class GreeterService(url: URL) : Closeable {
    val responseState = mutableStateOf("")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val channel = let {
        val port = if (url.port == -1) url.defaultPort else url.port

        println("Connecting to ${url.host}:$port")

        val builder = ManagedChannelBuilder.forAddress(url.host, port)
        if (url.protocol == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        builder.executor(Dispatchers.Default.asExecutor()).build()
    }

    private val greeter = GreeterGrpcKt.GreeterCoroutineStub(channel)

    fun sayHello(name: String) = scope.launch {
        try {
            val request = helloRequest { this.name = name }
            val response = greeter.sayHello(request)
            responseState.value = response.message
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    override fun close() {
        channel.shutdownNow()
        scope.cancel()
    }
}

@Composable
fun Greeter(greeterService: GreeterService) {

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
        Text(stringResource(R.string.name_hint), modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(nameState.value, { nameState.value = it })

        Button({ greeterService.sayHello(nameState.value.text) }, Modifier.padding(10.dp)) {
        Text(stringResource(R.string.send_request))
    }

        if (greeterService.responseState.value.isNotEmpty()) {
            Text(stringResource(R.string.server_response), modifier = Modifier.padding(top = 10.dp))
            Text(greeterService.responseState.value)
        }
    }
}
