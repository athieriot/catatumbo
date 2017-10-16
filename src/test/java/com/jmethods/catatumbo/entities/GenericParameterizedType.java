package com.jmethods.catatumbo.entities;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class GenericParameterizedType implements ParameterizedType {

  private Type[] types;

  public GenericParameterizedType(Type[] types) {
    this.types = types;
  }

  @Override
  public Type[] getActualTypeArguments() {
    return types;
  }

  @Override
  public Type getRawType() {
    return GenericEntity.class;
  }

  @Override
  public Type getOwnerType() {
    return null;
  }
}
