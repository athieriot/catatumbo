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

import java.util.Objects;

import com.jmethods.catatumbo.Embeddable;
import com.jmethods.catatumbo.Property;

/**
 * @author Sai Pullabhotla
 *
 */
@Embeddable
public class AnotherEmbeddable {

	private String field1;
	@Property(name = "FIELD2")
	private String field2;

	/**
	 * 
	 */
	public AnotherEmbeddable() {
		// TODO Auto-generated constructor stub
	}

	public AnotherEmbeddable(String field1, String field2) {
		this.field1 = field1;
		this.field2 = field2;
	}

	/**
	 * @return the field1
	 */
	public String getField1() {
		return field1;
	}

	/**
	 * @param field1
	 *            the field1 to set
	 */
	public void setField1(String field1) {
		this.field1 = field1;
	}

	/**
	 * @return the field2
	 */
	public String getField2() {
		return field2;
	}

	/**
	 * @param field2
	 *            the field2 to set
	 */
	public void setField2(String field2) {
		this.field2 = field2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AnotherEmbeddable)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		AnotherEmbeddable that = (AnotherEmbeddable) obj;
		return Objects.equals(this.field1, that.field1) && Objects.equals(this.field2, that.field2);
	}

	public boolean nullified() {
		return Objects.isNull(field1) && Objects.isNull(field2);
	}

	@Override
	public int hashCode() {
		return Objects.hash(field1, field2);
	}

}
