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

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.ProjectionEntity;
import com.google.cloud.datastore.Value;
import com.jmethods.catatumbo.DefaultDatastoreKey;
import com.jmethods.catatumbo.EntityManagerException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import static com.jmethods.catatumbo.impl.IntrospectionUtils.selectConstructorFor;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * Converts Entities retrieved from the Cloud Datastore into Entity POJOs.
 *
 * @author Sai Pullabhotla
 */
public class Unmarshaller {

	/**
	 * Input - Native Entity to unmarshal, could be a ProjectionEntity or an
	 * Entity
	 */
	private final BaseEntity<?> nativeEntity;

	/**
	 * Output - unmarshalled object
	 */
	private Object entity;

	/**
	 * Entity metadata
	 */
	private final EntityMetadata entityMetadata;

	/**
	 * Creates a new instance of <code>Unmarshaller</code>.
	 * 
	 * @param nativeEntity
	 *            the native entity to unmarshal
	 * @param entityClass
	 *            the expected model type
	 */
	private Unmarshaller(BaseEntity<?> nativeEntity, Class<?> entityClass) {
		this.nativeEntity = nativeEntity;
		entityMetadata = EntityIntrospector.introspect(entityClass);

	}

	/**
	 * Unmarshals the given native Entity into an object of given type,
	 * entityClass.
	 * 
	 * @param <T>
	 *            target object type
	 * @param nativeEntity
	 *            the native Entity
	 * @param entityClass
	 *            the target type
	 * @return Object that is equivalent to the given native entity. If the
	 *         given <code>datastoreEntity</code> is <code>null</code>, returns
	 *         <code>null</code>.
	 */
	public static <T> T unmarshal(Entity nativeEntity, Class<T> entityClass) {
		return unmarshalBaseEntity(nativeEntity, entityClass);
	}

	/**
	 * Unmarshals the given native ProjectionEntity into an object of given
	 * type, entityClass.
	 * 
	 * @param <T>
	 *            target object type
	 * @param nativeEntity
	 *            the native Entity
	 * @param entityClass
	 *            the target type
	 * @return Object that is equivalent to the given native entity. If the
	 *         given <code>datastoreEntity</code> is <code>null</code>, returns
	 *         <code>null</code>.
	 */
	public static <T> T unmarshal(ProjectionEntity nativeEntity, Class<T> entityClass) {
		return unmarshalBaseEntity(nativeEntity, entityClass);
	}

	/**
	 * Unmarshals the given BaseEntity and returns the equivalent model object.
	 * 
	 * @param nativeEntity
	 *            the native entity to unmarshal
	 * @param entityClass
	 *            the target type of the model class
	 * @return the model object
	 */
	private static <T> T unmarshalBaseEntity(BaseEntity<?> nativeEntity, Class<T> entityClass) {
		if (nativeEntity == null) {
			return null;
		}
		Unmarshaller unmarshaller = new Unmarshaller(nativeEntity, entityClass);
		return unmarshaller.unmarshal();
	}

	/**
	 * Unmarshals the given Datastore Entity and returns the equivalent Entity
	 * POJO.
	 *
	 * @param <T>
	 *            type
	 * @return the entity POJO
	 */
	@SuppressWarnings("unchecked")
	private <T> T unmarshal() {

		try {
			entity = instantiateEntity(
					entityMetadata,
					new ArrayList<FieldDescriptor>() {{
						add(unmarshalIdentifier());
						add(unmarshalKey());
						add(unmarshalParentKey());
						addAll(unmarshalProperties());
					}},
					unmarshalEmbeddedFields()
			);
			return (T) entity;
		} catch (EntityManagerException exp) {
			throw exp;
		} catch (Throwable t) {
			throw new EntityManagerException(t.getMessage(), t);
		}
	}

	/**
	 * Instantiates the entity.
	 */
	public static Object instantiateEntity(
			MetadataBase metadata,
			List<FieldDescriptor> descriptors,
			List<EmbeddedFieldDescriptor> embeddedDescriptors) throws Throwable {
		descriptors.removeIf(Objects::isNull);
		embeddedDescriptors.removeIf(Objects::isNull);

		if (metadata.isImmutable()) {
			return instantiateImmutableEntity(metadata, descriptors, embeddedDescriptors);
		} else {
			return instantiateMutableEntity(metadata, descriptors, embeddedDescriptors);
		}
	}

