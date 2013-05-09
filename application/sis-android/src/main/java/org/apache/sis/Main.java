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
package org.apache.sis;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


/**
 * Entry point of Android application, almost empty for now (experimental).
 */
public class Main extends Activity {

    private static String TAG = "sis-android";

    /**
     * Invoked when the activity is first created.
     *
     * @param savedInstanceState The data supplied to {@link #onSaveInstanceState(Bundle)}
     *        when the previous instance has been shut down, or {@code null} if none.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        setContentView(R.layout.main);
    }
}
