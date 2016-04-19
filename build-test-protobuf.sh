export SRC_DIR=./src/test/proto
export DST_DIR=./src/test/java
protoc -I=$SRC_DIR --java_out=$DST_DIR $SRC_DIR/transform.proto