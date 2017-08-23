/*
 * Copyright 2016 Sai Pullabhotla.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jmethods.catatumbo.impl;

/**
 * Contains embedded field metadata and value
 *
 * @author Aurelien Thieriot
 */
public class EmbeddedFieldDescriptor {

    private final EmbeddedMetadata metadata;
    private final Object value;

    private EmbeddedFieldDescriptor(EmbeddedMetadata metadata, Object value) {
        this.metadata = metadata;
        this.value = value;
    }

    /**
     * Create a new instance of <code>EmbeddedFieldDescriptor</code>
     *
     * @param metadata The embedded field metadata
     * @param value The actual value of this field
     */
    public static EmbeddedFieldDescriptor of(EmbeddedMetadata metadata, Object value) {
        return new EmbeddedFieldDescriptor(metadata, value);
    }

    public EmbeddedMetadata metadata() {
        return metadata;
    }

    public Object value() {
        return value;
    }
}
