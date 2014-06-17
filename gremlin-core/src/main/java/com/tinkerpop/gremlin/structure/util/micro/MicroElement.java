package com.tinkerpop.gremlin.structure.util.micro;

import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.util.ElementHelper;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class MicroElement implements Element, Serializable {

    Object id;
    String label;

    protected MicroElement() {

    }

    protected MicroElement(final Element element) {
        if (null == element)
            throw Graph.Exceptions.argumentCanNotBeNull("element");

        this.id = element.id();
        this.label = element.label();
    }

    public Object id() {
        return this.id;
    }

    public String label() {
        return this.label;
    }

    public Property property(final String key, final Object value) {
        throw new UnsupportedOperationException("Micro elements do not store properties (inflate): " + this.toString());
    }

    public <V> Property<V> property(final String key) {
        throw new UnsupportedOperationException("Micro elements do not store properties (inflate): " + this.toString());
    }

    public Map<String, Property> properties() {
        throw new UnsupportedOperationException("Micro elements do not store properties (inflate): " + this.toString());
    }

    public Map<String, Property> hiddens() {
        throw new UnsupportedOperationException("Micro elements do not store properties (inflate): " + this.toString());
    }

    public void remove() {
        throw new UnsupportedOperationException("Micro elements can not be removed (inflate): " + this.toString());
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals(final Object object) {
        return ElementHelper.areEqual(this, object);
    }
}
