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

package com.jmethods.catatumbo.entities;

import com.jmethods.catatumbo.DatastoreKey;
import com.jmethods.catatumbo.Embedded;
import com.jmethods.catatumbo.Entity;
import com.jmethods.catatumbo.EntityConstructor;
import com.jmethods.catatumbo.Exploded;
import com.jmethods.catatumbo.Identifier;
import com.jmethods.catatumbo.Imploded;
import com.jmethods.catatumbo.Key;
import com.jmethods.catatumbo.Property;
import com.jmethods.catatumbo.PropertyOverride;
import com.jmethods.catatumbo.PropertyOverrides;

import java.util.Objects;

/**
 * @author Aurelien Thieriot
 *
 */
@Entity
public class ImmutableWithNotEnoughArgs {

	@Identifier
	private final String name;

	private String surname;

	@EntityConstructor
	public ImmutableWithNotEnoughArgs(@Property(name = "name") String name) {
		this.name = name;
		this.surname = "Doe";
	}

	public String getName() {
		return name;
	}

	public String getSurname() {
		return surname;
	}
}
