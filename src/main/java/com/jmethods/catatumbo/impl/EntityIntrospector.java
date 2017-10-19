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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.jmethods.catatumbo.CreatedTimestamp;
import com.jmethods.catatumbo.DatastoreKey;
import com.jmethods.catatumbo.Embeddable;
import com.jmethods.catatumbo.Embedded;
import com.jmethods.catatumbo.Entity;
import com.jmethods.catatumbo.EntityManagerException;
import com.jmethods.catatumbo.Identifier;
import com.jmethods.catatumbo.Key;
import com.jmethods.catatumbo.MappedSuperClass;
import com.jmethods.catatumbo.OverrideKind;
import com.jmethods.catatumbo.ParentKey;
import com.jmethods.catatumbo.ProjectedEntity;
import com.jmethods.catatumbo.Property;
import com.jmethods.catatumbo.PropertyOverride;
import com.jmethods.catatumbo.PropertyOverrides;
import com.jmethods.catatumbo.UpdatedTimestamp;
import com.jmethods.catatumbo.Version;

/**
 * Introspector for model classes with the annotation of {@link Entity} or {@link ProjectedEntity}.
 * The introspect method gathers metadata about the given entity class. This metadata is needed for
 * performing the object to Datastore mapping and vice versa. The first time an entity class is
 * introspected, the metadata is cached and reused for better performance. The introspector
 * traverses through the entire entity graph to process simple (or primitive) fields as well as an
 * embedded complex objects.
 *
 * @author Sai Pullabhotla
 */
public class EntityIntrospector {

  /**
   * Valid data types for fields that are marked with either {@link CreatedTimestamp} or
   * {@link UpdatedTimestamp}.
   */
  private static final List<String> VALID_TIMESTAMP_TYPES = Arrays.asList(new String[] {
      long.class.getName(), Long.class.getName(), Date.class.getName(), Calendar.class.getName(),
      OffsetDateTime.class.getName(), ZonedDateTime.class.getName() });

  static {
    // Sort the valid types so we can do a binary search.
    Collections.sort(VALID_TIMESTAMP_TYPES);
  }

  /**
   * Cache of introspected classes
   */
  private static final Cache<Type, EntityMetadata> cache = new Cache<>(64);

  /**
   * The type to introspect
   */
  private final Type entityType;


  /**
   * The raw class corresponding to the type
   */
  private final Class<?> rawType;

  /**
   * Output of the introspection, the metadata about the entity
   */
  private EntityMetadata entityMetadata;

  /**
   * Creates a new instance of <code>EntityIntrospector</code>.
   *
   * @param entityType
   *          the entity type to introspect
   */
  private EntityIntrospector(Type entityType) {
    this.entityType = entityType;
    this.rawType = resolveRawType(entityType);
  }

  /**
   * Introspects the given entity and returns the metadata associated with the entity.
   *
   * @param entity
   *          the entity object to introspect.
   * @return the metadata of the entity
   */
  public static EntityMetadata introspect(Object entity, Type entityType) {
    return introspect(entityType != null ? entityType : entity.getClass());
  }

  /**
   * Introspects the given entity class and returns the metadata of the entity.
   *
   * @param entityType
   *            the entity type to introspect
   * @return the metadata of the entity
   */
  public static EntityMetadata introspect(Type entityType) {
    EntityMetadata cachedMetadata = cache.get(entityType);
    if (cachedMetadata != null) {
      return cachedMetadata;
    }
    return loadMetadata(entityType);
  }

  /**
   * Loads the metadata for the given class and puts it in the cache and returns it.
   *
   * @param entityType
   *          the entity type
   * @return the metadata for the given class
   */
  private static EntityMetadata loadMetadata(Type entityType) {
    synchronized (entityType) {
      EntityMetadata metadata = cache.get(entityType);
      if (metadata == null) {
        EntityIntrospector introspector = new EntityIntrospector(entityType);
        introspector.process();
        metadata = introspector.entityMetadata;
        cache.put(entityType, metadata);
      }
      return metadata;
    }
  }

  /**
   * Processes the entity class using reflection and builds the metadata.
   */
  private void process() {
    Entity entity = rawType.getAnnotation(Entity.class);
    ProjectedEntity projectedEntity = rawType.getAnnotation(ProjectedEntity.class);
    if (entity != null) {
      initEntityMetadata(entity);
    } else if (projectedEntity != null) {
      initEntityMetadata(projectedEntity);
    } else {
      String message = String.format("Class %s must either have %s or %s annotation",
              rawType.getName(), Entity.class.getName(), ProjectedEntity.class.getName());
      throw new EntityManagerException(message);
    }

    processGenericParameters();
    processPropertyOverrides();

    processFields();
    // If we did not find valid Identifier...
    if (entityMetadata.getIdentifierMetadata() == null) {
      throw new EntityManagerException(
          String.format("Class %s requires a field with annotation of %s", rawType.getName(),
              Identifier.class.getName()));
    }
    entityMetadata.setEntityListenersMetadata(EntityListenersIntrospector.introspect(rawType));
    entityMetadata.ensureUniqueProperties();
    entityMetadata.cleanup();
  }

