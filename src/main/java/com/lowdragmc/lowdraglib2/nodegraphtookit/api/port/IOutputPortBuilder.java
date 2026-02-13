package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import java.lang.reflect.Type;

/**
 * Interface for defining an output port.
 *
 * <p>Use this interface to create an output port before you assign its data type.
 * To assign a data type, call {@link #withDataType(Type)}.</p>
 */
public interface IOutputPortBuilder<T extends IOutputPortBuilder<T>> extends IPortBuilder<T> {
}
