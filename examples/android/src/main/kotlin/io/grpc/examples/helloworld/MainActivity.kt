package io.grpc.examples.helloworld

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.net.URL
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking

// todo: suspend funs
class MainActivity : AppCompatActivity() {
    private val logger = Logger.getLogger(this.javaClass.name)

    private fun channel(): ManagedChannel {
        val url = URL(resources.getString(R.string.server_url))
        val port = if (url.port == -1) url.defaultPort else url.port

        logger.info("Connecting to ${url.host}:$port")

        val builder = ManagedChannelBuilder.forAddress(url.host, port)
        if (url.protocol == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        return builder.executor(Dispatchers.Default.asExecutor()).build()
    }

    // lazy otherwise resources is null
    private val greeter by lazy { GreeterGrpcKt.GreeterCoroutineStub(channel()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)

        val nameText = findViewById<EditText>(R.id.name)
        nameText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                button.isEnabled = s.isNotEmpty()
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
            override fun afterTextChanged(s: Editable) { }
        })

        val responseText = findViewById<TextView>(R.id.response)

        fun sendReq() = runBlocking {
            try {
                val request = helloRequest { name = nameText.text.toString() }
                val response = greeter.sayHello(request)
                responseText.text = response.message
            } catch (e: Exception) {
                responseText.text = e.message
                e.printStackTrace()
            }
        }

        button.setOnClickListener {
            sendReq()
        }

        nameText.setOnKeyListener { _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                sendReq()
                true
            } else {
                false
            }
        }
    }
}
