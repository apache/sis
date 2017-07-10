/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#pragma once

#if defined(_WIN32)
#include <Windows.h>
#elif defined (__unix__) || (defined (__APPLE__) && defined (__MACH__))
#include <dlfcn.h>
#else
#error Unsupported platform.
#endif


/*
 * Handler or pointer to a function of the shared library.
 */
#if defined(_WIN32)
    #define DL_HANDLER HMODULE
#else
    #define DL_HANDLER void*
#endif


/*
 * Opens a library of shared objects and prepares it for use.
 * The PATH parameter is a 'const char*'  to the library name.
 * The return value is an opaque 'DL_HANDLER' to the library.
 */
#if defined (_WIN32)
    #define DL_OPEN(PATH) LoadLibrary(PATH);
#else
    #define DL_OPEN(PATH) dlopen(PATH, RTLD_LAZY);
#endif


/*
 * Returns a pointer to the function of the given name.
 * The HANDLE parameter is the value returned by DL_OPEN
 * and the NAME parameter is a 'const char*' to the library name.
 * The return value is a 'void*'.
 */
#if defined(_WIN32)
    #define DL_FUNCTION(HANDLE, NAME) GetProcAddress(HANDLE, NAME);
#else
    #define DL_FUNCTION(HANDLE, NAME) dlsym(HANDLE, NAME);
#endif


/*
 * Closes a library after use.
 * The HANDLE parameter is the value returned by DL_OPEN.
 */
#if defined(_WIN32)
    #define DL_CLOSE(HANDLE) FreeLibrary(HANDLE)
#else
    #define DL_CLOSE(HANDLE) dlclose(HANDLE)
#endif
