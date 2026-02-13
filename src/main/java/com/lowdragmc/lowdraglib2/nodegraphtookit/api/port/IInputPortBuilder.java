package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import java.lang.reflect.Type;

/**
 * Interface for defining an input port.
 *
 * <p>Use this interface to create an input port before you assign its data type.
 * To assign a data type, call {@link #withDataType(Type)}.</p>
 */
public interface IInputPortBuilder<T extends IInputPortBuilder<T>> extends IPortBuilder<T> {

}
