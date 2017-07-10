FLAG=-shared -fPIC
INCLUDE_DIR=-I$(JAVA_HOME)include -I$(JAVA_HOME)include/linux -I/home/amiotj/Documents/geomatys/proj.4/src
LIB=-lm -ldl -lproj
SRC=c/org_apache_sis_storage_gdal_PJ.c
OUT=libproj-binding.so


all:
	gcc $(FLAG) -o $(OUT) $(SRC) $(INCLUDE_DIR) $(LIB)
	mv $(OUT) resources/
