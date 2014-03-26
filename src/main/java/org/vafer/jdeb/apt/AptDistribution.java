/*
 * Copyright 2014 The jdeb developers.
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
package org.vafer.jdeb.apt;

import java.util.HashSet;
import java.util.Set;

/**
 * An APT distribution description
 * 
 * @author Jens Reimann
 * 
 */
public class AptDistribution {

    private String name = "devel";

    private String label = "Development";

    private String origin = "Unknown";

    private String description;

    private Set<AptComponent> components = new HashSet<AptComponent>();

    public AptDistribution() {
    }

    public AptDistribution( AptDistribution other ) {
        this.name = other.name;
        this.label = other.label;
        this.origin = other.origin;
        this.description = other.description;
        for (AptComponent comp : other.components) {
            addComponent(comp);
        }
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setOrigin( String origin ) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    public void setLabel( String label ) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setName( String name ) {
        AptNames.validate("name", name);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Set<AptComponent> getComponents() {
        return this.components;
    }

    /**
     * Add a new component to the distribution. <br/>
     * The component is copied and cannot be altered after adding
     * 
     * @param component
     *            the component to add
     */
    public void addComponent( AptComponent component ) {
        AptComponent newComp = new AptComponent(component);
        newComp.setDistribution(this);
        this.components.add(newComp);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AptDistribution other = (AptDistribution)obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
