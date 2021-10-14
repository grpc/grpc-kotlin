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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colors.background) {
                val url = URL(stringResource(R.string.server_url))
                GreeterEffect(url)
            }
        }
    }
}

@Composable
fun GreeterEffect(url: URL) {

    val channel = remember {
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

    val greeter = GreeterGrpcKt.GreeterCoroutineStub(channel)

    val responseState = remember { mutableStateOf("") }

    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            channel.shutdownNow()
            scope.cancel()
        }
    }

    Greeter(responseState.value) { name ->
        scope.launch {
            try {
                val request = helloRequest { this.name = name }
                val response = greeter.sayHello(request)
                responseState.value = response.message
            } catch (e: Exception) {
                responseState.value = e.message ?: "Unknown Error"
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun Greeter(response: String, greet: (String) -> Unit) {

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
        Text(stringResource(R.string.name_hint), modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(nameState.value, { nameState.value = it })

        Button({ greet(nameState.value.text) }, Modifier.padding(10.dp)) {
        Text(stringResource(R.string.send_request))
    }

        if (response.isNotEmpty()) {
            Text(stringResource(R.string.server_response), modifier = Modifier.padding(top = 10.dp))
            Text(response)
        }
    }
}
