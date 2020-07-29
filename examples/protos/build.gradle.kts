// todo: maybe use variants / configurations to do both stub & stub-lite here

plugins {
    `java-library`
}

java {
    sourceSets.getByName("main").resources.srcDir("src/main/proto")
}
