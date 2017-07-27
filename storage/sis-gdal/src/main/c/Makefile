#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Compile the native part of the bindings to Proj.4.  This build process requires the Proj.4 library to be installed
# on the current platform, preferably by the platform packaging mechanism (e.g. synaptic packages on Ubuntu, macport
# on MacOS, etc). The Proj.4 header files are expected to be found either in the "/usr/local/include" directory (for
# example on Linux distributions), or "/opt/local/include" on some other platforms.
#
# HOW TO BUILD:
# C/C++ compilation is currently not part of Maven build. Compilation shall be done manually like below:
#
#     cd storage/sis-gdal/src/main/c
#     make
#
# The executable will be created in the "storage/sis-gdal/src/resources/native/<platform>/" directory for inclusion
# in the JAR file. Only the executable for the current platform will be updated. If the JNI have been updated, then
# 'make' will need to be executed on all supported platforms.
#
# GCC options reminder:
#    -shared		Produce a shared object which can then be linked with other objects to form an executable.
#    -shared-libgcc	Use the shared version of libgcc. Recommended for JNI when exceptions may be thrown from gcc.
#    -fpic		Generate position-independent code suitable for use in a shared library.
#    -O2		Performs nearly all supported optimizations that do not involve a space-speed tradeoff.
#
FLAG   = -shared -shared-libgcc -fpic -O2
LIB    = -lm -ldl -lproj
SRC    = org_apache_sis_storage_gdal_PJ.c
OUT    = libproj-binding.so

ifndef JAVA_HOME
    $(error JAVA_HOME is not set.)
endif

INCLUDE_DIR = -I$(JAVA_HOME)/include -I/usr/local/include -I/opt/local/include

ifndef OS
    OS = $(shell uname)
endif

ifeq ($(OS),Windows_NT)
    OUTDIR = window
endif

ifeq ($(OS),Linux)
    INCLUDE_DIR += -I$(JAVA_HOME)/include/linux
    OUTDIR = linux
endif

ifeq ($(OS),Darwin)
    INCLUDE_DIR += -I$(JAVA_HOME)/include/darwin
    LIB += -L/opt/local/lib
    OUTDIR = darwin
endif

ifndef OUTDIR
    $(error The $(OS) platform is not supported by this makefile.)
endif

# The $(LIB) parameter should be last (it way work anywhere,
# but putting it last allows more predictable results).
all:
	gcc $(FLAG) $(INCLUDE_DIR) -o ../resources/native/$(OUTDIR)/$(OUT) $(SRC) $(LIB)