  /**
   * Resolve the raw class from a given type
   *
   * @param type
   *          the type to resolve
   * @return the raw class of the underlying type
   */
  private Class<?> resolveRawType(Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) type).getRawType();
    } else {
      String message = String.format("Given type %s not supported."
              + "Only Class and ParameterizedType are valid.", type.getTypeName());
      throw new EntityManagerException(message);
    }
  }

  /**
   * Resolve the field type if generic
   *
   * @param field
   *          the field to resolve
   * @return the actual class of the field
   */
  private Class<?> reifyFieldType(Field field) {
    // Resolve the field type if it's a generic parameterizedType
    Class<?> fieldType = field.getType();
    if (field.getGenericType() instanceof TypeVariable) {
      Class<?> reifiedType = entityMetadata.reify((TypeVariable) field.getGenericType());
      if (reifiedType != null) {
        fieldType = reifiedType;
      }
    }

    return fieldType;
  }


  /**
   * Initializes the metadata using the given {@link Entity} annotation.
   *
   * @param entity
   *          the {@link Entity} annotation.
   */
  private void initEntityMetadata(Entity entity) {
    String kind = entity.kind();
    if (kind.trim().length() == 0) {
      kind = rawType.getSimpleName();
    }
    entityMetadata = new EntityMetadata(rawType, kind);
  }

  /**
   * Initializes the metadata using the given {@link ProjectedEntity} annotation.
   *
   * @param projectedEntity
   *          the {@link ProjectedEntity} annotation.
   */
  private void initEntityMetadata(ProjectedEntity projectedEntity) {
    String kind = projectedEntity.kind();
    if (kind.trim().length() == 0) {
      String message = String.format("Class %s requires a non-blank Kind", rawType.getName());
      throw new EntityManagerException(message);
    }
    entityMetadata = new EntityMetadata(rawType, kind, true);
  }

  /**
   * Resolve generic parameters if a {@link ParameterizedType} was given.
   */
  private void processGenericParameters() {
    if (entityType instanceof ParameterizedType) {
      TypeVariable<? extends Class<?>>[] typeParameters = rawType.getTypeParameters();
      Type[] reified = ((ParameterizedType) entityType).getActualTypeArguments();
      if (typeParameters.length != reified.length) {
        String message = String.format("ParameterizedType %s requires %d generic types",
                entityType.getTypeName(), typeParameters.length);
        throw new EntityManagerException(message);
      }

      for (int i = 0; i < typeParameters.length; i++) {
        entityMetadata.putGenericType(typeParameters[i], reified[i]);
      }
    }
  }

  /**
   * Processes the property overrides for the embedded objects, if any.
   */
  private void processPropertyOverrides() {
    PropertyOverrides propertyOverrides = rawType.getAnnotation(PropertyOverrides.class);
    if (propertyOverrides == null) {
      return;
    }
    PropertyOverride[] propertyOverridesArray = propertyOverrides.value();
    for (PropertyOverride propertyOverride : propertyOverridesArray) {
      entityMetadata.putPropertyOverride(propertyOverride);
    }
  }

  /**
   * Processes the fields defined in this entity and updates the metadata.
   */
  private void processFields() {
    List<Field> fields = getAllFields();
    for (Field field : fields) {
      Class<?> fieldType = reifyFieldType(field);
      if (field.isAnnotationPresent(Identifier.class)) {
        processIdentifierField(field, fieldType);
      } else if (field.isAnnotationPresent(Key.class)) {
        processKeyField(field, fieldType);
      } else if (field.isAnnotationPresent(ParentKey.class)) {
        processParentKeyField(field, fieldType);
      } else if (field.isAnnotationPresent(Embedded.class)) {
        processEmbeddedField(field, fieldType);
        processOverrideKind(field, fieldType);
      } else {
        processField(field, fieldType);
      }
    }
  }

  /**
   * Processes the entity class and any super classes that are MappedSupperClasses and returns the
   * fields.
   *
   * @return all fields of the entity hierarchy.
   */
  private List<Field> getAllFields() {
    List<Field> allFields = new ArrayList<>();
    Class<?> clazz = rawType;
    boolean stop;
    do {
      List<Field> fields = IntrospectionUtils.getPersistableFields(clazz);
      allFields.addAll(fields);
      clazz = clazz.getSuperclass();
      stop = clazz == null || !clazz.isAnnotationPresent(MappedSuperClass.class);
    } while (!stop);
    return allFields;
  }

  /**
   * Processes the identifier field and builds the identifier metadata.
   *
   * @param field
   *          the identifier field
   * @param type
   *          the field Type
   */
  private void processIdentifierField(Field field, Class<?> type) {
    Identifier identifier = field.getAnnotation(Identifier.class);
    boolean autoGenerated = identifier.autoGenerated();
    IdentifierMetadata identifierMetadata = new IdentifierMetadata(field, type, autoGenerated);
    entityMetadata.setIdentifierMetadata(identifierMetadata);
  }

  /**
   * Processes the Key field and builds the entity metadata.
   *
   * @param field
   *          the Key field
   * @param type
   *          the field Type
   */
  private void processKeyField(Field field, Class<?> type) {
    String fieldName = field.getName();
    if (!type.equals(DatastoreKey.class)) {
      String message = String.format("Invalid type, %s, for Key field %s in class %s. ", type,
              fieldName, rawType);
      throw new EntityManagerException(message);
    }
    KeyMetadata keyMetadata = new KeyMetadata(field, type);
    entityMetadata.setKeyMetadata(keyMetadata);
  }

  /**
   * Processes the ParentKey field and builds the entity metadata.
   *
   * @param field
   *          the ParentKey field
   * @param type
   *          the field type
   */
  private void processParentKeyField(Field field, Class<?> type) {
    String fieldName = field.getName();
    if (!type.equals(DatastoreKey.class)) {
      String message = String.format("Invalid type, %s, for ParentKey field %s in class %s. ", type,
              fieldName, rawType);
      throw new EntityManagerException(message);
    }
    ParentKeyMetadata parentKeyMetadata = new ParentKeyMetadata(field, type);
    entityMetadata.setParentKetMetadata(parentKeyMetadata);
  }

  /**
   * Processes the given field and generates the metadata.
   *
   * @param field
   *          the field to process
   * @param type
   *          the field type
   */
  private void processField(Field field, Class<?> type) {
    PropertyMetadata propertyMetadata = IntrospectionUtils.getPropertyMetadata(field, type);
    if (propertyMetadata != null) {
      // If the field is from a super class, there might be some
      // overrides, so process those.
      if (!field.getDeclaringClass().equals(rawType)) {
        applyPropertyOverride(propertyMetadata);
      }
      entityMetadata.putPropertyMetadata(propertyMetadata);
      if (field.isAnnotationPresent(Version.class)) {
        processVersionField(propertyMetadata);
      } else if (field.isAnnotationPresent(CreatedTimestamp.class)) {
        processCreatedTimestampField(propertyMetadata);
      } else if (field.isAnnotationPresent(UpdatedTimestamp.class)) {
        processUpdatedTimestampField(propertyMetadata);
      }
    }
  }

  /**
   * Processes the Version annotation of the field with the given metadata.
   *
   * @param propertyMetadata
   *          the metadata of the field that has the Version annotation.
   */
  private void processVersionField(PropertyMetadata propertyMetadata) {
    Class<?> dataClass = propertyMetadata.getDeclaredType();
    if (!long.class.equals(dataClass)) {
      String messageFormat = "Field %s in class %s must be of type %s";
      throw new EntityManagerException(String.format(messageFormat,
          propertyMetadata.getField().getName(), rawType, long.class));
    }
    entityMetadata.setVersionMetadata(propertyMetadata);
  }

  /**
   * Processes the field that is marked with {@link CreatedTimestamp} annotation.
   *
   * @param propertyMetadata
   *          the metadata of the field that was annotated with {@link CreatedTimestamp}.
   */
  private void processCreatedTimestampField(PropertyMetadata propertyMetadata) {
    validateAutoTimestampField(propertyMetadata);
    entityMetadata.setCreatedTimestampMetadata(propertyMetadata);
  }

  /**
   * Processes the field that is marked with {@link UpdatedTimestamp} annotation.
   *
   * @param propertyMetadata
   *          the metadata of the field that was annotated with {@link UpdatedTimestamp}.
   */
  private void processUpdatedTimestampField(PropertyMetadata propertyMetadata) {
    validateAutoTimestampField(propertyMetadata);
    entityMetadata.setUpdatedTimestampMetadata(propertyMetadata);
  }

  /**
   * Validates the given property metadata to ensure it is valid for an automatic timestamp field.
   *
   * @param propertyMetadata
   *          the metadata to validate
   */
  private void validateAutoTimestampField(PropertyMetadata propertyMetadata) {
    Class<?> dataClass = propertyMetadata.getDeclaredType();
    if (Collections.binarySearch(VALID_TIMESTAMP_TYPES, dataClass.getName()) < 0) {
      String messageFormat = "Field %s in class %s must be one of the following types - %s";
      throw new EntityManagerException(String.format(messageFormat,
          propertyMetadata.getField().getName(), rawType, VALID_TIMESTAMP_TYPES));
    }
  }

  /**
   * Applies any override information for the property with the given metadata.
   *
   * @param propertyMetadata
   *          the metadata of the property
   */
  private void applyPropertyOverride(PropertyMetadata propertyMetadata) {
    String name = propertyMetadata.getName();
    Property override = entityMetadata.getPropertyOverride(name);
    if (override != null) {
      String mappedName = override.name();
      if (mappedName != null && mappedName.trim().length() > 0) {
        propertyMetadata.setMappedName(mappedName);
      }
      propertyMetadata.setIndexed(override.indexed());
      propertyMetadata.setOptional(override.optional());
    }

  }

  /**
   * Processes and gathers the metadata for the given embedded field.
   *
   * @param field
   *          the embedded field
   * @param type
   *          the field type
   */
  private void processEmbeddedField(Field field, Class<?> type) {
    // First, create EmbeddedField so we can maintain the path/depth of the
    // embedded field
    EmbeddedField embeddedField = new EmbeddedField(field, type);
    // Introspect the embedded field.
    EmbeddedMetadata embeddedMetadata = EmbeddedIntrospector.introspect(embeddedField,
        entityMetadata);
    entityMetadata.putEmbeddedMetadata(embeddedMetadata);
  }

  /**
   * Process an OverrideKind annotation on an embedded field and
   * update the current entity metadata if needs be
   *
   * @param field
   *            the embedded field
   * @param type
   *          the field type
   */
  private void processOverrideKind(Field field, Class<?> type) {
    OverrideKind overrideKind = type.getAnnotation(OverrideKind.class);
    Embeddable embeddable = type.getAnnotation(Embeddable.class);

    if (overrideKind != null) {
      if (embeddable == null) {
        String message = String.format("A class annotated with %s must also be annotated with %s",
            OverrideKind.class.getName(),
            Embeddable.class.getName()
        );
        throw new EntityManagerException(message);
      }

      entityMetadata.setKind(overrideKind.kind());
    }
  }

  /**
   * Convenient method for getting the metadata of the field used for optimistic locking.
   *
   * @param entityType
   *          the entity type
   * @return the metadata of the field used for optimistic locking. Returns <code>null</code>, if
   *         the class does not have a field with {@link Version} annotation.
   */
  public static PropertyMetadata getVersionMetadata(Type entityType) {
    return introspect(entityType).getVersionMetadata();
  }

  /**
   * Convenient method for getting the metadata of the field used for optimistic locking.
   *
   * @param entity
   *          the entity
   * @return the metadata of the field used for optimistic locking. Returns <code>null</code>, if
   *         the class does not have a field with {@link Version} annotation.
   */
  public static PropertyMetadata getVersionMetadata(Object entity, Type entityType) {
    return introspect(entity, entityType).getVersionMetadata();
  }

  /**
   * Returns the Identifier Metadata for the given entity.
   *
   * @param entity
   *          the entity
   * @return the Identifier Metadata
   */
  public static IdentifierMetadata getIdentifierMetadata(Object entity, Type entityType) {
    return introspect(entity, entityType).getIdentifierMetadata();
  }

  /**
   * Returns the Identifier Metadata for the given entity class.
   *
   * @param entityType
   *          the entity type
   * @return the Identifier Metadata
   */
  public static IdentifierMetadata getIdentifierMetadata(Type entityType) {
    return introspect(entityType).getIdentifierMetadata();

  }

  /**
   * Returns the metadata of entity listeners associated with the given entity.
   *
   * @param entity
   *          the entity
   * @return the metadata of EntityListeners associated with the given entity.
   */
  public static EntityListenersMetadata getEntityListenersMetadata(Object entity, Type entityType) {
    return introspect(entity, entityType).getEntityListenersMetadata();
  }

  /**
   * Returns the metadata of entity listeners associated with the given entity class.
   *
   * @param entityType
   *          the entity type
   * @return the metadata of entity listeners associated with the given entity class.
   */
  public static EntityListenersMetadata getEntityListenersMetadata(Type entityType) {
    return introspect(entityType).getEntityListenersMetadata();
  }

}
