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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.jmethods.catatumbo.EntityManagerException;
import com.jmethods.catatumbo.Mapper;
import com.jmethods.catatumbo.MapperFactory;

/**
 * Base class for holding the metadata about an entity's or embedded object's
 * field (e.g. identifier, key or property).
 *
 * @author Sai Pullabhotla
 */
public abstract class FieldMetadata {

	/**
	 * Reference to the field
	 */
	protected Field field;

	/**
	 * Read method (or getter method) for this field
	 */
	protected Method readMethod;

	/**
	 * Write method (or setter method) for this field
	 */
	protected Method writeMethod;

	/**
	 * Mapper for the field represented by this metadata
	 */
	protected Mapper mapper;

	/**
	 * Creates a new instance of <code>FieldMetadata</code>.
	 * 
	 * @param field
	 *            the field
	 *
	 */
	public FieldMetadata(Field field) {
		this.field = field;
		initializeMapper();
	}

	/**
	 * Returns the field.
	 * 
	 * @return the field.
	 */
	public Field getField() {
		return field;
	}

	/**
	 * Returns the field name.
	 *
	 * @return the field name.
	 */
	public String getName() {
		return field.getName();
	}

	/**
	 * Returns the read method (or getter method) for this field.
	 *
	 * @return the read method (or getter method) for this field.
	 */
	public Method getReadMethod() {
		return readMethod;
	}

	/**
	 * Sets the read method (or getter method).
	 *
	 * @param readMethod
	 *            the read method (or getter method).
	 */
	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	/**
	 * Returns the write method (or setter method).
	 *
	 * @return the write method (or setter method).
	 */
	public Method getWriteMethod() {
		return writeMethod;
	}

	/**
	 * Sets the write method (or setter method).
	 *
	 * @param writeMethod
	 *            the write method (or setter method).
	 */
	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}

	/**
	 * Returns the declared type of the field to which this metadata belongs.
	 * 
	 * @return the declared type of the field to which this metadata belongs.
	 */
	@SuppressWarnings("rawtypes")
	public Class getDeclaredType() {
		return field.getType();
	}

	/**
	 * Returns the {@link Mapper} associated with the field to which this
	 * metadata belongs.
	 * 
	 * @return he {@link Mapper} associated with the field to which this
	 *         metadata belongs.
	 */
	public Mapper getMapper() {
		return mapper;
	}

	/**
	 * Initializes the {@link Mapper} for this field.
	 */
	private void initializeMapper() {
		try {
			this.mapper = MapperFactory.getInstance().getMapper(field);
		} catch (Exception exp) {
			String message = String.format(
					"No suitable mapper found or error occurred creating a mapper for field %s in class %s",
					field.getName(), field.getDeclaringClass().getName());
			throw new EntityManagerException(message, exp);

		}
	}

}
