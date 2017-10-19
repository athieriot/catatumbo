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

import com.jmethods.catatumbo.Embedded;
import com.jmethods.catatumbo.Entity;
import com.jmethods.catatumbo.Identifier;

/**
 * @author Aurelien Thieriot
 *
 */
@Entity(kind = "generic")
public class ZipCodeWrapper {

	@Identifier
	private long id;

	@Embedded
	private WrappedZipCode wrappedZipCode;

	public ZipCodeWrapper() {

	}

	public ZipCodeWrapper(long id, WrappedZipCode wrappedZipCode) {
		this.id = id;
		this.wrappedZipCode = wrappedZipCode;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	public WrappedZipCode getWrappedZipCode() {
		return wrappedZipCode;
	}

	public void setWrappedZipCode(WrappedZipCode wrappedZipCode) {
		this.wrappedZipCode = wrappedZipCode;
	}
}
