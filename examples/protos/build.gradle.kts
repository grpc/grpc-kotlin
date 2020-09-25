// todo: maybe use variants / configurations to do both stub & stub-lite here

// Note: We use the java-library plugin to get the protos into the artifact for this subproject
// because there doesn't seem to be an better way.
plugins {
    `java-library`
}

java {
    sourceSets.getByName("main").resources.srcDir("src/main/proto")
}
