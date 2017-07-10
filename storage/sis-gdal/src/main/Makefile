FLAG=-shared -fPIC
INCLUDE_DIR=-I$(JAVA_HOME)include -I$(JAVA_HOME)include/linux -I/home/amiotj/Documents/geomatys/proj.4/src
LIB=-lm -ldl

OUT=libproj-binding.so


all:
    gcc $(FLAG) -o $(OUT) c/jniproj.c $(INCLUDE_DIR) $(LIB)
    mv $(OUT) resources/
