package io.grpc.examples.helloworld

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.io.Closeable

class MainActivity : ComponentActivity() {

    private val uri by lazy { Uri.parse(resources.getString(R.string.server_url)) }
    private val greeterService by lazy { GreeterRCP(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colors.background) {
                Greeter(greeterService)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        greeterService.close()
    }
}

class GreeterRCP(uri: Uri) : Closeable {
    val responseState = mutableStateOf("")

    private val channel = let {
        println("Connecting to ${uri.host}:${uri.port}")

        val builder = ManagedChannelBuilder.forAddress(uri.host, uri.port)
        if (uri.scheme == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        builder.executor(Dispatchers.IO.asExecutor()).build()
    }

    private val greeter = GreeterGrpcKt.GreeterCoroutineStub(channel)

    suspend fun sayHello(name: String) {
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
    }
}

@Composable
fun Greeter(greeterRCP: GreeterRCP) {

    val scope = rememberCoroutineScope()

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
        Text(stringResource(R.string.name_hint), modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(nameState.value, { nameState.value = it })

        Button({ scope.launch { greeterRCP.sayHello(nameState.value.text) } }, Modifier.padding(10.dp)) {
        Text(stringResource(R.string.send_request))
    }

        if (greeterRCP.responseState.value.isNotEmpty()) {
            Text(stringResource(R.string.server_response), modifier = Modifier.padding(top = 10.dp))
            Text(greeterRCP.responseState.value)
        }
    }
}
