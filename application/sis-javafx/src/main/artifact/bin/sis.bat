@echo off

REM Licensed to the Apache Software Foundation (ASF) under one or more
REM contributor license agreements.  See the NOTICE file distributed with
REM this work for additional information regarding copyright ownership.
REM The ASF licenses this file to you under the Apache License, Version 2.0
REM (the "License"); you may not use this file except in compliance with the
REM License.  You may obtain a copy of the License at
REM
REM    http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.


SET BASE_DIR=%~dp0\..
SET SIS_DATA=%BASE_DIR%\data

REM Execute SIS with any optional JAR that the user may put in the 'lib' directory.
java -classpath "%BASE_DIR%\lib\sis-console-1.x-SNAPSHOT.jar"^
     -Djava.util.logging.config.class=org.apache.sis.util.logging.Initializer^
     -Djava.util.logging.config.file="%BASE_DIR%\conf\logging.properties"^
     -Dderby.stream.error.file="%BASE_DIR%\log\derby.log"^
     org.apache.sis.console.Command %SIS_OPTS% %*