	private static Object instantiateImmutableEntity(MetadataBase metadata,
													 List<FieldDescriptor> descriptors,
													 List<EmbeddedFieldDescriptor> embeddedDescriptors
	) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		Map<String, Object> fields = new HashMap<>();

		for (FieldDescriptor descriptor : descriptors) {
            fields.put(descriptor.metadata().getField().getName(), descriptor.value());
        }

		for (EmbeddedFieldDescriptor embeddedDescriptor : embeddedDescriptors) {
            fields.put(embeddedDescriptor.metadata().getField().getField().getName(), embeddedDescriptor.value());
        }

		Constructor<?> constructor = selectConstructorFor(metadata, fields.keySet());
		return IntrospectionUtils.instantiateWith(
				constructor,
				fields
		);
	}

	private static Object instantiateMutableEntity(MetadataBase metadata,
												   List<FieldDescriptor> descriptors,
												   List<EmbeddedFieldDescriptor> embeddedDescriptors) throws Throwable {
		Object target = IntrospectionUtils.instantiate(metadata);

		for (FieldDescriptor descriptor : descriptors) {
			//TODO: Only override if not Parent key?
			MethodHandle writeMethod = descriptor.metadata().getWriteMethod();
			writeMethod.invoke(target, descriptor.value());
		}

		for (EmbeddedFieldDescriptor embeddedDescriptor : embeddedDescriptors) {
			if (embeddedDescriptor.value() != null) {
				//TODO: Should we override values?
				MethodHandle writeMethod = embeddedDescriptor.metadata().getWriteMethod();
				writeMethod.invoke(target, embeddedDescriptor.value());
			}
		}

		return target;
	}

	/**
	 * Unamrshals the identifier.
	 * 
	 * @throws Throwable
	 *             propagated
	 */
	private FieldDescriptor unmarshalIdentifier() throws Throwable {
		IdentifierMetadata identifierMetadata = entityMetadata.getIdentifierMetadata();
		Object id = ((Key) nativeEntity.getKey()).getNameOrId();
		// If the ID is not a simple type...
		IdClassMetadata idClassMetadata = identifierMetadata.getIdClassMetadata();
		if (idClassMetadata != null) {
			id = idClassMetadata.getConstructor().invoke(id);
		}

		return FieldDescriptor.of(entityMetadata.getIdentifierMetadata(), id);
	}

	/**
	 * Unamrshals the entity's key.
	 * 
	 * @throws Throwable
	 *             propagated
	 */
	private FieldDescriptor unmarshalKey() {
		if (entityMetadata.getKeyMetadata() != null) {
			return FieldDescriptor.of(
					entityMetadata.getKeyMetadata(),
					new DefaultDatastoreKey((Key) nativeEntity.getKey())
			);
		}

		return null;
	}

	/**
	 * Unamrshals the parent key.
	 *
	 * @throws Throwable
	 *             propagated
	 */
	private FieldDescriptor unmarshalParentKey() {
		if (entityMetadata.getParentKeyMetadata() != null
				&& nativeEntity.getKey().getParent() != null) {

			return FieldDescriptor.of(
					entityMetadata.getParentKeyMetadata(),
					new DefaultDatastoreKey(nativeEntity.getKey().getParent())
			);
		}

		return null;
	}

	/**
	 * Unmarshal all the properties.
	 * 
	 * @throws Throwable
	 *             propagated
	 */
	private List<FieldDescriptor> unmarshalProperties() {
		return entityMetadata.getPropertyMetadataCollection().stream()
				.map(this::unmarshalProperty)
				.collect(toList());
	}

	/**
	 * Unmarshals the embedded fields of this entity.
	 * 
	 * @throws Throwable
	 *             propagated
	 */
	private List<EmbeddedFieldDescriptor> unmarshalEmbeddedFields() throws Throwable {
		List<EmbeddedFieldDescriptor> descriptors = new ArrayList<>();

		for (EmbeddedMetadata embeddedMetadata : entityMetadata.getEmbeddedMetadataCollection()) {
			if (embeddedMetadata.getStorageStrategy() == StorageStrategy.EXPLODED) {
				descriptors.addAll(unmarshalWithExplodedStrategy(embeddedMetadata));
			} else {
				descriptors.addAll(unmarshalWithImplodedStrategy(embeddedMetadata, nativeEntity));
			}
		}

		return descriptors;
	}

	/**
	 * Unmarshals the embedded field represented by the given embedded metadata.
	 * 
	 * @param embeddedMetadata
	 *            the embedded metadata
	 * @throws Throwable
	 *             propagated
	 */
	private List<EmbeddedFieldDescriptor> unmarshalWithExplodedStrategy(EmbeddedMetadata embeddedMetadata) throws Throwable {
		List<FieldDescriptor> descriptors = new ArrayList<>();
		List<EmbeddedFieldDescriptor> embeddedDescriptors = new ArrayList<>();

		for (PropertyMetadata propertyMetadata : embeddedMetadata.getPropertyMetadataCollection()) {
			descriptors.add(unmarshalProperty(propertyMetadata));
		}
		for (EmbeddedMetadata embeddedMetadata2 : embeddedMetadata.getEmbeddedMetadataCollection()) {
			embeddedDescriptors.addAll(unmarshalWithExplodedStrategy(embeddedMetadata2));
		}

		Object embeddedObject = instantiateEntity(
				embeddedMetadata,
				descriptors,
				embeddedDescriptors
		);

		return singletonList(EmbeddedFieldDescriptor.of(embeddedMetadata, embeddedObject));
	}

	/**
	 * Unmarshals the embedded field represented by the given metadata.
	 * 
	 * @param embeddedMetadata
	 *            the metadata of the field to unmarshal
	 * @param nativeEntity
	 *            the native entity from which the embedded entity is to be
	 *            extracted
	 * @throws Throwable
	 *             propagated
	 */
	private static List<EmbeddedFieldDescriptor> unmarshalWithImplodedStrategy(EmbeddedMetadata embeddedMetadata,
																			   BaseEntity<?> nativeEntity) throws Throwable {
		List<FieldDescriptor> descriptors = new ArrayList<>();
		List<EmbeddedFieldDescriptor> embeddedDescriptors = new ArrayList<>();

		FullEntity<?> nativeEmbeddedEntity = null;
		String propertyName = embeddedMetadata.getMappedName();
		if (nativeEntity.contains(propertyName)) {
			Value<?> nativeValue = nativeEntity.getValue(propertyName);
			if (nativeValue instanceof NullValue) {
				return singletonList(EmbeddedFieldDescriptor.of(embeddedMetadata, null));
			} else {
				nativeEmbeddedEntity = ((EntityValue) nativeValue).get();
			}
		}
		if (nativeEmbeddedEntity == null) {
			return new ArrayList<>();
		}
		for (PropertyMetadata propertyMetadata : embeddedMetadata.getPropertyMetadataCollection()) {
			descriptors.add(unmarshalProperty(propertyMetadata, nativeEmbeddedEntity));
		}
		for (EmbeddedMetadata embeddedMetadata2 : embeddedMetadata.getEmbeddedMetadataCollection()) {
			embeddedDescriptors.addAll(unmarshalWithImplodedStrategy(embeddedMetadata2, nativeEmbeddedEntity));
		}

		Object embeddedObject = instantiateEntity(
				embeddedMetadata,
				descriptors,
				embeddedDescriptors
		);

		return singletonList(EmbeddedFieldDescriptor.of(embeddedMetadata, embeddedObject));
	}

	/**
	 * Unmarshals the property represented by the given property metadata and
	 * updates the target object with the property value.
	 * 
	 * @param propertyMetadata
	 *            the property metadata
	 * @throws Throwable
	 *             propagated
	 */
	private FieldDescriptor unmarshalProperty(PropertyMetadata propertyMetadata) {
		return unmarshalProperty(propertyMetadata, nativeEntity);
	}

	/**
	 * Unmarshals the property with the given metadata and sets the unmarshalled
	 * value on the given <code>target</code> object.
	 * 
	 * @param propertyMetadata
	 *            the metadata of the property
	 * @param nativeEntity
	 *            the native entity containing the source property
	 * @throws Throwable
	 *             propagated
	 */
	private static FieldDescriptor unmarshalProperty(PropertyMetadata propertyMetadata, BaseEntity<?> nativeEntity) {
		// The datastore may not have every property that the entity class has
		// defined. For example, if we are running a projection query or if the
		// entity class added a new field without updating existing data...So
		// make sure there is a property or else, we get an exception from the
		// datastore.
		if (nativeEntity.contains(propertyMetadata.getMappedName())) {
			Value<?> datastoreValue = nativeEntity.getValue(propertyMetadata.getMappedName());
			Object entityValue = propertyMetadata.getMapper().toModel(datastoreValue);

			return FieldDescriptor.of(propertyMetadata, entityValue);
		}

		return null;
	}

}
