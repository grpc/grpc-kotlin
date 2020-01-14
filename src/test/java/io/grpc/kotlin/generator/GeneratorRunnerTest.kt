package io.grpc.kotlin.generator

import com.google.common.io.MoreFiles
import com.google.common.io.Resources
import com.google.common.jimfs.Jimfs
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protos.proto2.compiler.Plugin.CodeGeneratorRequest
import com.google.protos.proto2.compiler.Plugin.CodeGeneratorResponse
import io.grpc.examples.helloworld.HelloWorldProto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files

@RunWith(JUnit4::class)
class GeneratorRunnerTest {
  companion object {
    private const val HELLO_WORLD_DESCRIPTOR_SET_FILE_NAME = "hello-world-descriptor-set.proto.bin"
    private const val OUTPUT_DIR_NAME = "output"
    private val helloWorldDescriptor = HelloWorldProto.getDescriptor().toProto()
    private fun String.removeTrailingWhitespace(): String = lines().joinToString("\n") { it.trimEnd() }
  }

  @Test
  fun runnerCommandLine() {
    val fileSystem = Jimfs.newFileSystem()

    Files.write(
      fileSystem.getPath(HELLO_WORLD_DESCRIPTOR_SET_FILE_NAME),
      FileDescriptorSet.newBuilder()
        .addFile(helloWorldDescriptor)
        .build()
        .toByteArray()
    )

    GeneratorRunner.mainAsCommandLine(
      arrayOf(
        OUTPUT_DIR_NAME,
        HELLO_WORLD_DESCRIPTOR_SET_FILE_NAME,
        "--",
        HELLO_WORLD_DESCRIPTOR_SET_FILE_NAME
      ),
      fileSystem
    )

    val outputFile =
      fileSystem
        .getPath(OUTPUT_DIR_NAME, "io/grpc/examples/helloworld")
        .resolve("HelloWorldProtoGrpcKt.kt")
    assertThat(MoreFiles.asCharSource(outputFile, Charsets.UTF_8).read().removeTrailingWhitespace())
      .isEqualTo(
        Resources.toString(
          Resources.getResource("io/grpc/kotlin/generator/HelloWorldProtoGrpcKt.expected"),
          Charsets.UTF_8
        )
      )
  }

  @Test
  fun runnerProtocPlugin() {
    val output = ByteString.newOutput()
    GeneratorRunner.mainAsProtocPlugin(
      CodeGeneratorRequest.newBuilder()
        .addProtoFile(helloWorldDescriptor)
        .addFileToGenerate(helloWorldDescriptor.name)
        .build()
        .toByteString()
        .newInput(),
      output
    )

    val expectedFileContents =
      Resources.toString(
        Resources.getResource("io/grpc/kotlin/generator/HelloWorldProtoGrpcKt.expected"),
        Charsets.UTF_8
      )

    val result = CodeGeneratorResponse.parseFrom(output.toByteString())
    assertThat(result.fileList.single().content.removeTrailingWhitespace())
      .isEqualTo(expectedFileContents)
  }
}